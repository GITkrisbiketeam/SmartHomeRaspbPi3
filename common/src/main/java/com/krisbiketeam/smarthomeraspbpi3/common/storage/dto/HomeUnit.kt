package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

import com.google.firebase.database.GenericTypeIndicator
import com.krisbiketeam.smarthomeraspbpi3.common.FULL_DAY_IN_MILLIS
import com.krisbiketeam.smarthomeraspbpi3.common.getOnlyTodayLocalTime
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.toHomeUnitType
import kotlinx.coroutines.*
import timber.log.Timber

typealias ActuatorType = Boolean
typealias Actuator = GenericHomeUnit<ActuatorType>
typealias LightSwitchType = Boolean
typealias LightSwitch = LightSwitchHomeUnit<LightSwitchType>
typealias WaterCirculationType = Boolean
typealias WaterCirculation = WaterCirculationHomeUnit<WaterCirculationType>
typealias MCP23017WatchDogType = Boolean
typealias MCP23017WatchDog = MCP23017WatchDogHomeUnit<MCP23017WatchDogType>
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
        HomeUnitType.HOME_WATER_CIRCULATION -> object : GenericTypeIndicator<WaterCirculation>() {}
        HomeUnitType.HOME_MCP23017_WATCH_DOG -> object : GenericTypeIndicator<MCP23017WatchDog>() {}
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
    HomeUnitType.entries.filterNot { it == HomeUnitType.UNKNOWN }

val HOME_ACTION_STORAGE_UNITS: List<HomeUnitType> =
    listOf(
        HomeUnitType.HOME_LIGHT_SWITCHES,
        HomeUnitType.HOME_WATER_CIRCULATION,
        HomeUnitType.HOME_MCP23017_WATCH_DOG,
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
        HomeUnitType.HOME_WATER_CIRCULATION,
    )

sealed interface HomeUnit<T : Any>  {
    val name: String // Name should be unique for all units
    val type: HomeUnitType
    val room: String
    val hwUnitName: String?
    var value: T?
    var lastUpdateTime: Long?

    var lastTriggerSource: String?
    val firebaseNotify: Boolean

    @TriggerType
    val firebaseNotifyTrigger: String?
    val showInTaskList: Boolean
    val unitsTasks: Map<String, UnitTask>
    val unitJobs: MutableMap<String, Job>

    suspend fun taskApplyFunction(
        newVal: T,
        booleanApplyAction: suspend (applyData: BooleanApplyActionData) -> Unit
    ) {
        unitsTasks.values.forEach { task ->
            when (type) {
                HomeUnitType.HOME_ACTUATORS,
                HomeUnitType.HOME_LIGHT_SWITCHES,
                HomeUnitType.HOME_WATER_CIRCULATION,
                HomeUnitType.HOME_MCP23017_WATCH_DOG,
                HomeUnitType.HOME_REED_SWITCHES,
                HomeUnitType.HOME_MOTIONS,
                HomeUnitType.HOME_BLINDS -> {
                    if (newVal is Boolean) {
                        booleanTaskApply(newVal, task, booleanApplyAction)
                    } else {
                        Timber.e("taskApplyFunction new value is not Boolean or is null")
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
                        sensorTaskApply(newVal, task, booleanApplyAction)
                    } else {
                        Timber.e("taskApplyFunction new value is not Float or is null")
                    }
                }
                HomeUnitType.UNKNOWN -> throw IllegalArgumentException("NotSupported HomeUnitType requested")
            }
        }
    }

    fun makeNotification(): HomeUnit<T>

    fun isUnitAffected(hwUnit: HwUnit): Boolean

    fun unitValue(): T?

    suspend fun updateHomeUnitValuesAndTimes(
        hwUnit: HwUnit,
        unitValue: Any?,
        updateTime: Long,
        lastTriggerSource: String,
        booleanApplyAction: suspend (applyData: BooleanApplyActionData) -> Unit
    )

    fun shouldFirebaseNotify(newVal: Any?): Boolean {
        return firebaseNotify && (newVal !is Boolean || ((firebaseNotifyTrigger == null ||
                firebaseNotifyTrigger == BOTH) ||
                (firebaseNotifyTrigger == RISING_EDGE && newVal) ||
                (firebaseNotifyTrigger == FALLING_EDGE && !newVal)))
    }

    // region taskApplyFunction helper methods

    private suspend fun booleanTaskApply(
        newVal: Boolean,
        task: UnitTask,
        booleanApplyAction: suspend (applyData: BooleanApplyActionData) -> Unit
    ) {
        supervisorScope {
            launch {
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
                        task.taskJob = launch(Dispatchers.IO) {
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

    }

    private suspend fun sensorTaskApply(
        newVal: Float,
        task: UnitTask,
        booleanApplyAction: suspend (applyData: BooleanApplyActionData) -> Unit
    ) {
        supervisorScope {
            launch {
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
                            task.taskJob = launch(Dispatchers.IO) {
                                booleanTaskTimed(true, task, booleanApplyAction)
                            }
                        } else if ((task.trigger == null || task.trigger == BOTH
                                    || task.trigger == FALLING_EDGE) && newVal <= threshold - (task.hysteresis
                                ?: 0f)
                        ) {
                            task.taskJob?.cancel()
                            task.taskJob = launch(Dispatchers.IO) {
                                booleanTaskTimed(false, task, booleanApplyAction)
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun booleanTaskTimed(
        newVal: Boolean,
        task: UnitTask,
        booleanApplyAction: suspend (applyData: BooleanApplyActionData) -> Unit
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
        booleanApplyAction: suspend (applyData:BooleanApplyActionData) -> Unit
    ) {
        val newActionVal: Boolean = (task.inverse ?: false) xor actionVal
        task.homeUnitsList.forEach {
            booleanApplyAction(BooleanApplyActionData(
                newActionVal = newActionVal,
                taskHomeUnitType = it.type.toHomeUnitType(),
                taskHomeUnitName = it.name,
                taskName = task.name,
                sourceHomeUnitName = name,
                periodicallyOnlyHw = task.periodicallyOnlyHw ?: false
            )
            )
        }
    }

    // endregion
}

data class BooleanApplyActionData(
    val newActionVal: Boolean,
    val taskHomeUnitType: HomeUnitType,
    val taskHomeUnitName: String,
    val taskName: String,
    val sourceHomeUnitName: String,
    val periodicallyOnlyHw: Boolean,
)

private fun Long?.isValidTime(): Boolean {
    return this != null && this > 0
}