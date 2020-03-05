package com.krisbiketeam.smarthomeraspbpi3

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017Pin
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ChildEventType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.HomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata.HomeUnitsLiveData
import com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata.HwUnitErrorEventListLiveData
import com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata.HwUnitsLiveData
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.BaseHwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import com.krisbiketeam.smarthomeraspbpi3.units.hardware.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import kotlin.collections.HashMap

class Home(secureStorage: SecureStorage,
           private val homeInformationRepository: HomeInformationRepository) :
        Sensor.HwUnitListener<Any> {
    private var homeUnitsLiveData: HomeUnitsLiveData? = null
    private val homeUnitsList: MutableMap<String, HomeUnit<Any?>> = HashMap()

    private var hwUnitsLiveData: HwUnitsLiveData? = null
    private val hwUnitsList: MutableMap<String, BaseHwUnit<Any>> = HashMap()


    private var hwUnitErrorEventListLiveData: HwUnitErrorEventListLiveData? = null
    private val hwUnitErrorEventList: MutableMap<String, BaseHwUnit<Any>?> = HashMap()

    private var hwUnitRestartListLiveData: HwUnitErrorEventListLiveData? = null


    private val alarmEnabledLiveData: LiveData<Boolean> = secureStorage.alarmEnabledLiveData
    private var alarmEnabled: Boolean = secureStorage.alarmEnabled

    private var booleanApplyFunction: HomeUnit<in Boolean>.(Any) -> Unit = { newVal: Any ->
        Timber.d("booleanApplyFunction newVal: $newVal this: $this")
        if (newVal is Boolean) {
            unitsTasks.values.forEach { task ->
                homeUnitsList[task.homeUnitName]?.apply {
                    Timber.d("booleanApplyFunction task: $task for homeUnit: $this")
                    hwUnitsList[hwUnitName]?.let { taskHwUnit ->
                        Timber.d("booleanApplyFunction taskHwUnit: $taskHwUnit")
                        if (taskHwUnit is Actuator) {
                            value = newVal
                            Timber.d("booleanApplyFunction taskHwUnit setValue value: $value")
                            taskHwUnit.setValueWithException(newVal)
                            applyFunction(newVal)
                            homeInformationRepository.saveHomeUnit(this)
                            if (firebaseNotify && alarmEnabled) {
                                Timber.d("booleanApplyFunction notify with FCM Message")
                                homeInformationRepository.notifyHomeUnitEvent(this)
                            }
                        }
                    }
                }
            }
        } else {
            Timber.e("booleanApplyFunction new value is not Boolean or is null")
        }
    }

    private var sensorApplyFunction: HomeUnit<in Boolean>.(Any) -> Unit = { newVal: Any ->
        Timber.d("sensorApplyFunction newVal: $newVal this: $this")
        if (newVal is Float) {
            unitsTasks.values.forEach { task ->
                if (this.name == task.homeUnitName) {
                    task.delay
                } else {
                    homeUnitsList[task.homeUnitName]?.apply {
                        Timber.d("sensorApplyFunction task: $task for homeUnit: $this")
                        hwUnitsList[hwUnitName]?.let { taskHwUnit ->
                            Timber.d("sensorApplyFunction taskHwUnit: $taskHwUnit")
                            if (taskHwUnit is Actuator) {
                                value = newVal
                                Timber.d("sensorApplyFunction taskHwUnit setValue value: $value")
                                taskHwUnit.setValueWithException(newVal)
                                applyFunction(newVal)
                                homeInformationRepository.saveHomeUnit(this)
                                if (firebaseNotify && alarmEnabled) {
                                    Timber.d("sensorApplyFunction notify with FCM Message")
                                    homeInformationRepository.notifyHomeUnitEvent(this)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Timber.e("sensorApplyFunction new value is not Boolean or is null")
        }
    }


    init {
        //initHardwareUnitList()
        //initHomeUnitList()

        //saveToRepository()
    }

    fun start() {
        Timber.e("start; hwUnitsList.size: ${hwUnitsList.size}")
        homeUnitsLiveData = homeInformationRepository.homeUnitsLiveData().apply {
            observeForever(homeUnitsDataObserver)
        }
        hwUnitsLiveData = homeInformationRepository.hwUnitsLiveData().apply {
            observeForever(hwUnitsDataObserver)
        }
        hwUnitErrorEventListLiveData =
                homeInformationRepository.hwUnitErrorEventListLiveData().apply {
                    observeForever(hwUnitErrorEventListDataObserver)
                }
        hwUnitRestartListLiveData = homeInformationRepository.hwUnitRestartListLiveData().apply {
            observeForever(hwUnitRestartListLiveDataObserver)
        }
        alarmEnabledLiveData.observeForever {
            alarmEnabled = it
        }
    }

    fun stop() {
        Timber.e("stop; hwUnitsList.size: ${hwUnitsList.size}")
        homeUnitsLiveData?.removeObserver(homeUnitsDataObserver)
        hwUnitsLiveData?.removeObserver(hwUnitsDataObserver)
        hwUnitErrorEventListLiveData?.removeObserver(hwUnitErrorEventListDataObserver)
        hwUnitRestartListLiveData?.removeObserver(hwUnitRestartListLiveDataObserver)

        hwUnitsList.values.forEach(this::hwUnitStop)
    }

    private val homeUnitsDataObserver = Observer<Pair<ChildEventType, HomeUnit<Any?>>> { pair ->
        Timber.d("homeUnitsDataObserver changed: $pair")
        pair?.let { (action, homeUnit) ->
            when (action) {
                ChildEventType.NODE_ACTION_CHANGED -> {
                    Timber.d("homeUnitsDataObserver NODE_ACTION_CHANGED NEW:  $homeUnit")
                    homeUnitsList[homeUnit.name]?.run {
                        Timber.d("homeUnitsDataObserver NODE_ACTION_CHANGED EXISTING: $this}")
                        // set previous apply function to new homeUnit
                        homeUnit.applyFunction = applyFunction
                        hwUnitsList[homeUnit.hwUnitName]?.let { hwUnit ->
                            Timber.d("homeUnitsDataObserver NODE_ACTION_CHANGED hwUnit: $hwUnit")
                            // Actuators can be changed from remote mobile App so apply HomeUnitState to hwUnitState if it changed
                            if (hwUnit is Actuator) {
                                if (homeUnit.value != value) {
                                    Timber.d(
                                            "homeUnitsDataObserver NODE_ACTION_CHANGED baseUnit setValue value: ${homeUnit.value}")
                                    homeUnit.value?.let { newValue ->
                                        hwUnit.setValueWithException(newValue)
                                        homeUnit.applyFunction(newValue)
                                    }

                                    if (homeUnit.firebaseNotify && alarmEnabled) {
                                        Timber.d(
                                                "homeUnitsDataObserver NODE_ACTION_CHANGED notify with FCM Message")
                                        homeInformationRepository.notifyHomeUnitEvent(homeUnit)
                                    }
                                }
                            }
                        }
                        homeUnitsList[homeUnit.name] = homeUnit
                    }
                }
                ChildEventType.NODE_ACTION_ADDED   -> {
                    val existingUnit = homeUnitsList[homeUnit.name]
                    Timber.d(
                            "homeUnitsDataObserver NODE_ACTION_ADDED EXISTING $existingUnit ; NEW  $homeUnit")
                    when (homeUnit.type) {
                        HOME_LIGHTS, HOME_ACTUATORS, HOME_LIGHT_SWITCHES, HOME_REED_SWITCHES, HOME_MOTIONS -> {
                            Timber.d(
                                    "homeUnitsDataObserver NODE_ACTION_ADDED set boolean apply function")
                            homeUnit.applyFunction = booleanApplyFunction
                        }
                        HOME_TEMPERATURES, HOME_PRESSURES                                                  -> {
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
                                }
                            } else if (hwUnit is Sensor) {
                                homeUnit.value = hwUnit.unitValue
                                homeInformationRepository.saveHomeUnit(homeUnit)
                            }
                        }
                    }
                    homeUnitsList[homeUnit.name] = homeUnit
                    //TODO: should we also call applyFunction ???
                    homeUnit.value?.let { value ->
                        homeUnit.applyFunction(homeUnit, value)
                    }
                }
                ChildEventType.NODE_ACTION_DELETED -> {
                    val result = homeUnitsList.remove(homeUnit.name)
                    Timber.d("homeUnitsDataObserver NODE_ACTION_DELETED: $result")
                }
                else                               -> {
                    Timber.e("homeUnitsDataObserver unsupported action: $action")
                }
            }
        }
    }

    private val hwUnitsDataObserver = Observer<Pair<ChildEventType, HwUnit>> { pair ->
        Timber.d("hwUnitsDataObserver changed: $pair")
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
                ChildEventType.NODE_ACTION_ADDED   -> {
                    // consider this unit is already present in hwUnitsList
                    hwUnitsList[value.name]?.let {
                        Timber.w("NODE_ACTION_ADDED HwUnit already exist stop old one:")
                        hwUnitStop(it)
                    }
                    if (hwUnitErrorEventList.contains(value.name)) {
                        Timber.w("NODE_ACTION_ADDED HwUnit is on HwErrorList do not add it")
                    } else {
                        createHwUnit(value)?.let {
                            Timber.w("NODE_ACTION_ADDED HwUnit connect and eventually listen to it")
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
                else                               -> {
                    Timber.e("hwUnitsDataObserver unsupported action: $action")
                }
            }
        }
    }

    private val hwUnitErrorEventListDataObserver =
            Observer<List<HwUnitLog<Any>>> { errorEventList ->
                Timber.d(
                        "hwUnitErrorEventListDataObserver errorEventList: $errorEventList; errorEventList.size: ${errorEventList.size}")
                if (errorEventList.isNotEmpty()) {
                    var newErrorOccurred = false
                    errorEventList.forEach { hwUnitErrorEvent ->
                        hwUnitErrorEventList.computeIfAbsent(hwUnitErrorEvent.name) { key ->
                            // disable after first error
                            hwUnitsList.remove(key)?.also {
                                newErrorOccurred = true
                                hwUnitStop(it)
                                Timber.w(
                                        "hwUnitErrorEventListDataObserver error from hwUnit: $key, remove it from hwUnitsList")
                            }
                        }
                    }
                    if (newErrorOccurred) {
                        // If error occurred then restart All other as they can be corrupted by error in i2c
                        Timber.w(
                                "hwUnitErrorEventListDataObserver new error occurred restart others")
                        restartHwUnits()
                    }
                } else {
                    val unitToStart = hwUnitErrorEventList.values.toList()
                    hwUnitErrorEventList.clear()
                    unitToStart.forEach { hwUnit -> hwUnit?.let { hwUnitStart(it) } }
                }
            }

    private val hwUnitRestartListLiveDataObserver =
            Observer<List<HwUnitLog<Any>>> { restartEventList ->
                if (!restartEventList.isNullOrEmpty()) {
                    val removedHwUnitList = restartEventList.mapNotNull { hwUnitLog ->
                        hwUnitsList.remove(hwUnitLog.name)?.also { hwUnit ->
                            hwUnitStop(hwUnit)
                        }
                    }
                    Timber.d(
                            "hwUnitRestartListLiveDataObserver restartEventList.size: ${restartEventList.size} ; restarted count (no error Units) ${removedHwUnitList.size}; restartEventList: $restartEventList")
                    homeInformationRepository.clearHwRestarts()
                    removedHwUnitList.forEach { hwUnit ->
                        hwUnitStart(hwUnit)
                    }
                }
            }

    private fun restartHwUnits() {
        val restartHwUnitList = hwUnitsList.values.toList()
        restartHwUnitList.forEach(this::hwUnitStop)
        hwUnitsList.clear()
        Timber.d(
                "restartHwUnits restarted count (no error Units) ${restartHwUnitList.size}; removedHwUnitList: $restartHwUnitList")
        restartHwUnitList.forEach(this::hwUnitStart)
    }

    override fun onHwUnitChanged(hwUnit: HwUnit, unitValue: Any?, updateTime: String) {
        Timber.d("onHwUnitChanged unit: $hwUnit; unitValue: $unitValue; updateTime: $updateTime")
        homeInformationRepository.logUnitEvent(HwUnitLog(hwUnit, unitValue, updateTime))

        homeUnitsList.values.filter {
            it.hwUnitName == hwUnit.name
        }.forEach {
            it.apply {
                // We need to handel differently values of non Basic Types
                if (unitValue is TemperatureAndPressure) {
                    Timber.d("Received TemperatureAndPressure $value")
                    if (type == HOME_TEMPERATURES) {
                        value = unitValue.temperature
                    } else if (type == HOME_PRESSURES) {
                        value = unitValue.pressure
                    }
                } else {
                    value = unitValue
                }
                value?.let { newValue ->
                    applyFunction(newValue)
                }
                homeInformationRepository.saveHomeUnit(this)
                if (firebaseNotify && alarmEnabled) {
                    Timber.d("onHwUnitChanged notify with FCM Message")
                    homeInformationRepository.notifyHomeUnitEvent(this)
                }
            }
        }
    }

    private fun createHwUnit(hwUnit: HwUnit): BaseHwUnit<Any>? {
        return when (hwUnit.type) {
            BoardConfig.TEMP_SENSOR_TMP102          -> {
                HwUnitI2CTempTMP102Sensor(hwUnit.name, hwUnit.location, hwUnit.pinName,
                                          hwUnit.softAddress ?: 0,
                                          hwUnit.refreshRate) as BaseHwUnit<Any>
            }
            BoardConfig.TEMP_SENSOR_MCP9808         -> {
                HwUnitI2CTempMCP9808Sensor(hwUnit.name, hwUnit.location, hwUnit.pinName,
                                           hwUnit.softAddress ?: 0,
                                           hwUnit.refreshRate) as BaseHwUnit<Any>
            }
            BoardConfig.TEMP_PRESS_SENSOR_BMP280    -> {
                HwUnitI2CTempPressBMP280Sensor(hwUnit.name, hwUnit.location, hwUnit.pinName,
                                               hwUnit.softAddress ?: 0,
                                               hwUnit.refreshRate) as BaseHwUnit<Any>
            }
            BoardConfig.IO_EXTENDER_MCP23017_INPUT  -> {
                MCP23017Pin.Pin.values().find {
                    it.name == hwUnit.ioPin
                }?.let { ioPin ->
                    HwUnitI2CMCP23017Sensor(hwUnit.name, hwUnit.location, hwUnit.pinName,
                                            hwUnit.softAddress ?: 0, hwUnit.pinInterrupt ?: "",
                                            ioPin,
                                            hwUnit.internalPullUp ?: false) as BaseHwUnit<Any>
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
            else                                    -> null
        }
    }

    private fun hwUnitStart(unit: BaseHwUnit<Any>) {
        Timber.v("hwUnitStart connect unit: ${unit.hwUnit}")
        if (unit.connectValueWithException()) {
            when (unit) {
                is Sensor   -> {
                    GlobalScope.launch(Dispatchers.Main) {
                        val readVal = unit.readValueWithException()
                        Timber.w("hwUnitStart readVal:$readVal unit.unitValue:${unit.unitValue}")
                        unit.registerListenerWithException(this@Home)
                        hwUnitsList[unit.hwUnit.name] = unit
                        onHwUnitChanged(unit.hwUnit, readVal, unit.valueUpdateTime)
                    }
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

    private fun hwUnitStop(unit: BaseHwUnit<Any>) {
        Timber.v("hwUnitStop close unit: ${unit.hwUnit}")
        // close will automatically unregister listener
        unit.closeValueWithException()
    }


    // temporary Home init methods creating some units and hwUnits
    private var rooms: MutableMap<String, Room> = HashMap()

    fun saveToRepository() {
        rooms.values.forEach { homeInformationRepository.saveRoom(it) }
        homeUnitsList.values.forEach { homeInformationRepository.saveHomeUnit(it) }
        hwUnitsList.values.forEach { homeInformationRepository.saveHardwareUnit(it.hwUnit) }
    }

    private fun initHomeUnitList() {
        /*var roomName = "Kitchen"

        var temp = Temperature("Kitchen 1 Temp", HOME_TEMPERATURES, roomName, BoardConfig.TEMP_PRESS_SENSOR_BMP280) as HomeUnit<Any>

        val pressure = Pressure("Kitchen 1 Press", HOME_PRESSURES, roomName, BoardConfig.TEMP_PRESS_SENSOR_BMP280) as HomeUnit<Any>

        var light = Light("Kitchen 1 Light", HOME_LIGHTS, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B0) as HomeUnit<Any>
//        light.unitsTasks = mapOf(Pair("Turn on HW light",UnitTask(name = "Turn on HW light", hwUnitName = light.hwUnitName)))
        light.applyFunction = booleanApplyFunction

        var lightSwitch = LightSwitch("Kitchen 1 Light Switch", HOME_LIGHT_SWITCHES, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_IN_A7) as HomeUnit<Any>
        lightSwitch.unitsTasks = mapOf(Pair("Turn on light", UnitTask(name = "Turn on light", homeUnitName = light.name)))
        lightSwitch.applyFunction = booleanApplyFunction

        var reedSwitch = ReedSwitch("Kitchen 1 Reed Switch", HOME_REED_SWITCHES, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_IN_A6) as HomeUnit<Any>

        var motion = Motion("Kitchen 1 Motion Sensor", HOME_MOTIONS, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_IN_A0, firebaseNotify = true) as HomeUnit<Any>

        homeUnitsList[temp.name] = temp
        homeUnitsList[pressure.name] = pressure
        homeUnitsList[light.name] = light
        homeUnitsList[lightSwitch.name] = lightSwitch
        homeUnitsList[reedSwitch.name] = reedSwitch
        homeUnitsList[motion.name] = motion

        var room = Room(roomName, 0, listOf(light.name), listOf(lightSwitch.name), listOf(reedSwitch.name), listOf(motion.name), listOf(temp.name), listOf(pressure.name))
        rooms[room.name] = room*/

        // Second room
        /*roomName = "Bathroom"

        temp = Temperature("Bathroom 1 Temp TMP102", HOME_TEMPERATURES, roomName, BoardConfig.TEMP_SENSOR_TMP102) as HomeUnit<Any>
        val temp1 = Temperature("Bathroom 1 Temp MCP9808", HOME_TEMPERATURES, roomName, BoardConfig.TEMP_SENSOR_MCP9808) as HomeUnit<Any>

        light = Light("Bathroom 1 Light", HOME_LIGHTS, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B7, firebaseNotify = true) as HomeUnit<Any>
//        light.unitsTasks = mapOf(Pair("Turn on HW light", UnitTask(name = "Turn on HW light", hwUnitName = light.hwUnitName)))
        light.applyFunction = booleanApplyFunction

        lightSwitch = LightSwitch("Bathroom 1 Light Switch", HOME_LIGHT_SWITCHES, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_IN_A5) as HomeUnit<Any>
        lightSwitch.unitsTasks = mapOf(Pair("Turn on light", UnitTask(name = "Turn on light", homeUnitName = light.name)))
        lightSwitch.applyFunction = booleanApplyFunction

        reedSwitch = ReedSwitch("Bathroom 1 Reed Switch", HOME_REED_SWITCHES, roomName, BoardConfig.IO_EXTENDER_MCP23017_2_IN_B0) as HomeUnit<Any>
        motion = Motion("Bathroom 1 Motion Sensor", HOME_MOTIONS, roomName, BoardConfig.IO_EXTENDER_MCP23017_2_IN_A7, firebaseNotify = true) as HomeUnit<Any>

        homeUnitsList[temp.name] = temp
        homeUnitsList[temp1.name] = temp1
        homeUnitsList[light.name] = light
        homeUnitsList[lightSwitch.name] = lightSwitch
        homeUnitsList[reedSwitch.name] = reedSwitch
        homeUnitsList[motion.name] = motion

        room = Room(roomName, 0, listOf(light.name), listOf(lightSwitch.name), *//*ArrayList()*//*listOf(reedSwitch.name), *//*ArrayList()*//*listOf(motion.name), listOf(temp.name, temp1.name))
        rooms[room.name] = room*/
        var roomName = "New"

        val temp = Temperature("Bathroom 1 Temp MCP9808", HOME_TEMPERATURES, roomName,
                               BoardConfig.TEMP_SENSOR_MCP9808) as HomeUnit<Any?>
        homeUnitsList[temp.name] = temp

        var reedSwitch = ReedSwitch("New Reed Switch", HOME_REED_SWITCHES, roomName,
                                    BoardConfig.IO_EXTENDER_MCP23017_NEW_IN_B0) as HomeUnit<Any?>
        homeUnitsList[reedSwitch.name] = reedSwitch

        var light = Light("New Light", HOME_LIGHTS, roomName,
                          BoardConfig.IO_EXTENDER_MCP23017_NEW_OUT_B7) as HomeUnit<Any?>
        homeUnitsList[light.name] = light

        var homeUnits = mutableMapOf<String, MutableList<String>>()
        homeUnits[HOME_LIGHTS] = mutableListOf(light.name)
        homeUnits[HOME_TEMPERATURES] = mutableListOf(temp.name)
        homeUnits[HOME_REED_SWITCHES] = mutableListOf(reedSwitch.name)

        var room = Room(roomName, 0)
        rooms[room.name] = room
    }

    private fun initHardwareUnitList() {
        /*val motion = HwUnitGpioSensor(BoardConfig.MOTION_1, "Raspberry Pi",
                BoardConfig.MOTION_1_PIN,
                Gpio.ACTIVE_HIGH) as Sensor<Any>
        hwUnitsList[BoardConfig.MOTION_1] = motion*/

        /*val contactron = HwUnitGpioNoiseSensor(BoardConfig.REED_SWITCH_1, "Raspberry Pi", BoardConfig
                .REED_SWITCH_1_PIN, Gpio.ACTIVE_LOW) as Sensor<Any>
        hwUnitsList[BoardConfig.REED_SWITCH_1] = contactron*/

        /*val temperatureTMP102Sensor = HwUnitI2CTempTMP102Sensor(BoardConfig.TEMP_SENSOR_TMP102,
                "Raspberry Pi",
                BoardConfig.TEMP_SENSOR_TMP102_PIN,
                BoardConfig.TEMP_SENSOR_TMP102_ADDR) as Sensor<Any>
        hwUnitsList[BoardConfig.TEMP_SENSOR_TMP102] = temperatureTMP102Sensor

        val tempePressSensor = HwUnitI2CTempPressBMP280Sensor(BoardConfig.TEMP_PRESS_SENSOR_BMP280,
                "Raspberry Pi",
                BoardConfig.TEMP_PRESS_SENSOR_BMP280_PIN,
                BoardConfig.TEMP_PRESS_SENSOR_BMP280_ADDR) as Sensor<Any>
        hwUnitsList[BoardConfig.TEMP_PRESS_SENSOR_BMP280] = tempePressSensor

        val mcpLightSwitch = HwUnitI2CMCP23017Sensor(BoardConfig.IO_EXTENDER_MCP23017_1_IN_A7,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_1_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_1_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_IN_A7_PIN,
                true) as Sensor<Any>
        hwUnitsList[BoardConfig.IO_EXTENDER_MCP23017_1_IN_A7] = mcpLightSwitch

        val mcpContactron = HwUnitI2CMCP23017Sensor(BoardConfig.IO_EXTENDER_MCP23017_1_IN_A6,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_1_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_1_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_IN_A6_PIN,
                true) as Sensor<Any>
        hwUnitsList[BoardConfig.IO_EXTENDER_MCP23017_1_IN_A6] = mcpContactron

        val mcpLightSwitch2 = HwUnitI2CMCP23017Sensor(BoardConfig.IO_EXTENDER_MCP23017_1_IN_A5,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_1_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_1_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_IN_A5_PIN,
                true) as Sensor<Any>
        hwUnitsList[BoardConfig.IO_EXTENDER_MCP23017_1_IN_A5] = mcpLightSwitch2

        val mcpMotion = HwUnitI2CMCP23017Sensor(BoardConfig.IO_EXTENDER_MCP23017_1_IN_A0,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_1_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_1_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_IN_A0_PIN) as Sensor<Any>
        hwUnitsList[BoardConfig.IO_EXTENDER_MCP23017_1_IN_A0] = mcpMotion

        val mcpLed = HwUnitI2CMCP23017Actuator(BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B0,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_1_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_1_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B0_PIN) as Actuator<Any>
        hwUnitsList[BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B0] = mcpLed

        val mcpLed2 = HwUnitI2CMCP23017Actuator(BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B7,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_1_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_1_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B7_PIN) as Actuator<Any>
        hwUnitsList[BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B7] = mcpLed2*/

        /*val mcpNewContactron = HwUnitI2CMCP23017Sensor(BoardConfig.IO_EXTENDER_MCP23017_2_IN_B0,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_2_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_2_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_2_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_2_IN_B0_PIN) as Sensor<Any>
        hwUnitsList[BoardConfig.IO_EXTENDER_MCP23017_2_IN_B0] = mcpNewContactron

        val mcpNewMotion = HwUnitI2CMCP23017Sensor(BoardConfig.IO_EXTENDER_MCP23017_2_IN_A7,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_2_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_2_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_2_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_2_IN_A7_PIN) as Sensor<Any>
        hwUnitsList[BoardConfig.IO_EXTENDER_MCP23017_2_IN_A7] = mcpNewMotion*/

        val temperatureMCP9808Sensor =
                HwUnitI2CTempMCP9808Sensor(BoardConfig.TEMP_SENSOR_MCP9808, "Raspberry Pi",
                                           BoardConfig.TEMP_SENSOR_MCP9808_PIN,
                                           BoardConfig.TEMP_SENSOR_MCP9808_ADDR) as Sensor<Any>
        hwUnitsList[BoardConfig.TEMP_SENSOR_MCP9808] = temperatureMCP9808Sensor

        val mcpNewContactron =
                HwUnitI2CMCP23017Sensor(BoardConfig.IO_EXTENDER_MCP23017_NEW_IN_B0, "Raspberry Pi",
                                        BoardConfig.IO_EXTENDER_MCP23017_NEW_PIN,
                                        BoardConfig.IO_EXTENDER_MCP23017_NEW_ADDR,
                                        BoardConfig.IO_EXTENDER_MCP23017_NEW_INTA_PIN,
                                        BoardConfig.IO_EXTENDER_MCP23017_NEW_IN_B0_PIN) as Sensor<Any>
        hwUnitsList[BoardConfig.IO_EXTENDER_MCP23017_NEW_IN_B0] = mcpNewContactron

        val mcpNewLight = HwUnitI2CMCP23017Actuator(BoardConfig.IO_EXTENDER_MCP23017_NEW_OUT_B7,
                                                    "Raspberry Pi",
                                                    BoardConfig.IO_EXTENDER_MCP23017_NEW_PIN,
                                                    BoardConfig.IO_EXTENDER_MCP23017_NEW_ADDR,
                                                    BoardConfig.IO_EXTENDER_MCP23017_NEW_INTA_PIN,
                                                    BoardConfig.IO_EXTENDER_MCP23017_NEW_OUT_B7_PIN) as Actuator<Any>
        hwUnitsList[BoardConfig.IO_EXTENDER_MCP23017_NEW_OUT_B7] = mcpNewLight

    }

    private fun <T : Any> Actuator<T>.setValueWithException(value: T) {
        try {
            setValue(value)
        } catch (e: Exception) {
            addHwUnitErrorEvent(e, "Error updating hwUnit value on $hwUnit")
        }
    }

    private fun <T : Any> Sensor<T>.readValueWithException(): T? {
        return try {
            readValue()
        } catch (e: Exception) {
            addHwUnitErrorEvent(e, "Error reading hwUnit value on $hwUnit")
            null
        }
    }

    private fun <T : Any> Sensor<T>.registerListenerWithException(
            listener: Sensor.HwUnitListener<T>) {
        try {
            registerListener(listener, CoroutineExceptionHandler { _, error ->
                addHwUnitErrorEvent(error,
                                    "Error registerListener CoroutineExceptionHandler hwUnit on $hwUnit")
            })
        } catch (e: Exception) {
            addHwUnitErrorEvent(e, "Error registerListener hwUnit on $hwUnit")
        }
    }

    private fun <T : Any> BaseHwUnit<T>.closeValueWithException() {
        try {
            close()
        } catch (e: Exception) {
            addHwUnitErrorEvent(e, "Error closing hwUnit on $hwUnit")
        }
    }

    private fun <T : Any> BaseHwUnit<T>.connectValueWithException(): Boolean {
        return try {
            connect()
            true
        } catch (e: Exception) {
            addHwUnitErrorEvent(e, "Error connecting hwUnit on $hwUnit")
            false
        }
    }

    private fun <T : Any> BaseHwUnit<T>.addHwUnitErrorEvent(e: Throwable, logMessage: String) {
        hwUnitErrorEventList.computeIfAbsent(hwUnit.name) { key ->
            hwUnitsList.remove(key)?.also {
                hwUnitStop(it)
            }?: this as BaseHwUnit<Any>
        }
        homeInformationRepository.addHwUnitErrorEvent(
                HwUnitLog(hwUnit, unitValue, e.message, Date().toString()))
        Timber.e(e, logMessage)
    }
}