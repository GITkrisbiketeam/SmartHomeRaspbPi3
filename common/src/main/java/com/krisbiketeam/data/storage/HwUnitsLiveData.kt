package com.krisbiketeam.data.storage

import android.arch.lifecycle.LiveData
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.krisbiketeam.data.storage.FirebaseTables.HOME_HW_UNITS
import com.krisbiketeam.data.storage.dto.HomeUnit
import timber.log.Timber


class HwUnitsLiveData(private val databaseReference: DatabaseReference) : LiveData<Pair<ChildEventType,HomeUnit>>() {

    private val eventListener: MyChildEventListener = MyChildEventListener()

    inner class MyChildEventListener : ChildEventListener {

        override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
            // A new value has been added, add it to the displayed list
            val key = dataSnapshot.key
            val room = dataSnapshot.getValue(HomeUnit::class.java)
            Timber.d("onChildAdded (key=$key)(room=$room)")
            room?.let {
                value = ChildEventType.NODE_ACTION_ADDED to room
            }
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
            // A value has changed, use the key to determine if we are displaying this
            // value and if so displayed the changed value.
            val key = dataSnapshot.key
            val room = dataSnapshot.getValue(HomeUnit::class.java)
            Timber.d("onChildChanged (key=$key)(room=$room)")
            room?.let {
                value = ChildEventType.NODE_ACTION_CHANGED to room
            }
        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {
            // A value has changed, use the key to determine if we are displaying this
            // value and if so remove it.
            val key = dataSnapshot.key
            val room = dataSnapshot.getValue(HomeUnit::class.java)
            Timber.d("onChildRemoved (key=$key)(room=$room)")
            room?.let {
                value = ChildEventType.NODE_ACTION_DELETED to room
            }
        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {

            // A value has changed position, use the key to determine if we are
            // displaying this value and if so move it.
            val key = dataSnapshot.key
            val room = dataSnapshot.getValue(HomeUnit::class.java)
            Timber.d("onChildMoved (key=$key)(room=$room)")
            //TODO does it also cover onChildChanged ??? or are those events both called???
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Timber.e("onCancelled: $databaseError")
        }
    }

    override fun onActive() {
        databaseReference.child(HOME_HW_UNITS).addChildEventListener(eventListener)
    }

    override fun onInactive() {
        databaseReference.child(HOME_HW_UNITS).removeEventListener(eventListener)
    }
}
