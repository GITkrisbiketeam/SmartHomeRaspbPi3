package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import kotlinx.coroutines.Job

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
        booleanApplyAction: suspend (applyData: BooleanApplyActionData) -> Unit
    ): HomeUnit<T> {
        // We set Switch and normal value as updateHomeUnitValuesAndTimes is only called by HwUnit
        return when (hwUnit.name) {
            temperatureHwUnitName -> {
                copy(
                    temperatureValue = unitValue as TemperatureType?,
                    temperatureLastUpdateTime = updateTime
                ).also {
                    temperatureThreshold?.let { threshold ->
                        temperatureValue?.let { temperature ->
                            val timeoutCondition: Boolean = actionTimeout?.let { timeout ->
                                motionLastUpdateTime?.let { motionTime ->
                                    motionTime + timeout < updateTime
                                } ?: false
                            } ?: false
                            if (temperature > threshold || timeoutCondition) {
                                // turn Off circulation
                                booleanApplyAction(
                                    BooleanApplyActionData(
                                        false,
                                        type,
                                        name,
                                        name,
                                        name,
                                        false
                                    )
                                )
                            }
                        }
                    }
                }


            }
            motionHwUnitName -> {
                copy(
                    motionValue = unitValue as MotionType?,
                    motionLastUpdateTime = updateTime
                ).also {
                    // TODO: Should we also turn off circulation while no more motion???
                    if (motionValue == true &&
                        (temperatureValue ?: TemperatureType.MIN_VALUE) <
                        (temperatureThreshold ?: TemperatureType.MAX_VALUE)
                    ) {
                        booleanApplyAction(
                            BooleanApplyActionData(
                                true,
                                type,
                                name,
                                name,
                                name,
                                false
                            )
                        )
                    }
                }

            }
            else -> this
        }
    }
}