package com.krisbiketeam.smarthomeraspbpi3

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017Pin
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ChildEventType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata.HomeUnitsLiveData
import com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata.HwUnitsLiveData
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.BaseHwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import com.krisbiketeam.smarthomeraspbpi3.units.hardware.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext


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

class Home(secureStorage: SecureStorage,
           private val homeInformationRepository: FirebaseHomeInformationRepository,
           private val firebaseAnalytics: FirebaseAnalytics) :
        Sensor.HwUnitListener<Any> {
    private var homeUnitsLiveData: HomeUnitsLiveData? = null
    private val homeUnitsList: MutableMap<Pair<String, String>, HomeUnit<Any>> = ConcurrentHashMap()

    private var hwUnitsLiveData: HwUnitsLiveData? = null
    private val hwUnitsList: MutableMap<String, BaseHwUnit<Any>> = ConcurrentHashMap()
    private val hwUnitsListMutex = Mutex()


    private var hwUnitErrorEventListJob: Job? = null
    private var hwUnitRestartListJob: Job? = null

    private val hwUnitErrorEventList: MutableMap<String, BaseHwUnit<Any>?> = ConcurrentHashMap()
    private val hwUnitErrorEventListMutex = Mutex()


    private val alarmEnabledLiveData: LiveData<Boolean> = secureStorage.alarmEnabledLiveData
    private var alarmEnabled: Boolean = secureStorage.alarmEnabled

    private var booleanApplyFunction: suspend HomeUnit<in Boolean>.(Any) -> Unit = { newVal: Any ->
        Timber.d("booleanApplyFunction newVal: $newVal called from: $this")
        if (newVal is Boolean) {
            unitsTasks.values.forEach { task ->
                homeUnitsList[task.homeUnitType to task.homeUnitName]?.let { taskHomeUnit ->
                    Timber.d("booleanApplyFunction task: $task for homeUnit: $taskHomeUnit")
                    hwUnitsList[taskHomeUnit.hwUnitName]?.let { taskHwUnit ->
                        Timber.d("booleanApplyFunction taskHwUnit: ${taskHwUnit.hwUnit}")
                        if (taskHwUnit is Actuator && taskHwUnit.unitValue is Boolean?) {
                            booleanTaskApply(newVal, task, taskHomeUnit, taskHwUnit)
                        }
                    }
                }
            }
        } else {
            Timber.e("booleanApplyFunction new value is not Boolean or is null")
        }
    }

    private var sensorApplyFunction: suspend HomeUnit<in Float>.(Any) -> Unit = { newVal: Any ->
        Timber.d("sensorApplyFunction newVal: $newVal this: $this")
        if (newVal is Float) {
            unitsTasks.values.forEach { task ->
                homeUnitsList[task.homeUnitType to task.homeUnitName]?.let { taskHomeUnit ->
                    Timber.d("sensorApplyFunction task: $task for homeUnit: $this")
                    hwUnitsList[taskHomeUnit.hwUnitName]?.let { taskHwUnit ->
                        Timber.d("sensorApplyFunction taskHwUnit: ${taskHwUnit.hwUnit}")
                        if (taskHwUnit is Actuator && taskHwUnit.unitValue is Boolean?) {
                            sensorTaskApply(newVal, task, taskHomeUnit, taskHwUnit)
                        }
                    }
                }
            }
        } else {
            Timber.e("sensorApplyFunction new value is not Float or is null")
        }
    }

    private val homeUnitsDataObserver = Observer<Pair<ChildEventType, HomeUnit<Any>>> { pair ->
        Timber.d("homeUnitsDataObserver changed: $pair")
        GlobalScope.launch(Dispatchers.Default) {
            pair?.let { (action, homeUnit) ->
                when (action) {
                    ChildEventType.NODE_ACTION_CHANGED -> {
                        Timber.d("homeUnitsDataObserver NODE_ACTION_CHANGED NEW:  $homeUnit")
                        homeUnitsList[homeUnit.type to homeUnit.name]?.run {
                            Timber.d("homeUnitsDataObserver NODE_ACTION_CHANGED EXISTING: $this}")
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
                                                "homeUnitsDataObserver NODE_ACTION_CHANGED hwUnit: ${hwUnit.hwUnit}")
                                        // Actuators can be changed from remote mobile App so apply HomeUnitState to hwUnitState if it changed
                                        if (hwUnit is Actuator) {
                                            Timber.d(
                                                    "homeUnitsDataObserver NODE_ACTION_CHANGED baseUnit setValue newValue: $newValue")
                                            hwUnit.setValueWithException(newValue)
                                            homeUnit.lastUpdateTime = hwUnit.valueUpdateTime
                                        }
                                    }
                                    homeUnit.applyFunction(newValue)

                                    if (homeUnit.firebaseNotify && alarmEnabled) {
                                        Timber.d(
                                                "homeUnitsDataObserver NODE_ACTION_CHANGED notify with FCM Message")
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
                                "homeUnitsDataObserver NODE_ACTION_ADDED EXISTING $existingUnit ; NEW  $homeUnit")
                        when (homeUnit.type) {
                            HOME_ACTUATORS, HOME_LIGHT_SWITCHES, HOME_REED_SWITCHES, HOME_MOTIONS -> {
                                Timber.d(
                                        "homeUnitsDataObserver NODE_ACTION_ADDED set boolean apply function")
                                homeUnit.applyFunction = booleanApplyFunction
                            }
                            HOME_TEMPERATURES, HOME_PRESSURES, HOME_HUMIDITY -> {
                                Timber.d(
                                        "homeUnitsDataObserver NODE_ACTION_ADDED set sensor apply function")
                                homeUnit.applyFunction = sensorApplyFunction
                            }
                        }
                        // Set/Update HhUnit States according to HomeUnit state and vice versa
                        hwUnitsList[homeUnit.hwUnitName]?.let { hwUnit ->
                            Timber.d("homeUnitsDataObserver NODE_ACTION_ADDED hwUnit: $hwUnit")
                            if (homeUnit.value != hwUnit.unitValue) {
                                if (hwUnit is Actuator) {
                                    Timber.d(
                                            "homeUnitsDataObserver NODE_ACTION_ADDED baseUnit setValue value: ${homeUnit.value}")
                                    homeUnit.value?.let { value ->
                                        hwUnit.setValueWithException(value)
                                        homeUnit.lastUpdateTime = hwUnit.valueUpdateTime

                                    }
                                } else if (hwUnit is Sensor) {
                                    updateHomeUnitValuesAndTimes(homeUnit, hwUnit.unitValue, hwUnit.valueUpdateTime)
                                    homeInformationRepository.saveHomeUnit(homeUnit)
                                }
                            }
                        }
                        homeUnitsList[homeUnit.type to homeUnit.name] = homeUnit
                    }
                    ChildEventType.NODE_ACTION_DELETED -> {
                        val result = homeUnitsList.remove(homeUnit.type to homeUnit.name)
                        Timber.d("homeUnitsDataObserver NODE_ACTION_DELETED: $result")
                    }
                    else -> {
                        Timber.e("homeUnitsDataObserver unsupported action: $action")
                    }
                }
            }
        }
    }

    private val hwUnitsDataObserver = Observer<Pair<ChildEventType, HwUnit>> { pair ->
        Timber.d("hwUnitsDataObserver changed: $pair")
        GlobalScope.launch(Dispatchers.Default) {
            pair?.let { (action, value) ->
                when (action) {
                    ChildEventType.NODE_ACTION_CHANGED -> {
                        hwUnitsList[value.name]?.let {
                            Timber.w("NODE_ACTION_CHANGED HwUnit already exist stop existing one")
                            hwUnitStop(it)
                        }
                        if (hwUnitErrorEventList.contains(value.name)) {
                            Timber.w(
                                    "hwUnitsDataObserver NODE_ACTION_CHANGED remove from ErrorEventList value: ${value.name}")
                            hwUnitErrorEventList.remove(value.name)
                            homeInformationRepository.clearHwErrorEvent(value.name)
                            homeInformationRepository.clearHwRestartEvent(value.name)
                        }
                        createHwUnit(value)?.let {
                            Timber.w("HwUnit recreated connect and eventually listen to it")
                            hwUnitStart(it)
                        }
                    }
                    ChildEventType.NODE_ACTION_ADDED -> {
                        // consider this unit is already present in hwUnitsList
                        hwUnitsList[value.name]?.let {
                            Timber.w("NODE_ACTION_ADDED HwUnit already exist stop old one:")
                            hwUnitStop(it)
                        }
                        if (hwUnitErrorEventList.contains(value.name)) {
                            Timber.w("NODE_ACTION_ADDED HwUnit is on HwErrorList do not add it")
                        } else {
                            createHwUnit(value)?.let {
                                Timber.w(
                                        "NODE_ACTION_ADDED HwUnit connect and eventually listen to it")
                                hwUnitStart(it)
                            }
                        }
                    }
                    ChildEventType.NODE_ACTION_DELETED -> {
                        hwUnitsList[value.name]?.let {
                            hwUnitStop(it)
                        }
                        val result = hwUnitsList.remove(value.name)
                        if (hwUnitErrorEventList.contains(value.name)) {
                            Timber.w(
                                    "hwUnitsDataObserver NODE_ACTION_DELETED remove from ErrorEventList value: ${value.name}")
                            hwUnitErrorEventList.remove(value.name)
                            homeInformationRepository.clearHwErrorEvent(value.name)
                            homeInformationRepository.clearHwRestartEvent(value.name)
                        }
                        Timber.d("hwUnitsDataObserver HwUnit NODE_ACTION_DELETED: $result")

                    }
                    else -> {
                        Timber.e("hwUnitsDataObserver unsupported action: $action")
                    }
                }
            }
        }
    }

    fun start() {
        Timber.e("start; hwUnitsList.size: ${hwUnitsList.size}")
        homeUnitsLiveData = homeInformationRepository.homeUnitsLiveData().apply {
            observeForever(homeUnitsDataObserver)
        }
        hwUnitsLiveData = homeInformationRepository.hwUnitsLiveData().apply {
            observeForever(hwUnitsDataObserver)
        }
        hwUnitErrorEventListJob = GlobalScope.launch(Dispatchers.Default) {
            homeInformationRepository.hwUnitErrorEventListFlow().distinctUntilChanged().collect {
                hwUnitErrorEventListDataProcessor(it)
            }
        }
        hwUnitRestartListJob = GlobalScope.launch(Dispatchers.Default) {
            homeInformationRepository.hwUnitRestartListFlow().distinctUntilChanged().collect {
                hwUnitRestartListProcessor(it)
            }
        }
        alarmEnabledLiveData.observeForever {
            alarmEnabled = it
        }
    }

    fun stop() {
        Timber.e("stop; hwUnitsList.size: ${hwUnitsList.size}")
        homeUnitsLiveData?.removeObserver(homeUnitsDataObserver)
        hwUnitsLiveData?.removeObserver(hwUnitsDataObserver)
        hwUnitErrorEventListJob?.cancel()
        hwUnitRestartListJob?.cancel()
        GlobalScope.launch(Dispatchers.Default) {
            hwUnitsList.values.forEach { hwUnitStop(it) }
        }
    }

    private suspend fun hwUnitErrorEventListDataProcessor(errorEventList: List<HwUnitLog<Any>>) {
        Timber.e(
                "hwUnitErrorEventListDataProcessor errorEventList: $errorEventList; errorEventList.size: ${errorEventList.size}")
        if (errorEventList.isNotEmpty()) {
            var newErrorOccurred = false
            errorEventList.forEach { hwUnitErrorEvent ->
                if (!hwUnitErrorEventList.containsKey(hwUnitErrorEvent.name)) {
                    // disable after first error
                    hwUnitsList.remove(hwUnitErrorEvent.name)?.let {
                        newErrorOccurred = true
                        hwUnitStop(it)
                        hwUnitErrorEventList[hwUnitErrorEvent.name] = it
                        Timber.w(
                                "hwUnitErrorEventListDataProcessor error from hwUnit: ${hwUnitErrorEvent.name}, remove it from hwUnitsList")
                    }
                }
            }
            if (newErrorOccurred) {
                // If error occurred then restart All other as they can be corrupted by error in i2c
                Timber.w(
                        "hwUnitErrorEventListDataProcessor new error occurred restart others")
                restartHwUnits()
            }
        } else {
            val unitToStart = hwUnitErrorEventListMutex.withLock {
                val list = hwUnitErrorEventList.values.toList()
                hwUnitErrorEventList.clear()
                list
            }
            unitToStart.forEach { hwUnit -> hwUnit?.let { hwUnitStart(it) } }
        }
    }

    private suspend fun hwUnitRestartListProcessor(restartEventList: List<HwUnitLog<Any>>) {
        Timber.e("hwUnitRestartListProcessor $restartEventList")
        if (!restartEventList.isNullOrEmpty()) {
            val removedHwUnitList = restartEventList.mapNotNull { hwUnitLog ->
                hwUnitsList.remove(hwUnitLog.name)?.also { hwUnit ->
                    hwUnitStop(hwUnit)
                }
            }
            Timber.d(
                    "hwUnitRestartListProcessor restartEventList.size: ${restartEventList.size} ; restarted count (no error Units) ${removedHwUnitList.size}; restartEventList: $restartEventList")
            homeInformationRepository.clearHwRestarts()
            removedHwUnitList.forEach { hwUnit ->
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
        restartHwUnitList.forEach { this.hwUnitStart(it) }
    }

    override fun onHwUnitChanged(hwUnit: HwUnit, unitValue: Any?, updateTime: Long) {
        Timber.d("onHwUnitChanged unit: $hwUnit; unitValue: $unitValue; updateTime: ${
            Date(updateTime)
        }")
        //TODO :disable logging as its can overload firebase DB
        //homeInformationRepository.logUnitEvent(HwUnitLog(hwUnit, unitValue, updateTime))

        GlobalScope.launch(Dispatchers.Default) {
            homeUnitsList.values.filter {
                if (it.type != HOME_LIGHT_SWITCHES) {
                    it.hwUnitName == hwUnit.name
                } else {
                    it.secondHwUnitName == hwUnit.name
                }
            }.forEach { homeUnit ->
                updateHomeUnitValuesAndTimes(homeUnit, unitValue, updateTime)
                val newValue = if (homeUnit.type != HOME_LIGHT_SWITCHES) homeUnit.value else homeUnit.secondValue
                if (newValue != null) {
                    homeUnit.applyFunction(homeUnit, newValue)
                }
                homeInformationRepository.saveHomeUnit(homeUnit)
                if (homeUnit.firebaseNotify && alarmEnabled) {
                    Timber.d("onHwUnitChanged notify with FCM Message")
                    homeInformationRepository.notifyHomeUnitEvent(homeUnit)
                }
            }
        }
    }

    override fun onHwUnitError(hwUnit: HwUnit, error: String, updateTime: Long) {
        Timber.d("onHwUnitError unit: $hwUnit; error: $error; updateTime: ${
            Date(updateTime)
        }")
        GlobalScope.launch(Dispatchers.Default) {
            hwUnitsList[hwUnit.name]?.addHwUnitErrorEvent(Throwable(), "Error on $hwUnit : error")
        }
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
                            hwUnit.softAddress ?: 0, hwUnit.pinInterrupt ?: "",
                            ioPin) as BaseHwUnit<Any>
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
                    hwUnitsList[unit.hwUnit.name] = unit
                    val readVal = unit.readValueWithException()
                    Timber.w("hwUnitStart readVal:$readVal unit.unitValue:${unit.unitValue}")
                    unit.registerListenerWithException(this@Home)
                    onHwUnitChanged(unit.hwUnit, readVal, unit.valueUpdateTime)
                }
                is Actuator -> {
                    hwUnitsList[unit.hwUnit.name] = unit
                    homeUnitsList.values.filter {
                        it.hwUnitName == unit.hwUnit.name
                    }.forEach { homeUnit ->
                        Timber.d(
                                "hwUnitStart update value based on homeUnitValue, setValue value: ${homeUnit.value}")
                        homeUnit.value?.let {
                            unit.setValueWithException(it)
                        }
                    }
                }
            }
        }
    }

    private suspend fun hwUnitStop(unit: BaseHwUnit<Any>) {
        Timber.v("hwUnitStop close unit: ${unit.hwUnit}")
        // close will automatically unregister listener
        unit.closeValueWithException()
    }

    // region applyFunction helper methods

    private suspend fun booleanTaskApply(newVal: Boolean, task: UnitTask, taskHomeUnit: HomeUnit<Any>, taskHwUnit: Actuator<in Boolean>) {
        Timber.e("booleanTaskApply before cancel task.taskJob:${task.taskJob} isActive:${task.taskJob?.isActive} isCancelled:${task.taskJob?.isCancelled} isCompleted:${task.taskJob?.isCompleted}")
        task.taskJob?.cancel()
        Timber.e("booleanTaskApply after cancel task.taskJob:${task.taskJob} isActive:${task.taskJob?.isActive} isCancelled:${task.taskJob?.isCancelled} isCompleted:${task.taskJob?.isCompleted}")

        if ((task.trigger == null || task.trigger == BOTH)
                || (task.trigger == RISING_EDGE && newVal)
                || (task.trigger == FALLING_EDGE && !newVal)) {
            task.taskJob = GlobalScope.launch(Dispatchers.Default) {
                booleanTaskTimed(this.coroutineContext, newVal, task, taskHomeUnit, taskHwUnit)
            }
            Timber.e("booleanTaskApply after booleanTaskTimed task.taskJob:${task.taskJob} isActive:${task.taskJob?.isActive} isCancelled:${task.taskJob?.isCancelled} isCompleted:${task.taskJob?.isCompleted}")
        } else if(task.resetOnInverseTrigger == true){
            booleanApplyAction(newVal, task.inverse, taskHomeUnit, taskHwUnit)
        }
    }

    private suspend fun sensorTaskApply(newVal: Float, task: UnitTask, taskHomeUnit: HomeUnit<Any>, taskHwUnit: Actuator<in Boolean>) {
        task.threshold?.let { threshold ->
            if (newVal >= threshold + (task.hysteresis ?: 0f)) {
                task.taskJob?.cancel()
                task.taskJob = GlobalScope.launch(Dispatchers.Default) {
                    booleanTaskTimed(this.coroutineContext, true, task, taskHomeUnit, taskHwUnit)
                }
            } else if (newVal <= threshold - (task.hysteresis ?: 0f)) {
                task.taskJob?.cancel()
                task.taskJob = GlobalScope.launch(Dispatchers.Default) {
                    booleanTaskTimed(this.coroutineContext, false, task, taskHomeUnit, taskHwUnit)
                }
            }
        }
    }

    private suspend fun booleanTaskTimed(coroutineContext: CoroutineContext, newVal: Boolean, task: UnitTask, taskHomeUnit: HomeUnit<Any>, taskHwUnit: Actuator<in Boolean>) {
        task.delay.takeIf { it != null && it > 0 }?.let { delay ->
            delay(delay)
            booleanApplyAction(newVal, task.inverse, taskHomeUnit, taskHwUnit)
            task.duration?.let { duration ->
                delay(duration)
                booleanApplyAction(!newVal, task.inverse, taskHomeUnit, taskHwUnit)
                if (task.periodically == true) {
                    withContext(coroutineContext) {
                        booleanTaskTimed(coroutineContext, newVal, task, taskHomeUnit, taskHwUnit)
                    }
                }
            }
        } ?: task.duration.takeIf { it != null && it > 0 }?.let { duration ->
            booleanApplyAction(newVal, task.inverse, taskHomeUnit, taskHwUnit)
            delay(duration)
            booleanApplyAction(!newVal, task.inverse, taskHomeUnit, taskHwUnit)
        } ?: booleanApplyAction(newVal, task.inverse, taskHomeUnit, taskHwUnit)
    }

    private suspend fun booleanApplyAction(actionVal: Boolean, inverse: Boolean?, taskHomeUnit: HomeUnit<Any>, taskHwUnit: Actuator<in Boolean>) {
        val newActionVal: Boolean = (inverse ?: false) xor actionVal
        if (taskHomeUnit.value != newActionVal) {
            taskHomeUnit.value = newActionVal
            Timber.w("booleanApplyAction taskHwUnit actionVal: $actionVal setValue value: $newActionVal")
            taskHwUnit.setValueWithException(newActionVal)
            taskHomeUnit.lastUpdateTime = taskHwUnit.valueUpdateTime
            taskHomeUnit.applyFunction(taskHomeUnit, newActionVal)
            homeInformationRepository.saveHomeUnit(taskHomeUnit)
            if (taskHomeUnit.firebaseNotify && alarmEnabled) {
                Timber.d("booleanApplyAction notify with FCM Message")
                homeInformationRepository.notifyHomeUnitEvent(taskHomeUnit)
            }
        }
    }

    // endregion

    // region HwUnit helperFunctions handling HwUnit Exceptions

    private suspend fun <T : Any> Actuator<in T>.setValueWithException(value: T) {
        try {
            withContext(Dispatchers.Main) {
                setValue(value)
            }
            firebaseAnalytics.logEvent(EVENT_SENSOR_SET_VALUE) {
                param(SENSOR_NAME, this@setValueWithException.hwUnit.name)
                param(SENSOR_VALUE, value.toString())
            }
        } catch (e: Exception) {
            addHwUnitErrorEvent(e, "Error updating hwUnit value on $hwUnit")
        }
    }

    private suspend fun <T : Any> Sensor<out T>.readValueWithException(): T? {
        return try {
            withContext(Dispatchers.Main) {
                readValue()
            }.also {
                firebaseAnalytics.logEvent(EVENT_SENSOR_READ_VALUE) {
                    param(SENSOR_NAME, this@readValueWithException.hwUnit.name)
                    param(SENSOR_VALUE, it.toString())
                }
            }
        } catch (e: Exception) {
            addHwUnitErrorEvent(e, "Error reading hwUnit value on $hwUnit")
            null
        }
    }

    private suspend fun <T : Any> Sensor<out T>.registerListenerWithException(
            listener: Sensor.HwUnitListener<T>) {
        try {
            withContext(Dispatchers.Main) {
                registerListener(listener, CoroutineExceptionHandler { _, error ->
                    GlobalScope.launch(Dispatchers.Default) {
                        addHwUnitErrorEvent(error,
                                "Error registerListener CoroutineExceptionHandler hwUnit on $hwUnit")
                    }
                })
                firebaseAnalytics.logEvent(EVENT_REGISTER_LISTENER) {
                    param(SENSOR_NAME, this@registerListenerWithException.toString())
                }
            }
        } catch (e: Exception) {
            addHwUnitErrorEvent(e, "Error registerListener hwUnit on $hwUnit")
        }
    }

    private suspend fun <T : Any> BaseHwUnit<in T>.closeValueWithException() {
        try {
            withContext(Dispatchers.Main) {
                close()
            }
            firebaseAnalytics.logEvent(EVENT_SENSOR_CLOSE) {
                param(SENSOR_NAME, this@closeValueWithException.hwUnit.name)
            }
        } catch (e: Exception) {
            addHwUnitErrorEvent(e, "Error closing hwUnit on $hwUnit")
        }
    }

    private suspend fun <T : Any> BaseHwUnit<in T>.connectValueWithException(): Boolean {
        return try {
            withContext(Dispatchers.Main) {
                connect()
            }
            firebaseAnalytics.logEvent(EVENT_SENSOR_CONNECT) {
                param(SENSOR_NAME, this@connectValueWithException.hwUnit.name)
            }
            true
        } catch (e: Exception) {
            addHwUnitErrorEvent(e, "Error connecting hwUnit on $hwUnit")
            false
        }
    }

    private suspend fun <T : Any> BaseHwUnit<in T>.addHwUnitErrorEvent(e: Throwable,
                                                                       logMessage: String) {

        if (!hwUnitErrorEventList.containsKey(hwUnit.name)) {
            hwUnitsList.remove(hwUnit.name)?.let {
                hwUnitStop(it)
                hwUnitErrorEventList[hwUnit.name] = it
            }
        }
        homeInformationRepository.addHwUnitErrorEvent(
                HwUnitLog(hwUnit, unitValue, "$logMessage \n ${e.message}", Date().toString()))

        firebaseAnalytics.logEvent(EVENT_SENSOR_EXCEPTION) {
            param(SENSOR_NAME, this@addHwUnitErrorEvent.hwUnit.name)
            param(SENSOR_LOG_MESSAGE, logMessage)
            param(SENSOR_ERROR, e.toString())
        }
        FirebaseCrashlytics.getInstance().recordException(e)
        Timber.e(e, logMessage)
    }

    // endregion

}

