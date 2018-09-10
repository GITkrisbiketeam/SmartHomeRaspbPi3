package com.krisbiketeam.data.storage.livedata

import android.arch.lifecycle.LiveData
import com.google.firebase.database.*
import com.krisbiketeam.data.storage.dto.StorageUnit
import com.krisbiketeam.data.storage.dto.storageUnitTypeIndicatorMap
import timber.log.Timber


class StorageUnitLiveData(private val databaseReference: DatabaseReference, private val type: String, private val name: String) : LiveData<StorageUnit<Any?>>() {
    private val clazz = storageUnitTypeIndicatorMap[type]
    val typeIndicator  = object : GenericTypeIndicator<StorageUnit<Any?>>() {}

    private val storageUnitListener: ValueEventListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            clazz?.let {
                // A new value has been added, add it to the displayed list
                val key = dataSnapshot.key

                dataSnapshot.getValue(typeIndicator)?.let {storageUnit ->
                    Timber.d("onDataChange (key=$key)(room=$storageUnit)")

                    value = storageUnit
                }
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Timber.e("onCancelled: $databaseError")
        }
    }

    override fun onActive() {
        Timber.d("onActive")
        databaseReference.child(type).child(name).addValueEventListener(storageUnitListener)
    }

    override fun onInactive() {
        Timber.d("onInactive")
        databaseReference.child(type).child(name).removeEventListener(storageUnitListener)
    }
}
