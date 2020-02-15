package com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata

import androidx.lifecycle.LiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import timber.log.Timber


class HomesListLiveData(private val databaseReference: DatabaseReference?) :
        LiveData<List<String>>() {
    private val homeUnitListener: ValueEventListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {

            // A new value has been added, add it to the displayed list
            value = dataSnapshot.children.mapNotNull { it.key }
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Timber.e("onCancelled: $databaseError")
        }
    }

    override fun onActive() {
        Timber.d("onActive")
        databaseReference?.addValueEventListener(homeUnitListener)
    }

    override fun onInactive() {
        Timber.d("onInactive")
        databaseReference?.removeEventListener(homeUnitListener)
    }
}
