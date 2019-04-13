package com.krisbiketeam.smarthomeraspbpi3.common.storage

import com.google.android.gms.tasks.Task
import com.google.firebase.database.FirebaseDatabase
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata.*
import timber.log.Timber


interface HomeInformationRepository {
    /**
     *  Adds given @see[HwUnitLog] to the log @see[LOG_INFORMATION_BASE] list in DB
     */
    fun logUnitEvent(hwUnitLog: HwUnitLog<out Any>)

    /**
     *  Adds given @see[HwUnitLog] to the hw Error lg @see[HW_ERROR_INFORMATION_BASE] list in DB
     */
    fun addHwUnitErrorEvent(hwUnitError: HwUnitLog<out Any>)

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
     *  Saves/updates given @see[UnitTask] in DB
     */
    fun saveUnitTask(homeUnitType: String, homeUnitName: String, unitTask: UnitTask): Task<Void>

    /**
     *  Deletes given @see[UnitTask] from DB
     */
    fun deleteUnitTask(homeUnitType: String, homeUnitName: String, unitTask: UnitTask): Task<Void>

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
     * get instance of @see[HwUnitsLiveData] for listening to changes in entries in DB
     */
    fun hwUnitLiveData(hwUnitName: String): HwUnitLiveData

    /**
     * get instance of @see[HwUnitListLiveData] for listening to changes in Room entries in DB
     */
    fun hwUnitListLiveData(): HwUnitListLiveData

    /**
     * get instance of @see[RoomListLiveData] for listening to changes in Room entries in DB
     */
    fun roomListLiveData(): RoomListLiveData

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
     * get instance of @see[HwUnitErrorEventListLiveData] for listening to changes HwUnit Error Event List in DB
     */
    fun hwUnitErrorEventListLiveData(): HwUnitErrorEventListLiveData

        /**
     * Clear all Logs entries from DB
     */
    fun clearLog()

    /**
     * Clear all hw Error entries from DB
     */
    fun clearHwErrors()

    /**
     * Clear hw Error Event entry from DB
     */
    fun clearHwErrorEvent(hwUnitName: String)
}

object FirebaseHomeInformationRepository : HomeInformationRepository {
    // Reference for all home related "Units"
    private val referenceHome = FirebaseDatabase.getInstance()
            .reference.child(HOME_INFORMATION_BASE)
    // Reference for all log related events
    private val referenceLog = FirebaseDatabase.getInstance().reference.child(LOG_INFORMATION_BASE)
    // Reference for all log related events
    private val referenceHwError = FirebaseDatabase.getInstance().reference.child(HW_ERROR_INFORMATION_BASE)
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

    private val hwUnitErrorEventListLiveData = HwUnitErrorEventListLiveData(referenceHwError)

    init {
        // Enable offline this causes some huge delays :(
        //FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        // Keep tracking changes even if there are not active listeners
        referenceHome.keepSynced(true)
    }

    override fun logUnitEvent(hwUnitLog: HwUnitLog<out Any>) {
        referenceLog.push().setValue(hwUnitLog)
    }

    override fun addHwUnitErrorEvent(hwUnitError: HwUnitLog<out Any>) {
        referenceHwError.child(hwUnitError.name).setValue(hwUnitError)
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
        Timber.w("saveHomeUnit $homeUnit")
        return referenceHome.child(homeUnit.type).child(homeUnit.name)
                .setValue(homeUnit)
    }

    override fun <T> deleteHomeUnit(homeUnit: HomeUnit<T>): Task<Void> {
        return referenceHome.child(homeUnit.type).child(homeUnit.name).removeValue()
    }

    override fun saveHardwareUnit(hwUnit: HwUnit): Task<Void> {
        return referenceHome.child(HOME_HW_UNITS).child(hwUnit.name).setValue(hwUnit)
    }

    override fun deleteHardwareUnit(hwUnit: HwUnit): Task<Void> {
        return referenceHome.child(HOME_HW_UNITS).child(hwUnit.name).removeValue()
    }

    override fun saveUnitTask(homeUnitType: String, homeUnitName: String, unitTask: UnitTask): Task<Void> {
        return referenceHome.child(homeUnitType).child(homeUnitName).child(HOME_UNIT_TASKS).child(unitTask.name).setValue(unitTask)
    }

    override fun deleteUnitTask(homeUnitType: String, homeUnitName: String, unitTask: UnitTask): Task<Void>{
        return referenceHome.child(homeUnitType).child(homeUnitName).child(HOME_UNIT_TASKS).child(unitTask.name).removeValue()
    }

    override fun clearLog() {
        referenceLog.removeValue()
    }

    override fun clearHwErrors() {
        referenceHwError.removeValue()
    }

    override fun clearHwErrorEvent(hwUnitName: String) {
        referenceHwError.child(hwUnitName).removeValue()
    }

    override fun homeUnitListLiveData(unitType: String): HomeUnitListLiveData {
        return HomeUnitListLiveData(referenceHome, unitType)
    }

    override fun homeUnitsLiveData(): HomeUnitsLiveData {
        return homeUnitsLiveData
    }

    override fun homeUnitsLiveData(roomName: String): HomeUnitsLiveData {
        return HomeUnitsLiveData(referenceHome, roomName)
    }

    override fun homeUnitLiveData(unitType: String, unitName:String): HomeUnitLiveData {
        return HomeUnitLiveData(referenceHome, unitType, unitName)
    }

    override fun hwUnitListLiveData(): HwUnitListLiveData {
        return hwUnitListLiveData
    }

    override fun hwUnitsLiveData(): HwUnitsLiveData {
        return hwUnitsLiveData
    }

    override fun hwUnitLiveData(hwUnitName: String): HwUnitLiveData {
        return HwUnitLiveData(referenceHome, hwUnitName)
    }

    override fun roomListLiveData(): RoomListLiveData {
        return roomsLiveData
    }

    override fun roomLiveData(roomName: String): RoomLiveData {
        return RoomLiveData(referenceHome, roomName)
    }

    override fun unitTaskListLiveData(unitType: String, unitName:String): UnitTaskListLiveData {
        return UnitTaskListLiveData(referenceHome, unitType, unitName)
    }

    override fun hwUnitErrorEventListLiveData(): HwUnitErrorEventListLiveData {
        return hwUnitErrorEventListLiveData
    }

}
