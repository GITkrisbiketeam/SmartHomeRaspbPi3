package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

import com.google.firebase.database.GenericTypeIndicator
import com.krisbiketeam.smarthomeraspbpi3.common.FULL_DAY_IN_MILLIS
import com.krisbiketeam.smarthomeraspbpi3.common.getOnlyTodayLocalTime
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.toHomeUnitType
import kotlinx.coroutines.*
import timber.log.Timber

typealias LightType = Boolean
typealias Light = GenericHomeUnit<LightType>
typealias ActuatorType = Boolean
typealias Actuator = GenericHomeUnit<ActuatorType>
typealias LightSwitchType = Boolean
typealias LightSwitch = LightSwitchHomeUnit<LightSwitchType>
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

val HOME_STORAGE_UNITS: List<HomeUnitType> =
    HomeUnitType.values().filterNot { it == HomeUnitType.UNKNOWN }

val HOME_ACTION_STORAGE_UNITS: List<HomeUnitType> =
    listOf(
        HomeUnitType.HOME_LIGHT_SWITCHES,
        HomeUnitType.HOME_BLINDS,
        HomeUnitType.HOME_ACTUATORS,
    )
val HOME_FIREBASE_NOTIFY_STORAGE_UNITS: List<HomeUnitType> =
    listOf(
        HomeUnitType.HOME_ACTUATORS,
        HomeUnitType.HOME_BLINDS,
        HomeUnitType.HOME_REED_SWITCHES,
        HomeUnitType.HOME_MOTIONS,
        HomeUnitType.HOME_LIGHT_SWITCHES,
    )

interface HomeUnit<T : Any> {
    var name: String // Name should be unique for all units
    var type: HomeUnitType
    var room: String
    var hwUnitName: String?
    var value: T?
    var lastUpdateTime: Long?

    var lastTriggerSource: String?
    var firebaseNotify: Boolean

    @TriggerType
    var firebaseNotifyTrigger: String?
    var showInTaskList: Boolean
    var unitsTasks: Map<String, UnitTask>

    suspend fun applyFunction(
        scope: CoroutineScope,
        newVal: T,
        booleanApplyAction: suspend HomeUnit<T>.(actionVal: Boolean, taskHomeUnitType: HomeUnitType, taskHomeUnitName: String, taskName: String, periodicallyOnlyHw: Boolean) -> Unit
    ) {
        unitsTasks.values.forEach { task ->
            when (type) {
                HomeUnitType.HOME_ACTUATORS,
                HomeUnitType.HOME_LIGHT_SWITCHES,
                HomeUnitType.HOME_REED_SWITCHES,
                HomeUnitType.HOME_MOTIONS,
                HomeUnitType.HOME_BLINDS -> {
                    if (newVal is Boolean) {
                        booleanTaskApply(scope, newVal, task, booleanApplyAction)
                    } else {
                        Timber.e("applyFunction new value is not Boolean or is null")
                    }
                }
                HomeUnitType.HOME_TEMPERATURES,
                HomeUnitType.HOME_PRESSURES,
                HomeUnitType.HOME_HUMIDITY,
                HomeUnitType.HOME_GAS,
                HomeUnitType.HOME_GAS_PERCENT,
                HomeUnitType.HOME_IAQ,
                HomeUnitType.HOME_STATIC_IAQ,
                HomeUnitType.HOME_CO2,
                HomeUnitType.HOME_BREATH_VOC -> {
                    if (newVal is Float) {
                        sensorTaskApply(scope, newVal, task, booleanApplyAction)
                    } else {
                        Timber.e("applyFunction new value is not Float or is null")
                    }
                }
                HomeUnitType.UNKNOWN -> throw IllegalArgumentException("NotSupported HomeUnitType requested")
            }
        }
    }

    fun makeNotification(): HomeUnit<T>

    fun isUnitAffected(hwUnit: HwUnit): Boolean

    fun getHomeUnitValue(): T?

    fun updateHomeUnitValuesAndTimes(hwUnit: HwUnit, unitValue: Any?, updateTime: Long)

