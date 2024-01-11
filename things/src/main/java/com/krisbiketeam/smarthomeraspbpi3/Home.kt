package com.krisbiketeam.smarthomeraspbpi3

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.krisbiketeam.smarthomeraspbpi3.common.Analytics
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017Pin
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ChildEventType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.BooleanApplyActionData
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnitLog
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.LAST_TRIGGER_SOURCE_BOOLEAN_APPLY
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.LAST_TRIGGER_SOURCE_HOME_UNIT_ADDED
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.LAST_TRIGGER_SOURCE_HW_UNIT
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.BaseHwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitValue
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import com.krisbiketeam.smarthomeraspbpi3.units.hardware.*
import com.krisbiketeam.smarthomeraspbpi3.utils.FirebaseDBLoggerTree
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
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

class Home(
    private val secureStorage: SecureStorage,
    private val homeInformationRepository: FirebaseHomeInformationRepository,
    private val analytics: Analytics
) :
    Sensor.HwUnitListener<Any> {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var lifecycleStartStopJob: Job? = null

    private val homeUnitsList: MutableMap<Pair<HomeUnitType, String>, HomeUnit<Any>> =
        ConcurrentHashMap()

    private val hwUnitsList: MutableMap<String, BaseHwUnit<Any>> = ConcurrentHashMap()
    private val hwUnitsListMutex = Mutex()

    private val hwUnitErrorEventList: MutableMap<String, Triple<Long, Int, BaseHwUnit<Any>?>> =
        ConcurrentHashMap()
    private val hwUnitErrorEventListMutex = Mutex()

    private var alarmEnabled: Boolean = secureStorage.alarmEnabled

    private val booleanApplyAction: suspend (BooleanApplyActionData) -> Unit =
        { applyData: BooleanApplyActionData -> booleanApplyAction(applyData) }

    @ExperimentalCoroutinesApi
    fun start() {
        Timber.d("start; hwUnitsList.size: ${hwUnitsList.size}")
        lifecycleStartStopJob = scope.launch {
            // get the full lists first synchronously
            // first HwUnitErrorList
            hwUnitErrorEventListDataProcessor(
                homeInformationRepository.hwUnitErrorEventListFlow().first()
            )
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

            launch {
                Timber.i("start: start listen to homeUnitsFlow")
                homeInformationRepository.homeUnitsFlow().distinctUntilChanged().collect {
                    // why I need to launch new coroutine? why hwUnitStart blocks completely
                    launch(Dispatchers.IO) { homeUnitsDataProcessor(it) }
                }
            }
            launch {
                Timber.i("start: start listen to hwUnitsFlow")
                homeInformationRepository.hwUnitsFlow().distinctUntilChanged().collect {
                    // why I need to launch new coroutine? comment above
                    launch(Dispatchers.IO) { hwUnitsDataProcessor(it) }
                }
            }
            launch {
                Timber.i("start: start listen to hwUnitErrorEventListFlow")
                homeInformationRepository.hwUnitErrorEventListFlow().distinctUntilChanged()
                    .collect {
                        hwUnitErrorEventListDataProcessor(it)
                    }
            }
            launch {
                Timber.i("start: start listen to hwUnitRestartListFlow")
                homeInformationRepository.hwUnitRestartListFlow().distinctUntilChanged().collect {
                    hwUnitRestartListProcessor(it)
                }
            }
            launch {
                Timber.i("start: start listen to alarmEnabledFlow")
                secureStorage.alarmEnabledFlow.distinctUntilChanged().collect {
                    Timber.i("alarmEnabledFlow changed $it")
                    alarmEnabled = it
                }
            }
            launch {
                Timber.i("start: start listen to remoteLoggingLevelFlow")
                secureStorage.remoteLoggingLevelFlow.distinctUntilChanged().collect { level ->
                    Timber.i("remoteLoggingLevel changed:$level")
                    FirebaseDBLoggerTree.setMinPriority(level)
                }
            }
        }
    }

    suspend fun stop() {
        Timber.e("stop; hwUnitsList.size: ${hwUnitsList.size}")
        lifecycleStartStopJob?.cancel()
        hwUnitsList.values.forEach { hwUnitStop(it) }
        homeUnitsList.values.forEach { homeUnit ->
            homeUnit.unitsTasks.values.forEach { it.taskJob?.cancel() }
            homeUnit.unitJobs.values.forEach { it.cancel() }
        }
    }

    private suspend fun homeUnitsDataProcessor(pair: Pair<ChildEventType, HomeUnit<Any>>) {
        Timber.d("homeUnitsDataProcessor changed: $pair")
        val (action, newHomeUnit)  = pair
        when (action) {
            ChildEventType.NODE_ACTION_CHANGED -> {
                Timber.d("homeUnitsDataProcessor NODE_ACTION_CHANGED NEW:  $newHomeUnit")

                val newValue = newHomeUnit.value
                var updateValue = false
                val existingHomeUnit = homeUnitsList.compute(newHomeUnit.type to newHomeUnit.name){ _, existingHomeUnit ->
                    Timber.d("homeUnitsDataProcessor NODE_ACTION_CHANGED EXISTING: $existingHomeUnit}")

                    updateValue = newHomeUnit.value != null && newHomeUnit.value != existingHomeUnit?.value

                    if (newHomeUnit != existingHomeUnit) {
                        Timber.d("homeUnitsDataProcessor HomeUnit changed Inmutable: ${existingHomeUnit?.type} ${existingHomeUnit?.name}")
                        // set previous apply function to new homeUnit
                        existingHomeUnit?.unitsTasks?.forEach { (key, value) ->
                            if (newHomeUnit.unitsTasks.contains(key)) {
                                newHomeUnit.unitsTasks[key]?.taskJob = value.taskJob
                            } else {
                                value.taskJob?.cancel()
                            }
                        }
                        // set previous unitJobs to new homeUnit
                        newHomeUnit.unitJobs.putAll(existingHomeUnit?.unitJobs?: mutableMapOf())
                        newHomeUnit
                    } else {
                        existingHomeUnit
                    }
                }
                Timber.d("homeUnitsDataProcessor NODE_ACTION_CHANGED EXISTING: $existingHomeUnit}")

                if (updateValue && existingHomeUnit != null && newValue != null) {
                    hwUnitsList[existingHomeUnit.hwUnitName]?.let { hwUnit ->
                        Timber.d(
                            "homeUnitsDataProcessor NODE_ACTION_CHANGED hwUnit: ${hwUnit.hwUnit}"
                        )
                        // Actuators can be changed from remote mobile App so apply HomeUnitState to hwUnitState if it changed
                        if (hwUnit is Actuator) {
                            Timber.d(
                                "homeUnitsDataProcessor NODE_ACTION_CHANGED baseUnit setValue newValue: $newValue"
                            )

                            hwUnit.setValueWithException(newValue).join()

                            homeInformationRepository.logHwUnitEvent(
                                HwUnitLog(
                                    hwUnit.hwUnit,
                                    hwUnit.hwUnitValue.unitValue,
                                    "homeUnitsDataProcessor",
                                    hwUnit.hwUnitValue.valueUpdateTime
                                )
                            )

                            existingHomeUnit.updateHomeUnitValuesAndTimes(
                                hwUnit.hwUnit,
                                newValue,
                                hwUnit.hwUnitValue.valueUpdateTime,
                                newHomeUnit.lastTriggerSource?:"${LAST_TRIGGER_SOURCE_HW_UNIT}_from_${hwUnit.hwUnit.name}",
                                booleanApplyAction
                                )

                            // taskApplyFunction is suspending/blocking so need to launch new coroutine
                            scope.launch(Dispatchers.IO) {
                                Timber.d(
                                    "homeUnitsDataProcessor NODE_ACTION_CHANGED taskApplyFunction for: $existingHomeUnit newValue:$newValue"
                                )
                                existingHomeUnit.taskApplyFunction(newValue, booleanApplyAction)
                            }
                        }
                    }

                }
            }
            ChildEventType.NODE_ACTION_ADDED -> {
                val existingUnit = homeUnitsList[newHomeUnit.type to newHomeUnit.name]
                Timber.d(
                    "homeUnitsDataProcessor NODE_ACTION_ADDED EXISTING $existingUnit ; NEW  $newHomeUnit"
                )
                // set previous apply function to new homeUnit
                existingUnit?.unitsTasks?.forEach { (key, value) ->
                    if (newHomeUnit.unitsTasks.contains(key)) {
                        newHomeUnit.unitsTasks[key]?.taskJob = value.taskJob
                    } else {
                        value.taskJob?.cancel()
                    }
                }
                // set previous unitJobs to new homeUnit
                newHomeUnit.unitJobs.putAll(existingUnit?.unitJobs?: emptyMap())

                homeUnitsList[newHomeUnit.type to newHomeUnit.name] = newHomeUnit

                // Set/Update HhUnit States according to HomeUnit state and vice versa
                hwUnitsList[newHomeUnit.hwUnitName]?.let { hwUnit ->
                    Timber.d("homeUnitsDataProcessor NODE_ACTION_ADDED hwUnit: ${hwUnit.hwUnit.name} hwUnit value:${hwUnit.hwUnitValue.unitValue}")

                    val newValue = newHomeUnit.value
                    if (newValue != hwUnit.hwUnitValue.unitValue) {
                        if (hwUnit is Actuator && newValue != null) {
                            Timber.d(
                                "homeUnitsDataProcessor NODE_ACTION_ADDED baseUnit ${newHomeUnit.name} setValue value: ${newHomeUnit.value}"
                            )
                            hwUnit.setValueWithException(newValue).join()
                                homeInformationRepository.updateHomeUnitValue(
                                    newHomeUnit.type,
                                    newHomeUnit.name,
                                    newValue,
                                    hwUnit.hwUnitValue.valueUpdateTime,
                                    newHomeUnit.lastTriggerSource
                                )
                        } else if (hwUnit is Sensor && hwUnit.hwUnitValue.unitValue != null) {
                            newHomeUnit.updateHomeUnitValuesAndTimes(
                                hwUnit.hwUnit,
                                hwUnit.hwUnitValue.unitValue,
                                hwUnit.hwUnitValue.valueUpdateTime,
                                LAST_TRIGGER_SOURCE_HOME_UNIT_ADDED,
                                booleanApplyAction
                            )
                            homeInformationRepository.saveHomeUnit(newHomeUnit)
                        }
                    }
                }
                newHomeUnit.value?.let { value ->
                    // taskApplyFunction is suspending/blocking so need to launch new coroutine
                    scope.launch(Dispatchers.IO) {
                        newHomeUnit.taskApplyFunction(value, booleanApplyAction)
                    }
                }
            }
            ChildEventType.NODE_ACTION_DELETED -> {
                val result = homeUnitsList.remove(newHomeUnit.type to newHomeUnit.name)
                Timber.d("homeUnitsDataProcessor NODE_ACTION_DELETED: $result")
            }
            else -> {
                Timber.e("homeUnitsDataProcessor unsupported action: $action")
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
                        hwUnitStop(it).join()
                    }
                    if (hwUnitErrorEventList.contains(hwUnit.name)) {
                        Timber.w(
                            "hwUnitsDataObserver NODE_ACTION_CHANGED remove from ErrorEventList value: ${hwUnit.name}"
                        )
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
                        Timber.w("hwUnitsDataObserver NODE_ACTION_ADDED HwUnit already exist, return")
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
                                "hwUnitsDataObserver NODE_ACTION_ADDED HwUnit connect and eventually listen to it"
                            )
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
                            "hwUnitsDataObserver NODE_ACTION_DELETED remove from ErrorEventList value: ${hwUnit.name}"
                        )
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
            "hwUnitErrorEventListDataProcessor errorEventList.size: ${errorEventList.size}; errorEventList: ${
                errorEventList.joinToString {
                    it.name
                }
            }"
        )
        if (errorEventList.isNotEmpty()) {
            errorEventList.forEach { hwUnitErrorEvent ->
                val baseHwUnit = hwUnitsList.remove(hwUnitErrorEvent.name)?.also {
                    hwUnitStop(it)
                }
                hwUnitErrorEventList.compute(hwUnitErrorEvent.name) { _, existing ->
                    Triple(
                        existing?.first ?: hwUnitErrorEvent.localtime, Int.MAX_VALUE, baseHwUnit
                            ?: existing?.third
                    )
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
            Timber.w(
                "hwUnitErrorEventListDataProcessor unitToStart.size: ${unitToStart.size}; unitToStart: $unitToStart"
            )
            unitToStart.forEach { hwUnit ->
                delay(Random.nextLong(10, 100))
                hwUnitStart(hwUnit)
            }
        }
    }

    private suspend fun hwUnitRestartListProcessor(restartEventList: List<HwUnitLog<Any>>) {
        Timber.d("hwUnitRestartListProcessor $restartEventList")
        if (restartEventList.isNotEmpty()) {
            homeInformationRepository.clearHwRestarts()
            val removedHwUnitList = restartEventList.mapNotNull { hwUnitLog ->
                hwUnitsList.remove(hwUnitLog.name)?.also { hwUnit ->
                    hwUnitStop(hwUnit).join()
                }
            }
            Timber.d(
                "hwUnitRestartListProcessor restartEventList.size: ${restartEventList.size} ; restarted count (no error Units) ${removedHwUnitList.size}; restartEventList: $restartEventList"
            )
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
        restartHwUnitList.forEach { this.hwUnitStop(it).join() }
        Timber.d(
            "restartHwUnits restarted count (no error Units) ${restartHwUnitList.size}; removedHwUnitList: $restartHwUnitList"
        )
        restartHwUnitList.forEach { hwUnit ->
            delay(Random.nextLong(10, 100))
            hwUnitStart(hwUnit)
        }
    }

    override fun onHwUnitChanged(hwUnit: HwUnit, result: Result<HwUnitValue<Any?>>) {
        Timber.d(
            "onHwUnitChanged unit: $hwUnit; result: $result"
        )
        scope.launch(Dispatchers.IO) {
            result.onSuccess { hwUnitValue ->
                homeInformationRepository.logHwUnitEvent(
                    HwUnitLog(
                        hwUnit,
                        hwUnitValue.unitValue,
                        "onHwUnitChanged",
                        hwUnitValue.valueUpdateTime
                    )
                )

                homeUnitsList.values.filter { it.isUnitAffected(hwUnit) }.forEach { homeUnit ->
                    homeUnit.updateHomeUnitValuesAndTimes(
                        hwUnit,
                        hwUnitValue.unitValue,
                        hwUnitValue.valueUpdateTime,
                        "${LAST_TRIGGER_SOURCE_HW_UNIT}_from_${hwUnit.name}",
                        booleanApplyAction
                    )

                    val newValue = homeUnit.unitValue()
                    if (newValue != null) {
                        launch {
                            // taskApplyFunction is suspending/blocking so need to launch new coroutine
                            homeUnit.taskApplyFunction(newValue, booleanApplyAction)
                        }
                    }

                    homeInformationRepository.saveHomeUnit(homeUnit)

                    if (alarmEnabled && homeUnit.shouldFirebaseNotify(newValue)) {
                        Timber.d("onHwUnitChanged notify with FCM Message")
                        homeInformationRepository.notifyHomeUnitEvent(homeUnit)
                    }
                }
                // TODO: SHOULD THIS BE HERE?
                // remove possible error from hwUnitErrorEventList for successful read of hwUnit
                if (hwUnitValue.unitValue != null) {
                    hwUnitErrorEventList.remove(hwUnit.name)
                }
            }.onFailure {
                hwUnitsList[hwUnit.name]?.addHwUnitErrorEvent(
                    it,
                    "Error on $hwUnit"
                )
            }
        }
    }

    private fun createHwUnit(hwUnit: HwUnit): BaseHwUnit<Any>? {
        return when (hwUnit.type) {
            BoardConfig.TEMP_SENSOR_TMP102 -> {
                HwUnitI2CTempTMP102Sensor(
                    hwUnit.name, hwUnit.location, hwUnit.pinName,
                    hwUnit.softAddress ?: 0,
                    hwUnit.refreshRate
                ) as BaseHwUnit<Any>
            }
            BoardConfig.TEMP_SENSOR_MCP9808 -> {
                HwUnitI2CTempMCP9808Sensor(
                    hwUnit.name, hwUnit.location, hwUnit.pinName,
                    hwUnit.softAddress ?: 0,
                    hwUnit.refreshRate
                ) as BaseHwUnit<Any>
            }
            BoardConfig.TEMP_RH_SENSOR_SI7021 -> {
                HwUnitI2CTempRhSi7021Sensor(
                    hwUnit.name, hwUnit.location, hwUnit.pinName,
                    hwUnit.softAddress ?: 0,
                    hwUnit.refreshRate
                ) as BaseHwUnit<Any>
            }
            BoardConfig.TEMP_RH_SENSOR_AM2320 -> {
                HwUnitI2CTempRhAm2320Sensor(
                    hwUnit.name, hwUnit.location, hwUnit.pinName,
                    hwUnit.softAddress ?: 0,
                    hwUnit.refreshRate
                ) as BaseHwUnit<Any>
            }
            BoardConfig.AIR_QUALITY_SENSOR_BME680 -> {
                HwUnitI2CAirQualityBme680Sensor(
                    secureStorage, hwUnit.name, hwUnit.location, hwUnit.pinName,
                    hwUnit.softAddress ?: 0,
                    hwUnit.refreshRate
                ) as BaseHwUnit<Any>
            }
            BoardConfig.LIGHT_SENSOR_BH1750 -> {
                HwUnitI2CAmbientLightBH1750Sensor(
                    hwUnit.name, hwUnit.location, hwUnit.pinName,
                    hwUnit.softAddress ?: 0,
                    hwUnit.refreshRate
                ) as BaseHwUnit<Any>
            }
            BoardConfig.PRESS_TEMP_SENSOR_LPS331 -> {
                HwUnitI2CPressTempLps331Sensor(
                    hwUnit.name, hwUnit.location, hwUnit.pinName,
                    hwUnit.softAddress ?: 0,
                    hwUnit.refreshRate
                ) as BaseHwUnit<Any>
            }
            BoardConfig.TEMP_PRESS_SENSOR_BMP280 -> {
                HwUnitI2CTempPressBMP280Sensor(
                    hwUnit.name, hwUnit.location, hwUnit.pinName,
                    hwUnit.softAddress ?: 0,
                    hwUnit.refreshRate
                ) as BaseHwUnit<Any>
            }
            BoardConfig.IO_EXTENDER_MCP23017_INPUT -> {
                MCP23017Pin.Pin.values().find {
                    it.name == hwUnit.ioPin
                }?.let { ioPin ->
                    HwUnitI2CMCP23017Sensor(
                        hwUnit.name, hwUnit.location, hwUnit.pinName,
                        hwUnit.softAddress ?: 0, hwUnit.pinInterrupt ?: "",
                        ioPin,
                        hwUnit.internalPullUp ?: false, hwUnit.inverse
                            ?: false
                    ) as BaseHwUnit<Any>
                }
            }
            BoardConfig.IO_EXTENDER_MCP23017_OUTPUT -> {
                MCP23017Pin.Pin.values().find {
                    it.name == hwUnit.ioPin
                }?.let { ioPin ->
                    HwUnitI2CMCP23017Actuator(
                        hwUnit.name, hwUnit.location, hwUnit.pinName,
                        hwUnit.softAddress ?: 0, ioPin, hwUnit.inverse
                            ?: false
                    ) as BaseHwUnit<Any>
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
                    scope.launch(Dispatchers.IO) {
                        unit.readValue().onSuccess { hwUnitValue ->
                            Timber.w("hwUnitStart hwUnitValue:$hwUnitValue")
                            analytics.logEvent(EVENT_SENSOR_READ_VALUE) {
                                param(SENSOR_NAME, unit.hwUnit.name)
                                param(SENSOR_VALUE, hwUnitValue.unitValue.toString())
                            }
                            unit.registerListener(this@Home).onSuccess {
                                onHwUnitChanged(unit.hwUnit, Result.success(hwUnitValue))
                                analytics.logEvent(EVENT_REGISTER_LISTENER) {
                                    param(SENSOR_NAME, unit.hwUnit.name)
                                }
                            }.onFailure {
                                Timber.e("hwUnitStart; Error registering hwUnit listener on $unit $it")
                                unit.addHwUnitErrorEvent(
                                    it,
                                    "hwUnitStart; Error registering hwUnit listener on $unit"
                                )
                            }
                        }.onFailure {
                            Timber.e("hwUnitStart; Error reading hwUnit value on $unit $it")
                            unit.addHwUnitErrorEvent(
                                it,
                                "hwUnitStart; Error reading hwUnit value on $unit"
                            )
                        }
                    }
                    hwUnitsList[unit.hwUnit.name] = unit
                }
                is Actuator -> {
                    homeUnitsList.values.filter {
                        it.hwUnitName == unit.hwUnit.name
                    }.forEach { homeUnit ->
                        Timber.d(
                            "hwUnitStart update value based on homeUnitValue, setValue value: ${homeUnit.value}"
                        )
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

    private suspend fun hwUnitStop(
        unit: BaseHwUnit<Any>,
        doNotAddToHwUnitErrorList: Boolean = false
    ): Job {
        Timber.v("hwUnitStop close unit: ${unit.hwUnit}")
        // close will automatically unregister listener
        return unit.closeValueWithException(doNotAddToHwUnitErrorList)
    }

    // region applyFunction helper methods

    private suspend fun booleanApplyAction(applyData: BooleanApplyActionData) {
        Timber.d("booleanApplyAction applyData: $applyData")
        homeUnitsList[applyData.taskHomeUnitType to applyData.taskHomeUnitName]?.let { taskHomeUnit ->
            Timber.d("booleanApplyAction taskHomeUnit: $taskHomeUnit")
            hwUnitsList[taskHomeUnit.hwUnitName]?.let { taskHwUnit ->
                Timber.d("booleanApplyAction taskHwUnit: ${taskHwUnit.hwUnit} unitValue:${taskHwUnit.hwUnitValue}")
                if (taskHwUnit is Actuator && taskHwUnit.hwUnitValue.unitValue is Boolean?) {
                    if (taskHomeUnit.value != applyData.newActionVal
                        || taskHwUnit.hwUnitValue.unitValue != applyData.newActionVal) {
                        Timber.i("booleanApplyAction taskHwUnit setValue value: ${applyData.newActionVal} periodicallyOnlyHw: ${applyData.periodicallyOnlyHw}")
                        taskHwUnit.setValueWithException(
                            applyData.newActionVal,
                            !applyData.periodicallyOnlyHw
                        ).join()
                        Timber.d("booleanApplyAction after set HW Value taskHwUnit: ${taskHwUnit.hwUnit} unitValue:${taskHwUnit.hwUnitValue}")

                        taskHomeUnit.value = applyData.newActionVal
                        taskHomeUnit.lastUpdateTime = if (!applyData.periodicallyOnlyHw) taskHwUnit.hwUnitValue.valueUpdateTime else taskHomeUnit.lastUpdateTime
                        taskHomeUnit.lastTriggerSource = if (!applyData.periodicallyOnlyHw) "${LAST_TRIGGER_SOURCE_BOOLEAN_APPLY}_from_${applyData.sourceHomeUnitName}_home_unit_by_${applyData.taskName}_task" else taskHomeUnit.lastTriggerSource

                        if (!applyData.periodicallyOnlyHw) {
                            homeInformationRepository.saveHomeUnit(taskHomeUnit)
                            // Firebase will be notified by homeUnitsDataProcessor
                            homeInformationRepository.logHwUnitEvent(
                                HwUnitLog(
                                    taskHwUnit.hwUnit,
                                    applyData.newActionVal,
                                    "booleanApplyAction",
                                    taskHwUnit.hwUnitValue.valueUpdateTime
                                )
                            )
                            /*updatedTaskHomeUnit.taskApplyFunction(
                                    applyData.newActionVal,
                                    booleanApplyAction
                                )*/
                        }

                        Timber.d("booleanApplyAction after set HW Value taskHomeUnit: $taskHomeUnit")
                    }
                }
            }
                ?: Timber.w("booleanApplyAction taskHwUnit:${taskHomeUnit.hwUnitName}: not exist yet or anymore")
        }
    }

    // endregion

    // region HwUnit helperFunctions handling HwUnit Exceptions

    private suspend fun Actuator<Any>.setValueWithException(value: Any, logEvent: Boolean = true): Job {
        return scope.launch(Dispatchers.IO) {
            setValue(value).onFailure {
                addHwUnitErrorEvent(it, "Error updating hwUnit value on $hwUnit")
            }.onSuccess {
                if (logEvent) {
                    analytics.logEvent(EVENT_SENSOR_SET_VALUE) {
                        param(SENSOR_NAME, hwUnit.name)
                        param(SENSOR_VALUE, value.toString())
                    }
                }
            }
        }
    }

    private suspend fun BaseHwUnit<Any>.closeValueWithException(doNotAddToHwUnitErrorList: Boolean): Job {
        return scope.launch(Dispatchers.IO) {
            withContext(NonCancellable) {
                close().onFailure { exception ->
                    if (doNotAddToHwUnitErrorList) {
                        Timber.e("closeValueWithException; IGNORED Error closing hwUnit on $hwUnit $exception")
                    } else {
                        Timber.e("closeValueWithException; Error closing hwUnit on $hwUnit $exception")
                        addHwUnitErrorEvent(
                            exception,
                            "Error closing hwUnit on $hwUnit",
                            doNotReStartHwUnit = true
                        )
                    }
                }.onSuccess {
                    analytics.logEvent(EVENT_SENSOR_CLOSE) {
                      param(SENSOR_NAME, hwUnit.name)
                  }
                }
            }
        }
    }

    private suspend fun BaseHwUnit<Any>.connectValueWithException(): Boolean {
        return connect().onFailure {e ->
            Timber.e("connectValueWithException; Error connecting hwUnit on $hwUnit $e")
            scope.launch(Dispatchers.IO) {
                addHwUnitErrorEvent(e, "Error connecting hwUnit on $hwUnit")
            }
        }.onSuccess {
            analytics.logEvent(EVENT_SENSOR_CONNECT) {
                    param(SENSOR_NAME, hwUnit.name)
                }
        }.isSuccess
    }

    private suspend fun BaseHwUnit<Any>.addHwUnitErrorEvent(
        e: Throwable,
        logMessage: String,
        doNotReStartHwUnit: Boolean = false
    ) {
        hwUnitStop(this@addHwUnitErrorEvent, doNotAddToHwUnitErrorList = true).join()

        val hwUnitLog = HwUnitLog(hwUnit, this.hwUnitValue, "$logMessage; \n" +
                " message: ${e.message}; \n" +
                " cause: ${e.cause}; \n" +
                " stacktrace: ${e.stackTraceToString()}")

        homeInformationRepository.logHwUnitError(hwUnitLog)

        hwUnitErrorEventList[hwUnit.name] = hwUnitErrorEventList[hwUnit.name]?.let { triple ->
            Triple(hwUnitLog.localtime, triple.second.inc(), this@addHwUnitErrorEvent).apply {
                if (triple.second >= 3) {
                    hwUnitsList.remove(hwUnit.name)?.also {
                        Timber.w(
                            "addHwUnitErrorEvent to many errors ($second) from hwUnit: ${hwUnit.name}, remove it from hwUnitsList: $it"
                        )
                    }
                    homeInformationRepository.addHwUnitErrorEvent(hwUnitLog)
                    scope.launch {
                        restartHwUnits()
                    }
                } else {
                    Timber.w(
                        "addHwUnitErrorEvent another error occurred do not restart this hwUnit?:$doNotReStartHwUnit"
                    )
                    if (!doNotReStartHwUnit) {
                        scope.launch {
                            delay(5000)
                            triple.third?.let {
                                hwUnitStart(it)
                            }
                        }
                    }
                }
            }
        } ?: Triple(hwUnitLog.localtime, 0, this@addHwUnitErrorEvent.also {
            Timber.w(
                "addHwUnitErrorEvent new error occurred do not restart this hwUnit?:$doNotReStartHwUnit"
            )
            if (!doNotReStartHwUnit) {
                scope.launch {
                    delay(1000)
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
        Firebase.crashlytics.recordException(e)
        Timber.i(e, "addHwUnitErrorEvent finished : $logMessage")
    }

    // endregion
}