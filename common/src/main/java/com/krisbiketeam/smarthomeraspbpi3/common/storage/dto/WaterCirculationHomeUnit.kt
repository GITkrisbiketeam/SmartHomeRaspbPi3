package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import timber.log.Timber

data class WaterCirculationHomeUnit<T : Any>(
    override val name: String = "", // Name should be unique for all units
    override val type: HomeUnitType = HomeUnitType.HOME_WATER_CIRCULATION,
    override val room: String = "",
    override val hwUnitName: String? = "",
    override val value: T? = null,
    override val lastUpdateTime: Long? = null,
    val temperatureHwUnitName: String? = null,
    val temperatureValue: TemperatureType? = null,
    val temperatureLastUpdateTime: Long? = null,
    val temperatureMin: TemperatureType? = null,
    val temperatureMinLastUpdateTime: Long? = null,
    val temperatureMax: TemperatureType? = null,
    val temperatureMaxLastUpdateTime: Long? = null,
    val temperatureThreshold: TemperatureType? = null,
    val motionHwUnitName: String? = null,
    val motionValue: MotionType? = null,
    val motionLastUpdateTime: Long? = null,
    val actionTimeout: Long? = null,
    val enabled: Boolean = true,             // should this be here?
    override val lastTriggerSource: String? = null,
    override val firebaseNotify: Boolean = false,
    @TriggerType override val firebaseNotifyTrigger: String? = null,
    override val showInTaskList: Boolean = false,
    override val unitsTasks: Map<String, UnitTask> = HashMap(),
    override val unitJobs: MutableMap<String, Job> = mutableMapOf(),
) : HomeUnit<T> {

    override fun makeNotification(): WaterCirculationHomeUnit<T> {
        return WaterCirculationHomeUnit(
            name,
            type,
            room,
            hwUnitName,
            value,
            lastUpdateTime,
            temperatureHwUnitName,
            temperatureValue,
            temperatureLastUpdateTime,
            temperatureMin,
            temperatureMinLastUpdateTime,
            temperatureMax,
            temperatureMaxLastUpdateTime,
            temperatureThreshold,
            motionHwUnitName,
            motionValue,
            motionLastUpdateTime,
            actionTimeout,
            enabled,
            lastTriggerSource = lastTriggerSource
        )
    }

    override fun copyWithValues(
        value: T?,
        lastUpdateTime: Long?,
        lastTriggerSource: String?,
    ): HomeUnit<T> {
        // previus copy was not copying unitJobs
        return copy(
            value = value,
            lastUpdateTime = lastUpdateTime,
            lastTriggerSource = lastTriggerSource,
        )
    }

    override fun isUnitAffected(hwUnit: HwUnit): Boolean {
        return temperatureHwUnitName == hwUnit.name || motionHwUnitName == hwUnit.name
    }

    override fun unitValue(): T? {
        return value
    }

    override suspend fun updateHomeUnitValuesAndTimes(
        hwUnit: HwUnit,
        unitValue: Any?,
        updateTime: Long,
        lastTriggerSource: String,
        booleanApplyAction: suspend (applyData: BooleanApplyActionData) -> HomeUnit<T>?
    ): HomeUnit<T> {
        Timber.d("updateHomeUnitValuesAndTimes hwUnit:$hwUnit unitValue:$unitValue")
        // We set Switch and normal value as updateHomeUnitValuesAndTimes is only called by HwUnit
        return when (hwUnit.name) {
            temperatureHwUnitName -> {
                updateTemperatureValueMinMax(
                    temperatureValue = unitValue as TemperatureType?,
                    temperatureLastUpdateTime = updateTime,
                    lastTriggerSource = lastTriggerSource
                ).let { homeUnitCopy ->
                    temperatureThreshold?.let { threshold ->
                        homeUnitCopy.temperatureValue?.let { temperature ->
                            val timeoutCondition: Boolean = actionTimeout?.let { timeout ->
                                motionLastUpdateTime?.let { motionTime ->
                                    motionTime + timeout < updateTime
                                } ?: false
                            } ?: false
                            if (temperature > threshold || timeoutCondition) {
                                // turn Off circulation
                                Timber.d("updateHomeUnitValuesAndTimes hwUnit:$hwUnit apply temperature")
                                //supervisorScope {
                                    //launch(Dispatchers.IO) {
                                        booleanApplyAction(
                                            BooleanApplyActionData(
                                                newActionVal = false,
                                                taskHomeUnitType = type,
                                                taskHomeUnitName = name,
                                                taskName = name,
                                                sourceHomeUnitName = name,
                                                periodicallyOnlyHw = false
                                            )
                                        )?: homeUnitCopy
                                    //}
                                //}
                            } else {
                                homeUnitCopy
                            }
                        }?:homeUnitCopy
                    }?: homeUnitCopy
                }
            }
            motionHwUnitName -> {
                copy(
                    motionValue = unitValue as MotionType?,
                    motionLastUpdateTime = updateTime,
                    lastTriggerSource = lastTriggerSource
                ).let { homeUnitCopy ->
                    // TODO: Should we also turn off circulation while no more motion???
                    if (homeUnitCopy.motionValue == true &&
                        (temperatureValue ?: TemperatureType.MIN_VALUE) <
                        (temperatureThreshold ?: TemperatureType.MAX_VALUE)
                    ) {
                        Timber.d("updateHomeUnitValuesAndTimes hwUnit:$hwUnit apply motion")
                        //supervisorScope {
                            //launch(Dispatchers.IO) {
                                booleanApplyAction(
                                    BooleanApplyActionData(
                                        newActionVal = true,
                                        taskHomeUnitType = type,
                                        taskHomeUnitName = name,
                                        taskName = name,
                                        sourceHomeUnitName = name,
                                        periodicallyOnlyHw = false
                                    )
                                )?: homeUnitCopy
                            //}
                        //}
                    } else {
                        homeUnitCopy
                    }
                }

            }
            else -> this
        }
    }

    private fun updateTemperatureValueMinMax(
        temperatureValue: Any?,
        temperatureLastUpdateTime: Long,
        lastTriggerSource: String,
    ): WaterCirculationHomeUnit<T> {
        when (temperatureValue) {
            is Float -> {
                return if (temperatureValue <= (temperatureMin ?: Float.MAX_VALUE)) {
                    copy(
                        temperatureValue = temperatureValue as TemperatureType?,
                        temperatureLastUpdateTime = temperatureLastUpdateTime,
                        temperatureMin = temperatureValue,
                        temperatureMinLastUpdateTime = temperatureLastUpdateTime,
                        lastTriggerSource = lastTriggerSource
                    )
                } else if (temperatureValue >= (temperatureMax ?: Float.MIN_VALUE)) {
                    copy(
                        temperatureValue = temperatureValue as TemperatureType?,
                        temperatureLastUpdateTime = temperatureLastUpdateTime,
                        temperatureMax = temperatureValue,
                        temperatureMaxLastUpdateTime = temperatureLastUpdateTime,
                        lastTriggerSource = lastTriggerSource
                    )
                } else {
                    copy(
                        temperatureValue = temperatureValue as TemperatureType?,
                        temperatureLastUpdateTime = temperatureLastUpdateTime,
                        lastTriggerSource = lastTriggerSource
                    )
                }
            }
        }
        return copy(
            temperatureValue = temperatureValue as TemperatureType?,
            temperatureLastUpdateTime = temperatureLastUpdateTime,
            lastTriggerSource = lastTriggerSource
        )
    }
}