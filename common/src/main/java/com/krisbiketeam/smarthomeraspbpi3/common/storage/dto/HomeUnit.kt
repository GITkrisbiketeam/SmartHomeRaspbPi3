package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

import com.google.firebase.database.Exclude
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.*

typealias LightType = Boolean
typealias Light = HomeUnit<LightType?>
typealias ActuatorType = Boolean
typealias Actuator = HomeUnit<LightType?>
typealias LightSwitchType = Boolean
typealias LightSwitch = HomeUnit<LightSwitchType?>
typealias ReedSwitchType = Boolean
typealias ReedSwitch = HomeUnit<ReedSwitchType?>
typealias MotionType = Boolean
typealias Motion = HomeUnit<MotionType?>
typealias TemperatureType = Float
typealias Temperature = HomeUnit<TemperatureType?>
typealias PressureType = Float
typealias Pressure = HomeUnit<PressureType?>
typealias HumidityType = Float
typealias Humidity = HomeUnit<HumidityType?>
typealias BlindType = Int
typealias Blind = HomeUnit<BlindType?>

val homeUnitTypeIndicatorMap: HashMap<String, Class<out Any?>> = hashMapOf(
        HOME_LIGHTS to LightType::class.java,
        HOME_ACTUATORS to ActuatorType::class.java,
        HOME_LIGHT_SWITCHES to LightSwitchType::class.java,
        HOME_REED_SWITCHES to ReedSwitchType::class.java,
        HOME_MOTIONS to MotionType::class.java,
        HOME_TEMPERATURES to TemperatureType::class.java,
        HOME_PRESSURES to PressureType::class.java,
        HOME_HUMIDITY to HumidityType::class.java,
        HOME_BLINDS to BlindType::class.java
)

val HOME_STORAGE_UNITS: List<String> = homeUnitTypeIndicatorMap.keys.toList()

data class HomeUnit<T>(var name: String = "", // Name should be unique for all units
                       var type: String = "",
                       var room: String = "",
                       var hwUnitName: String = "",
                       var value: T? = null,
                       var lastUpdateTime: Long? = null,
                       var firebaseNotify: Boolean = false,
                       var unitsTasks: Map<String,UnitTask> = HashMap()) {
    constructor(homeUnit: HomeUnit<T?>) : this(
            homeUnit.name,
            homeUnit.type,
            homeUnit.room,
            homeUnit.hwUnitName,
            homeUnit.value,
            homeUnit.lastUpdateTime,
            homeUnit.firebaseNotify,
            homeUnit.unitsTasks)

    @Exclude
    @set:Exclude
    @get:Exclude
    var applyFunction: HomeUnit<T>.(Any) -> Unit = { Unit}

    fun makeInvariant(): HomeUnit<Any?>{
        return HomeUnit(
                name,
                type,
                room,
                hwUnitName,
                value,
                lastUpdateTime,
                firebaseNotify,
                unitsTasks)
    }
    fun makeNotification(): HomeUnit<Any?>{
        return HomeUnit(
                name,
                type,
                room,
                hwUnitName,
                value,
                lastUpdateTime)
    }
}