package com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata

import androidx.lifecycle.LiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.UnitTask
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_UNIT_TASKS
import timber.log.Timber


class UnitTaskListLiveData(private val databaseReference: DatabaseReference?, private val type: String, private val name: String) : LiveData<Map<String, UnitTask>>() {

    private val roomsListener: ValueEventListener = object: ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            // A new value has been added, add it to the displayed list
            val key = dataSnapshot.key
            val unitTasks: MutableMap<String, UnitTask> = HashMap()
            for(r: DataSnapshot in dataSnapshot.children){
                val unitTask = r.getValue(UnitTask::class.java)
                Timber.d("onDataChange (key=$key)(unitTask=$unitTask)")
                unitTask?.let {
                    unitTasks[unitTask.name] = unitTask
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
        databaseReference?.child(type)?.child(name)?.child(HOME_UNIT_TASKS)?.addValueEventListener(roomsListener)
    }

    override fun onInactive() {
        Timber.d("onInactive")
        databaseReference?.child(type)?.child(name)?.child(HOME_UNIT_TASKS)?.removeEventListener(roomsListener)
    }
}
