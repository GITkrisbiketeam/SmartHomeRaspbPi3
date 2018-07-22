package com.krisbiketeam.smarthomeraspbpi3

import android.arch.lifecycle.Observer
import com.krisbiketeam.data.storage.FirebaseTables
import com.krisbiketeam.data.storage.HomeInformationRepository
import com.krisbiketeam.data.storage.dto.*
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.BaseUnit
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import com.krisbiketeam.smarthomeraspbpi3.units.hardware.*
import timber.log.Timber

class Home(private val homeInformationRepository: HomeInformationRepository) : Sensor.HomeUnitListener<Any> {
    private var unitsLiveData = homeInformationRepository.unitsLiveData()


    private var rooms: MutableMap<String, Room> = HashMap()
    private var lights: MutableMap<String, Light> = HashMap()
    private var lightSwitches: MutableMap<String, LightSwitch> = HashMap()
    private var reedSwitches: MutableMap<String, ReedSwitch> = HashMap()
    private var motions: MutableMap<String, Motion> = HashMap()
    private var temperatures: MutableMap<String, Temperature> = HashMap()
    private var pressures: MutableMap<String, Pressure> = HashMap()
    private var blinds: MutableMap<String, Blind> = HashMap()

    private val unitList: MutableMap<String, BaseUnit<Any>> = HashMap()

    private var booleanApplyFunction: StorageUnit<Boolean>.(Any?) -> Unit = {
        Timber.d("booleanApplyFunction it: $it this: $this")
        if (it is Boolean) {
            val newVal = it
            this.unitsTasks.forEach {
                var unit: StorageUnit<Boolean>? = null
                when (it.unitType) {
                    FirebaseTables.HOME_LIGHTS -> lights[it.unitName]?.run {
                        Timber.d("booleanApplyFunction task: $it for this: $this")
                        unit = this
                    }
                }
                unit?.run {
                    this.value = newVal
                    Timber.d("booleanApplyFunction run on: $it from : $this")
                    homeInformationRepository.saveStorageUnit(this)
                }
            }
        } else {
            Timber.e("booleanApplyFunction new value is not Boolean or is null")
        }
    }

    init {

        initUnitList()

        var roomName = "Kitchen"
        var temp = Temperature("Kitchen 1", FirebaseTables.HOME_TEMPERATURES, roomName, BoardConfig.TEMP_PRESS_SENSOR_BMP280)
        var pressure = Pressure("Kitchen 1", FirebaseTables.HOME_PRESSURES, roomName, BoardConfig.TEMP_PRESS_SENSOR_BMP280)
        var light = Light("Kitchen 1", FirebaseTables.HOME_LIGHTS, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B0)
        var lightSwitch = LightSwitch("Kitchen 1", FirebaseTables.HOME_LIGHT_SWITCHES, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_IN_A7)
        lightSwitch.unitsTasks.add(UnitTask(light.name, light.firebaseTableName))
        lightSwitch.applyFunction = booleanApplyFunction
        var reedSwitch = ReedSwitch("Kitchen 1", FirebaseTables.HOME_REED_SWITCHES, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_IN_A6)
        val motion = Motion("Kitchen 1", FirebaseTables.HOME_MOTIONS, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_IN_A0)
        temperatures[temp.name] = temp
        lights[light.name] = light
        lightSwitches[lightSwitch.name] = lightSwitch
        reedSwitches[reedSwitch.name] = reedSwitch
        motions[motion.name] = motion
        pressures[pressure.name] = pressure
        var room = Room(roomName, 0, listOf(light.name), listOf(lightSwitch.name), listOf(reedSwitch.name), listOf(motion.name), listOf(temp.name), listOf(pressure.name))
        rooms[room.name] = room

        roomName = "Bathroom"
        temp = Temperature("Bathroom 1", FirebaseTables.HOME_TEMPERATURES, roomName, BoardConfig.TEMP_SENSOR_TMP102)
        light = Light("Bathroom 1", FirebaseTables.HOME_LIGHTS, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B7)
        lightSwitch = LightSwitch("Bathroom 1", FirebaseTables.HOME_LIGHT_SWITCHES, roomName, BoardConfig.IO_EXTENDER_MCP23017_1_IN_A5)
        lightSwitch.unitsTasks.add(UnitTask(light.name, light.firebaseTableName))
        lightSwitch.applyFunction = booleanApplyFunction
        temperatures[temp.name] = temp
        lights[light.name] = light
        lightSwitches[lightSwitch.name] = lightSwitch
        room = Room(roomName, 0, listOf(light.name), listOf(lightSwitch.name), ArrayList(), ArrayList(), listOf(temp.name))
        rooms[room.name] = room


        saveToRepository()
    }

