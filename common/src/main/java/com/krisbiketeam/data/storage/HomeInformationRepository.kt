package com.krisbiketeam.data.storage

import android.arch.lifecycle.LiveData
import com.google.firebase.database.FirebaseDatabase
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_INFORMATION_BASE
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_ROOMS
import com.krisbiketeam.data.storage.FirebaseTables.Companion.LOG_INFORMATION_BASE
import com.krisbiketeam.data.storage.FirebaseTables.Companion.OLD_HOME_INFORMATION_BASE
import com.krisbiketeam.data.storage.FirebaseTables.Companion.OLD_HOME_INFORMATION_BUTTON
import com.krisbiketeam.data.storage.FirebaseTables.Companion.OLD_HOME_INFORMATION_LIGHT
import com.krisbiketeam.data.storage.FirebaseTables.Companion.OLD_HOME_INFORMATION_MESSAGE
import com.krisbiketeam.data.storage.FirebaseTables.Companion.OLD_HOME_INFORMATION_PRESSURE
import com.krisbiketeam.data.storage.FirebaseTables.Companion.OLD_HOME_INFORMATION_TEMPERATURE
import com.krisbiketeam.data.storage.dto.HomeUnitLog
import com.krisbiketeam.data.storage.dto.Room
import com.krisbiketeam.data.storage.dto.StorageUnit
import com.krisbiketeam.data.storage.obsolete.HomeInformation
import com.krisbiketeam.data.storage.obsolete.HomeInformationLiveData

interface HomeInformationRepository {
    //Obsolete Code Start
    fun saveMessage(message: String)
    fun saveLightState(isOn: Boolean)
    fun saveButtonState(isPressed: Boolean)
    fun saveTemperature(temperature: Float)
    fun savePressure(pressure: Float)

    fun lightLiveData(): LiveData<HomeInformation>
    //Obsolete Code End

    fun logUnitEvent(homeUnit: HomeUnitLog<out Any>)

    fun saveRoom(room: Room)
    fun <T>saveStorageUnit(storageUnit: StorageUnit<T>)

    fun unitsLiveData(): UnitsLiveData

    fun clearLog()
}

class FirebaseHomeInformationRepository : HomeInformationRepository {
    // Obsolete Code Start
    private val referenceOldHome = FirebaseDatabase.getInstance().reference.child(OLD_HOME_INFORMATION_BASE)
    private val lightLiveData = HomeInformationLiveData(referenceOldHome)
    // Obsolete Code End

    private val referenceHome = FirebaseDatabase.getInstance().reference.child(HOME_INFORMATION_BASE)
    private val referenceLog = FirebaseDatabase.getInstance().reference.child(LOG_INFORMATION_BASE)

    private val unitsDataList: UnitsLiveData = UnitsLiveData(referenceHome)

    init {
        FirebaseDatabase.getInstance().reference.keepSynced(true)
    }

    // Obsolete Code Start
    override fun saveMessage(message: String) {
        referenceOldHome.child(OLD_HOME_INFORMATION_MESSAGE).setValue(message)
    }
    override fun saveLightState(isOn: Boolean) {
        referenceOldHome.child(OLD_HOME_INFORMATION_LIGHT).setValue(isOn)
    }
    override fun saveButtonState(isPressed: Boolean) {
        referenceOldHome.child(OLD_HOME_INFORMATION_BUTTON).setValue(isPressed)
    }
    override fun saveTemperature(temperature: Float) {
        referenceOldHome.child(OLD_HOME_INFORMATION_TEMPERATURE).setValue(temperature)
    }
    override fun savePressure(pressure: Float) {
        referenceOldHome.child(OLD_HOME_INFORMATION_PRESSURE).setValue(pressure)
    }
    override fun lightLiveData(): LiveData<HomeInformation> {
        return lightLiveData
    }
    // Obsolete Code End


    override fun logUnitEvent(homeUnit: HomeUnitLog<out Any>) {
        referenceLog.push().setValue(homeUnit)
    }


    override fun saveRoom(room: Room) {
        referenceHome.child(HOME_ROOMS).child(room.name).setValue(room)
    }

    override fun <T> saveStorageUnit(storageUnit: StorageUnit<T>) {
        referenceHome.child(storageUnit.firebaseTableName).child(storageUnit.name).setValue(storageUnit)
    }


    override fun unitsLiveData(): UnitsLiveData {
        return unitsDataList
    }


    override fun clearLog() {
        referenceLog.removeValue()
    }
}
