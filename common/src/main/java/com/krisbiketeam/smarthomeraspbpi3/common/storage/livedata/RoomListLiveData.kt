package com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata

import androidx.lifecycle.LiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Room
import timber.log.Timber

@Deprecated("please use GenericListReferenceFlow")
class RoomListLiveData(private val databaseReference: DatabaseReference?) : LiveData<List<Room>>() {

    private val roomsListener: ValueEventListener by lazy {
        object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // A new value has been added, add it to the displayed list
                val key = dataSnapshot.key
                val rooms: ArrayList<Room> = ArrayList()
                for (r: DataSnapshot in dataSnapshot.children) {
                    val room = r.getValue(Room::class.java)
                    room?.let {
                        rooms.add(room)
                    }
                }
                Timber.d("onDataChange (key=$key)(rooms=$rooms)")
                value = rooms
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Timber.e("onCancelled: $databaseError")
            }
        }
    }

    override fun onActive() {
        Timber.d("onActive")
        databaseReference?.addValueEventListener(roomsListener)
    }

    override fun onInactive() {
        Timber.d("onInactive")
        databaseReference?.removeEventListener(roomsListener)
    }
}