    private fun initUnitList() {
        /*val motion = HomeUnitGpioSensor(BoardConfig.MOTION_1, "Raspberry Pi",
                BoardConfig.MOTION_1_PIN,
                Gpio.ACTIVE_HIGH) as Sensor<Any>
        unitList[BoardConfig.MOTION_1] = motion*/

        /*val contactron = HomeUnitGpioNoiseSensor(BoardConfig.REED_SWITCH_1, "Raspberry Pi", BoardConfig
                .REED_SWITCH_1_PIN, Gpio.ACTIVE_LOW) as Sensor<Any>
        unitList[BoardConfig.REED_SWITCH_1] = contactron*/

        val temperatureSensor = HomeUnitI2CTempTMP102Sensor(BoardConfig.TEMP_SENSOR_TMP102,
                "Raspberry Pi",
                BoardConfig.TEMP_SENSOR_TMP102_PIN,
                BoardConfig.TEMP_SENSOR_TMP102_ADDR) as Sensor<Any>
        unitList[BoardConfig.TEMP_SENSOR_TMP102] = temperatureSensor

        val tempePressSensor = HomeUnitI2CTempPressBMP280Sensor(BoardConfig.TEMP_PRESS_SENSOR_BMP280,
                "Raspberry Pi",
                BoardConfig.TEMP_PRESS_SENSOR_BMP280_PIN,
                BoardConfig.TEMP_PRESS_SENSOR_BMP280_ADDR) as Sensor<Any>
        unitList[BoardConfig.TEMP_PRESS_SENSOR_BMP280] = tempePressSensor

        val mcpLightSwitch = HomeUnitI2CMCP23017Sensor(BoardConfig.IO_EXTENDER_MCP23017_1_IN_A7,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_1_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_1_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_IN_A7_PIN,
                true) as Sensor<Any>
        unitList[BoardConfig.IO_EXTENDER_MCP23017_1_IN_A7] = mcpLightSwitch

        val mcpContactron = HomeUnitI2CMCP23017Sensor(BoardConfig.IO_EXTENDER_MCP23017_1_IN_A6,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_1_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_1_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_IN_A6_PIN,
                true) as Sensor<Any>
        unitList[BoardConfig.IO_EXTENDER_MCP23017_1_IN_A6] = mcpContactron

        val mcpLightSwitch2 = HomeUnitI2CMCP23017Sensor(BoardConfig.IO_EXTENDER_MCP23017_1_IN_A5,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_1_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_1_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_IN_A5_PIN,
                true) as Sensor<Any>
        unitList[BoardConfig.IO_EXTENDER_MCP23017_1_IN_A5] = mcpLightSwitch2

        val mcpMotion = HomeUnitI2CMCP23017Sensor(BoardConfig.IO_EXTENDER_MCP23017_1_IN_A0,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_1_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_1_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_IN_A0_PIN) as Sensor<Any>
        unitList[BoardConfig.IO_EXTENDER_MCP23017_1_IN_A0] = mcpMotion

        val mcpLed = HomeUnitI2CMCP23017Actuator(BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B0,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_1_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_1_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B0_PIN) as Actuator<Any>
        unitList[BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B0] = mcpLed

        val mcpLed2 = HomeUnitI2CMCP23017Actuator(BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B7,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_MCP23017_1_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_ADDR,
                BoardConfig.IO_EXTENDER_MCP23017_1_INTA_PIN,
                BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B7_PIN) as Actuator<Any>
        unitList[BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B7] = mcpLed2
    }

