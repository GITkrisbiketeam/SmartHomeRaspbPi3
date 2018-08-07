package com.krisbiketeam.data.storage

import android.arch.lifecycle.LiveData
import com.google.firebase.database.FirebaseDatabase
import com.krisbiketeam.data.storage.FirebaseTables.*
import com.krisbiketeam.data.storage.dto.HomeUnit
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

    /**
     *  Adds given @see[HomeUnitLog] to the log @see[LOG_INFORMATION_BASE] list in DB
     */
    fun logUnitEvent(homeUnit: HomeUnitLog<out Any>)

    /**
     *  Saves/updates given @see[Room] in DB
     */
    fun saveRoom(room: Room)
    /**
     *  Saves/updates given @see[StorageUnit] in DB
     */
    fun <T>saveStorageUnit(storageUnit: StorageUnit<T>)

    /**
     *  Saves/updates given @see[HomeUnitLog] as a hardware module list in DB
     */
    fun saveHardwareUnit(hwUnit: HomeUnit)

    /**
     * get instance of @see[StorageUnitsLiveData] for listening to changes in entries in DB
     */
    fun storageUnitsLiveData(): StorageUnitsLiveData

    /**
     * Clear all Logs entries from DB
     */
    fun clearLog()
}

class FirebaseHomeInformationRepository : HomeInformationRepository {
    // Obsolete Code Start
    private val referenceOldHome = FirebaseDatabase.getInstance().reference.child(OLD_HOME_INFORMATION_BASE)
    private val lightLiveData = HomeInformationLiveData(referenceOldHome)
    // Obsolete Code End

    // Reference for all home related "Units"
    private val referenceHome = FirebaseDatabase.getInstance().reference.child(HOME_INFORMATION_BASE)
    // Reference for all log related events
    private val referenceLog = FirebaseDatabase.getInstance().reference.child(LOG_INFORMATION_BASE)

    private val storageUnitsLiveData: StorageUnitsLiveData = StorageUnitsLiveData(referenceHome)

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

    override fun saveHardwareUnit(hwUnit: HomeUnit) {
        referenceHome.child(HOME_HW_UNITS).child(hwUnit.name).setValue(hwUnit)
    }

    override fun storageUnitsLiveData(): StorageUnitsLiveData {
        return storageUnitsLiveData
    }

    override fun clearLog() {
        referenceLog.removeValue()
    }
}
