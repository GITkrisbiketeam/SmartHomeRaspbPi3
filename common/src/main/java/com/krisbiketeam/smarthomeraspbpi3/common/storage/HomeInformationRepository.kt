package com.krisbiketeam.smarthomeraspbpi3.common.storage

import androidx.lifecycle.LiveData
import com.google.android.gms.tasks.Task
import com.google.firebase.database.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata.*
import timber.log.Timber


interface HomeInformationRepository {
    /**
     *  Sets Firebase homeReference to given homeName
     */
    fun setHomeReference(homeName: String)

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
    fun notifyHomeUnitEvent(homeUnit: HomeUnit<out Any?>)

    /**
     *  Saves/updates given @see[Room] in DB
     */
    fun saveRoom(room: Room): Task<Void>?

    /**
     *  Deletes given @see[Room] from DB
     */
    fun deleteRoom(roomName: String): Task<Void>?

    /**
     *  Saves/updates given @see[HomeUnit] in DB
     */
    fun <T> saveHomeUnit(homeUnit: HomeUnit<T>): Task<Void>?

    /**
     *  Deletes given @see[HomeUnit] from DB
     */
    fun <T> deleteHomeUnit(homeUnit: HomeUnit<T>): Task<Void>?

    /**
     *  Saves/updates given @see[HwUnit] as a hardware module list in DB
     */
    fun saveHardwareUnit(hwUnit: HwUnit): Task<Void>?

    /**
     *  Deletes given @see[HwUnit] from hardware module list in DB
     */
    fun deleteHardwareUnit(hwUnit: HwUnit): Task<Void>?

    /**
     *  Saves/updates given @see[UnitTask] in DB
     */
    fun saveUnitTask(homeUnitType: String, homeUnitName: String, unitTask: UnitTask): Task<Void>?

    /**
     *  Deletes given @see[UnitTask] from DB
     */
    fun deleteUnitTask(homeUnitType: String, homeUnitName: String, unitTask: UnitTask): Task<Void>?

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
    fun homeUnitLiveData(unitType: String, unitName: String): HomeUnitLiveData

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
    fun unitTaskListLiveData(unitType: String, unitName: String): UnitTaskListLiveData

    /**
     * get instance of @see[UnitTaskListLiveData] for listening to changes in specific HomeUnit UnitTask List entry in DB
     */
    //fun unitTaskListLiveData(taskName: String, unitType: String, unitName:String): UnitTaskListLiveData

    /**
     * get instance of @see[HwUnitErrorEventListLiveData] for listening to changes HwUnit Error Event List in DB
     */
    fun hwUnitErrorEventListLiveData(): HwUnitErrorEventListLiveData

    /**
     * get instance of @see[HwUnitErrorEventListLiveData] for listening to request to restart hw units
     */
    fun hwUnitRestartListLiveData(): HwUnitErrorEventListLiveData

    /**
     *  Sets Firebase homePreference key/Value
     */
    fun setHomePreference(key: String, value: Any?)

    /**
     *  Gets Firebase homePreference Value for given key
     */
    fun getHomePreference(key: String): DatabaseReference?

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

    /**
     * start monitoring for Firebase Connection active
     */
    fun startHomeToFirebaseConnectionActiveMonitor()

    /**
     * Checks if Home Module is online
     */
    fun isHomeOnline(): LiveData<Boolean?>

    /**
     * Checks when Home Module was last online
     */
    fun lastHomeOnlineTime(): LiveData<Long?>

    /**
     *
     */
    fun getHomes(): LiveData<List<String>>

    fun clearHwRestarts()
    fun clearHwRestartEvent(hwUnitName: String)
    fun addHwUnitToRestart(hwUnitError: HwUnitLog<out Any>)
    fun addHwUnitListToRestart(hwUnitErrorList: List<HwUnitLog<out Any>>)
}