    fun copy(): HomeUnit<T>

    private suspend fun booleanTaskApply(
        scope: CoroutineScope,
        newVal: Boolean,
        task: UnitTask,
        booleanApplyAction: suspend HomeUnit<T>.(actionVal: Boolean, taskHomeUnitType: HomeUnitType, taskHomeUnitName: String, taskName: String, periodicallyOnlyHw: Boolean) -> Unit
    ) {
        scope.launch {
            Timber.v("booleanTaskApply before cancel task.taskJob:${task.taskJob} isActive:${task.taskJob?.isActive} isCancelled:${task.taskJob?.isCancelled} isCompleted:${task.taskJob?.isCompleted}")
            task.taskJob?.cancel()
            Timber.v("booleanTaskApply after cancel task.taskJob:${task.taskJob} isActive:${task.taskJob?.isActive} isCancelled:${task.taskJob?.isCancelled} isCompleted:${task.taskJob?.isCompleted}")

            if (task.disabled == true) {
                Timber.d("booleanTaskApply task not enabled $task")
            } else {
                if ((task.trigger == null || task.trigger == BOTH)
                    || (task.trigger == RISING_EDGE && newVal)
                    || (task.trigger == FALLING_EDGE && !newVal)
                ) {
                    task.taskJob = scope.launch(Dispatchers.IO) {
                        do {
                            booleanTaskTimed(newVal, task, booleanApplyAction)
                        } while (this.isActive && task.periodically == true && ((task.delay.isValidTime() && task.duration.isValidTime())
                                    || (task.startTime.isValidTime() && task.endTime.isValidTime())
                                    || (task.startTime.isValidTime() && task.duration.isValidTime()))
                        )
                    }
                    Timber.v("booleanTaskApply after booleanTaskTimed task.taskJob:${task.taskJob} isActive:${task.taskJob?.isActive} isCancelled:${task.taskJob?.isCancelled} isCompleted:${task.taskJob?.isCompleted}")
                } else if (task.resetOnInverseTrigger == true) {
                    booleanApplyAction(newVal, task, booleanApplyAction)
                }
            }
        }

    }

    private suspend fun sensorTaskApply(
        scope: CoroutineScope,
        newVal: Float,
        task: UnitTask,
        booleanApplyAction: suspend HomeUnit<T>.(actionVal: Boolean, taskHomeUnitType: HomeUnitType, taskHomeUnitName: String, taskName: String, periodicallyOnlyHw: Boolean) -> Unit
    ) {
        scope.launch {
            if (task.disabled == true) {
                Timber.d("sensorTaskApply task not enabled $task")
                task.taskJob?.cancel()
            } else {
                task.threshold?.let { threshold ->
                    if ((task.trigger == null || task.trigger == BOTH
                                || task.trigger == RISING_EDGE) && newVal >= threshold + (task.hysteresis
                            ?: 0f)
                    ) {
                        task.taskJob?.cancel()
                        task.taskJob = scope.launch(Dispatchers.IO) {
                            booleanTaskTimed(true, task, booleanApplyAction)
                        }
                    } else if ((task.trigger == null || task.trigger == BOTH
                                || task.trigger == FALLING_EDGE) && newVal <= threshold - (task.hysteresis
                            ?: 0f)
                    ) {
                        task.taskJob?.cancel()
                        task.taskJob = scope.launch(Dispatchers.IO) {
                            booleanTaskTimed(false, task, booleanApplyAction)
                        }
                    }
                }
            }
        }
    }

