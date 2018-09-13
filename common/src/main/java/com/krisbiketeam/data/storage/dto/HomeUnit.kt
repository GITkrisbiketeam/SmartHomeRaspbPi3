package com.krisbiketeam.data.storage.dto

import com.google.firebase.database.Exclude
import com.krisbiketeam.data.storage.firebaseTables.*

typealias LightType = Boolean
typealias Light = HomeUnit<LightType>
typealias LightSwitchType = Boolean
typealias LightSwitch = HomeUnit<LightSwitchType>
typealias ReedSwitchType = Boolean
typealias ReedSwitch = HomeUnit<ReedSwitchType>
typealias MotionType = Boolean
typealias Motion = HomeUnit<MotionType>
typealias TemperatureType = Float
typealias Temperature = HomeUnit<TemperatureType>
typealias PressureType = Float
typealias Pressure = HomeUnit<PressureType>
typealias BlindType = Int
typealias Blind = HomeUnit<BlindType>

val homeUnitTypeIndicatorMap: HashMap<String, Class<out Any>> = hashMapOf(
        HOME_LIGHTS to LightType::class.java,
        HOME_LIGHT_SWITCHES to LightSwitchType::class.java,
        HOME_REED_SWITCHES to ReedSwitchType::class.java,
        HOME_MOTIONS to MotionType::class.java,
        HOME_TEMPERATURES to TemperatureType::class.java,
        HOME_PRESSURES to PressureType::class.java,
        HOME_BLINDS to BlindType::class.java
)

val HOME_STORAGE_UNITS: List<String> = listOf(
        HOME_LIGHTS,
        HOME_LIGHT_SWITCHES,
        HOME_REED_SWITCHES,
        HOME_MOTIONS,
        HOME_TEMPERATURES,
        HOME_PRESSURES,
        HOME_BLINDS)

data class HomeUnit<T>(var name: String = "", // Name should be unique for all units
                       var firebaseTableName: String = "",
                       var room: String = "",
                       var hardwareUnitName: String = "",
                       var value: T? = null,
                       val firebaseNotify: Boolean? = null,
                       val unitsTasks: MutableList<UnitTask> = ArrayList()) {
    constructor(homeUnit: HomeUnit<T>) : this(
            homeUnit.name,
            homeUnit.firebaseTableName,
            homeUnit.room,
            homeUnit.hardwareUnitName,
            homeUnit.value,
            homeUnit.firebaseNotify,
            homeUnit.unitsTasks)

    @Exclude
    @set:Exclude
    @get:Exclude
    var applyFunction: HomeUnit<T>.(Any?) -> Unit = { _: Any? -> Unit}

    fun makeInvariant(): HomeUnit<Any>{
        return HomeUnit<Any>(
                name,
                firebaseTableName,
                room,
                hardwareUnitName,
                value,
                firebaseNotify,
                unitsTasks)
    }
    fun makeNotification(): HomeUnit<Any>{
        return HomeUnit<Any>(
                name,
                firebaseTableName,
                room,
                hardwareUnitName,
                value)
    }
}