package com.krisbiketeam.data.storage

import android.arch.lifecycle.LiveData
import com.google.firebase.database.*
import com.krisbiketeam.data.storage.dto.*
import timber.log.Timber
import com.google.firebase.database.GenericTypeIndicator
import com.krisbiketeam.data.storage.FirebaseTables.*


class StorageUnitsLiveData(private val databaseReference: DatabaseReference) : LiveData<Pair<ChildEventType,StorageUnit<out Any>>>() {

    private val unitsList: List<MyChildEventListener> = HOME_STORAGE_UNITS.map { MyChildEventListener(it)}

    private val typeIndicatorMap: HashMap<String, GenericTypeIndicator<out StorageUnit<out Any>>> = hashMapOf(
            HOME_LIGHTS to object : GenericTypeIndicator<StorageUnit<LightType>>() {},
            HOME_LIGHT_SWITCHES to object : GenericTypeIndicator<StorageUnit<LightSwitchType>>() {},
            HOME_REED_SWITCHES to object : GenericTypeIndicator<StorageUnit<ReedSwitchType>>() {},
            HOME_MOTIONS to object : GenericTypeIndicator<StorageUnit<MotionType>>() {},
            HOME_TEMPERATURES to object : GenericTypeIndicator<StorageUnit<TemperatureType>>() {},
            HOME_PRESSURES to object : GenericTypeIndicator<StorageUnit<PressureType>>() {},
            HOME_BLINDS to object : GenericTypeIndicator<StorageUnit<BlindType>>() {}
    )

    inner class MyChildEventListener(val childNode: String) : ChildEventListener {

        override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
            // A new value has been added, add it to the displayed list
            val key = dataSnapshot.key
            typeIndicatorMap[childNode]?.run {
                val unit = dataSnapshot.getValue(this)
                Timber.d("onChildAdded (key=$key)(unit=$unit)")
                unit?.let {
                    value = ChildEventType.NODE_ACTION_ADDED to unit
                }
            }
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
            // A value has changed, use the key to determine if we are displaying this
            // value and if so displayed the changed value.
            val key = dataSnapshot.key
            typeIndicatorMap[childNode]?.run {
                val unit = dataSnapshot.getValue(this)
                Timber.d("onChildChanged (key=$key)(unit=$unit)")
                unit?.let {
                    value = ChildEventType.NODE_ACTION_CHANGED to unit
                }
            }
        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {
            Timber.d("onChildRemoved:" + dataSnapshot.key)

            // A value has changed, use the key to determine if we are displaying this
            // value and if so remove it.
            val key = dataSnapshot.key
            typeIndicatorMap[childNode]?.run {
                val unit = dataSnapshot.getValue(this)
                Timber.d("onChildRemoved (key=$key)(unit=$unit)")
                unit?.let {
                    value = ChildEventType.NODE_ACTION_DELETED to unit
                }
            }
        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {

            // A value has changed position, use the key to determine if we are
            // displaying this value and if so move it.
            val key = dataSnapshot.key
            typeIndicatorMap[childNode]?.run {
                val unit = dataSnapshot.getValue(this)
                //TODO does it also cover onChildChanged ??? or are those events both called???
                Timber.d("onChildMoved (key=$key)(unit=$unit)")
            }
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
