package com.krisbiketeam.smarthomeraspbpi3

import androidx.lifecycle.LiveData
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.krisbiketeam.smarthomeraspbpi3.common.Analytics
import com.krisbiketeam.smarthomeraspbpi3.common.FULL_DAY_IN_MILLIS
import com.krisbiketeam.smarthomeraspbpi3.common.getOnlyTodayLocalTime
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017Pin
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ChildEventType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.*
import com.krisbiketeam.smarthomeraspbpi3.units.*
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.hardware.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random


const val EVENT_SENSOR_EXCEPTION = "sensor_exception"
const val EVENT_SENSOR_READ_VALUE = "sensor_read_value"
const val EVENT_SENSOR_SET_VALUE = "sensor_set_value"
const val EVENT_SENSOR_CLOSE = "sensor_close"
const val EVENT_SENSOR_CONNECT = "sensor_connect"
const val EVENT_REGISTER_LISTENER = "sensor_register_listener"

const val SENSOR_LOG_MESSAGE = "log_message"
const val SENSOR_ERROR = "error"
const val SENSOR_NAME = "sensor"
const val SENSOR_VALUE = "value"

class Home(private val secureStorage: SecureStorage,
           private val homeInformationRepository: FirebaseHomeInformationRepository,
           private val analytics: Analytics) :
        Sensor.HwUnitListener<Any> {
    private val scope = CoroutineScope(Dispatchers.IO)

    private var lifecycleStartStopJob: Job? = null

    private val homeUnitsList: MutableMap<Pair<String, String>, HomeUnit<Any>> = ConcurrentHashMap()

    private val hwUnitsList: MutableMap<String, BaseHwUnit<Any>> = ConcurrentHashMap()
    private val hwUnitsListMutex = Mutex()

    private val hwUnitErrorEventList: MutableMap<String, Triple<Long, Int, BaseHwUnit<Any>?>> =
            ConcurrentHashMap()
    private val hwUnitErrorEventListMutex = Mutex()


    private val alarmEnabledLiveData: LiveData<Boolean> = secureStorage.alarmEnabledLiveData
    private var alarmEnabled: Boolean = secureStorage.alarmEnabled

    private var booleanApplyFunction: suspend HomeUnit<in Boolean>.(Any) -> Unit = { newVal: Any ->
        if (newVal is Boolean) {
            unitsTasks.values.forEach { task ->
                booleanTaskApply(newVal, task, this.name)
            }
        } else {
            Timber.e("booleanApplyFunction new value is not Boolean or is null")
        }
    }

    private var sensorApplyFunction: suspend HomeUnit<in Float>.(Any) -> Unit = { newVal: Any ->
        if (newVal is Float) {
            unitsTasks.values.forEach { task ->
                sensorTaskApply(newVal, task, this.name)
            }
        } else {
            Timber.e("sensorApplyFunction new value is not Float or is null")
        }
    }

    @ExperimentalCoroutinesApi
    fun start() {
        Timber.d("start; hwUnitsList.size: ${hwUnitsList.size}")
        lifecycleStartStopJob = scope.launch {
            // get the full lists first synchronously
            // first HwUnitErrorList
            hwUnitErrorEventListDataProcessor(homeInformationRepository.hwUnitErrorEventListFlow().first())
            // second HwUnitList which bases on HwUnitErrorList
            Timber.i("start: get initial hwUnitList")
            homeInformationRepository.hwUnitListFlow().first().forEach { hwUnit ->
                Timber.i("start: get initial hwUnitList add/start Hw Unit:${hwUnit.name}")
                hwUnitsDataProcessor(ChildEventType.NODE_ACTION_ADDED to hwUnit)
            }

            Timber.i("start: get initial homeUnitList")
            // and finally HomeUnitList which bases on HwUnitList
            homeInformationRepository.homeUnitListFlow().first().forEach { homeUnit ->
                Timber.i("start: get initial homeUnitList add:${homeUnit.name}")
                homeUnitsList[homeUnit.type to homeUnit.name] = homeUnit
            }

            launch(Dispatchers.IO) {
                Timber.i("start: start listen to homeUnitsFlow")
                homeInformationRepository.homeUnitsFlow().distinctUntilChanged().collect {
                    // why I need to launch new coroutine? why hwUnitStart blocks completely
                    launch { homeUnitsDataProcessor(it) }
                }
            }
            launch(Dispatchers.IO) {
                Timber.i("start: start listen to hwUnitsFlow")
                homeInformationRepository.hwUnitsFlow().distinctUntilChanged().collect {
                    // why I need to launch new coroutine? comment above
                    launch { hwUnitsDataProcessor(it) }
                }
            }
            launch(Dispatchers.IO) {
                Timber.i("start: start listen to hwUnitErrorEventListFlow")
                homeInformationRepository.hwUnitErrorEventListFlow().distinctUntilChanged().collect {
                    hwUnitErrorEventListDataProcessor(it)
                }
            }
            launch(Dispatchers.IO) {
                Timber.i("start: start listen to hwUnitRestartListFlow")
                homeInformationRepository.hwUnitRestartListFlow().distinctUntilChanged().collect {
                    hwUnitRestartListProcessor(it)
                }
            }
        }
        alarmEnabledLiveData.observeForever {
            alarmEnabled = it
        }
    }

    suspend fun stop() {
        Timber.e("stop; hwUnitsList.size: ${hwUnitsList.size}")
        lifecycleStartStopJob?.cancel()
        hwUnitsList.values.forEach { hwUnitStop(it) }
        homeUnitsList.values.forEach { homeUnit ->
            homeUnit.unitsTasks.values.forEach { it.taskJob?.cancel() }
        }
    }

    private suspend fun homeUnitsDataProcessor(pair: Pair<ChildEventType, HomeUnit<Any>>) {
        pair.let { (action, homeUnit) ->
            Timber.d("homeUnitsDataProcessor changed: $pair")
            when (action) {
                ChildEventType.NODE_ACTION_CHANGED -> {
                    Timber.d("homeUnitsDataProcessor NODE_ACTION_CHANGED NEW:  $homeUnit")
                    homeUnitsList[homeUnit.type to homeUnit.name]?.run {
                        Timber.d("homeUnitsDataProcessor NODE_ACTION_CHANGED EXISTING: $this}")
                        // set previous apply function to new homeUnit
                        homeUnit.applyFunction = applyFunction
                        unitsTasks.forEach { (key, value) ->
                            if (homeUnit.unitsTasks.contains(key)) {
                                homeUnit.unitsTasks[key]?.taskJob = value.taskJob
                            } else {
                                value.taskJob?.cancel()
                            }
                        }
                        homeUnit.value?.let { newValue ->
                            if (newValue != value) {
                                hwUnitsList[homeUnit.hwUnitName]?.let { hwUnit ->
                                    Timber.d(
                                            "homeUnitsDataProcessor NODE_ACTION_CHANGED hwUnit: ${hwUnit.hwUnit}")
                                    // Actuators can be changed from remote mobile App so apply HomeUnitState to hwUnitState if it changed
                                    if (hwUnit is Actuator) {
                                        Timber.d(
                                                "homeUnitsDataProcessor NODE_ACTION_CHANGED baseUnit setValue newValue: $newValue")

                                        hwUnit.setValueWithException(newValue)
                                        homeUnit.lastUpdateTime = hwUnit.valueUpdateTime

                                        homeInformationRepository.logHwUnitEvent(HwUnitLog(hwUnit.hwUnit, newValue, "homeUnitsDataProcessor", hwUnit.valueUpdateTime))
                                    }
                                }
                                homeUnit.applyFunction(newValue)

                                if (homeUnit.shouldFirebaseNotify(newValue)) {
                                    Timber.d(
                                            "homeUnitsDataProcessor NODE_ACTION_CHANGED notify with FCM Message")
                                    homeInformationRepository.notifyHomeUnitEvent(homeUnit)
                                }
                            }
                        }
                        homeUnitsList[homeUnit.type to homeUnit.name] = homeUnit
                    }
                }
                ChildEventType.NODE_ACTION_ADDED -> {
                    val existingUnit = homeUnitsList[homeUnit.type to homeUnit.name]
                    Timber.d(
                            "homeUnitsDataProcessor NODE_ACTION_ADDED EXISTING $existingUnit ; NEW  $homeUnit")
                    when (homeUnit.type) {
                        HOME_ACTUATORS, HOME_LIGHT_SWITCHES, HOME_REED_SWITCHES, HOME_MOTIONS -> {
                            Timber.d(
                                    "homeUnitsDataProcessor NODE_ACTION_ADDED set boolean apply function")
                            homeUnit.applyFunction = booleanApplyFunction
                        }
                        HOME_TEMPERATURES, HOME_PRESSURES, HOME_HUMIDITY, HOME_GAS, HOME_GAS_PERCENT, HOME_IAQ, HOME_STATIC_IAQ, HOME_CO2, HOME_BREATH_VOC -> {
                            Timber.d(
                                    "homeUnitsDataProcessor NODE_ACTION_ADDED set sensor apply function")
                            homeUnit.applyFunction = sensorApplyFunction
                        }
                    }
                    // Set/Update HhUnit States according to HomeUnit state and vice versa
                    hwUnitsList[homeUnit.hwUnitName]?.let { hwUnit ->
                        Timber.d("homeUnitsDataProcessor NODE_ACTION_ADDED hwUnit: $hwUnit")
                        if (homeUnit.value != hwUnit.unitValue) {
                            if (hwUnit is Actuator) {
                                Timber.d(
                                        "homeUnitsDataProcessor NODE_ACTION_ADDED baseUnit setValue value: ${homeUnit.value}")
                                homeUnit.value?.let { value ->
                                    hwUnit.setValueWithException(value)
                                    homeUnit.lastUpdateTime = hwUnit.valueUpdateTime
                                }
                            } else if (hwUnit is Sensor) {
                                homeUnit.updateHomeUnitValuesAndTimes(hwUnit.unitValue, hwUnit.valueUpdateTime)
                                homeUnit.lastTriggerSource = LAST_TRIGGER_SOURCE_HOME_UNIT_ADDED
                                homeInformationRepository.saveHomeUnit(homeUnit)
                            }
                        }
                    }
                    homeUnit.value?.let { value ->
                        homeUnit.applyFunction(homeUnit, value)
                    }
                    homeUnitsList[homeUnit.type to homeUnit.name] = homeUnit
                }
                ChildEventType.NODE_ACTION_DELETED -> {
                    val result = homeUnitsList.remove(homeUnit.type to homeUnit.name)
                    Timber.d("homeUnitsDataProcessor NODE_ACTION_DELETED: $result")
                }
                else -> {
                    Timber.e("homeUnitsDataProcessor unsupported action: $action")
                }
            }
        }
    }

    private suspend fun hwUnitsDataProcessor(pair: Pair<ChildEventType, HwUnit>) {
        pair.let { (action, hwUnit) ->
            Timber.d("hwUnitsDataObserver changed: $pair")
            when (action) {
                ChildEventType.NODE_ACTION_CHANGED -> {
                    hwUnitsList[hwUnit.name]?.let {
                        Timber.w("hwUnitsDataObserver NODE_ACTION_CHANGED HwUnit already exist stop existing one")
                        hwUnitStop(it)
                    }
                    if (hwUnitErrorEventList.contains(hwUnit.name)) {
                        Timber.w(
                                "hwUnitsDataObserver NODE_ACTION_CHANGED remove from ErrorEventList value: ${hwUnit.name}")
                        hwUnitErrorEventList.remove(hwUnit.name)
                        homeInformationRepository.clearHwErrorEvent(hwUnit.name)
                        homeInformationRepository.clearHwRestartEvent(hwUnit.name)
                    }
                    createHwUnit(hwUnit)?.let {
                        Timber.w("hwUnitsDataObserver HwUnit recreated connect and eventually listen to it")
                        hwUnitStart(it)
                    }
                }
                ChildEventType.NODE_ACTION_ADDED -> {
                    // consider this unit is already present in hwUnitsList
                    hwUnitsList[hwUnit.name]?.let {
                        Timber.w("hwUnitsDataObserver NODE_ACTION_ADDED HwUnit already exist stop old one:")
                        //hwUnitStop(it)
                        return@hwUnitsDataProcessor
                    }
                    if (hwUnitErrorEventList.contains(hwUnit.name)) {
                        Timber.w("hwUnitsDataObserver NODE_ACTION_ADDED HwUnit is on HwErrorList do not start it")
                        hwUnitErrorEventList.compute(hwUnit.name) { _, triple ->
                            val newHwUnit = createHwUnit(hwUnit)
                            newHwUnit?.let {
                                triple?.run {
                                    Triple(first, second, it)
                                }
                            }
                        }
                    } else {
                        createHwUnit(hwUnit)?.let {
                            Timber.w(
                                    "hwUnitsDataObserver NODE_ACTION_ADDED HwUnit connect and eventually listen to it")
                            hwUnitStart(it)
                        }
                    }
                }
                ChildEventType.NODE_ACTION_DELETED -> {
                    hwUnitsList[hwUnit.name]?.let {
                        hwUnitStop(it)
                    }
                    val result = hwUnitsList.remove(hwUnit.name)
                    if (hwUnitErrorEventList.contains(hwUnit.name)) {
                        Timber.w(
                                "hwUnitsDataObserver NODE_ACTION_DELETED remove from ErrorEventList value: ${hwUnit.name}")
                        hwUnitErrorEventList.remove(hwUnit.name)
                        homeInformationRepository.clearHwErrorEvent(hwUnit.name)
                        homeInformationRepository.clearHwRestartEvent(hwUnit.name)
                    }
                    Timber.d("hwUnitsDataObserver HwUnit NODE_ACTION_DELETED: $result")

                }
                else -> {
                    Timber.e("hwUnitsDataObserver unsupported action: $action")
                }
            }
            Timber.d("hwUnitsDataObserver END: $pair")
        }
    }

    private suspend fun hwUnitErrorEventListDataProcessor(errorEventList: List<HwUnitLog<Any>>) {
        Timber.e(
                "hwUnitErrorEventListDataProcessor errorEventList: $errorEventList; errorEventList.size: ${errorEventList.size}")
        if (errorEventList.isNotEmpty()) {
            errorEventList.forEach { hwUnitErrorEvent ->
                val baseHwUnit = hwUnitsList.remove(hwUnitErrorEvent.name)?.also {
                    hwUnitStop(it)
                }
                hwUnitErrorEventList.compute(hwUnitErrorEvent.name) { _, existing ->
                    Triple(existing?.first ?: hwUnitErrorEvent.localtime, Int.MAX_VALUE, baseHwUnit
                            ?: existing?.third)
                }
            }
        } else {
            val unitToStart = hwUnitErrorEventListMutex.withLock {
                val list = hwUnitErrorEventList.values.mapNotNull { (_, _, hwUnit) ->
                    hwUnit
                }
                hwUnitErrorEventList.clear()
                list
            }
            unitToStart.forEach { hwUnit ->
                delay(Random.nextLong(10, 100))
                hwUnitStart(hwUnit)
            }
        }
    }

    private suspend fun hwUnitRestartListProcessor(restartEventList: List<HwUnitLog<Any>>) {
        Timber.e("hwUnitRestartListProcessor $restartEventList")
        if (!restartEventList.isNullOrEmpty()) {
            homeInformationRepository.clearHwRestarts()
            val removedHwUnitList = restartEventList.mapNotNull { hwUnitLog ->
                hwUnitsList.remove(hwUnitLog.name)?.also { hwUnit ->
                    hwUnitStop(hwUnit)
                }
            }
            Timber.d(
                    "hwUnitRestartListProcessor restartEventList.size: ${restartEventList.size} ; restarted count (no error Units) ${removedHwUnitList.size}; restartEventList: $restartEventList")
            removedHwUnitList.forEach { hwUnit ->
                delay(Random.nextLong(10, 100))
                hwUnitStart(hwUnit)
            }
        }
    }

    private suspend fun restartHwUnits() {
        val restartHwUnitList = hwUnitsListMutex.withLock {
            val list = hwUnitsList.values.toList()
            hwUnitsList.clear()
            list
        }
        restartHwUnitList.forEach { this.hwUnitStop(it) }
        Timber.d(
                "restartHwUnits restarted count (no error Units) ${restartHwUnitList.size}; removedHwUnitList: $restartHwUnitList")
        restartHwUnitList.forEach { hwUnit ->
            delay(Random.nextLong(10, 100))
            hwUnitStart(hwUnit)
        }
    }

    override suspend fun onHwUnitChanged(hwUnit: HwUnit, unitValue: Any?, updateTime: Long) {
        Timber.d("onHwUnitChanged unit: $hwUnit; unitValue: $unitValue; updateTime: ${
            Date(updateTime)
        }")
        //TODO :disable logging as its can overload firebase DB
        homeInformationRepository.logHwUnitEvent(HwUnitLog(hwUnit, unitValue, "onHwUnitChanged", updateTime))

// TODO: IS THIS NEEDED TO RUN NEW SCOPE
//        scope.launch {
        homeUnitsList.values.filter {
            if (it.type != HOME_LIGHT_SWITCHES) {
                it.hwUnitName == hwUnit.name
            } else {
                it.secondHwUnitName == hwUnit.name
            }
        }.forEach { homeUnit ->
            homeUnit.updateHomeUnitValuesAndTimes(unitValue, updateTime)
            val newValue = if (homeUnit.type != HOME_LIGHT_SWITCHES) homeUnit.value else homeUnit.secondValue
            if (newValue != null) {
                homeUnit.applyFunction(homeUnit, newValue)
            }
            homeUnit.lastTriggerSource = "${LAST_TRIGGER_SOURCE_HW_UNIT}_from_${hwUnit.name}"
            homeInformationRepository.saveHomeUnit(homeUnit)
            if (homeUnit.shouldFirebaseNotify(newValue)) {
                Timber.d("onHwUnitChanged notify with FCM Message")
                homeInformationRepository.notifyHomeUnitEvent(homeUnit)
            }
        }
        // remove possible error from hwUnitErrorEventList for successful read of hwUnit
// TODO: SHOULD THIS BE HERE?
        if (unitValue != null) {
            hwUnitErrorEventList.remove(hwUnit.name)
        }
//        }
    }

    override suspend fun onHwUnitError(hwUnit: HwUnit, error: String, updateTime: Long) {
        Timber.d("onHwUnitError unit: $hwUnit; error: $error; updateTime: ${
            Date(updateTime)
        }")
// TODO: IS THIS NEEDED TO RUN NEW SCOPE
        //scope.launch {
        hwUnitsList[hwUnit.name]?.addHwUnitErrorEvent(Throwable(), "Error on $hwUnit : error")
        //}
    }

    private fun createHwUnit(hwUnit: HwUnit): BaseHwUnit<Any>? {
        return when (hwUnit.type) {
            BoardConfig.TEMP_SENSOR_TMP102 -> {
                HwUnitI2CTempTMP102Sensor(hwUnit.name, hwUnit.location, hwUnit.pinName,
                        hwUnit.softAddress ?: 0,
                        hwUnit.refreshRate) as BaseHwUnit<Any>
            }
            BoardConfig.TEMP_SENSOR_MCP9808 -> {
                HwUnitI2CTempMCP9808Sensor(hwUnit.name, hwUnit.location, hwUnit.pinName,
                        hwUnit.softAddress ?: 0,
                        hwUnit.refreshRate) as BaseHwUnit<Any>
            }
            BoardConfig.TEMP_RH_SENSOR_SI7021 -> {
                HwUnitI2CTempRhSi7021Sensor(hwUnit.name, hwUnit.location, hwUnit.pinName,
                        hwUnit.softAddress ?: 0,
                        hwUnit.refreshRate) as BaseHwUnit<Any>
            }
            BoardConfig.TEMP_RH_SENSOR_AM2320 -> {
                HwUnitI2CTempRhAm2320Sensor(hwUnit.name, hwUnit.location, hwUnit.pinName,
                        hwUnit.softAddress ?: 0,
                        hwUnit.refreshRate) as BaseHwUnit<Any>
            }
            BoardConfig.AIR_QUALITY_SENSOR_BME680 -> {
                HwUnitI2CAirQualityBme680Sensor(secureStorage, hwUnit.name, hwUnit.location, hwUnit.pinName,
                        hwUnit.softAddress ?: 0,
                        hwUnit.refreshRate) as BaseHwUnit<Any>
            }
            BoardConfig.LIGHT_SENSOR_BH1750 -> {
                HwUnitI2CAmbientLightBH1750Sensor(hwUnit.name, hwUnit.location, hwUnit.pinName,
                        hwUnit.softAddress ?: 0,
                        hwUnit.refreshRate) as BaseHwUnit<Any>
            }
            BoardConfig.PRESS_TEMP_SENSOR_LPS331 -> {
                HwUnitI2CPressTempLps331Sensor(hwUnit.name, hwUnit.location, hwUnit.pinName,
                        hwUnit.softAddress ?: 0,
                        hwUnit.refreshRate) as BaseHwUnit<Any>
            }
            BoardConfig.TEMP_PRESS_SENSOR_BMP280 -> {
                HwUnitI2CTempPressBMP280Sensor(hwUnit.name, hwUnit.location, hwUnit.pinName,
                        hwUnit.softAddress ?: 0,
                        hwUnit.refreshRate) as BaseHwUnit<Any>
            }
            BoardConfig.IO_EXTENDER_MCP23017_INPUT -> {
                MCP23017Pin.Pin.values().find {
                    it.name == hwUnit.ioPin
                }?.let { ioPin ->
                    HwUnitI2CMCP23017Sensor(hwUnit.name, hwUnit.location, hwUnit.pinName,
                            hwUnit.softAddress ?: 0, hwUnit.pinInterrupt ?: "",
                            ioPin,
                            hwUnit.internalPullUp ?: false, hwUnit.inverse
                            ?: false) as BaseHwUnit<Any>
                }
            }
            BoardConfig.IO_EXTENDER_MCP23017_OUTPUT -> {
                MCP23017Pin.Pin.values().find {
                    it.name == hwUnit.ioPin
                }?.let { ioPin ->
                    HwUnitI2CMCP23017Actuator(hwUnit.name, hwUnit.location, hwUnit.pinName,
                            hwUnit.softAddress ?: 0, ioPin, hwUnit.inverse
                            ?: false) as BaseHwUnit<Any>
                }
            }
            else -> null
        }
    }

    private suspend fun hwUnitStart(unit: BaseHwUnit<Any>) {
        Timber.v("hwUnitStart connect unit: ${unit.hwUnit}")
        if (unit.connectValueWithException()) {
            when (unit) {
                is Sensor -> {
                    val readVal = unit.readValueWithException()
                    Timber.w("hwUnitStart readVal:$readVal unit.unitValue:${unit.unitValue}")
                    hwUnitsList[unit.hwUnit.name] = unit
                    unit.registerListenerWithException(this@Home)
                    Timber.v("hwUnitStart Sensor after registerListenerWithException unit: ${unit.hwUnit}")
                    if (readVal != null) {
                        onHwUnitChanged(unit.hwUnit, readVal, unit.valueUpdateTime)
                    }
                }
                is Actuator -> {
                    homeUnitsList.values.filter {
                        it.hwUnitName == unit.hwUnit.name
                    }.forEach { homeUnit ->
                        Timber.d(
                                "hwUnitStart update value based on homeUnitValue, setValue value: ${homeUnit.value}")
                        homeUnit.value?.let {
                            unit.setValueWithException(it)
                        }
                    }
                    hwUnitsList[unit.hwUnit.name] = unit
                }
            }
        }
        Timber.v("hwUnitStart END: ${unit.hwUnit}")
    }

    private suspend fun hwUnitStop(unit: BaseHwUnit<Any>, doNotAddToHwUnitErrorList: Boolean = false) {
        Timber.v("hwUnitStop close unit: ${unit.hwUnit}")
        // close will automatically unregister listener
        unit.closeValueWithException(doNotAddToHwUnitErrorList)
    }

    // region applyFunction helper methods

    private suspend fun booleanTaskApply(newVal: Boolean, task: UnitTask, homeUnitName: String) {
        scope.launch {
            Timber.v("booleanTaskApply before cancel task.taskJob:${task.taskJob} isActive:${task.taskJob?.isActive} isCancelled:${task.taskJob?.isCancelled} isCompleted:${task.taskJob?.isCompleted}")
            task.taskJob?.cancel()
            Timber.v("booleanTaskApply after cancel task.taskJob:${task.taskJob} isActive:${task.taskJob?.isActive} isCancelled:${task.taskJob?.isCancelled} isCompleted:${task.taskJob?.isCompleted}")

            if (task.disabled == true) {
                Timber.d("booleanTaskApply task not enabled $task")
            } else {
                if ((task.trigger == null || task.trigger == BOTH)
                        || (task.trigger == RISING_EDGE && newVal)
                        || (task.trigger == FALLING_EDGE && !newVal)) {
                    task.taskJob = scope.launch(Dispatchers.IO) {
                        do {
                            booleanTaskTimed(newVal, task, homeUnitName)
                        } while (this.isActive && task.periodically == true && ((task.delay.isValidTime() && task.duration.isValidTime())
                                        || (task.startTime.isValidTime() && task.endTime.isValidTime())
                                        || (task.startTime.isValidTime() && task.duration.isValidTime())))
                    }
                    Timber.v("booleanTaskApply after booleanTaskTimed task.taskJob:${task.taskJob} isActive:${task.taskJob?.isActive} isCancelled:${task.taskJob?.isCancelled} isCompleted:${task.taskJob?.isCompleted}")
                } else if (task.resetOnInverseTrigger == true) {
                    booleanApplyAction(newVal, task.inverse, task.periodicallyOnlyHw, task.homeUnitsList, task.name, homeUnitName)
                }
            }
        }
    }

    private suspend fun sensorTaskApply(newVal: Float, task: UnitTask, homeUnitName: String) {
        scope.launch {
            if (task.disabled == true) {
                Timber.d("sensorTaskApply task not enabled $task")
                task.taskJob?.cancel()
            } else {
                task.threshold?.let { threshold ->
                    if ((task.trigger == null || task.trigger == BOTH
                                    || task.trigger == RISING_EDGE) && newVal >= threshold + (task.hysteresis
                                    ?: 0f)) {
                        task.taskJob?.cancel()
                        task.taskJob = scope.launch(Dispatchers.IO) {
                            booleanTaskTimed(true, task, homeUnitName)
                        }
                    } else if ((task.trigger == null || task.trigger == BOTH
                                    || task.trigger == FALLING_EDGE) && newVal <= threshold - (task.hysteresis
                                    ?: 0f)) {
                        task.taskJob?.cancel()
                        task.taskJob = scope.launch(Dispatchers.IO) {
                            booleanTaskTimed(false, task, homeUnitName)
                        }
                    }
                }
            }
        }
    }

    private suspend fun booleanTaskTimed(newVal: Boolean, task: UnitTask, homeUnitName: String) {
        task.startTime.takeIf { it.isValidTime() }?.let { startTime ->
            val currTime = System.currentTimeMillis().getOnlyTodayLocalTime()
            task.endTime.takeIf { it.isValidTime() }?.let { endTime ->
                Timber.e("booleanTaskTimed startTime:$startTime endTime:$endTime currTime: $currTime")
                if (startTime < endTime) {
                    if (currTime < startTime) {
                        delay(startTime - currTime)
                        booleanApplyAction(newVal, task.inverse, task.periodicallyOnlyHw, task.homeUnitsList, task.name, homeUnitName)
                        val endCurrTime = System.currentTimeMillis().getOnlyTodayLocalTime()
                        delay(endTime - endCurrTime)
                        booleanApplyAction(!newVal, task.inverse, task.periodicallyOnlyHw, task.homeUnitsList, task.name, homeUnitName)
                        delay(FULL_DAY_IN_MILLIS - endCurrTime)
                    } else if (endTime < currTime) {
                        delay(FULL_DAY_IN_MILLIS - currTime)
                    } else {
                        booleanApplyAction(newVal, task.inverse, task.periodicallyOnlyHw, task.homeUnitsList, task.name, homeUnitName)
                        val endCurrTime = System.currentTimeMillis().getOnlyTodayLocalTime()
                        delay(endTime - endCurrTime)
                        booleanApplyAction(!newVal, task.inverse, task.periodicallyOnlyHw, task.homeUnitsList, task.name, homeUnitName)
                        delay(FULL_DAY_IN_MILLIS - endCurrTime)
                    }
                } else if (endTime < startTime) {
                    if (currTime < endTime) {
                        booleanApplyAction(newVal, task.inverse, task.periodicallyOnlyHw, task.homeUnitsList, task.name, homeUnitName)
                        delay(endTime - currTime)
                        booleanApplyAction(!newVal, task.inverse, task.periodicallyOnlyHw, task.homeUnitsList, task.name, homeUnitName)
                        val endCurrTime = System.currentTimeMillis().getOnlyTodayLocalTime()
                        delay(startTime - endCurrTime)
                        booleanApplyAction(newVal, task.inverse, task.periodicallyOnlyHw, task.homeUnitsList, task.name, homeUnitName)
                        delay(FULL_DAY_IN_MILLIS - startTime)
                    } else if (currTime < startTime) {
                        delay(startTime - currTime)
                        booleanApplyAction(newVal, task.inverse, task.periodicallyOnlyHw, task.homeUnitsList, task.name, homeUnitName)
                        delay(FULL_DAY_IN_MILLIS - startTime)
                    } else {
                        booleanApplyAction(newVal, task.inverse, task.periodicallyOnlyHw, task.homeUnitsList, task.name, homeUnitName)
                        delay(FULL_DAY_IN_MILLIS - currTime)
                    }
                }
            } ?: task.duration?.let { duration ->
                booleanApplyAction(newVal, task.inverse, task.periodicallyOnlyHw, task.homeUnitsList, task.name, homeUnitName)
                delay(duration)
                booleanApplyAction(!newVal, task.inverse, task.periodicallyOnlyHw, task.homeUnitsList, task.name, homeUnitName)
            } ?: run {
                if (currTime < startTime) {
                    delay(startTime - currTime)
                }
                booleanApplyAction(newVal, task.inverse, task.periodicallyOnlyHw, task.homeUnitsList, task.name, homeUnitName)
            }
        } ?: task.endTime.takeIf { it.isValidTime() }?.let { endTime ->
            val endCurrTime = System.currentTimeMillis().getOnlyTodayLocalTime()
            if (endCurrTime < endTime) {
                delay(endTime - endCurrTime)
            }
            booleanApplyAction(!newVal, task.inverse, task.periodicallyOnlyHw, task.homeUnitsList, task.name, homeUnitName)
        } ?: task.delay.takeIf { it.isValidTime() }?.let { delay ->
            delay(delay)
            booleanApplyAction(newVal, task.inverse, task.periodicallyOnlyHw, task.homeUnitsList, task.name, homeUnitName)
            task.duration?.let { duration ->
                delay(duration)
                booleanApplyAction(!newVal, task.inverse, task.periodicallyOnlyHw, task.homeUnitsList, task.name, homeUnitName)
            }
        } ?: task.duration.takeIf { it.isValidTime() }?.let { duration ->
            booleanApplyAction(newVal, task.inverse, task.periodicallyOnlyHw, task.homeUnitsList, task.name, homeUnitName)
            delay(duration)
            booleanApplyAction(!newVal, task.inverse, task.periodicallyOnlyHw, task.homeUnitsList, task.name, homeUnitName)
        }
        ?: booleanApplyAction(newVal, task.inverse, task.periodicallyOnlyHw, task.homeUnitsList, task.name, homeUnitName)
    }

    private suspend fun booleanApplyAction(actionVal: Boolean, inverse: Boolean?, periodicallyOnlyHw: Boolean?, taskHomeUnitList: List<UnitTaskHomeUnit>, taskName: String, homeUnitName: String) {
        val newActionVal: Boolean = (inverse ?: false) xor actionVal
        taskHomeUnitList.forEach {
            homeUnitsList[it.type to it.name]?.let { taskHomeUnit ->
                Timber.d("booleanApplyAction homeUnit: $taskHomeUnit")
                hwUnitsList[taskHomeUnit.hwUnitName]?.let { taskHwUnit ->
                    Timber.d("booleanApplyAction taskHwUnit: ${taskHwUnit.hwUnit}")
                    if (taskHwUnit is Actuator && taskHwUnit.unitValue is Boolean?) {
                        if (taskHomeUnit.value != newActionVal) {
                            taskHomeUnit.value = newActionVal
                            Timber.w("booleanApplyAction taskHwUnit actionVal: $actionVal setValue value: $newActionVal periodicallyOnlyHw: $periodicallyOnlyHw")
                            taskHwUnit.setValueWithException(newActionVal, periodicallyOnlyHw != true)
                            Timber.e("booleanApplyAction after set HW Value homeUnit: $taskHomeUnit")
                            if (periodicallyOnlyHw != true) {
                                taskHomeUnit.lastUpdateTime = taskHwUnit.valueUpdateTime
                                taskHomeUnit.lastTriggerSource = "${LAST_TRIGGER_SOURCE_BOOLEAN_APPLY}_from_${homeUnitName}_home_unit_by${taskName}_task"
                                taskHomeUnit.applyFunction(taskHomeUnit, newActionVal)
                                homeInformationRepository.saveHomeUnit(taskHomeUnit)
                                // Firebase will be notified by homeUnitsDataProcessor

                                //TODO :disable logging as its can overload firebase DB
                                homeInformationRepository.logHwUnitEvent(HwUnitLog(taskHwUnit.hwUnit, newActionVal, "booleanApplyAction", taskHwUnit.valueUpdateTime))
                            }
                        }
                    }
                }
                        ?: Timber.w("booleanApplyAction taskHwUnit:${taskHomeUnit.hwUnitName}: not exist yet or anymore")
            }
                    ?: Timber.w("booleanApplyAction taskHomeUnit:${it.type}/${it.name}: not exist yet or anymore")

        }
    }

    // endregion

    // region HwUnit helperFunctions handling HwUnit Exceptions

    private suspend fun Actuator<Any>.setValueWithException(value: Any, logEvent: Boolean = true) {
        val handler = CoroutineExceptionHandler { _, exception ->
            Timber.e("setValueWithException; Error updating hwUnit value on $hwUnit $exception")
            scope.launch {
                addHwUnitErrorEvent(exception, "Error updating hwUnit value on $hwUnit")
            }
        }

        scope.launch {
            supervisorScope {
                launch(Dispatchers.IO + handler) {
                    setValue(value)
                    /*if (logEvent) {
                        analytics.logEvent(EVENT_SENSOR_SET_VALUE) {
                            param(SENSOR_NAME, this@setValueWithException.hwUnit.name)
                            param(SENSOR_VALUE, value.toString())
                        }
                    }*/
                }
            }
        }
    }

    private suspend fun Sensor<Any>.readValueWithException(): Any? {
        return supervisorScope {
            val deferred: Deferred<Any?> = async {
                readValue()
                /*.also {
                    analytics.logEvent(EVENT_SENSOR_READ_VALUE) {
                        param(SENSOR_NAME, this@readValueWithException.hwUnit.name)
                        param(SENSOR_VALUE, it.toString())
                    }
                }*/
            }
            try {
                Timber.e("readValueWithException readValue")
                deferred.await()
            } catch (e: Exception) {
                Timber.e("readValueWithException; Error reading hwUnit value on $hwUnit $e")
                addHwUnitErrorEvent(e, "Error reading hwUnit value on $hwUnit")
                null
            }
        }
    }

    private suspend fun Sensor<Any>.registerListenerWithException(
            listener: Sensor.HwUnitListener<Any>) {
        val handler = CoroutineExceptionHandler { _, error ->
            Timber.e("registerListenerWithException; Error registerListener hwUnit on $hwUnit ${error.stackTraceToString()}")
            // TODO: is this needed???
            scope.launch {
                addHwUnitErrorEvent(error, "Error registerListener hwUnit on $hwUnit")
            }
        }
        scope.launch {
            supervisorScope {
                registerListener(this, listener, handler)
                /*analytics.logEvent(EVENT_REGISTER_LISTENER) {
                param(SENSOR_NAME, this@registerListenerWithException.toString())
            }*/
                Timber.e("registerListenerWithException; supervisorScope END hwUnit on $hwUnit")

            }
        }
        Timber.e("registerListenerWithException; END hwUnit on $hwUnit")
    }

    private suspend fun BaseHwUnit<Any>.closeValueWithException(doNotAddToHwUnitErrorList: Boolean) {
        val handler = CoroutineExceptionHandler { _, exception ->
            if (doNotAddToHwUnitErrorList) {
                Timber.e("closeValueWithException; IGNORED Error closing hwUnit on $hwUnit $exception")
            } else {
                Timber.e("closeValueWithException; Error closing hwUnit on $hwUnit $exception")
                scope.launch {
                    addHwUnitErrorEvent(exception, "Error closing hwUnit on $hwUnit", doNotReStartHwUnit = true)
                }
            }
        }

        scope.launch {
            supervisorScope {
                launch(Dispatchers.IO + handler) {
                    withContext(NonCancellable) {
                        close()
                        /*analytics.logEvent(EVENT_SENSOR_CLOSE) {
                        param(SENSOR_NAME, this@closeValueWithException.hwUnit.name)
                    }*/
                    }
                }
            }
        }
    }

    private suspend fun BaseHwUnit<Any>.connectValueWithException(): Boolean {
        return supervisorScope {
            val deferred: Deferred<Boolean> = async {
                connect()
                /*analytics.logEvent(EVENT_SENSOR_CONNECT) {
                    param(SENSOR_NAME, this@connectValueWithException.hwUnit.name)
                }*/
                true
            }
            try {
                deferred.await()
            } catch (e: Exception) {
                Timber.e("connectValueWithException; Error connecting hwUnit on $hwUnit $e")
                addHwUnitErrorEvent(e, "Error connecting hwUnit on $hwUnit")
                false
            }
        }
    }

    private suspend fun BaseHwUnit<Any>.addHwUnitErrorEvent(e: Throwable, logMessage: String, doNotReStartHwUnit: Boolean = false) {
        hwUnitStop(this@addHwUnitErrorEvent, doNotAddToHwUnitErrorList = true)

        val hwUnitLog = HwUnitLog(hwUnit, unitValue, "$logMessage \n ${e.message}")

        homeInformationRepository.logHwUnitError(hwUnitLog)

        hwUnitErrorEventList[hwUnit.name] = hwUnitErrorEventList[hwUnit.name]?.let { triple ->
            Triple(hwUnitLog.localtime, triple.second.inc(), this@addHwUnitErrorEvent).apply {
                if (triple.second >= 3) {
                    hwUnitsList.remove(hwUnit.name)?.also {
                        Timber.w(
                                "addHwUnitErrorEvent to many errors ($second) from hwUnit: ${hwUnit.name}, remove it from hwUnitsList: $it")
                    }
                    homeInformationRepository.addHwUnitErrorEvent(hwUnitLog)
                    scope.launch {
                        restartHwUnits()
                    }
                } else {
                    Timber.w(
                            "addHwUnitErrorEvent another error occurred do not restart this hwUnit?:$doNotReStartHwUnit")
                    if (!doNotReStartHwUnit) {
                        scope.launch {
                            delay(100)
                            triple.third?.let {
                                hwUnitStart(it)
                            }
                        }
                    }
                }
            }
        } ?: Triple(hwUnitLog.localtime, 0, this@addHwUnitErrorEvent.also {
            Timber.w(
                    "addHwUnitErrorEvent new error occurred do not restart this hwUnit?:$doNotReStartHwUnit")
            if (!doNotReStartHwUnit) {
                scope.launch {
                    delay(100)
                    hwUnitStart(it)
                }
            }
        })
        Timber.e("addHwUnitErrorEvent hwUnitErrorEventList[hwUnit.name]:${hwUnitErrorEventList[hwUnit.name]?.third?.hwUnit?.name} ${hwUnitErrorEventList[hwUnit.name]?.second}")

        analytics.logEvent(EVENT_SENSOR_EXCEPTION) {
            param(SENSOR_NAME, this@addHwUnitErrorEvent.hwUnit.name)
            param(SENSOR_LOG_MESSAGE, logMessage)
            param(SENSOR_ERROR, e.toString())
        }
        FirebaseCrashlytics.getInstance().recordException(e)
        Timber.e(e, "addHwUnitErrorEvent finished : $logMessage")
    }

    // endregion


    // region  HomeUnit values update helper methods

    private fun HomeUnit<Any>.updateHomeUnitValuesAndTimes(unitValue: Any?, updateTime: Long) {
        // We need to handle differently values of non Basic Types
        if (unitValue is PressureAndTemperature) {
            Timber.d("Received PressureAndTemperature $unitValue")
            if (type == HOME_TEMPERATURES) {
                updateValueMinMax(unitValue.temperature, updateTime)
            } else if (type == HOME_PRESSURES) {
                updateValueMinMax(unitValue.pressure, updateTime)
            }
        } else if (unitValue is TemperatureAndHumidity) {
            Timber.d("Received TemperatureAndHumidity $unitValue")
            if (type == HOME_TEMPERATURES) {
                updateValueMinMax(unitValue.temperature, updateTime)
            } else if (type == HOME_HUMIDITY) {
                updateValueMinMax(unitValue.humidity, updateTime)
            }
        } else if (unitValue is Bme680Data) {
            Timber.d("Received TemperatureAndHumidity $unitValue")
            when (type) {
                HOME_TEMPERATURES -> {
                    updateValueMinMax(unitValue.temperature, updateTime)
                }
                HOME_HUMIDITY -> {
                    updateValueMinMax(unitValue.humidity, updateTime)
                }
                HOME_PRESSURES -> {
                    updateValueMinMax(unitValue.pressure, updateTime)
                }
                HOME_GAS -> {
                    updateValueMinMax(unitValue.gas, updateTime)
                }
                HOME_GAS_PERCENT -> {
                    updateValueMinMax(unitValue.gasPercentage, updateTime)
                }
                HOME_IAQ -> {
                    updateValueMinMax(unitValue.iaq, updateTime)
                }
                HOME_STATIC_IAQ -> {
                    updateValueMinMax(unitValue.staticIaq, updateTime)
                }
                HOME_CO2 -> {
                    updateValueMinMax(unitValue.co2Equivalent, updateTime)
                }
                HOME_BREATH_VOC -> {
                    updateValueMinMax(unitValue.breathVocEquivalent, updateTime)
                }
            }
        } else {
            updateValueMinMax(unitValue, updateTime)
        }
    }

    private fun HomeUnit<Any>.updateValueMinMax(unitValue: Any?, updateTime: Long) {
        if (type != HOME_LIGHT_SWITCHES) {
            value = unitValue
            lastUpdateTime = updateTime
        } else {
            secondValue = unitValue
            secondLastUpdateTime = updateTime
        }
        when (unitValue) {
            is Float -> {
                if (unitValue <= (min.takeIf { it is Number? } as Number?)?.toFloat() ?: Float.MAX_VALUE) {
                    min = unitValue
                    minLastUpdateTime = updateTime
                }
                if (unitValue >= (max.takeIf { it is Number? } as Number?)?.toFloat() ?: Float.MIN_VALUE) {
                    max = unitValue
                    maxLastUpdateTime = updateTime
                }
            }
        }
    }

    private fun HomeUnit<Any>.shouldFirebaseNotify(newVal: Any?): Boolean {
        return alarmEnabled && firebaseNotify &&
                (newVal !is Boolean || ((firebaseNotifyTrigger == null ||
                        firebaseNotifyTrigger == BOTH) ||
                        (firebaseNotifyTrigger == RISING_EDGE && newVal) ||
                        (firebaseNotifyTrigger == FALLING_EDGE && !newVal)))
    }

    // endregion
}

private fun Long?.isValidTime(): Boolean {
    return this != null && this > 0
}