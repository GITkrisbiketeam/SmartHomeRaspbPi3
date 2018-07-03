package com.krisbiketeam.data.storage.livedata

import android.arch.lifecycle.LiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.krisbiketeam.data.storage.dto.Motion
import timber.log.Timber

class MotionsLiveData(private val databaseReference: DatabaseReference) : LiveData<Motion>() {

    private val valueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val newValue = snapshot.getValue(Motion::class.java)
            Timber.d("onDataChange (value=$value) (newValue=$newValue)")
            value = newValue
        }

        override fun onCancelled(error: DatabaseError) {
            Timber.w(error.toException(), "onCancelled")
        }
    }

    override fun onActive() {
        databaseReference.addValueEventListener(valueEventListener)
    }

    override fun onInactive() {
        databaseReference.removeEventListener(valueEventListener)
    }
}
