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

typealias LightSw = StorageUnit<Boolean>
typealias Lig = StorageUnit<Boolean>



class Home(val homeInformationRepository: HomeInformationRepository) : Sensor.HomeUnitListener<Any> {
    private var unitsLiveData = homeInformationRepository.unitsLiveData()


    var rooms: MutableMap<String, Room> = HashMap()
    var temperatures: MutableMap<String, Temperature> = HashMap()
    var lights: MutableMap<String, Lig> = HashMap()
    var lightSwitches: MutableMap<String, LightSw> = HashMap()
    var reedSwitches: MutableMap<String, ReedSwitch> = HashMap()
    var motions: MutableMap<String, Motion> = HashMap()
    var blinds: MutableMap<String, Blind> = HashMap()
    var pressures: MutableMap<String, Pressure> = HashMap()

    val unitList: MutableMap<String, BaseUnit<Any>> = HashMap()


    init {

        initUnitList()

        var roomName = "Kitchen"
        var temp = Temperature("Kitchen 1", roomName, BoardConfig.TEMP_PRESS_SENSOR_BMP280)
        var pressure = Pressure("Kitchen 1", roomName, BoardConfig.TEMP_PRESS_SENSOR_BMP280)
        var light = Lig("Kitchen 1", roomName, BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B0, FirebaseTables.HOME_LIGHTS)
        var lightSwitch = LightSw("Kitchen 1", roomName, BoardConfig.IO_EXTENDER_MCP23017_1_IN_A7, FirebaseTables.HOME_LIGHT_SWITCHES)
        lightSwitch.unitsTasks.add(UnitTask(light.name, light.firebaseTableName))
        lightSwitch.applyFunction = {
            Timber.d("lightSwitch applyFunction it: $it this: $this")
            val value = it
            this.unitsTasks.forEach {
                val l = lights[it.unitName]
                l?.run {
                    this.value = value
                    Timber.d("lightSwitch applyFunction run it: $it this: $this")
                    homeInformationRepository.saveStorageUnit(this)
                }
            }
        }
        var reedSwitch = ReedSwitch("Kitchen 1", roomName, BoardConfig.IO_EXTENDER_MCP23017_1_IN_A6)
        val motion = Motion("Kitchen 1", roomName, BoardConfig.IO_EXTENDER_MCP23017_1_IN_A0)
        temperatures[temp.name] = temp
        lights[light.name] = light
        lightSwitches[lightSwitch.name] = lightSwitch
        reedSwitches[reedSwitch.name] = reedSwitch
        motions[motion.name] = motion
        pressures[pressure.name] = pressure
        var room = Room(roomName, 0, listOf(temp), /*listOf(light)*/emptyList(), /*listOf(lightSwitch)*/emptyList(), listOf(reedSwitch), listOf(motion), ArrayList(), listOf(pressure))
        rooms[room.name] = room

        roomName = "Bathroom"
        temp = Temperature("Bathroom 1", roomName, BoardConfig.TEMP_SENSOR_TMP102)
        //light = Light("Bathroom 1", roomName, BoardConfig.IO_EXTENDER_MCP23017_1_OUT_B7)
        /*lightSwitch = LightSwitch("Bathroom 1", roomName, BoardConfig.IO_EXTENDER_MCP23017_1_IN_A5, light.name)
        lightSwitch.applyFunction = {
            Timber.d("lightSwitch applyFunction it: $it this: $this")
            val light = lights[this.lightName]
            light?.run{
                this.on = it
                Timber.d("lightSwitch applyFunction run it: $it this: $this")
                homeInformationRepository.saveLight(this)
            }
        }*/
        temperatures[temp.name] = temp
        lights[light.name] = light
        lightSwitches[lightSwitch.name] = lightSwitch
        room = Room(roomName, 0, listOf(temp)/*, listOf(light), listOf(lightSwitch)*/)
        rooms[room.name] = room

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
                        lightSwitch ?. applyFunction ?. invoke (lightSwitch, value.value as Boolean)
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

            is LightSwitch -> {
                val lightSwitch = lightSwitches[value.name]
                Timber.d("unitsDataObserver lightSwitch: $lightSwitch")
                lightSwitch?.applyFunction?.invoke(lightSwitch, value.active)
            }
            is Light -> {
                val unit = unitList[value.unitName]
                Timber.d("unitsDataObserver unit: $unit")
                if (unit is Actuator) {
                    Timber.d("unitsDataObserver unit setValue")
                    unit.setValue(value.on)
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
                homeInformationRepository.saveTemperature(
                        temperatures.values.first().apply {
                            this.value = value.temperature
                        })
            }
            BoardConfig.TEMP_SENSOR_TMP102 -> if (value is Float) {
                Timber.d("Received Temperature $value")
                homeInformationRepository.saveTemperature(
                        temperatures.values.last().apply {
                            this.value = value
                        })
            }
            BoardConfig.MOTION_1 -> if (value is Boolean) {
                //lightOnOffOneRainbowLed(0, (value as Boolean?)!!)
                homeInformationRepository.saveMotion(
                        motions.values.first().apply {
                            this.active = value
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


    /*fun saveToRepository(){
        rooms.values.forEach{homeInformationRepository.saveRoom(it)}
        blinds.values.forEach{homeInformationRepository.saveBlind(it)}
        lights.values.forEach{homeInformationRepository.saveLight(it)}
        motions.values.forEach{homeInformationRepository.saveMotion(it)}
        pressures.values.forEach{homeInformationRepository.savePressure(it)}
        lightSwitches.values.forEach{homeInformationRepository.saveLightSwitch(it)}
        reedSwitches.values.forEach{homeInformationRepository.saveReedSwitch(it)}
        temperatures.values.forEach{homeInformationRepository.saveTemperature(it)}
    }*/
}