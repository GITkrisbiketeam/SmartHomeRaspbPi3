package com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata

import androidx.lifecycle.LiveData
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.UnitTask
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_UNITS_BASE
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_UNIT_TASKS
import timber.log.Timber

class UnitTaskListLiveData(private val homeNamePath: String?, private val type: String,
                           private val name: String) : LiveData<Map<String, UnitTask>>() {

    private val databaseReference: DatabaseReference? by lazy {
        homeNamePath?.let {
            Firebase.database.getReference("$it/$HOME_UNITS_BASE/$type/$name/$HOME_UNIT_TASKS")
        }
    }

    private val roomsListener: ValueEventListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            // A new value has been added, add it to the displayed list
            val unitTasks: MutableMap<String, UnitTask> = HashMap()
            for (r: DataSnapshot in dataSnapshot.children) {
                val unitTask = r.getValue<UnitTask>()
                Timber.d("onDataChange (key=${dataSnapshot.key})(unitTask=$unitTask)")
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
        databaseReference?.addValueEventListener(roomsListener)
    }

    override fun onInactive() {
        Timber.d("onInactive")
        databaseReference?.removeEventListener(roomsListener)
    }
}
