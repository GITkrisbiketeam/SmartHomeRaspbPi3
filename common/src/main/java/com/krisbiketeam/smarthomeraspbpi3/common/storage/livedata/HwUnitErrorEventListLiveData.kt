package com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata

import androidx.lifecycle.LiveData
import com.google.firebase.database.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnitLog
import timber.log.Timber


class HwUnitErrorEventListLiveData(private val databaseReference: DatabaseReference) : LiveData<List<HwUnitLog<Any>>>() {

    private val typeIndicator  = object : GenericTypeIndicator<HwUnitLog<Any>>() {}

    private val hwUnitsListener: ValueEventListener = object: ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            // A new value has been added, add it to the displayed list
            val key = dataSnapshot.key
            val hwUnitErrorEventList: ArrayList<HwUnitLog<Any>> = ArrayList()
            for (child: DataSnapshot in dataSnapshot.children) {
                val hwUnitErrorEvent = child.getValue(typeIndicator)
                hwUnitErrorEvent?.let {
                    hwUnitErrorEventList.add(hwUnitErrorEvent)
                }
            }
            Timber.d("onDataChange (key=$key)(hwUnitErrorEventList=${hwUnitErrorEventList.size})")
            value = hwUnitErrorEventList
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Timber.e("onCancelled: $databaseError")
        }
    }

    override fun onActive() {
        Timber.d("onActive")
        databaseReference.addValueEventListener(hwUnitsListener)
    }

    override fun onInactive() {
        Timber.d("onInactive")
        databaseReference.removeEventListener(hwUnitsListener)
    }
}
