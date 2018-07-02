package com.krisbiketeam.data.storage

import android.arch.lifecycle.LiveData
import com.google.firebase.database.FirebaseDatabase
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_INFORMATION_BASE
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_INFORMATION_BUTTON
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_INFORMATION_LIGHT
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_INFORMATION_MESSAGE
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_INFORMATION_PRESSURE
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_INFORMATION_TEMPERATURE

interface HomeInformationRepository {
    fun saveMessage(message: String)
    fun saveLightState(isOn: Boolean)
    fun saveButtonState(isPressed: Boolean)
    fun saveTemperature(temperature: Float)
    fun savePressure(pressure: Float)
    fun lightsLiveData(): LiveData<HomeInformation>
    fun logUnitEvent(homeUnitDB: HomeUnitDB)
}

class FirebaseHomeInformationRepository : HomeInformationRepository {

    private val reference = FirebaseDatabase.getInstance().reference.child(HOME_INFORMATION_BASE)
    private val lightsLiveData = HomeInformationLiveData(reference)

    override fun saveMessage(message: String) {
        reference.child(HOME_INFORMATION_MESSAGE).setValue(message)
    }

    override fun saveLightState(isOn: Boolean) {
        reference.child(HOME_INFORMATION_LIGHT).setValue(isOn)
    }

    override fun saveButtonState(isPressed: Boolean) {
        reference.child(HOME_INFORMATION_BUTTON).setValue(isPressed)
    }

    override fun saveTemperature(temperature: Float) {
        reference.child(HOME_INFORMATION_TEMPERATURE).setValue(temperature)
    }

    override fun savePressure(pressure: Float) {
        reference.child(HOME_INFORMATION_PRESSURE).setValue(pressure)
    }

    override fun lightsLiveData(): LiveData<HomeInformation> {
        return lightsLiveData
    }

    override fun logUnitEvent(homeUnitDB: HomeUnitDB) {
        reference
                .child("log")
                .push()
                .setValue(homeUnitDB)
    }
}
