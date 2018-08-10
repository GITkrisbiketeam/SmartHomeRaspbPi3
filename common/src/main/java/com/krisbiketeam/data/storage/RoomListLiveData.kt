package com.krisbiketeam.data.storage

import android.arch.lifecycle.LiveData
import com.google.firebase.database.*
import com.krisbiketeam.data.storage.dto.*
import timber.log.Timber
import com.google.firebase.database.GenericTypeIndicator
import com.krisbiketeam.data.storage.FirebaseTables.*


class RoomListLiveData(private val databaseReference: DatabaseReference) : LiveData<List<Room>>() {

    private val roomsListener: ValueEventListener = object: ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            // A new value has been added, add it to the displayed list
            val key = dataSnapshot.key
            val rooms: ArrayList<Room> = ArrayList()
            for(room: DataSnapshot in dataSnapshot.children){
                val room = dataSnapshot.getValue(Room::class.java)
                Timber.d("onDataChange (key=$key)(room=$room)")
                room?.let {
                    rooms.add(room)
                }
            }
            value = rooms
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Timber.e("onCancelled: $databaseError")
        }
    }

    override fun onActive() {
        databaseReference.child(HOME_ROOMS).addValueEventListener(roomsListener)
    }

    override fun onInactive() {
        databaseReference.child(HOME_ROOMS).removeEventListener(roomsListener)
    }
}
