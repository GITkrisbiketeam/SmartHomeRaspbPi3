package com.krisbiketeam.smarthomeraspbpi3

import android.arch.lifecycle.Observer
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.*
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017Pin
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ChildEventType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.BaseUnit
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import com.krisbiketeam.smarthomeraspbpi3.units.hardware.*
import timber.log.Timber

class Home : Sensor.HwUnitListener<Any> {
    private var homeUnitsLiveData = FirebaseHomeInformationRepository.homeUnitsLiveData()

    private var hwUnitsLiveData = FirebaseHomeInformationRepository.hwUnitsLiveData()

    private var rooms: MutableMap<String, Room> = HashMap()

    private val homeUnitList: MutableMap<String, HomeUnit<Any>> = HashMap()

    private val hardwareUnitList: MutableMap<String, BaseUnit<Any>> = HashMap()

    private var booleanApplyFunction: HomeUnit<in Boolean>.(Any?) -> Unit = { newVal: Any? ->
        Timber.d("booleanApplyFunction newVal: $newVal this: $this")
        if (newVal is Boolean) {
            this.unitsTasks.values.forEach { task ->
                task.homeUnitName?.let { taskHomeUnitName ->
                    homeUnitList[taskHomeUnitName]?.run {
                        Timber.d("booleanApplyFunction task: $task for homeUnit: $this")
                        this.value = newVal
                        hardwareUnitList[this.hwUnitName]?.let {baseUnit ->
                            Timber.d("homeUnitsDataObserver baseUnit: $baseUnit")
                            if (baseUnit is Actuator) {
                                Timber.d("homeUnitsDataObserver baseUnit setValue value: ${this.value}")
                                baseUnit.setValue(this.value)
                            }
                        }
                        this.applyFunction(newVal)
                        FirebaseHomeInformationRepository.saveHomeUnit(this)
                        if (firebaseNotify){
                            Timber.d("homeUnitsDataObserver notify with FCM Message")
                            FirebaseHomeInformationRepository.notifyHomeUnitEvent(this)
                        }
                    }
                }
                // Below should not be needed anymore
                /*task.hwUnitName?.let { hardwareUnit ->
                    hardwareUnitList[hardwareUnit]?.run {
                        Timber.d("booleanApplyFunction task: $task for this: $this")
                        if (this is Actuator) {
                            Timber.d("booleanApplyFunction unit setValue newVal: $newVal")
                            this.setValue(newVal)
                        }
                    }
                }*/
            }
        } else {
            Timber.e("booleanApplyFunction new value is not Boolean or is null")
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

        hardwareUnitList.values.forEach(this::hwUnitStart)
    }

    fun stop() {
        Timber.e("start; hardwareUnitList.size: ${hardwareUnitList.size}")
        homeUnitsLiveData.removeObserver(homeUnitsDataObserver)
        hwUnitsLiveData.removeObserver(hwUnitsDataObserver)

        hardwareUnitList.values.forEach(this::hwUnitStop)
    }

    private fun hwUnitStart(unit: BaseUnit<Any>) {
        Timber.v("hwUnitStart connect unit: ${unit.hwUnit}")
        unit.connect()
        if (unit is Sensor) {
            unit.registerListener(this)
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
                        if (homeUnit.value != value) {
                            homeUnit.applyFunction(homeUnit.value)
                            hardwareUnitList[homeUnit.hwUnitName]?.let {baseUnit ->
                                Timber.d("homeUnitsDataObserver baseUnit: $baseUnit")
                                if (baseUnit is Actuator) {
                                    Timber.d("homeUnitsDataObserver baseUnit setValue value: $homeUnit.value")
                                    baseUnit.setValue(homeUnit.value)
                                }
                            }
                            if (homeUnit.firebaseNotify){
                                Timber.d("homeUnitsDataObserver notify with FCM Message")
                                FirebaseHomeInformationRepository.notifyHomeUnitEvent(homeUnit)
                            }
                        }
                        homeUnitList[homeUnit.name] = homeUnit
                    }
                }
                ChildEventType.NODE_ACTION_ADDED -> {
                    val existingUnit = homeUnitList[homeUnit.name]
                    Timber.d("homeUnitsDataObserver EXISTING $existingUnit ; NEW  $homeUnit")
                    homeUnitTypeIndicatorMap[homeUnit.type]?.isInstance(Boolean::class.java)
                            .let {
                                homeUnit.applyFunction = booleanApplyFunction
                            }
                    homeUnitList[homeUnit.name] = homeUnit
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
                    Timber.e("hwUnitsDataObserver NODE_ACTION_CHANGED unsupported value: $value")
                }
                ChildEventType.NODE_ACTION_ADDED -> {
                    // TODO: consider this unit is already present in hardwareUnitList
                    var unit = hardwareUnitList[value.name]
                    Timber.d("hwUnitsDataObserver HwUnit NODE_ACTION_ADDED: $unit")
                    unit?.let {
                        Timber.w("HwUnit already exist recreate it")
                        hwUnitStop(it)
                    }
                    when (value.type) {
                        BoardConfig.TEMP_SENSOR_TMP102 -> {
                            unit = HwUnitI2CTempTMP102Sensor(
                                    value.name,
                                    value.location,
                                    value.pinName,
                                    value.softAddress!!) as BaseUnit<Any>
                        }
                        BoardConfig.TEMP_PRESS_SENSOR_BMP280 -> {
                            unit = HwUnitI2CTempPressBMP280Sensor(
                                    value.name,
                                    value.location,
                                    value.pinName,
                                    value.softAddress!!) as BaseUnit<Any>
                        }
                        BoardConfig.IO_EXTENDER_MCP23017_INPUT -> {
                            unit = HwUnitI2CMCP23017Sensor(
                                    value.name,
                                    value.location,
                                    value.pinName,
                                    value.softAddress!!,
                                    value.pinInterrupt!!,
                                    MCP23017Pin.Pin.valueOf(value.ioPin!!),
                                    value.internalPullUp!!) as BaseUnit<Any>
                        }
                        BoardConfig.IO_EXTENDER_MCP23017_OUTPUT -> {
                            unit = HwUnitI2CMCP23017Actuator(
                                    value.name,
                                    value.location,
                                    value.pinName,
                                    value.softAddress!!,
                                    value.pinInterrupt!!,
                                    MCP23017Pin.Pin.valueOf(value.ioPin!!)) as BaseUnit<Any>
                        }
                    }
                    unit?.let {
                        Timber.w("HwUnit recreated connect and eventually listen to it")
                        hardwareUnitList[value.name] = it
                        hwUnitStart(it)
                    }
                }
                ChildEventType.NODE_ACTION_DELETED -> {
                    val result = hardwareUnitList.remove(value.name)
                    Timber.d("hwUnitsDataObserver HwUnit NODE_ACTION_DELETED: $result")

                }
                else -> {
                    Timber.e("hwUnitsDataObserver unsupported action: $action")
                }
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
                if (firebaseNotify){
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
        var roomName = "Kitchen"

        var temp = Temperature("Kitchen 1 Temp", HOME_TEMPERATURES, roomName, BoardConfig.TEMP_PRESS_SENSOR_BMP280) as HomeUnit<Any>

        val pressure = Pressure("Kitchen 1 Press", HOME_PRESSURES, roomName, BoardConfig.TEMP_PRESS_SENSOR_BMP280) as HomeUnit<Any>

        var light = Light("Kitchen 1 Light", HOME_LIGHTS, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B0) as HomeUnit<Any>
//        light.unitsTasks = mapOf(Pair("Turn on HW light",UnitTask(name = "Turn on HW light", hwUnitName = light.hwUnitName)))
        light.applyFunction = booleanApplyFunction

        var lightSwitch = LightSwitch("Kitchen 1 Light Switch", HOME_LIGHT_SWITCHES, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_IN_A7) as HomeUnit<Any>
        lightSwitch.unitsTasks = mapOf(Pair("Turn on light", UnitTask(name = "Turn on light", homeUnitName = light.name)))
        lightSwitch.applyFunction = booleanApplyFunction

        var reedSwitch = ReedSwitch("Kitchen 1 Reed Switch", HOME_REED_SWITCHES, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_IN_A6) as HomeUnit<Any>

        val motion = Motion("Kitchen 1 Motion Sensor", HOME_MOTIONS, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_IN_A0, firebaseNotify = true) as HomeUnit<Any>

        homeUnitList[temp.name] = temp
        homeUnitList[pressure.name] = pressure
        homeUnitList[light.name] = light
        homeUnitList[lightSwitch.name] = lightSwitch
        homeUnitList[reedSwitch.name] = reedSwitch
        homeUnitList[motion.name] = motion

        var room = Room(roomName, 0, listOf(light.name), listOf(lightSwitch.name), listOf(reedSwitch.name), listOf(motion.name), listOf(temp.name), listOf(pressure.name))
        rooms[room.name] = room

        // Second room
        roomName = "Bathroom"

        temp = Temperature("Bathroom 1 Temp", HOME_TEMPERATURES, roomName, BoardConfig.TEMP_SENSOR_TMP102) as HomeUnit<Any>

        light = Light("Bathroom 1 Light", HOME_LIGHTS, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B7, firebaseNotify = true) as HomeUnit<Any>
//        light.unitsTasks = mapOf(Pair("Turn on HW light", UnitTask(name = "Turn on HW light", hwUnitName = light.hwUnitName)))
        light.applyFunction = booleanApplyFunction

        lightSwitch = LightSwitch("Bathroom 1 Light Switch", HOME_LIGHT_SWITCHES, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_IN_A5) as HomeUnit<Any>
        lightSwitch.unitsTasks = mapOf(Pair("Turn on light", UnitTask(name = "Turn on light", homeUnitName = light.name)))
        lightSwitch.applyFunction = booleanApplyFunction

        reedSwitch = ReedSwitch("Bathroom 1 Reed Switch", HOME_REED_SWITCHES, roomName, BoardConfig.IO_EXTENDER_MCP23017_2_IN_B0) as HomeUnit<Any>

        homeUnitList[temp.name] = temp
        homeUnitList[light.name] = light
        homeUnitList[lightSwitch.name] = lightSwitch
        homeUnitList[reedSwitch.name] = reedSwitch
        room = Room(roomName, 0, listOf(light.name), listOf(lightSwitch.name), listOf(reedSwitch.name), ArrayList(), listOf(temp.name))
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

        val temperatureSensor = HwUnitI2CTempTMP102Sensor(BoardConfig.TEMP_SENSOR_TMP102,
                "Raspberry Pi",
                BoardConfig.TEMP_SENSOR_TMP102_PIN,
                BoardConfig.TEMP_SENSOR_TMP102_ADDR) as Sensor<Any>
        hardwareUnitList[BoardConfig.TEMP_SENSOR_TMP102] = temperatureSensor

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
        hardwareUnitList[BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B7] = mcpLed2

        val mcpNewContactron = HwUnitI2CMCP23017Sensor(BoardConfig.IO_EXTENDER_MCP23017_2_IN_B0,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_2_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_2_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_2_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_2_IN_B0_PIN,
                false) as Sensor<Any>
        hardwareUnitList[BoardConfig.IO_EXTENDER_MCP23017_2_IN_B0] = mcpNewContactron


    }
}