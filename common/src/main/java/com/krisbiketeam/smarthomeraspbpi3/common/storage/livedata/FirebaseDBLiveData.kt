package com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.google.firebase.database.*
import timber.log.Timber

class FirebaseDBLiveData(private val ref: DatabaseReference?) : LiveData<DataSnapshot>() {
    private val listener = MyValueEventListener()

    override fun onActive() {
        Timber.d("onActive")
        ref?.addValueEventListener(listener)
    }

    override fun onInactive() {
        Timber.d("onInactive")
        ref?.removeEventListener(listener)
    }

    private inner class MyValueEventListener : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            value = dataSnapshot
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Timber.e(databaseError.toException(), "Can't listen to query $ref")
        }
    }

    inline fun <reified E> getObjectLiveData(): LiveData<E> = Transformations.map(this) {
        it.getValue(E::class.java)
    }
}

