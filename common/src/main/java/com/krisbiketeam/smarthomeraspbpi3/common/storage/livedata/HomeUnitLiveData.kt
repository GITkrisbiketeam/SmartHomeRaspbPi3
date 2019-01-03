package com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata

import androidx.lifecycle.LiveData
import com.google.firebase.database.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.homeUnitTypeIndicatorMap
import timber.log.Timber


class HomeUnitLiveData(private val databaseReference: DatabaseReference, private val type: String, private val name: String) : LiveData<HomeUnit<Any?>>() {
    private val clazz = homeUnitTypeIndicatorMap[type]
    val typeIndicator  = object : GenericTypeIndicator<HomeUnit<Any?>>() {}

    private val homeUnitListener: ValueEventListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            clazz?.let {
                // A new value has been added, add it to the displayed list
                //val key = dataSnapshot.key

                dataSnapshot.getValue(typeIndicator)?.let {homeUnit ->
                    //Timber.d("onDataChange (key=$key)(room=$homeUnit)")

                    value = homeUnit
                }
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Timber.e("onCancelled: $databaseError")
        }
    }

    override fun onActive() {
        Timber.d("onActive")
        databaseReference.child(type).child(name).addValueEventListener(homeUnitListener)
    }

    override fun onInactive() {
        Timber.d("onInactive")
        databaseReference.child(type).child(name).removeEventListener(homeUnitListener)
    }
}
