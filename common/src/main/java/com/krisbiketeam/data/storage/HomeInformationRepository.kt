package com.krisbiketeam.data.storage

import com.google.android.gms.tasks.Task
import com.google.firebase.database.FirebaseDatabase
import com.krisbiketeam.data.storage.dto.HomeUnit
import com.krisbiketeam.data.storage.dto.HwUnit
import com.krisbiketeam.data.storage.dto.HwUnitLog
import com.krisbiketeam.data.storage.dto.Room
import com.krisbiketeam.data.storage.firebaseTables.*
import com.krisbiketeam.data.storage.livedata.*


interface HomeInformationRepository {
    /**
     *  Adds given @see[HwUnitLog] to the log @see[LOG_INFORMATION_BASE] list in DB
     */
    fun logUnitEvent(hwUnitLog: HwUnitLog<out Any>)

    fun writeNewUser(name: String, email: String)

    fun addUserNotiToken(email: String, token: String)

    /**
     *  Adds given @see[HomeUnit] to the log @see[NOTIFICATION_INFORMATION_BASE] list in DB
     */
    fun notifyHomeUnitEvent(homeUnit: HomeUnit<out Any>)

    /**
     *  Saves/updates given @see[Room] in DB
     */
    fun saveRoom(room: Room)

    /**
     *  Saves/updates given @see[HomeUnit] in DB
     */
    fun <T> saveHomeUnit(homeUnit: HomeUnit<T>): Task<Void>

    /**
     *  Deletes given @see[HomeUnit] from DB
     */
    fun <T> deleteHomeUnit(homeUnit: HomeUnit<T>): Task<Void>

    /**
     *  Saves/updates given @see[HwUnit] as a hardware module list in DB
     */
    fun saveHardwareUnit(hwUnit: HwUnit): Task<Void>

    /**
     *  Deletes given @see[HwUnit] from hardware module list in DB
     */
    fun deleteHardwareUnit(hwUnit: HwUnit): Task<Void>

    /**
     * get instance of @see[HomeUnitsLiveData] for listening to changes in entries in DB
     */
    fun homeUnitsLiveData(): HomeUnitsLiveData

    /**
     * get instance of @see[HomeUnitListLiveData] for listening to changes in entries in DB
     */
    fun homeUnitListLiveData(unitType: String): HomeUnitListLiveData

    /**
     * get instance of @see[HomeUnitLiveData] for listening to changes in given HomeUnit in DB
     */
    fun homeUnitLiveData(unitType: String, unitName:String): HomeUnitLiveData

    /**
     * get instance of @see[HomeUnitsLiveData] for listening to changes in entries in DB
     */
    fun homeUnitsLiveData(roomName: String): HomeUnitsLiveData

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
     * get instance of @see[UnitTaskListLiveData] for listening to changes in specific HomeUnit UnitTask List entry in DB
     */
    fun unitTaskListLiveData(unitType: String, unitName:String): UnitTaskListLiveData

    /**
     * get instance of @see[UnitTaskListLiveData] for listening to changes in specific HomeUnit UnitTask List entry in DB
     */
    //fun unitTaskListLiveData(taskName: String, unitType: String, unitName:String): UnitTaskListLiveData

    /**
     * Clear all Logs entries from DB
     */
    fun clearLog()
}

object FirebaseHomeInformationRepository : HomeInformationRepository {
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

    private val homeUnitsLiveData = HomeUnitsLiveData(referenceHome)

    private val hwUnitsLiveData = HwUnitsLiveData(referenceHome)

    private val roomsLiveData = RoomListLiveData(referenceHome)

    private val hwUnitListLiveData = HwUnitListLiveData(referenceHome)

    init {
        // Enable offline this causes some huge delays :(
        //FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        // Keep tracking changes even if there are not active listeners
        referenceHome.keepSynced(true)
    }

    override fun logUnitEvent(hwUnitLog: HwUnitLog<out Any>) {
        referenceLog.push().setValue(hwUnitLog)
    }

    override fun notifyHomeUnitEvent(homeUnit: HomeUnit<out Any>) {
        referenceNotifications.push().setValue(homeUnit.makeNotification())
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

    override fun <T> saveHomeUnit(homeUnit: HomeUnit<T>): Task<Void> {
        return referenceHome.child(homeUnit.firebaseTableName).child(homeUnit.name)
                .setValue(homeUnit)
    }
    override fun <T> deleteHomeUnit(homeUnit: HomeUnit<T>): Task<Void> {
        return referenceHome.child(homeUnit.firebaseTableName).child(homeUnit.name).removeValue()
    }

    override fun saveHardwareUnit(hwUnit: HwUnit): Task<Void> {
        return referenceHome.child(HOME_HW_UNITS).child(hwUnit.name).setValue(hwUnit)
    }

    override fun deleteHardwareUnit(hwUnit: HwUnit): Task<Void> {
        return referenceHome.child(HOME_HW_UNITS).child(hwUnit.name).removeValue()
    }

    override fun clearLog() {
        referenceLog.removeValue()
    }

    override fun homeUnitsLiveData(): HomeUnitsLiveData {
        return homeUnitsLiveData
    }

    override fun homeUnitsLiveData(roomName: String): HomeUnitsLiveData {
        return HomeUnitsLiveData(referenceHome, roomName)
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

    override fun homeUnitListLiveData(unitType: String): HomeUnitListLiveData {
        return HomeUnitListLiveData(referenceHome, unitType)
    }

    override fun homeUnitLiveData(unitType: String, unitName:String): HomeUnitLiveData {
        return HomeUnitLiveData(referenceHome, unitType, unitName)
    }

    override fun unitTaskListLiveData(unitType: String, unitName:String): UnitTaskListLiveData {
        return UnitTaskListLiveData(referenceHome, unitType, unitName)
    }

}
