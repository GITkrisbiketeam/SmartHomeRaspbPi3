package com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata

import androidx.lifecycle.LiveData
import com.google.firebase.database.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.homeUnitTypeIndicatorMap
import timber.log.Timber


class HomeUnitListLiveData(private val databaseReference: DatabaseReference, private val unitType: String) : LiveData<List<HomeUnit<Any>>>() {
    private val clazz = homeUnitTypeIndicatorMap[unitType]
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
        databaseReference.child(unitType).addValueEventListener(homeUnitListener)
    }

    override fun onInactive() {
        Timber.d("onInactive")
        databaseReference.child(unitType).removeEventListener(homeUnitListener)
    }
}
