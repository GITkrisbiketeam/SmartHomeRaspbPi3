package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

import com.google.firebase.database.Exclude
import com.google.firebase.database.GenericTypeIndicator
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
typealias GasType = Float
typealias Gas = HomeUnit<GasType>
typealias GasPercentType = Float
typealias GasPercent = HomeUnit<GasPercentType>
typealias IaqType = Float
typealias Iaq = HomeUnit<IaqType>
typealias Co2Type = Float
typealias Co2 = HomeUnit<Co2Type>
typealias BreathVocType = Float
typealias BreathVoc = HomeUnit<BreathVocType>

val homeUnitValueTypeIndicatorMap: Map<String, Class<out Any?>> = mapOf(
    HOME_ACTUATORS to ActuatorType::class.java,
    HOME_LIGHT_SWITCHES to LightSwitchType::class.java,
    HOME_LIGHT_SWITCHES_V2 to LightSwitchType::class.java,
    HOME_REED_SWITCHES to ReedSwitchType::class.java,
    HOME_MOTIONS to MotionType::class.java,
    HOME_TEMPERATURES to TemperatureType::class.java,
    HOME_PRESSURES to PressureType::class.java,
    HOME_HUMIDITY to HumidityType::class.java,
    HOME_BLINDS to BlindType::class.java,
    HOME_GAS to GasType::class.java,
    HOME_GAS_PERCENT to GasPercentType::class.java,
    HOME_IAQ to IaqType::class.java,
    HOME_STATIC_IAQ to IaqType::class.java,
    HOME_CO2 to Co2Type::class.java,
    HOME_BREATH_VOC to BreathVocType::class.java
)
val homeUnitTypeIndicatorMap: HashMap<String, GenericTypeIndicator<out HomeUnit<out Any>>> by lazy {
    hashMapOf(HOME_ACTUATORS to object : GenericTypeIndicator<GenericHomeUnit<ActuatorType>>() {},
        HOME_LIGHT_SWITCHES to object :
            GenericTypeIndicator<GenericHomeUnit<LightSwitchType>>() {},
        HOME_LIGHT_SWITCHES_V2 to object :
            GenericTypeIndicator<LightSwitchHomeUnit<LightSwitchType>>() {},
        HOME_REED_SWITCHES to object :
            GenericTypeIndicator<GenericHomeUnit<ReedSwitchType>>() {},
        HOME_MOTIONS to object : GenericTypeIndicator<GenericHomeUnit<MotionType>>() {},
        HOME_TEMPERATURES to object :
            GenericTypeIndicator<GenericHomeUnit<TemperatureType>>() {},
        HOME_PRESSURES to object : GenericTypeIndicator<GenericHomeUnit<PressureType>>() {},
        HOME_HUMIDITY to object : GenericTypeIndicator<GenericHomeUnit<HumidityType>>() {},
        HOME_BLINDS to object : GenericTypeIndicator<GenericHomeUnit<BlindType>>() {})
}
fun getHomeUnitTypeIndicatorMap(type: String): GenericTypeIndicator<*> {
    return when(type) {
        HOME_ACTUATORS -> object : GenericTypeIndicator<GenericHomeUnit<ActuatorType>>() {}
        HOME_LIGHT_SWITCHES -> object :
            GenericTypeIndicator<GenericHomeUnit<LightSwitchType>>() {}
        HOME_LIGHT_SWITCHES_V2 -> object :
            GenericTypeIndicator<LightSwitchHomeUnit<LightSwitchType>>() {}
        HOME_REED_SWITCHES -> object :
            GenericTypeIndicator<GenericHomeUnit<ReedSwitchType>>() {}
        HOME_MOTIONS -> object : GenericTypeIndicator<GenericHomeUnit<MotionType>>() {}
        HOME_TEMPERATURES -> object :
            GenericTypeIndicator<GenericHomeUnit<TemperatureType>>() {}
        HOME_PRESSURES -> object : GenericTypeIndicator<GenericHomeUnit<PressureType>>() {}
        HOME_HUMIDITY -> object : GenericTypeIndicator<GenericHomeUnit<HumidityType>>() {}
        HOME_BLINDS -> object : GenericTypeIndicator<GenericHomeUnit<BlindType>>() {}
        else -> object : GenericTypeIndicator<GenericHomeUnit<Any>>() {}
    }
}

val HOME_STORAGE_UNITS: List<String> = homeUnitValueTypeIndicatorMap.keys.toList()

val HOME_ACTION_STORAGE_UNITS: List<String> =
    listOf(HOME_LIGHT_SWITCHES, HOME_BLINDS, HOME_ACTUATORS, HOME_LIGHT_SWITCHES_V2)

interface HomeUnit<T : Any> {
    var name: String // Name should be unique for all units
    var type: String
    var room: String
    var hwUnitName: String?
    var value: T?
    var lastUpdateTime: Long?

    var secondHwUnitName: String?
    var secondValue: T?
    var secondLastUpdateTime: Long?

    var min: T?
    var minLastUpdateTime: Long?
    var max: T?
    var maxLastUpdateTime: Long?
    var lastTriggerSource: String?
    var firebaseNotify: Boolean
    @TriggerType
    var firebaseNotifyTrigger: String?
    var showInTaskList: Boolean
    var unitsTasks: Map<String, UnitTask>

    @set:Exclude
    @get:Exclude
    var applyFunction: suspend HomeUnit<in Any>.(Any) -> Unit

    fun makeInvariant(): HomeUnit<Any>

    fun makeNotification(): HomeUnit<T>
}