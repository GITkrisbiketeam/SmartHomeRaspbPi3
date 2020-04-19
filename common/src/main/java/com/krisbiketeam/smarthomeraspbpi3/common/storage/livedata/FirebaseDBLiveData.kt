package com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import timber.log.Timber

class FirebaseDBLiveData(private val refName: String) : LiveData<DataSnapshot>() {
    private val listener = MyValueEventListener()

    override fun onActive() {
        Timber.d("onActive")
        FirebaseDatabase.getInstance().getReference(refName).addValueEventListener(listener)
    }

    override fun onInactive() {
        Timber.d("onInactive")
        FirebaseDatabase.getInstance().getReference(refName).removeEventListener(listener)
    }

    private inner class MyValueEventListener : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            value = dataSnapshot
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Timber.e(databaseError.toException(), "Can't listen to query $refName")
        }
    }

    inline fun <reified E> getObjectLiveData(): LiveData<E> = Transformations.map(this) {
        it.getValue(E::class.java)
    }
}