// region  HomeUnit values update helper methods

private fun updateHomeUnitValuesAndTimes(homeUnit: HomeUnit<Any>, unitValue: Any?, updateTime: Long) {
    // We need to handel differently values of non Basic Types
    if (unitValue is TemperatureAndPressure) {
        Timber.d("Received TemperatureAndPressure ${homeUnit.value}")
        if (homeUnit.type == HOME_TEMPERATURES) {
            updateValueMinMax(homeUnit, unitValue.temperature, updateTime)
        } else if (homeUnit.type == HOME_PRESSURES) {
            updateValueMinMax(homeUnit, unitValue.pressure, updateTime)
        }
    } else if (unitValue is TemperatureAndHumidity) {
        Timber.d("Received TemperatureAndHumidity ${homeUnit.value}")
        if (homeUnit.type == HOME_TEMPERATURES) {
            updateValueMinMax(homeUnit, unitValue.temperature, updateTime)
        } else if (homeUnit.type == HOME_HUMIDITY) {
            updateValueMinMax(homeUnit, unitValue.humidity, updateTime)
        }
    } else {
        updateValueMinMax(homeUnit, unitValue, updateTime)
    }
}

private fun updateValueMinMax(homeUnit: HomeUnit<Any>, unitValue: Any?, updateTime: Long) {
    if (homeUnit.type != HOME_LIGHT_SWITCHES) {
        homeUnit.value = unitValue
        homeUnit.lastUpdateTime = updateTime
    } else {
        homeUnit.secondValue = unitValue
        homeUnit.secondLastUpdateTime = updateTime
    }
    when (unitValue) {
        is Float -> {
            if (unitValue <= (homeUnit.min.takeIf { it is Number? } as Number?)?.toFloat() ?: Float.MAX_VALUE) {
                homeUnit.min = unitValue
                homeUnit.minLastUpdateTime = updateTime
            }
            if (unitValue >= (homeUnit.max.takeIf { it is Number? } as Number?)?.toFloat() ?: Float.MIN_VALUE) {
                homeUnit.max = unitValue
                homeUnit.maxLastUpdateTime = updateTime
            }
        }
    }
}

// endregion