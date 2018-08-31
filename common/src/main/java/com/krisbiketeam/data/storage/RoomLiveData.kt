package com.krisbiketeam.data.storage

import android.arch.lifecycle.LiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.krisbiketeam.data.storage.firebaseTables.HOME_ROOMS
import com.krisbiketeam.data.storage.dto.Room
import timber.log.Timber


class RoomLiveData(private val databaseReference: DatabaseReference, private val roomName: String) : LiveData<Room>() {

    private val roomListener: ValueEventListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            // A new value has been added, add it to the displayed list
            val key = dataSnapshot.key
            val room = dataSnapshot.getValue(Room::class.java)
            Timber.d("onDataChange (key=$key)(room=$room)")
            room?.let {
                value = room
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Timber.e("onCancelled: $databaseError")
        }
    }

    override fun onActive() {
        Timber.d("onActive")
        databaseReference.child(HOME_ROOMS).child(roomName).addValueEventListener(roomListener)
    }

    override fun onInactive() {
        Timber.d("onInactive")
        databaseReference.child(HOME_ROOMS).child(roomName).removeEventListener(roomListener)
    }
}