class FirebaseHomeInformationRepository : HomeInformationRepository {
    // Reference for all home related "Units"
    private var referenceHome: DatabaseReference? = null
    // Reference for all log related events
    private var referenceLog: DatabaseReference? = null
    // Reference for all hw error events
    private var referenceHwError: DatabaseReference? = null
    // Reference for all hw unit restart events
    private var referenceHwRestart: DatabaseReference? = null
    // Reference for all users
    private val referenceUsers =
            FirebaseDatabase.getInstance().reference.child(USER_INFORMATION_BASE)
    // Reference for all notifications
    private val referenceNotifications =
            FirebaseDatabase.getInstance().reference.child(NOTIFICATION_INFORMATION_BASE)

    private var homeUnitsLiveData = HomeUnitsLiveData(null)

    private var hwUnitsLiveData = HwUnitsLiveData(null)

    private var roomsLiveData = RoomListLiveData(null)

    private var hwUnitListLiveData = HwUnitListLiveData(null)

    private var hwUnitErrorEventListLiveData = HwUnitErrorEventListLiveData(null)

    private var hwUnitRestartListLiveData = HwUnitErrorEventListLiveData(null)

    init {
        // Enable offline this causes some huge delays :(
        //FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        // Keep tracking changes even if there are not active listeners
        //referenceHome?.keepSynced(true)
    }

    override fun setHomeReference(homeName: String) {
        referenceHome = FirebaseDatabase.getInstance().reference.child(HOME_INFORMATION_BASE)
                .child(homeName)
        //TODO :disable logging as its can overload firebase DB
        //referenceLog = referenceHome?.child(LOG_INFORMATION_BASE)
        referenceHwError = referenceHome?.child(HW_ERROR_INFORMATION_BASE)
        referenceHwRestart = referenceHome?.child(HW_RESTART_INFORMATION_BASE)

        homeUnitsLiveData = HomeUnitsLiveData(referenceHome)
        hwUnitsLiveData = HwUnitsLiveData(referenceHome)
        roomsLiveData = RoomListLiveData(referenceHome)
        hwUnitListLiveData = HwUnitListLiveData(referenceHome)
        hwUnitErrorEventListLiveData = HwUnitErrorEventListLiveData(referenceHwError)
        hwUnitRestartListLiveData = HwUnitErrorEventListLiveData(referenceHwRestart)
        // Keep tracking changes even if there are not active listeners
        referenceHome?.keepSynced(true)
    }

    override fun logUnitEvent(hwUnitLog: HwUnitLog<out Any>) {
        referenceLog?.push()?.setValue(hwUnitLog)
    }

    override fun addHwUnitErrorEvent(hwUnitError: HwUnitLog<out Any>) {
        referenceHwError?.child(hwUnitError.name)?.setValue(hwUnitError)
    }

    override fun addHwUnitToRestart(hwUnitError: HwUnitLog<out Any>) {
        referenceHwRestart?.child(hwUnitError.name)?.setValue(hwUnitError)
    }

    override fun addHwUnitListToRestart(hwUnitErrorList: List<HwUnitLog<out Any>>) {
        val pairs = hwUnitErrorList.map {it.name to it }
        referenceHwRestart?.setValue(pairs.toMap())
    }

