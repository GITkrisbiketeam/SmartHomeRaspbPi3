package com.krisbiketeam.data.storage

import android.arch.lifecycle.LiveData
import com.google.firebase.database.*
import com.krisbiketeam.data.storage.dto.*
import timber.log.Timber
import com.google.firebase.database.GenericTypeIndicator


class UnitsLiveData(private val databaseReference: DatabaseReference) : LiveData<Any>() {

    private val unitsList: List<MyChildEventListener> = listOf(
            MyChildEventListener(StorageUnit::class.java, FirebaseTables.HOME_LIGHTS),
            MyChildEventListener(StorageUnit::class.java, FirebaseTables.HOME_LIGHT_SWITCHES),
            MyChildEventListener(StorageUnit::class.java, FirebaseTables.HOME_REED_SWITCHES),
            MyChildEventListener(StorageUnit::class.java, FirebaseTables.HOME_MOTIONS),
            MyChildEventListener(StorageUnit::class.java, FirebaseTables.HOME_TEMPERATURES),
            MyChildEventListener(StorageUnit::class.java, FirebaseTables.HOME_PRESSURES),
            MyChildEventListener(StorageUnit::class.java, FirebaseTables.HOME_BLINDS),
            MyChildEventListener(Room::class.java, FirebaseTables.HOME_ROOMS)
    )

    private val typeIndicatorMap: HashMap<String, GenericTypeIndicator<out StorageUnit<out Any>>> = hashMapOf(
            FirebaseTables.HOME_LIGHTS to object : GenericTypeIndicator<StorageUnit<LightType>>() {},
            FirebaseTables.HOME_LIGHT_SWITCHES to object : GenericTypeIndicator<StorageUnit<LightSwitchType>>() {},
            FirebaseTables.HOME_REED_SWITCHES to object : GenericTypeIndicator<StorageUnit<ReedSwitchType>>() {},
            FirebaseTables.HOME_MOTIONS to object : GenericTypeIndicator<StorageUnit<MotionType>>() {},
            FirebaseTables.HOME_TEMPERATURES to object : GenericTypeIndicator<StorageUnit<TemperatureType>>() {},
            FirebaseTables.HOME_PRESSURES to object : GenericTypeIndicator<StorageUnit<PressureType>>() {},
            FirebaseTables.HOME_BLINDS to object : GenericTypeIndicator<StorageUnit<BlindType>>() {}
    )

    inner class MyChildEventListener(private val liveClass: Class<*>, val childNode: String) : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
            // A new value has been added, add it to the displayed list
            val key = dataSnapshot.key
            Timber.d("onChildAdded childNode=$childNode")
            if (liveClass == StorageUnit::class.java) {
                typeIndicatorMap[childNode]?.run {
                    value = dataSnapshot.getValue(this)
                }
            } else {
                value = dataSnapshot.getValue(liveClass)
            }
            Timber.d("onChildAdded (key=$key)(value=$value)")
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
            // A value has changed, use the key to determine if we are displaying this
            // value and if so displayed the changed value.
            val key = dataSnapshot.key
            if (liveClass == StorageUnit::class.java) {
                typeIndicatorMap[childNode]?.run {
                    value = dataSnapshot.getValue(this)
                }
            } else {
                value = dataSnapshot.getValue(liveClass)
            }
            Timber.d("onChildChanged (key=$key)(value=$value)")
        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {
            Timber.d("onChildRemoved:" + dataSnapshot.key)

            // A value has changed, use the key to determine if we are displaying this
            // value and if so remove it.
            val key = dataSnapshot.key
            Timber.d("onChildRemoved key: $key")
            // ...
        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {

            // A value has changed position, use the key to determine if we are
            // displaying this value and if so move it.
            val key = dataSnapshot.key
            if (liveClass == StorageUnit::class.java) {
                typeIndicatorMap[childNode]?.run {
                    value = dataSnapshot.getValue(this)
                }
            } else {
                value = dataSnapshot.getValue(liveClass)
            }
            Timber.d("onChildMoved key: $key value: $value")
            // ...
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
