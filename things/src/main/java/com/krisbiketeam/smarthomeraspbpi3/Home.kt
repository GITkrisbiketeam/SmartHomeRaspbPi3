package com.krisbiketeam.smarthomeraspbpi3

import androidx.lifecycle.Observer
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017Pin
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ChildEventType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.*
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.BaseUnit
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import com.krisbiketeam.smarthomeraspbpi3.units.hardware.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

class Home : Sensor.HwUnitListener<Any> {
    private var homeUnitsLiveData = FirebaseHomeInformationRepository.homeUnitsLiveData()

    private var hwUnitsLiveData = FirebaseHomeInformationRepository.hwUnitsLiveData()

    private var hwUnitErrorEventListLiveData = FirebaseHomeInformationRepository.hwUnitErrorEventListLiveData()

    private var rooms: MutableMap<String, Room> = HashMap()

    private val homeUnitList: MutableMap<String, HomeUnit<Any>> = HashMap()

    private val hardwareUnitList: MutableMap<String, BaseUnit<Any>> = HashMap()

    private val hwUnitErrorEventList: MutableMap<String, Triple<String, Int, BaseUnit<Any>>> = HashMap()

    private var booleanApplyFunction: HomeUnit<in Boolean>.(Any?) -> Unit = { newVal: Any? ->
        Timber.d("booleanApplyFunction newVal: $newVal this: $this")
        if (newVal is Boolean) {
            unitsTasks.values.forEach { task ->
                homeUnitList[task.homeUnitName]?.apply {
                    Timber.d("booleanApplyFunction task: $task for homeUnit: $this")
                    hardwareUnitList[hwUnitName]?.let { taskHwUnit ->
                        Timber.d("booleanApplyFunction taskHwUnit: $taskHwUnit")
                        if (taskHwUnit is Actuator) {
                            value = newVal
                            Timber.d("booleanApplyFunction taskHwUnit setValue value: $value")
                            taskHwUnit.setValue(value)
                            applyFunction(value)
                            FirebaseHomeInformationRepository.saveHomeUnit(this)
                            if (firebaseNotify) {
                                Timber.d("booleanApplyFunction notify with FCM Message")
                                FirebaseHomeInformationRepository.notifyHomeUnitEvent(this)
                            }
                        }
                    }
                }
            }
        } else {
            Timber.e("booleanApplyFunction new value is not Boolean or is null")
        }
    }

