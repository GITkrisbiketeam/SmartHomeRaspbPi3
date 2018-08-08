package com.krisbiketeam.data.storage

import android.arch.lifecycle.LiveData
import com.google.firebase.database.*
import com.krisbiketeam.data.storage.dto.*
import timber.log.Timber
import com.google.firebase.database.GenericTypeIndicator
import com.krisbiketeam.data.storage.FirebaseTables.*


class StorageUnitsLiveData(private val databaseReference: DatabaseReference) : LiveData<Pair<Int,*>>() {

    companion object {
        const val NODE_ACTION_ADDED = 1
        const val NODE_ACTION_CHANGED = 2
        const val NODE_ACTION_DELETED = 3
    }
    private val unitsList: List<MyChildEventListener> = listOf(
            MyChildEventListener(StorageUnit::class.java, HOME_LIGHTS),
            MyChildEventListener(StorageUnit::class.java, HOME_LIGHT_SWITCHES),
            MyChildEventListener(StorageUnit::class.java, HOME_REED_SWITCHES),
            MyChildEventListener(StorageUnit::class.java, HOME_MOTIONS),
            MyChildEventListener(StorageUnit::class.java, HOME_TEMPERATURES),
            MyChildEventListener(StorageUnit::class.java, HOME_PRESSURES),
            MyChildEventListener(StorageUnit::class.java, HOME_BLINDS),
            MyChildEventListener(Room::class.java, HOME_ROOMS),
            MyChildEventListener(HomeUnit::class.java, HOME_HW_UNITS)
    )

    private val typeIndicatorMap: HashMap<String, GenericTypeIndicator<out StorageUnit<out Any>>> = hashMapOf(
            HOME_LIGHTS to object : GenericTypeIndicator<StorageUnit<LightType>>() {},
            HOME_LIGHT_SWITCHES to object : GenericTypeIndicator<StorageUnit<LightSwitchType>>() {},
            HOME_REED_SWITCHES to object : GenericTypeIndicator<StorageUnit<ReedSwitchType>>() {},
            HOME_MOTIONS to object : GenericTypeIndicator<StorageUnit<MotionType>>() {},
            HOME_TEMPERATURES to object : GenericTypeIndicator<StorageUnit<TemperatureType>>() {},
            HOME_PRESSURES to object : GenericTypeIndicator<StorageUnit<PressureType>>() {},
            HOME_BLINDS to object : GenericTypeIndicator<StorageUnit<BlindType>>() {}
    )

    inner class MyChildEventListener(private val liveClass: Class<*>, val childNode: String) : ChildEventListener {

        override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
            // A new value has been added, add it to the displayed list
            val key = dataSnapshot.key
            Timber.d("onChildAdded childNode=$childNode")
            //TODO will this work?? (isInstance)
            if (liveClass.isInstance(StorageUnit::class.java)) {
                typeIndicatorMap[childNode]?.run {
                    value = NODE_ACTION_ADDED to dataSnapshot.getValue(this)
                }
            } else {
                value = NODE_ACTION_ADDED to dataSnapshot.getValue(liveClass)
            }
            Timber.d("onChildAdded (key=$key)(value=$value)")
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
            // A value has changed, use the key to determine if we are displaying this
            // value and if so displayed the changed value.
            val key = dataSnapshot.key
            if (liveClass == StorageUnit::class.java) {
                typeIndicatorMap[childNode]?.run {
                    value = NODE_ACTION_CHANGED to dataSnapshot.getValue(this)
                }
            } else {
                value = NODE_ACTION_CHANGED to dataSnapshot.getValue(liveClass)
            }
            Timber.d("onChildChanged (key=$key)(value=$value)")
        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {
            Timber.d("onChildRemoved:" + dataSnapshot.key)

            // A value has changed, use the key to determine if we are displaying this
            // value and if so remove it.
            val key = dataSnapshot.key
            if (liveClass == StorageUnit::class.java) {
                typeIndicatorMap[childNode]?.run {
                    value = NODE_ACTION_DELETED to dataSnapshot.getValue(this)
                }
            } else {
                value = NODE_ACTION_DELETED to dataSnapshot.getValue(liveClass)
            }
            Timber.d("onChildRemoved key: $key")
            // ...
        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {

            // A value has changed position, use the key to determine if we are
            // displaying this value and if so move it.
            val key = dataSnapshot.key
            var newVal: Any? = null
            if (liveClass == StorageUnit::class.java) {
                typeIndicatorMap[childNode]?.run {
                    newVal = dataSnapshot.getValue(this)

                }
            } else {
                newVal = dataSnapshot.getValue(liveClass)
            }
            //TODO does it also cover onChildChanged ??? or are those events both called???
            Timber.d("onChildMoved key: $key newVal: $newVal")
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Timber.e("onCancelled:", databaseError)

        }
    }

    override fun onActive() {
        unitsList.forEach { databaseReference.child(it.childNode).addChildEventListener(it) }
    }

    override fun onInactive() {
        unitsList.forEach { databaseReference.child(it.childNode).removeEventListener(it) }
    }
}
