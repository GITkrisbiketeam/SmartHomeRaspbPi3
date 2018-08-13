package com.krisbiketeam.smarthomeraspbpi3

import android.arch.lifecycle.Observer
import com.krisbiketeam.data.storage.*
import com.krisbiketeam.data.storage.FirebaseTables.*
import com.krisbiketeam.data.storage.dto.*
import com.krisbiketeam.smarthomeraspbpi3.driver.MCP23017Pin
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.BaseUnit
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import com.krisbiketeam.smarthomeraspbpi3.units.hardware.*
import timber.log.Timber

class Home() : Sensor.HomeUnitListener<Any> {
    private var storageUnitsLiveData = FirebaseHomeInformationRepository.storageUnitsLiveData()

    private var hwUnitsLiveData = FirebaseHomeInformationRepository.hwUnitsLiveData()


    private val storageUnitUnitList: MutableMap<String, StorageUnit<Any>> = HashMap()

    private val hardwareUnitList: MutableMap<String, BaseUnit<Any>> = HashMap()

    private var booleanApplyFunction: StorageUnit<in Boolean>.(Any?) -> Unit = { newVal: Any? ->
        Timber.d("booleanApplyFunction newVal: $newVal this: $this")
        if (newVal is Boolean) {
            this.unitsTasks.forEach {
                it.storageUnitName?.let { storageUnit ->
                    storageUnitUnitList[storageUnit]?.run {
                        Timber.d("booleanApplyFunction task: $it for this: $this")
                        this.value = newVal
                        Timber.d("booleanApplyFunction run on: $it from : $this")
                        FirebaseHomeInformationRepository.saveStorageUnit(this)
                    }
                }
                it.hardwareUnitName?.let { hardwareUnit ->
                    hardwareUnitList[hardwareUnit]?.run {
                        Timber.d("booleanApplyFunction task: $it for this: $this")
                        if (this is Actuator) {
                            Timber.d("booleanApplyFunction unit setValue newVal: $newVal")
                            this.setValue(newVal)
                        }
                    }
                }
            }
        } else {
            Timber.e("booleanApplyFunction new value is not Boolean or is null")
        }
    }

    init {
        //initHardwareUnitList()
        //initStorageUnitList()

        //saveToRepository()
    }

    fun start() {
        Timber.e("start; hardwareUnitList.size: ${hardwareUnitList.size}")
        storageUnitsLiveData.observeForever(storageUnitsDataObserver)
        hwUnitsLiveData.observeForever(hwUnitsDataObserver)

        hardwareUnitList.values.forEach(this::hwUnitStart)
    }

    fun stop() {
        Timber.e("start; hardwareUnitList.size: ${hardwareUnitList.size}")
        storageUnitsLiveData.removeObserver(storageUnitsDataObserver)
        hwUnitsLiveData.removeObserver(hwUnitsDataObserver)

        hardwareUnitList.values.forEach(this::hwUnitStop)
    }

    fun hwUnitStart(unit: BaseUnit<Any>) {
        Timber.v("hwUnitStart connect unit: ${unit.homeUnit}")
        unit.connect()
        if (unit is Sensor) {
            unit.registerListener(this)
        }
    }

    fun hwUnitStop(unit: BaseUnit<Any>) {
        Timber.v("hwUnitStop close unit: ${unit.homeUnit}")
        try {
            // close will automatically unregister listener
            unit.close()
        } catch (e: Exception) {
            Timber.e("Error on PeripheralIO API", e)
        }
    }

    private val storageUnitsDataObserver = Observer<Pair<ChildEventType,StorageUnit<out Any>>> { pair ->
        Timber.d("storageUnitsDataObserver changed: $pair")
        pair?.let { (action, value) ->
            when (action) {
                ChildEventType.NODE_ACTION_CHANGED -> {
                    Timber.d("storageUnitsDataObserver NODE_ACTION_CHANGED: ${storageUnitUnitList[value.name]}")
                    storageUnitUnitList[value.name]?.run {
                        applyFunction(value.value)
                    }
                }
                ChildEventType.NODE_ACTION_ADDED -> {
                    val storageUnit = storageUnitUnitList[value.name]
                    Timber.d("storageUnitsDataObserver OLD $storageUnit ; NEW  $value")
                    val newVal = value as StorageUnit<Any>
                    storageUnitTypeIndicatorMap[newVal.firebaseTableName]?.isInstance(Boolean::class.java)
                            .let {
                                newVal.applyFunction = booleanApplyFunction
                            }
                    storageUnitUnitList[newVal.name] = newVal
                }
                ChildEventType.NODE_ACTION_DELETED -> {
                    val result = storageUnitUnitList.remove(value.name)
                    Timber.d("storageUnitsDataObserver NODE_ACTION_DELETED: $result")
                }
                else -> {
                    Timber.e("storageUnitsDataObserver unsupported action: $action")
                }
            }
        }
    }

