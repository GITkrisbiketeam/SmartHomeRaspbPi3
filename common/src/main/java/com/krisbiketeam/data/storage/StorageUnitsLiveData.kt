package com.krisbiketeam.data.storage

import android.arch.lifecycle.LiveData
import com.google.firebase.database.*
import com.krisbiketeam.data.storage.dto.*
import timber.log.Timber
import com.google.firebase.database.GenericTypeIndicator
import com.krisbiketeam.data.storage.firebaseTables.*

//TODO: We should somehow only register for units from given roomName if present
class StorageUnitsLiveData(private val databaseReference: DatabaseReference, private val roomName: String? = null) :
        LiveData<Pair<ChildEventType, StorageUnit<Any>>>() {

    private val unitsList: List<MyChildEventListener> = HOME_STORAGE_UNITS.map { MyChildEventListener(it) }

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
                    Timber.d("onChildAdded (roomName=$roomName)(unit.room=${it.room})")
                    if (roomName == null || roomName == it.room) {
                        // We need to create new SecureStorage unit as the one returned from GenericTypeIndicator is covariant
                        //value = ChildEventType.NODE_ACTION_ADDED to StorageUnit(it.name, it.firebaseTableName, it.room, it.hardwareUnitName, it.value, it.unitsTasks)//StorageUnit<Any>(it)
                        value = ChildEventType.NODE_ACTION_ADDED to it.makeInvariant()
                    }
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
                    Timber.d("onChildChanged (roomName=$roomName)(unit.room=${it.room})")
                    if (roomName == null || roomName == it.room) {
                        //value = ChildEventType.NODE_ACTION_CHANGED to StorageUnit(it.name, it.firebaseTableName, it.room, it.hardwareUnitName, it.value, it.unitsTasks)
                        value = ChildEventType.NODE_ACTION_CHANGED to it.makeInvariant()
                    }
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
                    Timber.d("onChildRemoved (roomName=$roomName)(unit.room=${it.room})")
                    if (roomName == null || roomName == it.room) {
                        //value = ChildEventType.NODE_ACTION_DELETED to StorageUnit(it.name, it.firebaseTableName, it.room, it.hardwareUnitName, it.value, it.unitsTasks)
                        value = ChildEventType.NODE_ACTION_DELETED to it.makeInvariant()
                    }
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
        Timber.d("onActive")
        unitsList.forEach { databaseReference.child(it.childNode).addChildEventListener(it) }
    }

    override fun onInactive() {
        Timber.d("onInactive")
        unitsList.forEach { databaseReference.child(it.childNode).removeEventListener(it) }
    }
}
