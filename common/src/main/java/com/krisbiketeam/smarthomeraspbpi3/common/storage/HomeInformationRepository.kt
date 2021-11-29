package com.krisbiketeam.smarthomeraspbpi3.common.storage

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tasks.Task
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.krisbiketeam.smarthomeraspbpi3.common.resetableLazy
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.flows.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import timber.log.Timber

class FirebaseHomeInformationRepository {
    // region references
    // region Home reference
    // Reference for all home related "Units"
    private var homePathReference: String? = null
    private var userPathReference: String? = null

    private val referenceHWUnitsDelegate = resetableLazy {
        homePathReference?.let { Firebase.database.getReference("$it/$HOME_HW_UNITS_BASE") }
    }
    private val referenceHWUnits: DatabaseReference? by referenceHWUnitsDelegate

    private val referenceHwErrorDelegate = resetableLazy {
        homePathReference?.let {
            Firebase.database.getReference("$it/$HW_ERROR_INFORMATION_BASE")
        }
    }

    // Reference for all hw error events
    private val referenceHwError: DatabaseReference? by referenceHwErrorDelegate

    private val referenceHwRestartDelegate = resetableLazy {
        homePathReference?.let {
            Firebase.database.getReference("$it/$HW_RESTART_INFORMATION_BASE")
        }
    }

    // Reference for all hw unit restart events
    private val referenceHwRestart: DatabaseReference? by referenceHwRestartDelegate

    private val referenceRoomsDelegate = resetableLazy {
        homePathReference?.let { Firebase.database.getReference("$it/$HOME_ROOMS") }
    }

    // Reference for all Rooms stored remotely
    private val referenceRooms: DatabaseReference? by referenceRoomsDelegate

    private val referenceHomePreferencesDelegate = resetableLazy {
        homePathReference?.let {
            Firebase.database.getReference("$it/$HOME_PREFERENCES_BASE")
        }
    }

    // Reference for all sharedPreferences stored remotely
    private val referenceHomePreferences: DatabaseReference? by referenceHomePreferencesDelegate

    // endregion

    // region Users
    // Reference for all users
    private val referenceUsers by lazy { Firebase.database.getReference(USER_INFORMATION_BASE) }
    // endregion

    // region Notifications
    // Reference for all notifications
    private val referenceNotifications =
            Firebase.database.getReference(NOTIFICATION_INFORMATION_BASE)
    // endregion

    // endregion

    init {
        Timber.d("init FirebaseHomeInformationRepository")
        // Enable offline this causes some huge delays :(
        //Firebase.database.setPersistenceEnabled(true)

        // Keep tracking changes even if there are not active listeners
        //Firebase.database.getReference(referenceHomePath).keepSynced(true)
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

        // Keep tracking changes even if there are not active listeners
        //Firebase.database.getReference(this).keepSynced(true)
    }

    fun setUserReference(uid: String) {
        userPathReference = "$USER_INFORMATION_BASE/${uid}"
    }

    // region Firebase DB operations
    // region User and its Notification token handling

    fun writeNewUser(uid: String, name: String, email: String) {
        referenceUsers.child(uid).updateChildren(mapOf(Pair(USER_NAME, name), Pair(USER_EMAIL, email)))
    }

    fun addUserNotiToken(uid: String, token: String) {
        referenceUsers.child(uid).child(USER_NOTIFICATION_TOKENS)
                .child(token).setValue(true)
    }

