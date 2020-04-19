package com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata

import androidx.lifecycle.LiveData
import com.google.firebase.database.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import timber.log.Timber


class HwUnitLiveData(ref: DatabaseReference?, name: String) : LiveData<HwUnit>() {

    private val databaseReference: DatabaseReference? by lazy {
        ref?.child(name)
    }

    private val homeUnitListener: ValueEventListener by lazy {
        object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Timber.e("onDataChange dataSnapshot: $dataSnapshot")
                val hwUnit = dataSnapshot.getValue(HwUnit::class.java)
                hwUnit?.let {
                    value = it
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Timber.e("onCancelled: $databaseError")
            }
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
