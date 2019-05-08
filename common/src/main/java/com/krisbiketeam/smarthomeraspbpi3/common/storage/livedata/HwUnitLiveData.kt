package com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata

import androidx.lifecycle.LiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_HW_UNITS
import timber.log.Timber


class HwUnitLiveData(private val databaseReference: DatabaseReference?, private val name: String) : LiveData<HwUnit>() {

    private val homeUnitListener: ValueEventListener = object : ValueEventListener {
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

    override fun onActive() {
        Timber.d("onActive")
        databaseReference?.child(HOME_HW_UNITS)?.child(name)?.addValueEventListener(homeUnitListener)
    }

    override fun onInactive() {
        Timber.d("onInactive")
        databaseReference?.child(HOME_HW_UNITS)?.child(name)?.removeEventListener(homeUnitListener)
    }
}
