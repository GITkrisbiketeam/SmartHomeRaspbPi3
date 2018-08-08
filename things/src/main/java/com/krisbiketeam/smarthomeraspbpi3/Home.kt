package com.krisbiketeam.smarthomeraspbpi3

import android.arch.lifecycle.Observer
import com.krisbiketeam.data.storage.FirebaseTables.*
import com.krisbiketeam.data.storage.HomeInformationRepository
import com.krisbiketeam.data.storage.StorageUnitsLiveData
import com.krisbiketeam.data.storage.dto.*
import com.krisbiketeam.smarthomeraspbpi3.driver.MCP23017Pin
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.BaseUnit
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import com.krisbiketeam.smarthomeraspbpi3.units.hardware.*
import timber.log.Timber

class Home(private val homeInformationRepository: HomeInformationRepository) : Sensor.HomeUnitListener<Any> {
    private var storageUnitsLiveData = homeInformationRepository.storageUnitsLiveData()


    private var rooms: MutableMap<String, Room> = HashMap()

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
                        homeInformationRepository.saveStorageUnit(this)
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

        var room = Room(roomName, 0, listOf(light.name), listOf(lightSwitch.name), listOf(reedSwitch.name), listOf(motion.name), listOf(temp.name), listOf(pressure.name))
        rooms[room.name] = room

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

        room = Room(roomName, 0, listOf(light.name), listOf(lightSwitch.name), ArrayList(), ArrayList(), listOf(temp.name))
        rooms[room.name] = room

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

    fun start() {
        Timber.e("start; hardwareUnitList.size: ${hardwareUnitList.size}")
        storageUnitsLiveData.observeForever(storageUnitsDataObserver)
        hardwareUnitList.values.forEach(this::hwUnitStart)
    }

    fun stop() {
        Timber.e("start; hardwareUnitList.size: ${hardwareUnitList.size}")
        storageUnitsLiveData.removeObserver(storageUnitsDataObserver)
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

    private val storageUnitsDataObserver = Observer<Pair<Int,*>> { pair ->
        Timber.d("storageUnitsDataObserver changed: $pair")
        pair?.let{ (action, value) ->
            when (action) {
                StorageUnitsLiveData.NODE_ACTION_CHANGED -> when (value) {
                    is StorageUnit<*> -> {
                        Timber.d("storageUnitsDataObserver StorageUnit NODE_ACTION_CHANGED: ${storageUnitUnitList[value.name]}")
                        storageUnitUnitList[value.name]?.run {
                            applyFunction(value.value)
                        }
                    }
                    is HomeUnit -> {
                    }
                    else -> {
                        Timber.e("storageUnitsDataObserver NODE_ACTION_CHANGED unsupported value: $value")
                    }
                }
                StorageUnitsLiveData.NODE_ACTION_ADDED -> when (value) {
                    is StorageUnit<*> -> {
                        val storageUnit = storageUnitUnitList[value.name]
                        Timber.d("storageUnitsDataObserver OLD StorageUnit $storageUnit ; NEW StorageUnit: $value")
                        val newVal = value as StorageUnit<Any>
                        storageUnitTypeIndicatorMap[newVal.firebaseTableName]?.isInstance(Boolean::class.java).let {
                            newVal.applyFunction = booleanApplyFunction
                        }
                        storageUnitUnitList[newVal.name] = newVal
                    }
                    is Room -> {
                        val room = rooms[value.name]
                        Timber.d("storageUnitsDataObserver OLD Room $room ; NEW Room: $value")
                        rooms[value.name] = value
                    }
                    is HomeUnit -> {
                        // TODO: consider this unit is already present in hardwareUnitList
                        var unit = hardwareUnitList[value.name]
                        Timber.d("storageUnitsDataObserver HomeUnit NODE_ACTION_ADDED: $unit")
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
                                        value.softAddress!!) as Sensor<Any>
                            }
                            BoardConfig.TEMP_PRESS_SENSOR_BMP280 -> {
                                unit = HomeUnitI2CTempPressBMP280Sensor(
                                        value.name,
                                        value.location,
                                        value.pinName,
                                        value.softAddress!!) as Sensor<Any>
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
                                        value.internalPullUp!!) as Sensor<Any>
                            }
                            BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B0,
                            BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B7 -> {
                                unit = HomeUnitI2CMCP23017Actuator(
                                        value.name,
                                        value.location,
                                        value.pinName,
                                        value.softAddress!!,
                                        value.pinInterrupt!!,
                                        MCP23017Pin.Pin.valueOf(value.ioPin!!)) as Actuator<Any>
                            }
                        }
                        unit?.let {
                            Timber.w("HwUnit recreated connect and eventually listen to it")
                            hardwareUnitList[value.name] = it
                            hwUnitStart(it)
                        }
                    }
                    else -> {
                        Timber.e("storageUnitsDataObserver NODE_ACTION_ADDED unsupported value: $value")
                    }
                }
                StorageUnitsLiveData.NODE_ACTION_DELETED -> when (value) {
                    is StorageUnit<*> -> {
                        val result = storageUnitUnitList.remove(value.name)
                        Timber.d("storageUnitsDataObserver StorageUnit NODE_ACTION_DELETED: $result")
                    }
                    is Room -> {
                        val result = rooms.remove(value.name)
                        Timber.d("storageUnitsDataObserver Room NODE_ACTION_DELETED: $result")
                    }
                    is HomeUnit -> {
                        val result = hardwareUnitList.remove(value.name)
                        Timber.d("storageUnitsDataObserver HomeUnit NODE_ACTION_DELETED: $result")
                    }
                    else -> {
                        Timber.e("storageUnitsDataObserver NODE_ACTION_ADDED unsupported value: $value")
                    }
                }
                else -> {
                    Timber.e("storageUnitsDataObserver unsupported action: $action")
                }
            }
        }
    }

    override fun onUnitChanged(hwHomeUnit: HomeUnit, unitValue: Any?, updateTime: String) {
        Timber.d("onUnitChanged unit: $hwHomeUnit; unitValue: $unitValue; updateTime: $updateTime")
        homeInformationRepository.logUnitEvent(HomeUnitLog(hwHomeUnit, unitValue, updateTime))

        storageUnitUnitList.values.find {
            it.hardwareUnitName == hwHomeUnit.name
        }?.apply {
            // We need to handel differently values of non Basic Types
            if (unitValue is TemperatureAndPressure) {
                Timber.d("Received TemperatureAndPressure $value")
                // obsolete code start
                homeInformationRepository.saveTemperature(unitValue.temperature)
                homeInformationRepository.savePressure(unitValue.pressure)
                // obsolete code end
                if (firebaseTableName == HOME_TEMPERATURES) {
                    value = unitValue.temperature
                } else if (firebaseTableName == HOME_PRESSURES) {
                    value = unitValue.pressure
                }
            } else {
                value = unitValue
            }
            homeInformationRepository.saveStorageUnit(this)
        }
    }

    fun saveToRepository() {
        rooms.values.forEach { homeInformationRepository.saveRoom(it) }
        storageUnitUnitList.values.forEach { homeInformationRepository.saveStorageUnit(it) }
        hardwareUnitList.values.forEach { homeInformationRepository.saveHardwareUnit(it.homeUnit) }
    }
}