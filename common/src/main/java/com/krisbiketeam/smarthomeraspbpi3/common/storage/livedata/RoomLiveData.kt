package com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata

import androidx.lifecycle.LiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Room
import timber.log.Timber


class RoomLiveData(ref: DatabaseReference?, roomName: String) : LiveData<Room>() {

    private val databaseReference: DatabaseReference? by lazy {
        ref?.child(roomName)
    }

    private val roomListener: ValueEventListener by lazy {
        object : ValueEventListener {
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
    }

    override fun onActive() {
        Timber.d("onActive")
        databaseReference?.addValueEventListener(roomListener)
    }

    override fun onInactive() {
        Timber.d("onInactive")
        databaseReference?.removeEventListener(roomListener)
    }
}