    fun startUserToFirebaseConnectionActiveMonitor() {
        // Since I can connect from multiple devices, we store each connection instance separately
        // any time that connectionsRef's value is null (i.e. has no children) I am offline
        val myConnectionsRef = userPathReference?.let {
            Firebase.database.getReference("$it/$USER_ONLINE")
        }

        val connectedRef = Firebase.database.getReference(".info/connected")

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

    // endregion

    // region Notifications
    /**
     *  Adds given @see[HomeUnit] to the log @see[NOTIFICATION_INFORMATION_BASE] list in DB
     */
    fun notifyHomeUnitEvent(homeUnit: HomeUnit<out Any>) {
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

    /**
     *  Saves/updates given order of Rooms/HomeUnit of home screen in DB
     */
    fun saveRoomListOrder(listOrder: List<String>): Task<Void>? {
        return setHomePreference(HOME_ROOMS_ORDER, listOrder)
    }

    // endregion

    // region Task

    /**
     *  Saves/updates given order of Rooms/HomeUnit of home screen in DB
     */

    fun saveTaskListOrder(listOrder: List<String>): Task<Void>? {
        return setHomePreference(HOME_TASKS_ORDER, listOrder)
    }

    // endregion

    // region HomeUnit
    /**
     *  Saves/updates given @see[HomeUnit] in DB
     */
    fun saveHomeUnit(homeUnit: HomeUnit<Any>): Task<Void>? {
        Timber.w("saveHomeUnit $homeUnit")
        //return referenceHomeUnits?.child("${homeUnit.type}/${homeUnit.name}")?.setValue(homeUnit)
        return homePathReference?.let {
            Firebase.database.getReference("$it/$HOME_UNITS_BASE/${homeUnit.type}/${homeUnit.name}")
                    .setValue(homeUnit)
        }
    }

    /**
     *  Updates given @see[HomeUnit] value updateTime in DB
     */
    fun updateHomeUnitValue(homeUnit: HomeUnit<Any>): Task<Void>? {
        Timber.w("updateHomeUnitValue $homeUnit")
        return homePathReference?.let {
            Firebase.database.getReference("$it/$HOME_UNITS_BASE/${homeUnit.type}/${homeUnit.name}")
                    .let { reference ->
                        reference.child(HOME_VAL).setValue(homeUnit.value).continueWithTask {
                            reference.child(HOME_VAL_LAST_UPDATE).setValue(homeUnit.lastUpdateTime).continueWithTask {
                                reference.child(HOME_LAST_TRIGGER_SOURCE).setValue(homeUnit.lastTriggerSource)
                            }
                        }
                    }
        }
    }

    /**
     *  Updates given @see[HomeUnit] value updateTime in DB
     */
    fun updateHomeUnitValue(homeUnitType: String, homeUnitName: String, newVal: Any?): Task<Void>? {
        Timber.w("updateHomeUnitValue $homeUnitType $homeUnitName")
        return homePathReference?.let {
            Firebase.database.getReference("$it/$HOME_UNITS_BASE/$homeUnitType/$homeUnitName")
                    .let { reference ->
                        reference.child(HOME_VAL).setValue(newVal).continueWithTask {
                            reference.child(HOME_VAL_LAST_UPDATE).setValue(System.currentTimeMillis()).continueWithTask {
                                reference.child(HOME_LAST_TRIGGER_SOURCE).setValue(LAST_TRIGGER_SOURCE_DEVICE_CONTROL)
                            }
                        }
                    }
        }
    }

    /**
     *  Deletes given @see[HomeUnit] from DB
     */
    fun deleteHomeUnit(homeUnit: HomeUnit<Any>): Task<Void>? {
        //return referenceHomeUnits?.child("${homeUnit.type}/${homeUnit.name}")?.setValue(homeUnit)
        return homePathReference?.let {
            Firebase.database.getReference("$it/$HOME_UNITS_BASE/${homeUnit.type}/${homeUnit.name}")
                    .removeValue()
        }
    }

    /**
     *  Clears givens @see[HomeUnit] min value from DB
     */
    fun clearMinHomeUnitValue(homeUnit: HomeUnit<Any>): Task<Void>? {
        return homePathReference?.let {
            Firebase.database.getReference("$it/$HOME_UNITS_BASE/${homeUnit.type}/${homeUnit.name}").let { reference ->
                reference.child(HOME_MIN_VAL_LAST_UPDATE).removeValue().continueWithTask {
                    reference.child(HOME_MIN_VAL).removeValue()
                }
            }
        }
    }

    /**
     *  Clears givens @see[HomeUnit] min value from DB
     */
    fun clearMaxHomeUnitValue(homeUnit: HomeUnit<Any>): Task<Void>? {
        return homePathReference?.let {
            Firebase.database.getReference("$it/$HOME_UNITS_BASE/${homeUnit.type}/${homeUnit.name}").let { reference ->
                reference.child(HOME_MAX_VAL_LAST_UPDATE).removeValue().continueWithTask {
                    reference.child(HOME_MAX_VAL).removeValue()
                }
            }
        }
    }
    // endregion

    // region UnitTask
    /**
     *  Saves/updates given @see[UnitTask] in DB
     */
    fun saveUnitTask(homeUnitType: String, homeUnitName: String, unitTask: UnitTask): Task<Void>? {
        return homePathReference?.let {
            Firebase.database.getReference(
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
            Firebase.database.getReference(
                    "$it/$HOME_UNITS_BASE/$homeUnitType/$homeUnitName/$HOME_UNIT_TASKS/${unitTask.name}")
                    .removeValue()
        }
    }
    // endregion

    // region Preferences
    /**
     *  Sets Firebase homePreference key/Value
     */
    fun setHomePreference(key: String, value: Any?): Task<Void>? {
        return referenceHomePreferences?.child(key)?.setValue(value)
    }

    /**
     *  Gets Firebase homePreference Value for given key
     */
    fun getHomePreference(key: String): DatabaseReference? {
        return referenceHomePreferences?.child(key)
    }
    // endregion

    // region Home Online and last online time
    /**
     * start monitoring for Firebase Connection active
     */
    fun startHomeToFirebaseConnectionActiveMonitor() {
        homePathReference?.run {
            // Since I can connect from multiple devices, we store each connection instance separately
            // any time that connectionsRef's value is null (i.e. has no children) I am offline
            val myConnectionsRef = Firebase.database.getReference("$this/$HOME_ONLINE")

            // Stores the timestamp of my last disconnect (the last time I was seen online)
            val lastOnlineRef =
                    Firebase.database.getReference("$this/$HOME_LAST_ONLINE_TIME")

            val connectedRef = Firebase.database.getReference(".info/connected")

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

    // endregion

    // region Logs
    /**
     *  Adds given @see[HwUnitLog] to the log @see[LOG_INFORMATION_BASE] list in DB
     */
    fun logHwUnitEvent(hwUnitLog: HwUnitLog<out Any>) {
        homePathReference?.let {
            Firebase.database.getReference("$it/$LOG_INFORMATION_BASE/${hwUnitLog.name}/${hwUnitLog.getOnlyDateLocalTime()}/${hwUnitLog.localtime}").setValue(hwUnitLog)
        }
    }

    /**
     *  Adds given @see[HwUnitLog] to the log @see[LOG_INFORMATION_BASE] list in DB
     */
    fun logHwUnitError(hwUnitLog: HwUnitLog<out Any>) {
        homePathReference?.let {
            Firebase.database.getReference("$it/$LOG_INFORMATION_BASE/error/${hwUnitLog.name}").push().setValue(hwUnitLog)
        }
    }

    /**
     * Clear all Logs entries from DB
     */
    fun clearLog() {
        homePathReference?.let {
            Firebase.database.getReference("$it/$LOG_INFORMATION_BASE")
        }
    }

    /**
     * Clear all Logs entries from DB
     */
    fun clearLog(hwUnitLogName: String) {
        homePathReference?.let {
            Firebase.database.getReference("$it/$LOG_INFORMATION_BASE/error/$hwUnitLogName").removeValue()
        }
    }

    // endregion

    // region restarts

    fun setResetAppFlag(): Task<Void>? {
        return homePathReference?.let { Firebase.database.getReference(it) }?.child(RESTART_APP)?.setValue(true)
    }

    fun clearResetAppFlag(): Task<Void>? {
        return homePathReference?.let { Firebase.database.getReference(it) }?.child(RESTART_APP)?.removeValue()
    }

    // endregion

    // endregion

    // endregion

    // region Flow

    // region User Online

    @ExperimentalCoroutinesApi
    fun isUserOnlineFlow(): Flow<Boolean?> = isUserOnline

    private val isUserOnline: Flow<Boolean?> by lazy {
        genericReferenceFlow<Boolean?>(userPathReference?.let { Firebase.database.getReference("$it/$USER_ONLINE") }).shareIn(
                ProcessLifecycleOwner.get().lifecycleScope,
                SharingStarted.WhileSubscribed(),
                1
        )
    }

    // endregion

    // region Home

    // region HW Units
    /**
     * get [Flow] of [HwUnit] List for listening to changes in Room entries in DB
     */
    @ExperimentalCoroutinesApi
    fun hwUnitListFlow(): Flow<List<HwUnit>> {
        return genericListReferenceFlow(referenceHWUnits)
    }

    /**
     * get Flow of Pairs of @see[HwUnit] List changes for listening to changes in Room entries in DB
     */
    @ExperimentalCoroutinesApi
    fun hwUnitsFlow(): Flow<Pair<ChildEventType, HwUnit>> {
        return getHwUnitsFlow(referenceHWUnits)
    }

    /**
     * get Flow of @see[HwUnit] for listening to changes in entries in DB
     */
    @ExperimentalCoroutinesApi
    fun hwUnitFlow(hwUnitName: String, closeOnEmpty: Boolean = false): Flow<HwUnit> {
        return if(hwUnitName.isEmpty()){
            emptyFlow()
        }else {
            genericReferenceFlow(referenceHWUnits?.child(hwUnitName), closeOnEmpty)

        }
    }

    // region HW Unit Error/Restart
    /**
     * get [Flow] of [HwUnitLog] List for listening to changes HwUnit Error Event List in DB
     */
    @ExperimentalCoroutinesApi
    fun hwUnitErrorEventListFlow(): Flow<List<HwUnitLog<Any>>> = hwUnitErrorEventList

    @ExperimentalCoroutinesApi
    private val hwUnitErrorEventList: Flow<List<HwUnitLog<Any>>> by lazy {
        genericListReferenceFlow<HwUnitLog<Any>>(referenceHwError).shareIn(
                ProcessLifecycleOwner.get().lifecycleScope,
                SharingStarted.WhileSubscribed(),
                1)
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
     * get Flow of List of Rooms for listening to changes in Room entries in DB
     */
    @ExperimentalCoroutinesApi
    fun roomListFlow(): Flow<List<Room>> = roomList

    @ExperimentalCoroutinesApi
    private val roomList: Flow<List<Room>> by lazy {
        genericListReferenceFlow<Room>(referenceRooms).shareIn(
                ProcessLifecycleOwner.get().lifecycleScope,
                SharingStarted.WhileSubscribed(),
                1)
    }


    @ExperimentalCoroutinesApi
    fun roomUnitFlow(roomName: String, closeOnEmpty: Boolean = false): Flow<Room> {
        return genericReferenceFlow(referenceRooms?.child(roomName), closeOnEmpty)
    }

    @ExperimentalCoroutinesApi
    fun roomListOrderFlow() : Flow<List<String>> = roomListOrder

    @ExperimentalCoroutinesApi
    private val roomListOrder: Flow<List<String>> by lazy {
        genericListReferenceFlow<String>(getHomePreference(HOME_ROOMS_ORDER)).shareIn(
                ProcessLifecycleOwner.get().lifecycleScope,
                SharingStarted.WhileSubscribed(),
                1)
    }
    // endregion

    // region Task

    @ExperimentalCoroutinesApi
    fun taskListOrderFlow() : Flow<List<String>> = taskListOrder

    @ExperimentalCoroutinesApi
    private val taskListOrder: Flow<List<String>> by lazy {
        genericListReferenceFlow<String>(getHomePreference(HOME_TASKS_ORDER)).shareIn(
                ProcessLifecycleOwner.get().lifecycleScope,
                SharingStarted.WhileSubscribed(),
                1)
    }

    // endregion

    // region HomeUnit
    /**
     * get Flow of @see[List<HomeUnit<Any>>] for listening to changes in entries in DB
     */
    @ExperimentalCoroutinesApi
    fun homeUnitListFlow(unitType: String? = null): Flow<List<HomeUnit<Any>>> {
        return homePathReference?.let { home ->
            if (unitType != null) {
                Firebase.database.getReference("$home/$HOME_UNITS_BASE/$unitType").let { reference ->
                    genericListReferenceFlow(reference)
                }
            } else {
                combine(HOME_STORAGE_UNITS.map { type ->
                    Firebase.database.getReference("$home/$HOME_UNITS_BASE/$type").let { reference ->
                        genericListReferenceFlow<HomeUnit<Any>>(reference)
                    }
                }) { types ->
                    types.flatMap { it }
                }
            }
        } ?: emptyFlow()
    }

    /**
     * get instance of a Flow with @see[HomeUnit] List changes for listening to changes in entries in DB
     */
    @ExperimentalCoroutinesApi
    fun homeUnitsFlow(): Flow<Pair<ChildEventType, HomeUnit<Any>>> {
        return getHomeUnitsFlow(homePathReference)
    }

    /**
     * get Flow of @see[HomeUnit<Any>] for for given unit type and name for listening to changes
     * in entries in DB
     */
    @ExperimentalCoroutinesApi
    fun homeUnitFlow(unitType: String, unitName: String, closeOnEmpty: Boolean = false): Flow<HomeUnit<Any>> {
        return homePathReference?.let {
            genericReferenceFlow(Firebase.database.getReference("$it/$HOME_UNITS_BASE/$unitType/$unitName"), closeOnEmpty)
        } ?: emptyFlow()
    }

    // endregion

    // region UnitTask
    /**
     * get Flow of Map<String, UnitTask> for listening to changes in specific HomeUnit UnitTask List entry in DB
     */
    fun unitTaskListFlow(unitType: String, unitName: String): Flow<Map<String, UnitTask>> {
        return homePathReference?.let { home ->
            Firebase.database.getReference("$home/$HOME_UNITS_BASE/$unitType/$unitName/$HOME_UNIT_TASKS").let { reference ->
                genericListReferenceFlow<UnitTask>(reference)
            }.map { list ->
                list.associateBy { it.name }
            }
        } ?: emptyFlow()
    }
    // endregion

    // region Home Online and last online time

    /**
     * Checks if Home Module is online
     */
    fun isHomeOnlineFlow(): Flow<Boolean?> = isHomeOnline

    private val isHomeOnline: Flow<Boolean?> by lazy {
        genericReferenceFlow<Boolean?>(homePathReference?.let { Firebase.database.getReference("$it/$HOME_ONLINE") }).shareIn(
                ProcessLifecycleOwner.get().lifecycleScope,
                SharingStarted.WhileSubscribed(),
                1)
    }

    /**
     * Checks when Home Module was last online
     */
    fun lastHomeOnlineTimeFlow(): Flow<Long?> = lastHomeOnline

    private val lastHomeOnline: Flow<Long?> by lazy {
        genericReferenceFlow<Long?>(homePathReference?.let { Firebase.database.getReference("$it/$HOME_LAST_ONLINE_TIME") }).shareIn(
                ProcessLifecycleOwner.get().lifecycleScope,
                SharingStarted.WhileSubscribed(),
                1)
    }

    // endregion

    // region Logs

    fun logsFlow(hwUnitName:String): Flow<Map<String,Map<String,HwUnitLog<Any?>>>> {
        return homePathReference?.let {
            genericReferenceFlow(Firebase.database.getReference("$it/$LOG_INFORMATION_BASE/$hwUnitName"))
        }?: emptyFlow()
    }

    fun logsFlow(hwUnitName:String, date: Long): Flow<Map<String,HwUnitLog<Any?>>> {
        return homePathReference?.let {
            genericReferenceFlow(Firebase.database.getReference("$it/$LOG_INFORMATION_BASE/$hwUnitName/$date"))
        }?: emptyFlow()
    }

    // endregion

    // region restarts

    @ExperimentalCoroutinesApi
    fun restartAppFlow(): Flow<Boolean> {
        return homePathReference?.let { home ->
            Firebase.database.getReference("$home/$RESTART_APP").let { reference ->
                genericReferenceFlow<Boolean>(reference).distinctUntilChanged()
            }
        } ?: emptyFlow()
    }

    // endregion

    // endregion

    fun getHomesFLow(): Flow<List<String>> =
            genericListReferenceFlow(Firebase.database.reference.child(HOME_LIST))

    fun addHomeToList(homeName: String): Task<Void> = Firebase.database.reference.child(HOME_LIST).push().setValue(homeName)

    // endregion
}
