package com.krisbiketeam.data.storage.dto

import com.google.firebase.database.Exclude
import com.krisbiketeam.data.storage.FirebaseTables.*

typealias LightType = Boolean
typealias Light = StorageUnit<LightType>
typealias LightSwitchType = Boolean
typealias LightSwitch = StorageUnit<LightSwitchType>
typealias ReedSwitchType = Boolean
typealias ReedSwitch = StorageUnit<ReedSwitchType>
typealias MotionType = Boolean
typealias Motion = StorageUnit<MotionType>
typealias TemperatureType = Float
typealias Temperature = StorageUnit<TemperatureType>
typealias PressureType = Float
typealias Pressure = StorageUnit<PressureType>
typealias BlindType = Int
typealias Blind = StorageUnit<BlindType>

val storageUnitTypeIndicatorMap: HashMap<String, Class<*>> = hashMapOf(
        HOME_LIGHTS to LightType::class.java,
        HOME_LIGHT_SWITCHES to LightSwitchType::class.java,
        HOME_REED_SWITCHES to ReedSwitchType::class.java,
        HOME_MOTIONS to MotionType::class.java,
        HOME_TEMPERATURES to TemperatureType::class.java,
        HOME_PRESSURES to PressureType::class.java,
        HOME_BLINDS to BlindType::class.java
)

data class StorageUnit<T>(var name: String = "", // Name should be unique for all units
                          var firebaseTableName: String = "",
                          var room: String = "",
                          var hardwareUnitName: String = "",
                          var value: T? = null,
                          val unitsTasks: MutableList<UnitTask> = ArrayList()) {

    @Exclude
    @set:Exclude
    @get:Exclude
    var applyFunction: StorageUnit<T>.(Any?) -> Unit = { _: Any? -> Unit}
}