package com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata

import androidx.lifecycle.LiveData
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_UNITS_BASE
import timber.log.Timber


@Deprecated("please use HomeUnitListFlow")
class HomeUnitListLiveData(private val homeNamePath: String?, private val unitType: String) :
        LiveData<List<HomeUnit<Any>>>() {

    private val databaseReference: DatabaseReference? by lazy {
        homeNamePath?.let {
            Firebase.database.getReference("$it/$HOME_UNITS_BASE/$unitType")
        }
    }

    private val homeUnitListener: ValueEventListener by lazy {
        object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // A new value has been added, add it to the displayed list
                val homeUnits: ArrayList<HomeUnit<Any>> = ArrayList()
                for (child: DataSnapshot in dataSnapshot.children) {
                    val homeUnit = child.getValue<HomeUnit<Any>>()
                    homeUnit?.run {
                        homeUnits.add(homeUnit)
                    }
                }
                //Timber.d("onDataChange (key=${dataSnapshot.key})(homeUnits=$homeUnits)")
                value = homeUnits
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
