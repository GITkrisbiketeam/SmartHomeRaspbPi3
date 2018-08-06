package com.krisbiketeam.data.storage.dto

import com.google.firebase.database.Exclude

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

data class StorageUnit<T>(var name: String = "",
                          var firebaseTableName: String = "",
                          var room: String = "",
                          var hardwareUnitName: String = "",
                          var value: T? = null,
                          val unitsTasks: MutableList<UnitTask> = ArrayList()) {

    @Exclude
    @set:Exclude
    @get:Exclude
    var applyFunction: StorageUnit<T>.(Any?) -> Unit = { Unit }
}