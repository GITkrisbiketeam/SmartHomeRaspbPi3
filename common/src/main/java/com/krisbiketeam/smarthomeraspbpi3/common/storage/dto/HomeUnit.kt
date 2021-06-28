package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

import com.google.firebase.database.Exclude
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.*

typealias LightType = Boolean
typealias Light = HomeUnit<LightType>
typealias ActuatorType = Boolean
typealias Actuator = HomeUnit<LightType>
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
typealias HumidityType = Float
typealias Humidity = HomeUnit<HumidityType>
typealias BlindType = Int
typealias Blind = HomeUnit<BlindType>

val homeUnitTypeIndicatorMap: HashMap<String, Class<out Any?>> = hashMapOf(
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

val HOME_ACTION_STORAGE_UNITS: List<String> = listOf(HOME_LIGHT_SWITCHES, HOME_BLINDS, HOME_ACTUATORS)

data class HomeUnit<T:Any>(var name: String = "", // Name should be unique for all units
                       var type: String = "",
                       var room: String = "",
                       var hwUnitName: String? = "",
                       var value: T? = null,
                       var lastUpdateTime: Long? = null,
                       var secondHwUnitName: String? = null,
                       var secondValue: T? = null,
                       var secondLastUpdateTime: Long? = null,
                       var min: T? = null,
                       var minLastUpdateTime: Long? = null,
                       var max: T? = null,
                       var maxLastUpdateTime: Long? = null,
                       var firebaseNotify: Boolean = false,
                       @TriggerType var firebaseNotifyTrigger: String? = null,
                       var showInTaskList: Boolean = false,
                       var unitsTasks: Map<String,UnitTask> = HashMap()) {
    constructor(homeUnit: HomeUnit<T>) : this(
            homeUnit.name,
            homeUnit.type,
            homeUnit.room,
            homeUnit.hwUnitName,
            homeUnit.value,
            homeUnit.lastUpdateTime,
            homeUnit.secondHwUnitName,
            homeUnit.secondValue,
            homeUnit.secondLastUpdateTime,
            homeUnit.min,
            homeUnit.minLastUpdateTime,
            homeUnit.max,
            homeUnit.maxLastUpdateTime,
            homeUnit.firebaseNotify,
            homeUnit.firebaseNotifyTrigger,
            homeUnit.showInTaskList,
            homeUnit.unitsTasks)

    @Exclude
    @set:Exclude
    @get:Exclude
    var applyFunction: suspend HomeUnit<in Any>.(Any) -> Unit = { }

    fun makeInvariant(): HomeUnit<Any>{
        return HomeUnit(
                name,
                type,
                room,
                hwUnitName,
                value,
                lastUpdateTime,
                secondHwUnitName,
                secondValue,
                secondLastUpdateTime,
                min,
                minLastUpdateTime,
                max,
                maxLastUpdateTime,
                firebaseNotify,
                firebaseNotifyTrigger,
                showInTaskList,
                unitsTasks)
    }
    fun makeNotification(): HomeUnit<T>{
        return HomeUnit(
                name,
                type,
                room,
                hwUnitName,
                value,
                lastUpdateTime,
                secondHwUnitName,
                secondValue,
                secondLastUpdateTime,
                min,
                minLastUpdateTime,
                max,
                maxLastUpdateTime)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HomeUnit<*>

        if (name != other.name) return false
        if (type != other.type) return false
        if (room != other.room) return false
        if (hwUnitName != other.hwUnitName) return false
        if (value != other.value) return false
        if (lastUpdateTime != other.lastUpdateTime) return false
        if (secondHwUnitName != other.secondHwUnitName) return false
        if (secondValue != other.secondValue) return false
        if (secondLastUpdateTime != other.secondLastUpdateTime) return false
        if (min != other.min) return false
        if (minLastUpdateTime != other.minLastUpdateTime) return false
        if (max != other.max) return false
        if (maxLastUpdateTime != other.maxLastUpdateTime) return false
        if (firebaseNotify != other.firebaseNotify) return false
        if (firebaseNotifyTrigger != other.firebaseNotifyTrigger) return false
        if (showInTaskList != other.showInTaskList) return false
        if (unitsTasks != other.unitsTasks) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + room.hashCode()
        result = 31 * result + (hwUnitName?.hashCode() ?: 0)
        result = 31 * result + (value?.hashCode() ?: 0)
        result = 31 * result + (lastUpdateTime?.hashCode() ?: 0)
        result = 31 * result + (secondHwUnitName?.hashCode() ?: 0)
        result = 31 * result + (secondValue?.hashCode() ?: 0)
        result = 31 * result + (secondLastUpdateTime?.hashCode() ?: 0)
        result = 31 * result + (min?.hashCode() ?: 0)
        result = 31 * result + (minLastUpdateTime?.hashCode() ?: 0)
        result = 31 * result + (max?.hashCode() ?: 0)
        result = 31 * result + (maxLastUpdateTime?.hashCode() ?: 0)
        result = 31 * result + firebaseNotify.hashCode()
        result = 31 * result + (firebaseNotifyTrigger?.hashCode() ?: 0)
        result = 31 * result + showInTaskList.hashCode()
        result = 31 * result + unitsTasks.hashCode()
        return result
    }
}