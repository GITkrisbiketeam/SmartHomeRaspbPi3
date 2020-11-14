package com.krisbiketeam.smarthomeraspbpi3.common.storage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.firebase.database.*
import com.krisbiketeam.smarthomeraspbpi3.common.resetableLazy
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.flows.genericListReferenceFlow
import com.krisbiketeam.smarthomeraspbpi3.common.storage.flows.genericMapReferenceFlow
import com.krisbiketeam.smarthomeraspbpi3.common.storage.flows.genericReferenceFlow
import com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import timber.log.Timber

class FirebaseHomeInformationRepository {
    // region references
    // region Home reference
    // Reference for all home related "Units"
    private var homePathReference: String? = null
    private var userPathReference: String? = null

    private val referenceHWUnitsDelegate = resetableLazy {
        homePathReference?.let { FirebaseDatabase.getInstance().getReference("$it/$HOME_HW_UNITS_BASE") }
    }
    private val referenceHWUnits: DatabaseReference? by referenceHWUnitsDelegate

    private val referenceHwErrorDelegate = resetableLazy {
        homePathReference?.let {
            FirebaseDatabase.getInstance().getReference("$it/$HW_ERROR_INFORMATION_BASE")
        }
    }
    // Reference for all hw error events
    private val referenceHwError: DatabaseReference? by referenceHwErrorDelegate

    private val referenceHwRestartDelegate = resetableLazy {
        homePathReference?.let {
            FirebaseDatabase.getInstance().getReference("$it/$HW_RESTART_INFORMATION_BASE")
        }
    }
    // Reference for all hw unit restart events
    private val referenceHwRestart: DatabaseReference? by referenceHwRestartDelegate

    private val referenceRoomsDelegate = resetableLazy {
        homePathReference?.let { FirebaseDatabase.getInstance().getReference("$it/$HOME_ROOMS") }
    }
    // Reference for all Rooms stored remotely
    private val referenceRooms: DatabaseReference? by referenceRoomsDelegate

    private val referenceHomePreferencesDelegate = resetableLazy {
        homePathReference?.let {
            FirebaseDatabase.getInstance().getReference("$it/$HOME_PREFERENCES_BASE")
        }
    }
    // Reference for all sharedPreferences stored remotely
    private val referenceHomePreferences: DatabaseReference? by referenceHomePreferencesDelegate

    private val referenceLogDelegate = resetableLazy {
        homePathReference?.let {
            FirebaseDatabase.getInstance().getReference("$it/$LOG_INFORMATION_BASE")
        }
    }
    // Reference for all log related events
    private val referenceLog: DatabaseReference? by referenceLogDelegate
    // endregion

    // region Users
    // Reference for all users
    private val referenceUsers by lazy { FirebaseDatabase.getInstance().getReference(USER_INFORMATION_BASE) }
    // endregion

    // region Notifications
    // Reference for all notifications
    private val referenceNotifications =
            FirebaseDatabase.getInstance().getReference(NOTIFICATION_INFORMATION_BASE)
    // endregion

    // endregion

    init {
        // Enable offline this causes some huge delays :(
        //FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        // Keep tracking changes even if there are not active listeners
        //FirebaseDatabase.getInstance().getReference(referenceHomePath).keepSynced(true)
    }

    /**
     *  Sets Firebase homeReference to given homeName
     */
    fun setHomeReference(homeName: String) {
        homePathReference = "$HOME_INFORMATION_BASE/$homeName"

        referenceRoomsDelegate.reset()

        referenceHWUnitsDelegate.reset()
        referenceHwErrorDelegate.reset()
        referenceHwRestartDelegate.reset()

        referenceHomePreferencesDelegate.reset()

        referenceLogDelegate.reset()

        // Keep tracking changes even if there are not active listeners
        //FirebaseDatabase.getInstance().getReference(this).keepSynced(true)
    }

