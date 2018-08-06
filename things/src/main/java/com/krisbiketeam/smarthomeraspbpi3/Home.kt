package com.krisbiketeam.smarthomeraspbpi3

import android.arch.lifecycle.Observer
import com.krisbiketeam.data.storage.FirebaseTables
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_PRESSURES
import com.krisbiketeam.data.storage.FirebaseTables.Companion.HOME_TEMPERATURES
import com.krisbiketeam.data.storage.HomeInformationRepository
import com.krisbiketeam.data.storage.dto.*
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.BaseUnit
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import com.krisbiketeam.smarthomeraspbpi3.units.hardware.*
import timber.log.Timber

class Home(private val homeInformationRepository: HomeInformationRepository) : Sensor.HomeUnitListener<Any> {
    private var storageUnitsLiveData = homeInformationRepository.unitsLiveData()


    private var rooms: MutableMap<String, Room> = HashMap()

    private val storageUnitUnitList: MutableMap<String, StorageUnit<Any>> = HashMap()

    private val hardwareUnitList: MutableMap<String, BaseUnit<Any>> = HashMap()

    private var booleanApplyFunction: StorageUnit<in Boolean>.(Any?) -> Unit = {
        Timber.d("booleanApplyFunction it: $it this: $this")
        if (it is Boolean) {
            val newVal = it
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
                            Timber.d("booleanApplyFunction unit setValue")
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

        initHardwareUnitList()

        var roomName = "Kitchen"

        var temp = Temperature("Kitchen 1 Temp", FirebaseTables.HOME_TEMPERATURES, roomName, BoardConfig.TEMP_PRESS_SENSOR_BMP280) as StorageUnit<Any>

        var pressure = Pressure("Kitchen 1 Press", FirebaseTables.HOME_PRESSURES, roomName, BoardConfig.TEMP_PRESS_SENSOR_BMP280) as StorageUnit<Any>

        var light = Light("Kitchen 1 Light", FirebaseTables.HOME_LIGHTS, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B0) as StorageUnit<Any>
        light.unitsTasks.add(UnitTask(hardwareUnitName = light.hardwareUnitName))
        light.applyFunction = booleanApplyFunction

        var lightSwitch = LightSwitch("Kitchen 1 Light Switch", FirebaseTables.HOME_LIGHT_SWITCHES, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_IN_A7) as StorageUnit<Any>
        lightSwitch.unitsTasks.add(UnitTask(storageUnitName = light.name))
        lightSwitch.applyFunction = booleanApplyFunction

        var reedSwitch = ReedSwitch("Kitchen 1 Reed Switch", FirebaseTables.HOME_REED_SWITCHES, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_IN_A6) as StorageUnit<Any>

        val motion = Motion("Kitchen 1 Motion Sensor", FirebaseTables.HOME_MOTIONS, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_IN_A0) as StorageUnit<Any>

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

        temp = Temperature("Bathroom 1 Temp", FirebaseTables.HOME_TEMPERATURES, roomName, BoardConfig.TEMP_SENSOR_TMP102) as StorageUnit<Any>

        light = Light("Bathroom 1 Light", FirebaseTables.HOME_LIGHTS, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B7) as StorageUnit<Any>
        light.unitsTasks.add(UnitTask(hardwareUnitName = light.hardwareUnitName))
        light.applyFunction = booleanApplyFunction

        lightSwitch = LightSwitch("Bathroom 1 Light Switch", FirebaseTables.HOME_LIGHT_SWITCHES, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_IN_A5) as StorageUnit<Any>
        lightSwitch.unitsTasks.add(UnitTask(storageUnitName = light.name))
        lightSwitch.applyFunction = booleanApplyFunction

        storageUnitUnitList[temp.name] = temp
        storageUnitUnitList[light.name] = light
        storageUnitUnitList[lightSwitch.name] = lightSwitch

        room = Room(roomName, 0, listOf(light.name), listOf(lightSwitch.name), ArrayList(), ArrayList(), listOf(temp.name))
        rooms[room.name] = room


        saveToRepository()
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
        storageUnitsLiveData.observeForever(storageUnitsDataObserver)
        hardwareUnitList.values.forEach { unit ->
            Timber.d("onStart connect unit: ${unit.homeUnit}")
            unit.connect()
            if (unit is Sensor) {
                unit.registerListener(this)
            }
        }
    }

    fun stop() {
        storageUnitsLiveData.removeObserver(storageUnitsDataObserver)
        for (unit in hardwareUnitList.values) {
            try {
                // close will automatically unregister listener
                unit.close()
            } catch (e: Exception) {
                Timber.e("Error on PeripheralIO API", e)
            }
        }
    }

    private val storageUnitsDataObserver = Observer<Any> { value ->
        Timber.d("storageUnitsDataObserver changed: $value")
        when (value) {
            is StorageUnit<*> -> {
                when (value.firebaseTableName) {
                    FirebaseTables.HOME_LIGHT_SWITCHES -> {
                        val lightSwitch = storageUnitUnitList[value.name]
                        Timber.d("storageUnitsDataObserver lightSwitch: $lightSwitch")
                        lightSwitch?.applyFunction?.invoke(lightSwitch, value.value)
                    }
                    FirebaseTables.HOME_LIGHTS -> {
                        val unit = hardwareUnitList[value.hardwareUnitName]
                        Timber.d("storageUnitsDataObserver unit: $unit")
                        if (unit is Actuator) {
                            Timber.d("storageUnitsDataObserver unit setValue")
                            unit.setValue(value.value)
                        }
                    }
                }
                Timber.d("storageUnitsDataObserver unit: ${storageUnitUnitList[value.name]}")
                storageUnitUnitList[value.name]?.run{
                    applyFunction.invoke(this, value.value)
                }
            }
        }
    }

    override fun onUnitChanged(hardwareHomeUnit: HomeUnitLog<out Any>) {
        Timber.d("onUnitChanged unit: $hardwareHomeUnit")
        homeInformationRepository.logUnitEvent(hardwareHomeUnit)

        val hardwareValue = hardwareHomeUnit.value
        Timber.d("Received Hardware changed $hardwareValue")

        storageUnitUnitList.values.find { it.hardwareUnitName == hardwareHomeUnit.name }?.apply {
            if (hardwareValue is TemperatureAndPressure) {
                Timber.d("Received TemperatureAndPressure $value")
                // obsolete code start
                homeInformationRepository.saveTemperature(hardwareValue.temperature)
                homeInformationRepository.savePressure(hardwareValue.pressure)
                // obsolete code end
                if(firebaseTableName == HOME_TEMPERATURES) {
                    value = hardwareValue.temperature
                } else if(firebaseTableName == HOME_PRESSURES) {
                    value = hardwareValue.pressure
                }
            } else {
                value = hardwareValue
            }
            homeInformationRepository.saveStorageUnit(this)
        }
    }


    fun saveToRepository() {
        rooms.values.forEach { homeInformationRepository.saveRoom(it) }
        storageUnitUnitList.values.forEach { homeInformationRepository.saveStorageUnit(it) }
    }
}