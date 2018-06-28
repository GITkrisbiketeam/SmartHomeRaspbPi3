package com.krisbiketeam.smarthomeraspbpi3


import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.krisbiketeam.smarthomeraspbpi3.units.HomeUnit

/**
 * Handles connection to Firebase
 */
class DatabaseManager {
    private val TAG = DatabaseManager::class.java.simpleName

    private var currentEvent = "default"

    private val database: FirebaseDatabase
        get() = FirebaseDatabase.getInstance()

    init {
        database.setPersistenceEnabled(true)
        getFirebaseEvent()
    }


    private fun getFirebaseEvent() {
        val deviceData = database.reference.child("devices").child("default")
        deviceData.keepSynced(true)
        Log.d(TAG, "Try to get current event")

        deviceData.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.d(TAG, "getCurrentEvent:success")

                if (currentEvent == "none") {
                    currentEvent = "default"
                }

                Log.d(TAG, "current event is: $currentEvent")
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d(TAG, "DeviceDataListener:failure")
                currentEvent = "default"
            }
        })
    }

    fun addButtonPress(homeUnit: HomeUnit) {
        database.reference
                .child("data")
                .child(currentEvent)
                .push()
                .setValue(HomeUnitDB(homeUnit.name, homeUnit.connectionType, homeUnit.location, homeUnit.pinName, homeUnit.softAddress, homeUnit.value))
    }
}