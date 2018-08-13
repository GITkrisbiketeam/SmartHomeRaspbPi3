package com.krisbiketeam.data.storage

import android.arch.lifecycle.LiveData
import com.google.firebase.database.*
import com.krisbiketeam.data.storage.dto.*
import timber.log.Timber
import com.google.firebase.database.GenericTypeIndicator
import com.krisbiketeam.data.storage.FirebaseTables.*


class StorageUnitsListLiveData(private val databaseReference: DatabaseReference, private val roomName: String) : LiveData<List<StorageUnit<out Any>>>() {

    private val unitsList: List<MyChildEventListener> = HOME_STORAGE_UNITS.map { MyChildEventListener(it)}

    private val typeIndicatorMap: HashMap<String, GenericTypeIndicator<out List<StorageUnit<out Any>>>> = hashMapOf(
            HOME_LIGHTS to object : GenericTypeIndicator<List<StorageUnit<LightType>>>() {},
            HOME_LIGHT_SWITCHES to object : GenericTypeIndicator<List<StorageUnit<LightSwitchType>>>() {},
            HOME_REED_SWITCHES to object : GenericTypeIndicator<List<StorageUnit<ReedSwitchType>>>() {},
            HOME_MOTIONS to object : GenericTypeIndicator<List<StorageUnit<MotionType>>>() {},
            HOME_TEMPERATURES to object : GenericTypeIndicator<List<StorageUnit<TemperatureType>>>() {},
            HOME_PRESSURES to object : GenericTypeIndicator<List<StorageUnit<PressureType>>>() {},
            HOME_BLINDS to object : GenericTypeIndicator<List<StorageUnit<BlindType>>>() {}
    )

    inner class MyChildEventListener(val childNode: String) : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            // A value has changed, use the key to determine if we are displaying this
            // value and if so displayed the changed value.
            val key = dataSnapshot.key
            // A new value has been added, add it to the displayed list
            /*val rooms: ArrayList<Room> = ArrayList()
            for(room: DataSnapshot in dataSnapshot.children){
                val room = room.getValue(Room::class.java)
                Timber.d("onDataChange (key=$key)(room=$room)")
                room?.let {
                    rooms.add(room)
                }
            }
            value = rooms*/
            typeIndicatorMap[childNode]?.run {
                val storageUnits = dataSnapshot.getValue(this)
                Timber.d("onChildChanged (key=$key)(storageUnits=$storageUnits)")
                val filteredUnits = storageUnits?.filter { it.name == roomName }
                Timber.d("onChildChanged (key=$key)(filteredUnits=$filteredUnits)")
                val oldUnits: MutableList<StorageUnit<out Any>> = ArrayList()
                oldUnits.addAll(value!!)
                oldUnits.addAll(filteredUnits!!)

                oldUnits?.let {
                    value = oldUnits
                }
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Timber.e("onCancelled:", databaseError)

        }
    }

    override fun onActive() {
        unitsList.forEach { databaseReference.child(it.childNode).addValueEventListener(it) }
    }

    override fun onInactive() {
        unitsList.forEach { databaseReference.child(it.childNode).removeEventListener(it) }
    }
}