    override fun notifyHomeUnitEvent(homeUnit: HomeUnit<out Any?>) {
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

    override fun saveRoom(room: Room): Task<Void>? {
        return referenceHome?.child(HOME_ROOMS)?.child(room.name)?.setValue(room)
    }

    override fun deleteRoom(roomName: String): Task<Void>? {
        return referenceHome?.child(HOME_ROOMS)?.child(roomName)?.removeValue()
    }

    override fun <T> saveHomeUnit(homeUnit: HomeUnit<T>): Task<Void>? {
        Timber.w("saveHomeUnit $homeUnit")
        return referenceHome?.child(homeUnit.type)?.child(homeUnit.name)?.setValue(homeUnit)
    }

    override fun <T> deleteHomeUnit(homeUnit: HomeUnit<T>): Task<Void>? {
        return referenceHome?.child(homeUnit.type)?.child(homeUnit.name)?.removeValue()
    }

    override fun saveHardwareUnit(hwUnit: HwUnit): Task<Void>? {
        return referenceHome?.child(HOME_HW_UNITS)?.child(hwUnit.name)?.setValue(hwUnit)
    }

    override fun deleteHardwareUnit(hwUnit: HwUnit): Task<Void>? {
        return referenceHome?.child(HOME_HW_UNITS)?.child(hwUnit.name)?.removeValue()
    }

    override fun saveUnitTask(homeUnitType: String, homeUnitName: String,
                              unitTask: UnitTask): Task<Void>? {
        return referenceHome?.child(homeUnitType)?.child(homeUnitName)?.child(HOME_UNIT_TASKS)
                ?.child(unitTask.name)?.setValue(unitTask)
    }

    override fun deleteUnitTask(homeUnitType: String, homeUnitName: String,
                                unitTask: UnitTask): Task<Void>? {
        return referenceHome?.child(homeUnitType)?.child(homeUnitName)?.child(HOME_UNIT_TASKS)
                ?.child(unitTask.name)?.removeValue()
    }

    override fun clearLog() {
        referenceLog?.removeValue()
    }

    override fun clearHwErrors() {
        referenceHwError?.removeValue()
    }

    override fun clearHwErrorEvent(hwUnitName: String) {
        referenceHwError?.child(hwUnitName)?.removeValue()
    }

    override fun clearHwRestarts() {
        referenceHwRestart?.removeValue()
    }

    override fun clearHwRestartEvent(hwUnitName: String) {
        referenceHwRestart?.child(hwUnitName)?.removeValue()
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

    override fun homeUnitLiveData(unitType: String, unitName: String): HomeUnitLiveData {
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

    override fun unitTaskListLiveData(unitType: String, unitName: String): UnitTaskListLiveData {
        return UnitTaskListLiveData(referenceHome, unitType, unitName)
    }

    override fun hwUnitErrorEventListLiveData(): HwUnitErrorEventListLiveData {
        return hwUnitErrorEventListLiveData
    }

    override fun hwUnitRestartListLiveData(): HwUnitErrorEventListLiveData {
        return hwUnitRestartListLiveData
    }



    override fun setHomePreference(key: String, value: Any?) {
        referenceHome?.child(HOME_PREFERENCES)?.child(key)?.setValue(value)

    }

    override fun getHomePreference(key: String): DatabaseReference? {
        return referenceHome?.child(HOME_PREFERENCES)?.child(key)
    }

    override fun startHomeToFirebaseConnectionActiveMonitor() {
        referenceHome?.run {
            // Since I can connect from multiple devices, we store each connection instance separately
            // any time that connectionsRef's value is null (i.e. has no children) I am offline
            val myConnectionsRef = child(HOME_ONLINE)

            // Stores the timestamp of my last disconnect (the last time I was seen online)
            val lastOnlineRef = child(HOME_LAST_ONLINE_TIME)
            val connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected")
            connectedRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    Timber.w("Firebase Online Connected? $connected")
                    if (connected) {
                        // When this device disconnects, remove it
                        myConnectionsRef.onDisconnect().removeValue()

                        // When I disconnect, update the last time I was seen online
                        lastOnlineRef.onDisconnect().setValue(ServerValue.TIMESTAMP)

                        // Add this device to my connections list
                        // this value could contain info about the device or a timestamp too
                        myConnectionsRef.setValue(java.lang.Boolean.TRUE)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Timber.w("Listener was cancelled at .info/connected")
                }
            })
        }
    }

    override fun isHomeOnline(): LiveData<Boolean?> = FirebaseDBLiveData(
            referenceHome?.child(HOME_ONLINE)).getObjectLiveData()

    override fun lastHomeOnlineTime(): LiveData<Long?> = FirebaseDBLiveData(
            referenceHome?.child(HOME_LAST_ONLINE_TIME)).getObjectLiveData()

    override fun getHomes(): LiveData<List<String>> = HomesListLiveData(
            FirebaseDatabase.getInstance().reference.child(HOME_INFORMATION_BASE))
}
