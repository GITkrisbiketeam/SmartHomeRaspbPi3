package com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata

import androidx.lifecycle.LiveData
import com.google.firebase.database.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import timber.log.Timber


class HwUnitListLiveData(private val databaseReference: DatabaseReference?) : LiveData<List<HwUnit>>() {

    private val hwUnitsListener: ValueEventListener by lazy {
        object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // A new value has been added, add it to the displayed list
                val key = dataSnapshot.key
                val hwUnits: ArrayList<HwUnit> = ArrayList()
                for (r: DataSnapshot in dataSnapshot.children) {
                    val hwUnit = r.getValue(HwUnit::class.java)
                    hwUnit?.let {
                        hwUnits.add(hwUnit)
                    }
                }
                Timber.d("onDataChange (key=$key)(hwUnits=${hwUnits.size})")
                value = hwUnits
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Timber.e("onCancelled: $databaseError")
            }
        }
    }

    override fun onActive() {
        Timber.d("onActive")
        databaseReference?.addValueEventListener(hwUnitsListener)
    }

    override fun onInactive() {
        Timber.d("onInactive")
        databaseReference?.removeEventListener(hwUnitsListener)
    }
}
