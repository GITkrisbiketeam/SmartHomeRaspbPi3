package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

import com.google.firebase.database.Exclude
import com.google.firebase.database.GenericTypeIndicator
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.*
import java.lang.IllegalArgumentException

typealias LightType = Boolean
typealias Light = GenericHomeUnit<LightType>
typealias ActuatorType = Boolean
typealias Actuator = GenericHomeUnit<ActuatorType>
typealias LightSwitchType = Boolean
typealias LightSwitch = GenericHomeUnit<LightSwitchType>
typealias ReedSwitchType = Boolean
typealias ReedSwitch = GenericHomeUnit<ReedSwitchType>
typealias MotionType = Boolean
typealias Motion = GenericHomeUnit<MotionType>
typealias TemperatureType = Float
typealias Temperature = GenericHomeUnit<TemperatureType>
typealias PressureType = Float
typealias Pressure = GenericHomeUnit<PressureType>
typealias HumidityType = Float
typealias Humidity = GenericHomeUnit<HumidityType>
typealias BlindType = Int
typealias Blind = GenericHomeUnit<BlindType>
typealias GasType = Float
typealias Gas = GenericHomeUnit<GasType>
typealias GasPercentType = Float
typealias GasPercent = GenericHomeUnit<GasPercentType>
typealias IaqType = Float
typealias Iaq = GenericHomeUnit<IaqType>
typealias Co2Type = Float
typealias Co2 = GenericHomeUnit<Co2Type>
typealias BreathVocType = Float
typealias BreathVoc = GenericHomeUnit<BreathVocType>


fun getHomeUnitTypeIndicatorMap(type: HomeUnitType): GenericTypeIndicator<HomeUnit<Any>> {
    return when (type) {
        HomeUnitType.HOME_ACTUATORS -> object : GenericTypeIndicator<Actuator>() {}
        HomeUnitType.HOME_LIGHT_SWITCHES -> object : GenericTypeIndicator<LightSwitch>() {}
        HomeUnitType.HOME_LIGHT_SWITCHES_V2 -> object :
            GenericTypeIndicator<LightSwitchHomeUnit<LightSwitchType>>() {}
        HomeUnitType.HOME_REED_SWITCHES -> object : GenericTypeIndicator<ReedSwitch>() {}
        HomeUnitType.HOME_MOTIONS -> object : GenericTypeIndicator<Motion>() {}
        HomeUnitType.HOME_TEMPERATURES -> object : GenericTypeIndicator<Temperature>() {}
        HomeUnitType.HOME_PRESSURES -> object : GenericTypeIndicator<Pressure>() {}
        HomeUnitType.HOME_HUMIDITY -> object : GenericTypeIndicator<Humidity>() {}
        HomeUnitType.HOME_BLINDS -> object : GenericTypeIndicator<Blind>() {}
        HomeUnitType.HOME_GAS -> object : GenericTypeIndicator<Gas>() {}
        HomeUnitType.HOME_GAS_PERCENT -> object : GenericTypeIndicator<GasPercent>() {}
        HomeUnitType.HOME_IAQ -> object : GenericTypeIndicator<Iaq>() {}
        HomeUnitType.HOME_STATIC_IAQ -> object : GenericTypeIndicator<Iaq>() {}
        HomeUnitType.HOME_CO2 -> object : GenericTypeIndicator<Co2>() {}
        HomeUnitType.HOME_BREATH_VOC -> object : GenericTypeIndicator<BreathVoc>() {}
        HomeUnitType.UNKNOWN -> throw IllegalArgumentException("NotSupported HomeUnitType requested")
    } as GenericTypeIndicator<HomeUnit<Any>>
}

val HOME_STORAGE_UNITS: List<HomeUnitType> = HomeUnitType.values().filterNot { it == HomeUnitType.UNKNOWN }

val HOME_ACTION_STORAGE_UNITS: List<HomeUnitType> =
    listOf(
        HomeUnitType.HOME_LIGHT_SWITCHES,
        HomeUnitType.HOME_BLINDS,
        HomeUnitType.HOME_ACTUATORS,
        HomeUnitType.HOME_LIGHT_SWITCHES_V2
    )

interface HomeUnit<T : Any> {
    var name: String // Name should be unique for all units
    var type: HomeUnitType
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

    fun makeNotification(): HomeUnit<T>
}