    private suspend fun booleanTaskTimed(
        newVal: Boolean,
        task: UnitTask,
        booleanApplyAction: suspend HomeUnit<T>.(actionVal: Boolean, taskHomeUnitType: HomeUnitType, taskHomeUnitName: String, taskName: String, periodicallyOnlyHw: Boolean) -> Unit
    ) {
        task.startTime.takeIf { it.isValidTime() }?.let { startTime ->
            val currTime = System.currentTimeMillis().getOnlyTodayLocalTime()
            task.endTime.takeIf { it.isValidTime() }?.let { endTime ->
                Timber.e("booleanTaskTimed startTime:$startTime endTime:$endTime currTime: $currTime")
                if (startTime < endTime) {
                    if (currTime < startTime) {
                        delay(startTime - currTime)
                        booleanApplyAction(newVal, task, booleanApplyAction)
                        val endCurrTime = System.currentTimeMillis().getOnlyTodayLocalTime()
                        delay(endTime - endCurrTime)
                        booleanApplyAction(!newVal, task, booleanApplyAction)
                        delay(FULL_DAY_IN_MILLIS - endCurrTime)
                    } else if (endTime < currTime) {
                        delay(FULL_DAY_IN_MILLIS - currTime)
                    } else {
                        booleanApplyAction(newVal, task, booleanApplyAction)
                        val endCurrTime = System.currentTimeMillis().getOnlyTodayLocalTime()
                        delay(endTime - endCurrTime)
                        booleanApplyAction(!newVal, task, booleanApplyAction)
                        delay(FULL_DAY_IN_MILLIS - endCurrTime)
                    }
                } else if (endTime < startTime) {
                    if (currTime < endTime) {
                        booleanApplyAction(newVal, task, booleanApplyAction)
                        delay(endTime - currTime)
                        booleanApplyAction(!newVal, task, booleanApplyAction)
                        val endCurrTime = System.currentTimeMillis().getOnlyTodayLocalTime()
                        delay(startTime - endCurrTime)
                        booleanApplyAction(newVal, task, booleanApplyAction)
                        delay(FULL_DAY_IN_MILLIS - startTime)
                    } else if (currTime < startTime) {
                        delay(startTime - currTime)
                        booleanApplyAction(newVal, task, booleanApplyAction)
                        delay(FULL_DAY_IN_MILLIS - startTime)
                    } else {
                        booleanApplyAction(newVal, task, booleanApplyAction)
                        delay(FULL_DAY_IN_MILLIS - currTime)
                    }
                }
            } ?: task.duration?.let { duration ->
                booleanApplyAction(newVal, task, booleanApplyAction)
                delay(duration)
                booleanApplyAction(!newVal, task, booleanApplyAction)
            } ?: run {
                if (currTime < startTime) {
                    delay(startTime - currTime)
                }
                booleanApplyAction(newVal, task, booleanApplyAction)
            }
        } ?: task.endTime.takeIf { it.isValidTime() }?.let { endTime ->
            val endCurrTime = System.currentTimeMillis().getOnlyTodayLocalTime()
            if (endCurrTime < endTime) {
                delay(endTime - endCurrTime)
            }
            booleanApplyAction(!newVal, task, booleanApplyAction)
        } ?: task.delay.takeIf { it.isValidTime() }?.let { delay ->
            delay(delay)
            booleanApplyAction(newVal, task, booleanApplyAction)
            task.duration?.let { duration ->
                delay(duration)
                booleanApplyAction(!newVal, task, booleanApplyAction)
            }
        } ?: task.duration.takeIf { it.isValidTime() }?.let { duration ->
            booleanApplyAction(newVal, task, booleanApplyAction)
            delay(duration)
            booleanApplyAction(!newVal, task, booleanApplyAction)
        }
        ?: booleanApplyAction(newVal, task, booleanApplyAction)
    }

    private suspend fun booleanApplyAction(
        actionVal: Boolean,
        task: UnitTask,
        booleanApplyAction: suspend HomeUnit<T>.(actionVal: Boolean, taskHomeUnitType: HomeUnitType, taskHomeUnitName: String, taskName: String, periodicallyOnlyHw: Boolean) -> Unit
    ) {
        val newActionVal: Boolean = (task.inverse ?: false) xor actionVal
        task.homeUnitsList.forEach {
            booleanApplyAction(
                newActionVal,
                it.type.toHomeUnitType(),
                it.name,
                task.name,
                task.periodicallyOnlyHw ?: false
            )
        }
    }
}

private fun Long?.isValidTime(): Boolean {
    return this != null && this > 0
}