    fun start() {
        unitsLiveData.observeForever(unitsDataObserver)
        unitList.values.forEach { unit ->
            Timber.d("onStart connect unit: ${unit.homeUnit}")
            unit.connect()
            if (unit is Sensor) {
                unit.registerListener(this)
            }
        }
    }

    fun stop() {
        unitsLiveData.removeObserver(unitsDataObserver)
        for (unit in unitList.values) {
            try {
                // close will automatically unregister listener
                unit.close()
            } catch (e: Exception) {
                Timber.e("Error on PeripheralIO API", e)
            }
        }
    }

    private val unitsDataObserver = Observer<Any> { value ->
        Timber.d("unitsDataObserver changed: $value")
        when (value) {
            is StorageUnit<*> -> {
                when (value.firebaseTableName) {
                    FirebaseTables.HOME_LIGHT_SWITCHES -> {
                        val lightSwitch = lightSwitches[value.name]
                        Timber.d("unitsDataObserver lightSwitch: $lightSwitch")
                        lightSwitch?.applyFunction?.invoke(lightSwitch, value.value)
                    }
                    FirebaseTables.HOME_LIGHTS -> {
                        val unit = unitList[value.unitName]
                        Timber.d("unitsDataObserver unit: $unit")
                        if (unit is Actuator) {
                            Timber.d("unitsDataObserver unit setValue")
                            unit.setValue(value.value)
                        }
                    }
                }
            }
        }
    }

    override fun onUnitChanged(homeUnit: HomeUnitLog<out Any>) {
        Timber.d("onUnitChanged unit: $homeUnit")
        homeInformationRepository.logUnitEvent(homeUnit)
        val value = homeUnit.value
        when (homeUnit.name) {
            BoardConfig.TEMP_PRESS_SENSOR_BMP280 -> if (value is TemperatureAndPressure) {
                Timber.d("Received TemperatureAndPressure $homeUnit.value")
                // obsolete code start
                homeInformationRepository.saveTemperature(value.temperature)
                homeInformationRepository.savePressure(value.pressure)
                // obsolete code end
                homeInformationRepository.saveStorageUnit(
                        temperatures.values.first().apply {
                            this.value = value.temperature
                        })
            }
            BoardConfig.TEMP_SENSOR_TMP102 -> if (value is Float) {
                Timber.d("Received Temperature $value")
                homeInformationRepository.saveStorageUnit(
                        temperatures.values.last().apply {
                            this.value = value
                        })
            }
            BoardConfig.IO_EXTENDER_MCP23017_1_IN_A7 -> if (value is Boolean) {
                //lightOnOffOneRainbowLed(1, (value as Boolean?)!!)
                /*unit = unitList[BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B0]
                if (unit is Actuator && value != null) {
                    unit.setValue(value)
                }*/
                homeInformationRepository.saveStorageUnit(
                        lightSwitches.values.first().apply {
                            this.value = value
                        })
            }
            BoardConfig.IO_EXTENDER_MCP23017_1_IN_A5 -> if (value is Boolean) {
                //lightOnOffOneRainbowLed(1, (value as Boolean?)!!)
                /*unit = unitList[BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B0]
                if (unit is Actuator && value != null) {
                    unit.setValue(value)
                }*/
                homeInformationRepository.saveStorageUnit(
                        lightSwitches.values.last().apply {
                            this.value = value
                        })
            }
        }
    }


    fun saveToRepository() {
        rooms.values.forEach { homeInformationRepository.saveRoom(it) }
        blinds.values.forEach { homeInformationRepository.saveStorageUnit(it) }
        lights.values.forEach { homeInformationRepository.saveStorageUnit(it) }
        motions.values.forEach { homeInformationRepository.saveStorageUnit(it) }
        pressures.values.forEach { homeInformationRepository.saveStorageUnit(it) }
        lightSwitches.values.forEach { homeInformationRepository.saveStorageUnit(it) }
        reedSwitches.values.forEach { homeInformationRepository.saveStorageUnit(it) }
        temperatures.values.forEach { homeInformationRepository.saveStorageUnit(it) }
    }
}