package com.krisbiketeam.data.storage.livedata

import android.arch.lifecycle.LiveData
import com.google.firebase.database.*
import com.krisbiketeam.data.storage.dto.HomeUnit
import com.krisbiketeam.data.storage.dto.homeUnitTypeIndicatorMap
import timber.log.Timber


class HomeUnitListLiveData(private val databaseReference: DatabaseReference, private val firebaseTable: String) : LiveData<List<HomeUnit<Any>>>() {
    private val clazz = homeUnitTypeIndicatorMap[firebaseTable]
    private val typeIndicator  = object : GenericTypeIndicator<HomeUnit<Any>>() {}

    private val homeUnitListener: ValueEventListener = object: ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            clazz?.let {
                // A new value has been added, add it to the displayed list
                val homeUnits: ArrayList<HomeUnit<Any>> = ArrayList()
                for (child: DataSnapshot in dataSnapshot.children) {
                    val homeUnit = child.getValue(typeIndicator)
                    homeUnit?.run {
                        homeUnits.add(homeUnit)
                    }
                }
                //Timber.d("onDataChange (key=${dataSnapshot.key})(homeUnits=$homeUnits)")
                value = homeUnits
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Timber.e("onCancelled: $databaseError")
        }
    }

    override fun onActive() {
        Timber.d("onActive")
        databaseReference.child(firebaseTable).addValueEventListener(homeUnitListener)
    }

    override fun onInactive() {
        Timber.d("onInactive")
        databaseReference.child(firebaseTable).removeEventListener(homeUnitListener)
    }
}
