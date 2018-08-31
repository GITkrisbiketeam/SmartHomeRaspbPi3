package com.krisbiketeam.data.storage

import android.arch.lifecycle.LiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.krisbiketeam.data.storage.dto.storageUnitTypeIndicatorMap
import timber.log.Timber


class StorageUnitListLiveData(private val databaseReference: DatabaseReference, private val firebaseTable: String) : LiveData<List<Any>>() {
    val clazz = storageUnitTypeIndicatorMap[firebaseTable]

    private val storageUnitListener: ValueEventListener = object: ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            clazz?.let {
                // A new value has been added, add it to the displayed list
                val key = dataSnapshot.key
                val values: ArrayList<Any> = ArrayList()
                for (child: DataSnapshot in dataSnapshot.children) {
                    val storageUnit = child.getValue(clazz)
                    Timber.d("onDataChange (key=$key)(storageUnit=$storageUnit)")
                    storageUnit?.run {
                        values.add(storageUnit)
                    }
                }
                value = values
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Timber.e("onCancelled: $databaseError")
        }
    }

    override fun onActive() {
        Timber.d("onActive")
        databaseReference.child(firebaseTable).addValueEventListener(storageUnitListener)
    }

    override fun onInactive() {
        Timber.d("onInactive")
        databaseReference.child(firebaseTable).removeEventListener(storageUnitListener)
    }
}