    private val hwUnitsDataObserver = Observer<Pair<ChildEventType, HomeUnit>> { pair ->
        Timber.d("hwUnitsDataObserver changed: $pair")
        pair?.let { (action, value) ->
            when (action) {
                ChildEventType.NODE_ACTION_CHANGED -> {
                    Timber.e("hwUnitsDataObserver NODE_ACTION_CHANGED unsupported value: $value")
                }
                ChildEventType.NODE_ACTION_ADDED -> {
                    // TODO: consider this unit is already present in hardwareUnitList
                    var unit = hardwareUnitList[value.name]
                    Timber.d("hwUnitsDataObserver HomeUnit NODE_ACTION_ADDED: $unit")
                    unit?.let {
                        Timber.w("HwUnit already exist recreate it")
                        hwUnitStop(it)
                    }
                    when (value.name) {
                        BoardConfig.TEMP_SENSOR_TMP102 -> {
                            unit = HomeUnitI2CTempTMP102Sensor(
                                    value.name,
                                    value.location,
                                    value.pinName,
                                    value.softAddress!!) as BaseUnit<Any>
                        }
                        BoardConfig.TEMP_PRESS_SENSOR_BMP280 -> {
                            unit = HomeUnitI2CTempPressBMP280Sensor(
                                    value.name,
                                    value.location,
                                    value.pinName,
                                    value.softAddress!!) as BaseUnit<Any>
                        }
                        BoardConfig.IO_EXTENDER_MCP23017_1_IN_A7,
                        BoardConfig.IO_EXTENDER_MCP23017_1_IN_A6,
                        BoardConfig.IO_EXTENDER_MCP23017_1_IN_A5,
                        BoardConfig.IO_EXTENDER_MCP23017_1_IN_A0 -> {
                            unit = HomeUnitI2CMCP23017Sensor(
                                    value.name,
                                    value.location,
                                    value.pinName,
                                    value.softAddress!!,
                                    value.pinInterrupt!!,
                                    MCP23017Pin.Pin.valueOf(value.ioPin!!),
                                    value.internalPullUp!!) as BaseUnit<Any>
                        }
                        BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B0,
                        BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B7 -> {
                            unit = HomeUnitI2CMCP23017Actuator(
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
                    Timber.d("hwUnitsDataObserver HomeUnit NODE_ACTION_DELETED: $result")

                }
                else -> {
                    Timber.e("hwUnitsDataObserver unsupported action: $action")
                }
            }
        }
    }

    override fun onUnitChanged(homeUnit: HomeUnit, unitValue: Any?, updateTime: String) {
        Timber.d("onUnitChanged unit: $homeUnit; unitValue: $unitValue; updateTime: $updateTime")
        FirebaseHomeInformationRepository.logUnitEvent(HomeUnitLog(homeUnit, unitValue, updateTime))

        storageUnitUnitList.values.find {
            it.hardwareUnitName == homeUnit.name
        }?.apply {
            // We need to handel differently values of non Basic Types
            if (unitValue is TemperatureAndPressure) {
                Timber.d("Received TemperatureAndPressure $value")
                // obsolete code start
                FirebaseHomeInformationRepository.saveTemperature(unitValue.temperature)
                FirebaseHomeInformationRepository.savePressure(unitValue.pressure)
                // obsolete code end
                if (firebaseTableName == HOME_TEMPERATURES) {
                    value = unitValue.temperature
                } else if (firebaseTableName == HOME_PRESSURES) {
                    value = unitValue.pressure
                }
            } else {
                value = unitValue
            }
            FirebaseHomeInformationRepository.saveStorageUnit(this)
        }
    }

    fun saveToRepository() {
        storageUnitUnitList.values.forEach { FirebaseHomeInformationRepository.saveStorageUnit(it) }
        hardwareUnitList.values.forEach { FirebaseHomeInformationRepository.saveHardwareUnit(it.homeUnit) }
    }

    private fun initStorageUnitList() {
        var roomName = "Kitchen"

        var temp = Temperature("Kitchen 1 Temp", HOME_TEMPERATURES, roomName, BoardConfig.TEMP_PRESS_SENSOR_BMP280) as StorageUnit<Any>

        var pressure = Pressure("Kitchen 1 Press", HOME_PRESSURES, roomName, BoardConfig.TEMP_PRESS_SENSOR_BMP280) as StorageUnit<Any>

        var light = Light("Kitchen 1 Light", HOME_LIGHTS, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B0) as StorageUnit<Any>
        light.unitsTasks.add(UnitTask(hardwareUnitName = light.hardwareUnitName))
        light.applyFunction = booleanApplyFunction

        var lightSwitch = LightSwitch("Kitchen 1 Light Switch", HOME_LIGHT_SWITCHES, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_IN_A7) as StorageUnit<Any>
        lightSwitch.unitsTasks.add(UnitTask(storageUnitName = light.name))
        lightSwitch.applyFunction = booleanApplyFunction

        var reedSwitch = ReedSwitch("Kitchen 1 Reed Switch", HOME_REED_SWITCHES, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_IN_A6) as StorageUnit<Any>

        val motion = Motion("Kitchen 1 Motion Sensor", HOME_MOTIONS, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_IN_A0) as StorageUnit<Any>

        storageUnitUnitList[temp.name] = temp
        storageUnitUnitList[pressure.name] = pressure
        storageUnitUnitList[light.name] = light
        storageUnitUnitList[lightSwitch.name] = lightSwitch
        storageUnitUnitList[reedSwitch.name] = reedSwitch
        storageUnitUnitList[motion.name] = motion

        // Second room
        roomName = "Bathroom"

        temp = Temperature("Bathroom 1 Temp", HOME_TEMPERATURES, roomName, BoardConfig.TEMP_SENSOR_TMP102) as StorageUnit<Any>

        light = Light("Bathroom 1 Light", HOME_LIGHTS, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B7) as StorageUnit<Any>
        light.unitsTasks.add(UnitTask(hardwareUnitName = light.hardwareUnitName))
        light.applyFunction = booleanApplyFunction

        lightSwitch = LightSwitch("Bathroom 1 Light Switch", HOME_LIGHT_SWITCHES, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_IN_A5) as StorageUnit<Any>
        lightSwitch.unitsTasks.add(UnitTask(storageUnitName = light.name))
        lightSwitch.applyFunction = booleanApplyFunction

        storageUnitUnitList[temp.name] = temp
        storageUnitUnitList[light.name] = light
        storageUnitUnitList[lightSwitch.name] = lightSwitch
    }

    private fun initHardwareUnitList() {
        /*val motion = HomeUnitGpioSensor(BoardConfig.MOTION_1, "Raspberry Pi",
                BoardConfig.MOTION_1_PIN,
                Gpio.ACTIVE_HIGH) as Sensor<Any>
        hardwareUnitList[BoardConfig.MOTION_1] = motion*/

        /*val contactron = HomeUnitGpioNoiseSensor(BoardConfig.REED_SWITCH_1, "Raspberry Pi", BoardConfig
                .REED_SWITCH_1_PIN, Gpio.ACTIVE_LOW) as Sensor<Any>
        hardwareUnitList[BoardConfig.REED_SWITCH_1] = contactron*/

        val temperatureSensor = HomeUnitI2CTempTMP102Sensor(BoardConfig.TEMP_SENSOR_TMP102,
                "Raspberry Pi",
                BoardConfig.TEMP_SENSOR_TMP102_PIN,
                BoardConfig.TEMP_SENSOR_TMP102_ADDR) as Sensor<Any>
        hardwareUnitList[BoardConfig.TEMP_SENSOR_TMP102] = temperatureSensor

        val tempePressSensor = HomeUnitI2CTempPressBMP280Sensor(BoardConfig.TEMP_PRESS_SENSOR_BMP280,
                "Raspberry Pi",
                BoardConfig.TEMP_PRESS_SENSOR_BMP280_PIN,
                BoardConfig.TEMP_PRESS_SENSOR_BMP280_ADDR) as Sensor<Any>
        hardwareUnitList[BoardConfig.TEMP_PRESS_SENSOR_BMP280] = tempePressSensor

        val mcpLightSwitch = HomeUnitI2CMCP23017Sensor(BoardConfig.IO_EXTENDER_MCP23017_1_IN_A7,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_1_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_1_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_IN_A7_PIN,
                true) as Sensor<Any>
        hardwareUnitList[BoardConfig.IO_EXTENDER_MCP23017_1_IN_A7] = mcpLightSwitch

        val mcpContactron = HomeUnitI2CMCP23017Sensor(BoardConfig.IO_EXTENDER_MCP23017_1_IN_A6,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_1_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_1_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_IN_A6_PIN,
                true) as Sensor<Any>
        hardwareUnitList[BoardConfig.IO_EXTENDER_MCP23017_1_IN_A6] = mcpContactron

        val mcpLightSwitch2 = HomeUnitI2CMCP23017Sensor(BoardConfig.IO_EXTENDER_MCP23017_1_IN_A5,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_1_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_1_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_IN_A5_PIN,
                true) as Sensor<Any>
        hardwareUnitList[BoardConfig.IO_EXTENDER_MCP23017_1_IN_A5] = mcpLightSwitch2

        val mcpMotion = HomeUnitI2CMCP23017Sensor(BoardConfig.IO_EXTENDER_MCP23017_1_IN_A0,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_1_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_1_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_IN_A0_PIN) as Sensor<Any>
        hardwareUnitList[BoardConfig.IO_EXTENDER_MCP23017_1_IN_A0] = mcpMotion

        val mcpLed = HomeUnitI2CMCP23017Actuator(BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B0,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_1_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_1_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B0_PIN) as Actuator<Any>
        hardwareUnitList[BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B0] = mcpLed

        val mcpLed2 = HomeUnitI2CMCP23017Actuator(BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B7,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_1_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_1_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B7_PIN) as Actuator<Any>
        hardwareUnitList[BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B7] = mcpLed2
    }
}