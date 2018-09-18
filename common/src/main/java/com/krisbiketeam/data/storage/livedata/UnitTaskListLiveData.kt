package com.krisbiketeam.data.storage.livedata

import android.arch.lifecycle.LiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.krisbiketeam.data.storage.dto.UnitTask
import timber.log.Timber


class UnitTaskListLiveData(private val databaseReference: DatabaseReference, private val type: String, private val name: String) : LiveData<List<UnitTask>>() {

    private val roomsListener: ValueEventListener = object: ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            // A new value has been added, add it to the displayed list
            val key = dataSnapshot.key
            val unitTasks: ArrayList<UnitTask> = ArrayList()
            for(r: DataSnapshot in dataSnapshot.children){
                val unitTask = r.getValue(UnitTask::class.java)
                Timber.d("onDataChange (key=$key)(unitTask=$unitTask)")
                unitTask?.let {
                    unitTasks.add(unitTask)
                }
            }
            value = unitTasks
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Timber.e("onCancelled: $databaseError")
        }
    }

    override fun onActive() {
        Timber.d("onActive")
        databaseReference.child(type).child(name).child("unitsTasks").addValueEventListener(roomsListener)
    }

    override fun onInactive() {
        Timber.d("onInactive")
        databaseReference.child(type).child(name).child("unitsTasks").removeEventListener(roomsListener)
    }
}
