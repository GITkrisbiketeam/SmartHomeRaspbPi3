package com.krisbiketeam.data.storage

import android.arch.lifecycle.LiveData
import com.google.firebase.database.*
import com.krisbiketeam.data.storage.dto.*
import timber.log.Timber

class UnitsLiveData(private val databaseReference: DatabaseReference) : LiveData<Any>() {

    private val unitsList: List<MyChildEventListener> = listOf(
            MyChildEventListener(Blind::class.java, FirebaseTables.HOME_BLINDS),
            MyChildEventListener(Light::class.java, FirebaseTables.HOME_LIGHTS),
            MyChildEventListener(Motion::class.java, FirebaseTables.HOME_MOTIONS),
            MyChildEventListener(Pressure::class.java, FirebaseTables.HOME_PRESSURES),
            MyChildEventListener(LightSwitch::class.java, FirebaseTables.HOME_LIGHT_SWITCHES),
            MyChildEventListener(ReedSwitch::class.java, FirebaseTables.HOME_REED_SWITCHES),
            MyChildEventListener(Room::class.java, FirebaseTables.HOME_ROOMS),
            MyChildEventListener(Temperature::class.java, FirebaseTables.HOME_TEMPERATURES))

    inner class MyChildEventListener(private val liveClass: Class<*>, val childNode: String) : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
            // A new value has been added, add it to the displayed list
            val newValue = dataSnapshot.getValue(liveClass)
            val key = dataSnapshot.key

            Timber.d("onChildAdded (key=$key) (newValue=$newValue) (value=$value)")
            value = newValue
        }

        override  fun onChildChanged(dataSnapshot: DataSnapshot , previousChildName: String?) {
            // A value has changed, use the key to determine if we are displaying this
            // value and if so displayed the changed value.
            val newValue = dataSnapshot.getValue(liveClass)
            val key = dataSnapshot.key
            Timber.d("onChildChanged (key=$key) (newValue=$newValue) (value=$value)")
            value = newValue
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
