package com.krisbiketeam.data.storage

import android.arch.lifecycle.LiveData
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.krisbiketeam.data.storage.firebaseTables.*
import com.krisbiketeam.data.storage.obsolete.HomeInformation
import com.krisbiketeam.data.storage.obsolete.HomeInformationLiveData
import com.krisbiketeam.data.storage.dto.*


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

    fun writeNewUser(name: String, email: String)

    fun addUserNotiToken(email: String, token: String)

    /**
     *  Adds given @see[StorageUnit] to the log @see[NOTIFICATION_INFORMATION_BASE] list in DB
     */
    fun notifyStorageUnitEvent(storageUnit: StorageUnit<out Any>)

    /**
     *  Saves/updates given @see[Room] in DB
     */
    fun saveRoom(room: Room)

    /**
     *  Saves/updates given @see[StorageUnit] in DB
     */
    fun <T> saveStorageUnit(storageUnit: StorageUnit<T>)

    /**
     *  Saves/updates given @see[HomeUnitLog] as a hardware module list in DB
     */
    fun saveHardwareUnit(hwUnit: HomeUnit)

    /**
     * get instance of @see[StorageUnitsLiveData] for listening to changes in entries in DB
     */
    fun storageUnitsLiveData(): StorageUnitsLiveData

    /**
     * get instance of @see[StorageUnitListLiveData] for listening to changes in entries in DB
     */
    fun storageUnitListLiveData(storageType: String): StorageUnitListLiveData

    /**
     * get instance of @see[StorageUnitsLiveData] for listening to changes in entries in DB
     */
    fun storageUnitsLiveData(roomName: String): StorageUnitsLiveData

    /**
     * get instance of @see[HwUnitsLiveData] for listening to changes in Room entries in DB
     */
    fun hwUnitsLiveData(): HwUnitsLiveData

    /**
     * get instance of @see[HwUnitListLiveData] for listening to changes in Room entries in DB
     */
    fun hwUnitListLiveData(): HwUnitListLiveData

    /**
     * get instance of @see[RoomListLiveData] for listening to changes in Room entries in DB
     */
    fun roomsLiveData(): RoomListLiveData

    /**
     * get instance of @see[RoomLiveData] for listening to changes in specific Room entry in DB
     */
    fun roomLiveData(roomName: String): RoomLiveData

    /**
     * Clear all Logs entries from DB
     */
    fun clearLog()
}

object FirebaseHomeInformationRepository : HomeInformationRepository {
    // Obsolete Code Start
    private val referenceOldHome: DatabaseReference = FirebaseDatabase.getInstance()
            .reference.child(OLD_HOME_INFORMATION_BASE)
    private val lightLiveData = HomeInformationLiveData(referenceOldHome)
    // Obsolete Code End

    // Reference for all home related "Units"
    private val referenceHome = FirebaseDatabase.getInstance()
            .reference.child(HOME_INFORMATION_BASE)
    // Reference for all log related events
    private val referenceLog = FirebaseDatabase.getInstance().reference.child(LOG_INFORMATION_BASE)
    // Reference for all users
    private val referenceUsers = FirebaseDatabase.getInstance()
            .reference.child(USER_INFORMATION_BASE)
    // Reference for all notifications
    private val referenceNotifications = FirebaseDatabase.getInstance()
            .reference.child(NOTIFICATION_INFORMATION_BASE)

    private val storageUnitsLiveData = StorageUnitsLiveData(referenceHome)

    private val hwUnitsLiveData = HwUnitsLiveData(referenceHome)

    private val roomsLiveData = RoomListLiveData(referenceHome)

    private val hwUnitListLiveData = HwUnitListLiveData(referenceHome)

    init {
        // Enable offline this causes some huge delays :(
        //FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        // Keep tracking changes even if there are not active listeners
        referenceHome.keepSynced(true)
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

    override fun notifyStorageUnitEvent(storageUnit: StorageUnit<out Any>) {
        referenceNotifications.push().setValue(storageUnit.makeNotification())
    }

    override fun writeNewUser(name: String, email: String) {
        referenceUsers.child(email.hashCode().toString())
                .updateChildren(mapOf(Pair(USER_NAME, name), Pair(USER_EMAIL, email)))
    }

    override fun addUserNotiToken(email: String, token: String) {
        referenceUsers.child(email.hashCode().toString()).child(USER_NOTIFICATION_TOKENS)
                .child(token).setValue(true)
    }

    override fun saveRoom(room: Room) {
        referenceHome.child(HOME_ROOMS).child(room.name).setValue(room)
    }

    override fun <T> saveStorageUnit(storageUnit: StorageUnit<T>) {
        referenceHome.child(storageUnit.firebaseTableName).child(storageUnit.name)
                .setValue(storageUnit)
    }

    override fun saveHardwareUnit(hwUnit: HomeUnit) {
        referenceHome.child(HOME_HW_UNITS).child(hwUnit.name).setValue(hwUnit)
    }


    override fun clearLog() {
        referenceLog.removeValue()
    }

    override fun storageUnitsLiveData(): StorageUnitsLiveData {
        return storageUnitsLiveData
    }

    override fun storageUnitsLiveData(roomName: String): StorageUnitsLiveData {
        return StorageUnitsLiveData(referenceHome, roomName)
    }

    override fun hwUnitsLiveData(): HwUnitsLiveData {
        return hwUnitsLiveData
    }

    override fun hwUnitListLiveData(): HwUnitListLiveData {
        return hwUnitListLiveData
    }

    override fun roomsLiveData(): RoomListLiveData {
        return roomsLiveData
    }

    override fun roomLiveData(roomName: String): RoomLiveData {
        return RoomLiveData(referenceHome, roomName)
    }

    override fun storageUnitListLiveData(storageType: String): StorageUnitListLiveData {
        return StorageUnitListLiveData(referenceHome, storageType)
    }


}
