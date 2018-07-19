package com.krisbiketeam.data.storage

import android.arch.lifecycle.LiveData
import com.google.firebase.database.*
import com.krisbiketeam.data.storage.dto.*
import timber.log.Timber
import com.google.firebase.database.GenericTypeIndicator



class UnitsLiveData(private val databaseReference: DatabaseReference) : LiveData<Any>() {

    private val unitsList: List<MyChildEventListener> = listOf(
            //MyChildEventListener(Blind::class.java, FirebaseTables.HOME_BLINDS),
            MyChildEventListener(StorageUnit::class.java, FirebaseTables.HOME_LIGHTS),
            //MyChildEventListener(Motion::class.java, FirebaseTables.HOME_MOTIONS),
            //MyChildEventListener(Pressure::class.java, FirebaseTables.HOME_PRESSURES),
            MyChildEventListener(StorageUnit::class.java, FirebaseTables.HOME_LIGHT_SWITCHES)
            //MyChildEventListener(ReedSwitch::class.java, FirebaseTables.HOME_REED_SWITCHES),
            //MyChildEventListener(Room::class.java, FirebaseTables.HOME_ROOMS),
            //MyChildEventListener(Temperature::class.java, FirebaseTables.HOME_TEMPERATURES)
            )

    inner class MyChildEventListener(private val liveClass: Class<*>, val childNode: String) : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
            // A new value has been added, add it to the displayed list
            val key = dataSnapshot.key

            if (liveClass == StorageUnit::class.java) {
                val genericTypeIndicator = object : GenericTypeIndicator<StorageUnit<Boolean>>() {}
                value = dataSnapshot.getValue(genericTypeIndicator)
            } else {
                value = dataSnapshot.getValue(liveClass)
            }
            Timber.d("onChildAdded (key=$key)(value=$value)")
        }

        override  fun onChildChanged(dataSnapshot: DataSnapshot , previousChildName: String?) {
            // A value has changed, use the key to determine if we are displaying this
            // value and if so displayed the changed value.
            val key = dataSnapshot.key
            if (liveClass == StorageUnit::class.java) {
                val genericTypeIndicator = object : GenericTypeIndicator<StorageUnit<Boolean>>() {}
                value = dataSnapshot.getValue(genericTypeIndicator)
            } else {
                value = dataSnapshot.getValue(liveClass)
            }
            Timber.d("onChildChanged (key=$key)(value=$value)")
        }

        override  fun onChildRemoved(dataSnapshot: DataSnapshot) {
            Timber.d("onChildRemoved:" + dataSnapshot.key)

            // A value has changed, use the key to determine if we are displaying this
            // value and if so remove it.
            val key = dataSnapshot.key

            // ...
        }

        override  fun onChildMoved(dataSnapshot: DataSnapshot , previousChildName: String?) {
            Timber.d("onChildMoved:" + dataSnapshot.key)

            // A value has changed position, use the key to determine if we are
            // displaying this value and if so move it.
            val newValue = dataSnapshot.getValue(liveClass)
            val key = dataSnapshot.key

            // ...
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Timber.e("onCancelled:", databaseError)

        }
    }

    override fun onActive() {
        unitsList.forEach { databaseReference.child(it.childNode).addChildEventListener(it)}
    }

    override fun onInactive() {
        unitsList.forEach { databaseReference.child(it.childNode).removeEventListener(it)}
    }
}