    private var sensorApplyFunction: HomeUnit<in Boolean>.(Any?) -> Unit = { newVal: Any? ->
        Timber.d("booleanApplyFunction newVal: $newVal this: $this")
        if (newVal is Boolean) {
            unitsTasks.values.forEach { task ->
                if (this.name == task.homeUnitName){
                    task.delay
                } else {
                    homeUnitList[task.homeUnitName]?.apply {
                        Timber.d("sensorApplyFunction task: $task for homeUnit: $this")
                        hardwareUnitList[hwUnitName]?.let { taskHwUnit ->
                            Timber.d("sensorApplyFunction taskHwUnit: $taskHwUnit")
                            if (taskHwUnit is Actuator) {
                                value = newVal
                                Timber.d("sensorApplyFunction taskHwUnit setValue value: $value")
                                taskHwUnit.setValue(value)
                                applyFunction(value)
                                FirebaseHomeInformationRepository.saveHomeUnit(this)
                                if (firebaseNotify) {
                                    Timber.d("sensorApplyFunction notify with FCM Message")
                                    FirebaseHomeInformationRepository.notifyHomeUnitEvent(this)
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
        Timber.e("start; hardwareUnitList.size: ${hardwareUnitList.size}")
        homeUnitsLiveData.observeForever(homeUnitsDataObserver)
        hwUnitsLiveData.observeForever(hwUnitsDataObserver)
        hwUnitErrorEventListLiveData.observeForever(hwUnitErrorEventListDataObserver)
    }

    fun stop() {
        Timber.e("stop; hardwareUnitList.size: ${hardwareUnitList.size}")
        homeUnitsLiveData.removeObserver(homeUnitsDataObserver)
        hwUnitsLiveData.removeObserver(hwUnitsDataObserver)
        hwUnitErrorEventListLiveData.removeObserver(hwUnitErrorEventListDataObserver)

        hardwareUnitList.values.forEach(this::hwUnitStop)
    }

    private fun hwUnitStart(unit: BaseUnit<Any>) {
        Timber.v("hwUnitStart connect unit: ${unit.hwUnit}")
        unit.connect()
        if (unit is Sensor) {
            GlobalScope.launch(Dispatchers.Default) {
                val readVal = unit.readValue()
                Timber.w("hwUnitStart $readVal ${unit.unitValue}")
                unit.registerListener(this@Home)
                hardwareUnitList[unit.hwUnit.name] = unit
            }
        } else {
            hardwareUnitList[unit.hwUnit.name] = unit
        }
    }

    private fun hwUnitStop(unit: BaseUnit<Any>) {
        Timber.v("hwUnitStop close unit: ${unit.hwUnit}")
        try {
            // close will automatically unregister listener
            unit.close()
        } catch (e: Exception) {
            Timber.e(e,"Error on PeripheralIO API")
        }
    }

    private val homeUnitsDataObserver = Observer<Pair<ChildEventType, HomeUnit<Any>>> { pair ->
        Timber.d("homeUnitsDataObserver changed: $pair")
        pair?.let { (action, homeUnit) ->
            when (action) {
                ChildEventType.NODE_ACTION_CHANGED -> {
                    Timber.d("homeUnitsDataObserver NODE_ACTION_CHANGED NEW:  $homeUnit")
                    homeUnitList[homeUnit.name]?.run {
                        Timber.d("homeUnitsDataObserver NODE_ACTION_CHANGED EXISTING: $this}")
                        // set previous apply function to new homeUnit
                        homeUnit.applyFunction = applyFunction
                        hardwareUnitList[homeUnit.hwUnitName]?.let { baseUnit ->
                            Timber.d("homeUnitsDataObserver NODE_ACTION_CHANGED baseUnit: $baseUnit")
                            // Actuators can be changed from remote mobile App so apply HomeUnitState to hwUnitState if it changed
                            if (baseUnit is Actuator) {
                                if (homeUnit.value != value) {
                                    Timber.d("homeUnitsDataObserver NODE_ACTION_CHANGED baseUnit setValue value: ${homeUnit.value}")
                                    baseUnit.setValue(homeUnit.value)

                                    homeUnit.applyFunction(homeUnit.value)

                                    if (homeUnit.firebaseNotify) {
                                        Timber.d("homeUnitsDataObserver NODE_ACTION_CHANGED notify with FCM Message")
                                        FirebaseHomeInformationRepository.notifyHomeUnitEvent(homeUnit)
                                    }
                                }
                            }
                        }
                        homeUnitList[homeUnit.name] = homeUnit
                    }
                }
                ChildEventType.NODE_ACTION_ADDED -> {
                    val existingUnit = homeUnitList[homeUnit.name]
                    Timber.d("homeUnitsDataObserver NODE_ACTION_ADDED EXISTING $existingUnit ; NEW  $homeUnit")
                    when (homeUnit.type) {
                        HOME_LIGHTS,
                        HOME_ACTUATORS,
                        HOME_LIGHT_SWITCHES,
                        HOME_REED_SWITCHES,
                        HOME_MOTIONS -> {
                            Timber.d("homeUnitsDataObserver NODE_ACTION_ADDED set boolean apply function")
                            homeUnit.applyFunction = booleanApplyFunction
                        }
                        HOME_TEMPERATURES,
                        HOME_PRESSURES -> {
                            Timber.d("homeUnitsDataObserver NODE_ACTION_ADDED set sensor apply function")
                            homeUnit.applyFunction = sensorApplyFunction
                        }
                    }
                    // Set/Update HhUnit States according to HomeUnit state and vice versa
                    hardwareUnitList[homeUnit.hwUnitName]?.let { baseUnit ->
                        Timber.d("homeUnitsDataObserver NODE_ACTION_ADDED baseUnit: $baseUnit")
                        if (homeUnit.value != baseUnit.unitValue) {
                            if (baseUnit is Actuator) {
                                Timber.d("homeUnitsDataObserver NODE_ACTION_ADDED baseUnit setValue value: ${homeUnit.value}")
                                baseUnit.setValue(homeUnit.value)

                            } else if(baseUnit is Sensor){
                                homeUnit.value = baseUnit.unitValue
                                FirebaseHomeInformationRepository.saveHomeUnit(homeUnit)
                            }
                        }
                    }
                    homeUnitList[homeUnit.name] = homeUnit
                    //TODO: should we also call applyFunction ???
                    homeUnit.applyFunction(homeUnit, homeUnit.value)
                }
                ChildEventType.NODE_ACTION_DELETED -> {
                    val result = homeUnitList.remove(homeUnit.name)
                    Timber.d("homeUnitsDataObserver NODE_ACTION_DELETED: $result")
                }
                else -> {
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
                    hardwareUnitList[value.name]?.let {
                        Timber.w("NODE_ACTION_CHANGEDHwUnit already exist stop existing one")
                        hwUnitStop(it)
                    }
                    if (hwUnitErrorEventList.contains(value.name)) {
                        Timber.w("hwUnitsDataObserver NODE_ACTION_CHANGED remove from ErrorEventList value: ${value.name}")
                        hwUnitErrorEventList.remove(value.name)
                        FirebaseHomeInformationRepository.clearHwErrorEvent(value.name)
                    }
                    createHwUnit(value)?.let {
                        Timber.w("HwUnit recreated connect and eventually listen to it")
                        hwUnitStart(it)
                    }
                }
                ChildEventType.NODE_ACTION_ADDED -> {
                    // consider this unit is already present in hardwareUnitList
                    hardwareUnitList[value.name]?.let {
                        Timber.w("NODE_ACTION_ADDED HwUnit already exist stop old one:")
                        hwUnitStop(it)
                    }
                    createHwUnit(value)?.let {
                        Timber.w("NODE_ACTION_ADDED HwUnit recreated connect and eventually listen to it")
                        hwUnitStart(it)
                    }
                }
                ChildEventType.NODE_ACTION_DELETED -> {
                    hardwareUnitList[value.name]?.let {
                        hwUnitStop(it)
                    }
                    val result = hardwareUnitList.remove(value.name)
                    Timber.d("hwUnitsDataObserver HwUnit NODE_ACTION_DELETED: $result")

                }
                else -> {
                    Timber.e("hwUnitsDataObserver unsupported action: $action")
                }
            }
        }
    }

    private fun createHwUnit(hwUnit: HwUnit): BaseUnit<Any>? {
        return when (hwUnit.type) {
            BoardConfig.TEMP_SENSOR_TMP102 -> {
                HwUnitI2CTempTMP102Sensor(
                        hwUnit.name,
                        hwUnit.location,
                        hwUnit.pinName,
                        hwUnit.softAddress ?: 0,
                        hwUnit.refreshRate) as BaseUnit<Any>
            }
            BoardConfig.TEMP_SENSOR_MCP9808 -> {
                HwUnitI2CTempMCP9808Sensor(
                        hwUnit.name,
                        hwUnit.location,
                        hwUnit.pinName,
                        hwUnit.softAddress ?: 0,
                        hwUnit.refreshRate) as BaseUnit<Any>
            }
            BoardConfig.TEMP_PRESS_SENSOR_BMP280 -> {
                HwUnitI2CTempPressBMP280Sensor(
                        hwUnit.name,
                        hwUnit.location,
                        hwUnit.pinName,
                        hwUnit.softAddress ?: 0,
                        hwUnit.refreshRate) as BaseUnit<Any>
            }
            BoardConfig.IO_EXTENDER_MCP23017_INPUT -> {
                MCP23017Pin.Pin.values().find {
                    it.name == hwUnit.ioPin
                }?.let { ioPin ->
                    HwUnitI2CMCP23017Sensor(
                            hwUnit.name,
                            hwUnit.location,
                            hwUnit.pinName,
                            hwUnit.softAddress ?: 0,
                            hwUnit.pinInterrupt ?: "",
                            ioPin,
                            hwUnit.internalPullUp ?: false) as BaseUnit<Any>
                }
            }
            BoardConfig.IO_EXTENDER_MCP23017_OUTPUT -> {
                MCP23017Pin.Pin.values().find {
                    it.name == hwUnit.ioPin
                }?.let { ioPin ->
                    HwUnitI2CMCP23017Actuator(
                            hwUnit.name,
                            hwUnit.location,
                            hwUnit.pinName,
                            hwUnit.softAddress ?: 0,
                            hwUnit.pinInterrupt ?: "",
                            ioPin) as BaseUnit<Any>
                }
            }
            else -> null
        }
    }

    private val hwUnitErrorEventListDataObserver = Observer<List<HwUnitLog<Any>>> { errorEventList ->
        Timber.d("hwUnitErrorEventListDataObserver errorEventList: $errorEventList; errorEventList.size: ${errorEventList.size}")
        if (errorEventList.isNotEmpty()) {
            errorEventList.forEach { hwUnitErrorEvent ->
                hwUnitErrorEventList.compute(hwUnitErrorEvent.name) { key, value ->
                    value?.run {
                        if (hwUnitErrorEvent.localtime != first){
                            val count = second.inc()
                            if (count > 3) {
                                val result = hardwareUnitList.remove(key)
                                result?.let {
                                    hwUnitStop(result)
                                }
                                Timber.w("hwUnitErrorEventListDataObserver to many errors ($count) from hwUnit: $key, remove it from hardwareUnitList result: ${result != null}")
                            }
                            Triple(hwUnitErrorEvent.localtime, count, third)
                        } else {
                            value
                        }
                    } ?: hardwareUnitList[hwUnitErrorEvent.name]?.let { hwUnit ->
                        Triple(hwUnitErrorEvent.localtime, 0, hwUnit)
                    }
                }
            }
        } else {
            val hwUnitRestoreList: MutableList<BaseUnit<Any>> = mutableListOf()
            hwUnitErrorEventList.values.forEach { (_, _, hwUnit) ->
                hwUnitRestoreList.add(hwUnit)
            }
            hwUnitErrorEventList.clear()
            hwUnitRestoreList.forEach { hwUnit ->
                hwUnitStart(hwUnit)
            }
        }
    }

    override fun onUnitChanged(hwUnit: HwUnit, unitValue: Any?, updateTime: String) {
        Timber.d("onUnitChanged unit: $hwUnit; unitValue: $unitValue; updateTime: $updateTime")
        FirebaseHomeInformationRepository.logUnitEvent(HwUnitLog(hwUnit, unitValue, updateTime))

        homeUnitList.values.filter {
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
                applyFunction(value)
                FirebaseHomeInformationRepository.saveHomeUnit(this)
                if (firebaseNotify) {
                    Timber.d("onUnitChanged notify with FCM Message")
                    FirebaseHomeInformationRepository.notifyHomeUnitEvent(this)
                }
            }
        }
    }

    fun saveToRepository() {
        rooms.values.forEach { FirebaseHomeInformationRepository.saveRoom(it) }
        homeUnitList.values.forEach { FirebaseHomeInformationRepository.saveHomeUnit(it) }
        hardwareUnitList.values.forEach { FirebaseHomeInformationRepository.saveHardwareUnit(it.hwUnit) }
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

        homeUnitList[temp.name] = temp
        homeUnitList[pressure.name] = pressure
        homeUnitList[light.name] = light
        homeUnitList[lightSwitch.name] = lightSwitch
        homeUnitList[reedSwitch.name] = reedSwitch
        homeUnitList[motion.name] = motion

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

        homeUnitList[temp.name] = temp
        homeUnitList[temp1.name] = temp1
        homeUnitList[light.name] = light
        homeUnitList[lightSwitch.name] = lightSwitch
        homeUnitList[reedSwitch.name] = reedSwitch
        homeUnitList[motion.name] = motion

        room = Room(roomName, 0, listOf(light.name), listOf(lightSwitch.name), *//*ArrayList()*//*listOf(reedSwitch.name), *//*ArrayList()*//*listOf(motion.name), listOf(temp.name, temp1.name))
        rooms[room.name] = room*/
        var roomName = "New"

        val temp = Temperature("Bathroom 1 Temp MCP9808", HOME_TEMPERATURES, roomName, BoardConfig.TEMP_SENSOR_MCP9808) as HomeUnit<Any>
        homeUnitList[temp.name] = temp

        var reedSwitch = ReedSwitch("New Reed Switch", HOME_REED_SWITCHES, roomName, BoardConfig.IO_EXTENDER_MCP23017_NEW_IN_B0) as HomeUnit<Any>
        homeUnitList[reedSwitch.name] = reedSwitch

        var light = Light("New Light", HOME_LIGHTS, roomName, BoardConfig.IO_EXTENDER_MCP23017_NEW_OUT_B7) as HomeUnit<Any>
        homeUnitList[light.name] = light

        var homeUnits = mutableMapOf<String, MutableList<String>>()
        homeUnits[HOME_LIGHTS] = mutableListOf(light.name)
        homeUnits[HOME_TEMPERATURES] = mutableListOf(temp.name)
        homeUnits[HOME_REED_SWITCHES] = mutableListOf(reedSwitch.name)

        var room = Room(roomName, 0, homeUnits)
        rooms[room.name] = room
    }

    private fun initHardwareUnitList() {
        /*val motion = HwUnitGpioSensor(BoardConfig.MOTION_1, "Raspberry Pi",
                BoardConfig.MOTION_1_PIN,
                Gpio.ACTIVE_HIGH) as Sensor<Any>
        hardwareUnitList[BoardConfig.MOTION_1] = motion*/

        /*val contactron = HwUnitGpioNoiseSensor(BoardConfig.REED_SWITCH_1, "Raspberry Pi", BoardConfig
                .REED_SWITCH_1_PIN, Gpio.ACTIVE_LOW) as Sensor<Any>
        hardwareUnitList[BoardConfig.REED_SWITCH_1] = contactron*/

        /*val temperatureTMP102Sensor = HwUnitI2CTempTMP102Sensor(BoardConfig.TEMP_SENSOR_TMP102,
                "Raspberry Pi",
                BoardConfig.TEMP_SENSOR_TMP102_PIN,
                BoardConfig.TEMP_SENSOR_TMP102_ADDR) as Sensor<Any>
        hardwareUnitList[BoardConfig.TEMP_SENSOR_TMP102] = temperatureTMP102Sensor

        val tempePressSensor = HwUnitI2CTempPressBMP280Sensor(BoardConfig.TEMP_PRESS_SENSOR_BMP280,
                "Raspberry Pi",
                BoardConfig.TEMP_PRESS_SENSOR_BMP280_PIN,
                BoardConfig.TEMP_PRESS_SENSOR_BMP280_ADDR) as Sensor<Any>
        hardwareUnitList[BoardConfig.TEMP_PRESS_SENSOR_BMP280] = tempePressSensor

        val mcpLightSwitch = HwUnitI2CMCP23017Sensor(BoardConfig.IO_EXTENDER_MCP23017_1_IN_A7,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_1_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_1_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_IN_A7_PIN,
                true) as Sensor<Any>
        hardwareUnitList[BoardConfig.IO_EXTENDER_MCP23017_1_IN_A7] = mcpLightSwitch

        val mcpContactron = HwUnitI2CMCP23017Sensor(BoardConfig.IO_EXTENDER_MCP23017_1_IN_A6,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_1_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_1_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_IN_A6_PIN,
                true) as Sensor<Any>
        hardwareUnitList[BoardConfig.IO_EXTENDER_MCP23017_1_IN_A6] = mcpContactron

        val mcpLightSwitch2 = HwUnitI2CMCP23017Sensor(BoardConfig.IO_EXTENDER_MCP23017_1_IN_A5,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_1_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_1_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_IN_A5_PIN,
                true) as Sensor<Any>
        hardwareUnitList[BoardConfig.IO_EXTENDER_MCP23017_1_IN_A5] = mcpLightSwitch2

        val mcpMotion = HwUnitI2CMCP23017Sensor(BoardConfig.IO_EXTENDER_MCP23017_1_IN_A0,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_1_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_1_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_IN_A0_PIN) as Sensor<Any>
        hardwareUnitList[BoardConfig.IO_EXTENDER_MCP23017_1_IN_A0] = mcpMotion

        val mcpLed = HwUnitI2CMCP23017Actuator(BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B0,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_1_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_1_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B0_PIN) as Actuator<Any>
        hardwareUnitList[BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B0] = mcpLed

        val mcpLed2 = HwUnitI2CMCP23017Actuator(BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B7,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_1_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_1_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B7_PIN) as Actuator<Any>
        hardwareUnitList[BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B7] = mcpLed2*/

        /*val mcpNewContactron = HwUnitI2CMCP23017Sensor(BoardConfig.IO_EXTENDER_MCP23017_2_IN_B0,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_2_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_2_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_2_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_2_IN_B0_PIN) as Sensor<Any>
        hardwareUnitList[BoardConfig.IO_EXTENDER_MCP23017_2_IN_B0] = mcpNewContactron

        val mcpNewMotion = HwUnitI2CMCP23017Sensor(BoardConfig.IO_EXTENDER_MCP23017_2_IN_A7,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_2_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_2_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_2_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_2_IN_A7_PIN) as Sensor<Any>
        hardwareUnitList[BoardConfig.IO_EXTENDER_MCP23017_2_IN_A7] = mcpNewMotion*/

        val temperatureMCP9808Sensor = HwUnitI2CTempMCP9808Sensor(BoardConfig.TEMP_SENSOR_MCP9808,
                "Raspberry Pi",
                BoardConfig.TEMP_SENSOR_MCP9808_PIN,
                BoardConfig.TEMP_SENSOR_MCP9808_ADDR) as Sensor<Any>
        hardwareUnitList[BoardConfig.TEMP_SENSOR_MCP9808] = temperatureMCP9808Sensor

        val mcpNewContactron = HwUnitI2CMCP23017Sensor(BoardConfig.IO_EXTENDER_MCP23017_NEW_IN_B0,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_NEW_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_NEW_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_NEW_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_NEW_IN_B0_PIN) as Sensor<Any>
        hardwareUnitList[BoardConfig.IO_EXTENDER_MCP23017_NEW_IN_B0] = mcpNewContactron

        val mcpNewLight = HwUnitI2CMCP23017Actuator(BoardConfig.IO_EXTENDER_MCP23017_NEW_OUT_B7,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_NEW_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_NEW_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_NEW_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_NEW_OUT_B7_PIN) as Actuator<Any>
        hardwareUnitList[BoardConfig.IO_EXTENDER_MCP23017_NEW_OUT_B7] = mcpNewLight

    }
}