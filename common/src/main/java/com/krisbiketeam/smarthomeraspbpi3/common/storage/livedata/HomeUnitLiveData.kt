package com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata

import androidx.lifecycle.LiveData
import com.google.firebase.database.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit
import timber.log.Timber


class HomeUnitLiveData(private val homeNamePath: String?, private val type: String,
                       private val name: String) : LiveData<HomeUnit<Any?>>() {

    val typeIndicator = object : GenericTypeIndicator<HomeUnit<Any?>>() {}

    private val databaseReference: DatabaseReference? by lazy {
        homeNamePath?.let {
            FirebaseDatabase.getInstance().getReference("$it/$type/$name")
        }
    }

    private val homeUnitListener: ValueEventListener by lazy {

        object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // A new value has been added, add it to the displayed list
                //val key = dataSnapshot.key

                dataSnapshot.getValue(typeIndicator)?.let { homeUnit ->
                    //Timber.d("onDataChange (key=$key)(room=$homeUnit)")

                    value = homeUnit
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