    fun setUserReference(email: String) {
        userPathReference = "$USER_INFORMATION_BASE/${email.hashCode()}"
    }

    // region Firebase DB operations
    // region User and its Notification token handling

    fun writeNewUser(name: String, email: String) {
        referenceUsers.child(email.hashCode().toString())
                .updateChildren(mapOf(Pair(USER_NAME, name), Pair(USER_EMAIL, email)))
    }

    fun addUserNotiToken(email: String, token: String) {
        referenceUsers.child(email.hashCode().toString()).child(USER_NOTIFICATION_TOKENS)
                .child(token).setValue(true)
    }

    fun startUserToFirebaseConnectionActiveMonitor(email: String) {
        // Since I can connect from multiple devices, we store each connection instance separately
        // any time that connectionsRef's value is null (i.e. has no children) I am offline
        val myConnectionsRef = userPathReference?.let {
            FirebaseDatabase.getInstance().getReference("$it/$USER_ONLINE")
        }

        val connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected")

        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                Timber.w("Firebase Online Connected? $connected")
                if (connected) {
                    // When this device disconnects, remove it
                    myConnectionsRef?.onDisconnect()?.removeValue()

                    // Add this device to my connections list
                    // this value could contain info about the device or a timestamp too
                    myConnectionsRef?.setValue(java.lang.Boolean.TRUE)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.w("Listener was cancelled at .info/connected")
            }
        })
    }

    fun isUserOnline(): LiveData<Boolean?> = userPathReference?.let {
        FirebaseDBLiveData("$it/$USER_ONLINE").getObjectLiveData()
    } ?: MutableLiveData()

    @ExperimentalCoroutinesApi
    fun isUserOnlineFlow(): Flow<Boolean?> =
        genericReferenceFlow(userPathReference?.let{FirebaseDatabase.getInstance().getReference("$it/$USER_ONLINE")})

    // endregion

    // region Notifications
    /**
     *  Adds given @see[HomeUnit] to the log @see[NOTIFICATION_INFORMATION_BASE] list in DB
     */
    fun notifyHomeUnitEvent(homeUnit: HomeUnit<out Any?>) {
        referenceNotifications.push().setValue(homeUnit.makeNotification())
    }
    // endregion

    // region Home
    // region HW Unit
    /**
     *  Saves/updates given @see[HwUnit] as a hardware module list in DB
     */
    fun saveHardwareUnit(hwUnit: HwUnit): Task<Void>? {
        return referenceHWUnits?.child(hwUnit.name)?.setValue(hwUnit)
    }

    /**
     *  Deletes given @see[HwUnit] from hardware module list in DB
     */
    fun deleteHardwareUnit(hwUnit: HwUnit): Task<Void>? {
        return referenceHWUnits?.child(hwUnit.name)?.removeValue()
    }

    // region HW Unit errors and their restarts

    /**
     *  Adds given @see[HwUnitLog] to the hw Error lg @see[HW_ERROR_INFORMATION_BASE] list in DB
     */
    fun addHwUnitErrorEvent(hwUnitError: HwUnitLog<out Any>) {
        referenceHwError?.child(hwUnitError.name)?.setValue(hwUnitError)
    }

    /**
     * Clear all hw Error entries from DB
     */
    fun clearHwErrors() {
        referenceHwError?.removeValue()
    }

    /**
     * Clear hw Error Event entry from DB
     */
    fun clearHwErrorEvent(hwUnitName: String) {
        referenceHwError?.child(hwUnitName)?.removeValue()
    }

    fun addHwUnitToRestart(hwUnitError: HwUnitLog<out Any>) {
        referenceHwRestart?.child(hwUnitError.name)?.setValue(hwUnitError)
    }

    fun addHwUnitListToRestart(hwUnitErrorList: List<HwUnitLog<out Any>>) {
        val pairs = hwUnitErrorList.map { it.name to it }
        referenceHwRestart?.setValue(pairs.toMap())
    }

    fun clearHwRestarts() {
        referenceHwRestart?.removeValue()
    }

    fun clearHwRestartEvent(hwUnitName: String) {
        referenceHwRestart?.child(hwUnitName)?.removeValue()
    }

    // endregion
    // endregion

    // region Room
    /**
     *  Saves/updates given @see[Room] in DB
     */
    fun saveRoom(room: Room): Task<Void>? {
        return referenceRooms?.child(room.name)?.setValue(room)
    }

    /**
     *  Deletes given @see[Room] from DB
     */
    fun deleteRoom(roomName: String): Task<Void>? {
        return referenceRooms?.child(roomName)?.removeValue()
    }
    // endregion

    // region HomeUnit
    /**
     *  Saves/updates given @see[HomeUnit] in DB
     */
    fun <T> saveHomeUnit(homeUnit: HomeUnit<T>): Task<Void>? {
        Timber.w("saveHomeUnit $homeUnit")
        //return referenceHomeUnits?.child("${homeUnit.type}/${homeUnit.name}")?.setValue(homeUnit)
        return homePathReference?.let {
            FirebaseDatabase.getInstance().getReference("$it/$HOME_UNITS_BASE/${homeUnit.type}/${homeUnit.name}")
                    .setValue(homeUnit)
        }
    }

    /**
     *  Deletes given @see[HomeUnit] from DB
     */
    fun <T> deleteHomeUnit(homeUnit: HomeUnit<T>): Task<Void>? {
        //return referenceHomeUnits?.child("${homeUnit.type}/${homeUnit.name}")?.setValue(homeUnit)
        return homePathReference?.let {
            FirebaseDatabase.getInstance().getReference("$it/$HOME_UNITS_BASE/${homeUnit.type}/${homeUnit.name}")
                    .removeValue()
        }
    }
    // endregion

    // region UnitTask
    /**
     *  Saves/updates given @see[UnitTask] in DB
     */
    fun saveUnitTask(homeUnitType: String, homeUnitName: String, unitTask: UnitTask): Task<Void>? {
        return homePathReference?.let {
            FirebaseDatabase.getInstance().getReference(
                            "$it/$HOME_UNITS_BASE/$homeUnitType/$homeUnitName/$HOME_UNIT_TASKS/${unitTask.name}")
                    .setValue(unitTask)
        }
    }

    /**
     *  Deletes given @see[UnitTask] from DB
     */
    fun deleteUnitTask(homeUnitType: String, homeUnitName: String,
                       unitTask: UnitTask): Task<Void>? {
        return homePathReference?.let {
            FirebaseDatabase.getInstance().getReference(
                            "$it/$HOME_UNITS_BASE/$homeUnitType/$homeUnitName/$HOME_UNIT_TASKS/${unitTask.name}")
                    .removeValue()
        }
    }
    // endregion

    // region Preferences
    /**
     *  Sets Firebase homePreference key/Value
     */
    fun setHomePreference(key: String, value: Any?) {
        referenceHomePreferences?.child(key)?.setValue(value)
    }

    /**
     *  Gets Firebase homePreference Value for given key
     */
    fun getHomePreference(key: String): DatabaseReference? {
        return referenceHomePreferences?.child(key)
    }
    // endregion

    // region Online and last online time
    /**
     * start monitoring for Firebase Connection active
     */
    fun startHomeToFirebaseConnectionActiveMonitor() {
        homePathReference?.run {
            // Since I can connect from multiple devices, we store each connection instance separately
            // any time that connectionsRef's value is null (i.e. has no children) I am offline
            val myConnectionsRef = FirebaseDatabase.getInstance().getReference("$this/$HOME_ONLINE")

            // Stores the timestamp of my last disconnect (the last time I was seen online)
            val lastOnlineRef =
                    FirebaseDatabase.getInstance().getReference("$this/$HOME_LAST_ONLINE_TIME")

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

    /**
     * Checks if Home Module is online
     */
    fun isHomeOnline(): LiveData<Boolean?> = homePathReference?.let {
        FirebaseDBLiveData("$it/$HOME_ONLINE").getObjectLiveData()
    } ?: MutableLiveData()

    /**
     * Checks when Home Module was last online
     */
    fun lastHomeOnlineTime(): LiveData<Long?> = homePathReference?.let {
        FirebaseDBLiveData("$it/$HOME_LAST_ONLINE_TIME").getObjectLiveData()
    } ?: MutableLiveData()
    // endregion

    // region Logs
    /**
     *  Adds given @see[HwUnitLog] to the log @see[LOG_INFORMATION_BASE] list in DB
     */
    fun logUnitEvent(hwUnitLog: HwUnitLog<out Any>) {
        referenceLog?.push()?.setValue(hwUnitLog)
    }

    /**
     * Clear all Logs entries from DB
     */
    fun clearLog() {
        homePathReference?.let {
            referenceLog?.removeValue()
        }
    }

    /**
     * Clear all Logs entries from DB
     */
    fun clearLog(child: String) {
        homePathReference?.let {
            referenceLog?.child(child)?.removeValue()
        }
    }

    fun logsFlow(): Flow<Map<String, HwUnit>> {
        return genericMapReferenceFlow(referenceLog)
    }
    // endregion

    // region restarts

    fun setResetAppFlag(): Task<Void>? {
        return homePathReference?.let { FirebaseDatabase.getInstance().getReference(it) }?.child(RESTART_APP)?.setValue(true)
    }

    fun clearResetAppFlag(): Task<Void>? {
        return homePathReference?.let { FirebaseDatabase.getInstance().getReference(it) }?.child(RESTART_APP)?.removeValue()
    }

    // endregion

    // endregion

    // endregion

    // region LiveData/Flow

    // region HW Units
    /**
     * get [Flow] of [HwUnit] List for listening to changes in Room entries in DB
     */
    fun hwUnitListFlow(): Flow<List<HwUnit>> {
        return genericListReferenceFlow(referenceHWUnits)
    }

    /**
     * get instance of @see[HwUnitsLiveData] for listening to changes in Room entries in DB
     */
    fun hwUnitsLiveData(): HwUnitsLiveData {
        return HwUnitsLiveData(referenceHWUnits)
    }

    /**
     * get instance of @see[HwUnitsLiveData] for listening to changes in entries in DB
     */
    @Deprecated("please use hwUnitFlow")
    fun hwUnitLiveData(hwUnitName: String): HwUnitLiveData {
        return HwUnitLiveData(referenceHWUnits, hwUnitName)
    }
    fun hwUnitFlow(hwUnitName: String): Flow<HwUnit> {
        return genericReferenceFlow(referenceHWUnits?.child(hwUnitName))
    }

    // region HW Unit Error/Restart
    /**
     * get [Flow] of [HwUnitLog] List for listening to changes HwUnit Error Event List in DB
     */
    fun hwUnitErrorEventListFlow(): Flow<List<HwUnitLog<Any>>> {
        return genericListReferenceFlow(referenceHwError)
    }

    /**
     * get [Flow] of [HwUnitLog] List for listening to request to restart hw units
     */
    fun hwUnitRestartListFlow(): Flow<List<HwUnitLog<Any>>> {
        return genericListReferenceFlow(referenceHwRestart)
    }
    // endregion

    // endregion

    // region Room
    /**
     * get instance of @see[RoomListLiveData] for listening to changes in Room entries in DB
     */
    @Deprecated("please use roomListFlow")
    fun roomListLiveData(): RoomListLiveData {
        return RoomListLiveData(referenceRooms)
    }

    /**
     * get instance of @see[RoomListLiveData] for listening to changes in Room entries in DB
     */
    @ExperimentalCoroutinesApi
    fun roomListFlow(): Flow<List<Room>> {
        return genericListReferenceFlow(referenceRooms)
    }

    /**
     * get instance of @see[RoomLiveData] for listening to changes in specific Room entry in DB
     */
    fun roomLiveData(roomName: String): RoomLiveData {
        return RoomLiveData(referenceRooms, roomName)
    }
    // endregion

    // region HomeUnit
    /**
     * get instance of @see[HomeUnitListLiveData] for listening to changes in entries in DB
     */
    @Deprecated("please use homeUnitListFlow")
    fun homeUnitListLiveData(unitType: String): HomeUnitListLiveData {
        return HomeUnitListLiveData(homePathReference, unitType)
    }

    /**
     * get instance of @see[HomeUnitListLiveData] for listening to changes in entries in DB
     */
    @ExperimentalCoroutinesApi
    fun homeUnitListFlow(unitType: String? = null): Flow<List<HomeUnit<Any?>>> {
        return homePathReference?.let {home ->
            if (unitType != null) {
                FirebaseDatabase.getInstance().getReference("$home/$HOME_UNITS_BASE/$unitType").let { reference ->
                    genericListReferenceFlow(reference)
                }
            } else {
                combine(HOME_STORAGE_UNITS.map { type ->
                    FirebaseDatabase.getInstance().getReference("$home/$HOME_UNITS_BASE/$type").let { reference ->
                        genericListReferenceFlow<HomeUnit<Any?>>(reference)
                    }
                }){types ->
                    types.flatMap {it}
                }
            }
        } ?: emptyFlow()
    }

    /**
     * get instance of @see[HomeUnitsLiveData] for listening to changes in entries in DB
     */
    fun homeUnitsLiveData(): HomeUnitsLiveData {
        return HomeUnitsLiveData(homePathReference)
    }

    /**
     * get instance of @see[HomeUnitsLiveData] for listening to changes in entries in DB
     */
    fun homeUnitsLiveData(roomName: String): HomeUnitsLiveData {
        return HomeUnitsLiveData(homePathReference, roomName)
    }

    /**
     * get instance of @see[HomeUnitLiveData] for listening to changes in given HomeUnit in DB
     */
    fun homeUnitLiveData(unitType: String, unitName: String): HomeUnitLiveData {
        return HomeUnitLiveData(homePathReference, unitType, unitName)
    }

    // endregion

    // region UnitTask
    /**
     * get instance of @see[UnitTaskListLiveData] for listening to changes in specific HomeUnit UnitTask List entry in DB
     */
    @Deprecated("please use unitTaskListFlow")
    fun unitTaskListLiveData(unitType: String, unitName: String): UnitTaskListLiveData {
        return UnitTaskListLiveData(homePathReference, unitType, unitName)
    }

    /**
     * get instance of @see[UnitTaskListLiveData] for listening to changes in specific HomeUnit UnitTask List entry in DB
     */
    fun unitTaskListFlow(unitType: String, unitName: String): Flow<Map<String, UnitTask>> {
        return homePathReference?.let { home ->
            FirebaseDatabase.getInstance().getReference("$home/$HOME_UNITS_BASE/$unitType/$unitName/$HOME_UNIT_TASKS").let { reference ->
                genericListReferenceFlow<UnitTask>(reference)
            }.map { list ->
                list.associateBy { it.name }
            }
        } ?: emptyFlow()
    }
    // endregion

    // region restarts

    @ExperimentalCoroutinesApi
    fun restartAppFlow(): Flow<Boolean> {
        return homePathReference?.let { home ->
            FirebaseDatabase.getInstance().getReference("$home/$RESTART_APP").let { reference ->
                genericReferenceFlow(reference)
            }
        } ?: emptyFlow()
    }

    // endregion

    fun getHomes(): LiveData<List<String>> = HomesListLiveData(
            // TODO: this will overLoad FirebaseDB
            // need to add separate node with only rooms list
            FirebaseDatabase.getInstance().reference.child(HOME_INFORMATION_BASE))
}
