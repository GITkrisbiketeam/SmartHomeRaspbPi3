package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import kotlinx.coroutines.Job
import timber.log.Timber

data class GenericHomeUnit<T : Any>(
    override val name: String = "", // Name should be unique for all units
    override val type: HomeUnitType = HomeUnitType.HOME_TEMPERATURES,
    override val room: String = "",
    override val hwUnitName: String? = "",
    override val value: T? = null,
    override val lastUpdateTime: Long? = null,
    val min: T? = null,
    val minLastUpdateTime: Long? = null,
    val max: T? = null,
    val maxLastUpdateTime: Long? = null,
    override val lastTriggerSource: String? = null,
    override val firebaseNotify: Boolean = false,
    @TriggerType override val firebaseNotifyTrigger: String? = null,
    override val showInTaskList: Boolean = false,
    override val unitsTasks: Map<String, UnitTask> = HashMap(),
    override val unitJobs: MutableMap<String, Job> = mutableMapOf(),
) : HomeUnit<T> {

    override fun makeNotification(): GenericHomeUnit<T> {
        return GenericHomeUnit(
            name,
            type,
            room,
            hwUnitName,
            value,
            lastUpdateTime,
            min,
            minLastUpdateTime,
            max,
            maxLastUpdateTime,
            lastTriggerSource
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GenericHomeUnit<*>

        if (name != other.name) return false
        if (type != other.type) return false
        if (room != other.room) return false
        if (hwUnitName != other.hwUnitName) return false
        if (value != other.value) return false
        if (lastUpdateTime != other.lastUpdateTime) return false
        if (min != other.min) return false
        if (minLastUpdateTime != other.minLastUpdateTime) return false
        if (max != other.max) return false
        if (maxLastUpdateTime != other.maxLastUpdateTime) return false
        if (lastTriggerSource != other.lastTriggerSource) return false
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
        result = 31 * result + (min?.hashCode() ?: 0)
        result = 31 * result + (minLastUpdateTime?.hashCode() ?: 0)
        result = 31 * result + (max?.hashCode() ?: 0)
        result = 31 * result + (maxLastUpdateTime?.hashCode() ?: 0)
        result = 31 * result + (lastTriggerSource?.hashCode() ?: 0)
        result = 31 * result + firebaseNotify.hashCode()
        result = 31 * result + (firebaseNotifyTrigger?.hashCode() ?: 0)
        result = 31 * result + showInTaskList.hashCode()
        result = 31 * result + unitsTasks.hashCode()
        return result
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
        return hwUnitName == hwUnit.name
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
        // We need to handle differently values of non Basic Types
        return when (unitValue) {
            is PressureAndTemperature -> {
                Timber.d("Received PressureAndTemperature $unitValue for ${this.type}.${this.name}")
                when (type) {
                    HomeUnitType.HOME_TEMPERATURES -> {
                        updateValueMinMax(unitValue.temperature, updateTime, lastTriggerSource)
                    }
                    HomeUnitType.HOME_PRESSURES -> {
                        updateValueMinMax(unitValue.pressure, updateTime, lastTriggerSource)
                    }
                    else -> {
                        this
                    }
                }
            }
            is TemperatureAndHumidity -> {
                Timber.d("Received TemperatureAndHumidity $unitValue for ${this.type}.${this.name}")
                when (type) {
                    HomeUnitType.HOME_TEMPERATURES -> {
                        updateValueMinMax(unitValue.temperature, updateTime, lastTriggerSource)
                    }
                    HomeUnitType.HOME_HUMIDITY -> {
                        updateValueMinMax(unitValue.humidity, updateTime, lastTriggerSource)
                    }
                    else -> {
                        this
                    }
                }
            }
            is Bme680Data -> {
                Timber.d("Received Bme680Data $unitValue for ${this.type}.${this.name}")
                when (type) {
                    HomeUnitType.HOME_TEMPERATURES -> {
                        updateValueMinMax(unitValue.temperature, updateTime, lastTriggerSource)
                    }
                    HomeUnitType.HOME_HUMIDITY -> {
                        updateValueMinMax(unitValue.humidity, updateTime, lastTriggerSource)
                    }
                    HomeUnitType.HOME_PRESSURES -> {
                        updateValueMinMax(unitValue.pressure, updateTime, lastTriggerSource)
                    }
                    HomeUnitType.HOME_GAS -> {
                        updateValueMinMax(unitValue.gas, updateTime, lastTriggerSource)
                    }
                    HomeUnitType.HOME_GAS_PERCENT -> {
                        updateValueMinMax(unitValue.gasPercentage, updateTime, lastTriggerSource)
                    }
                    HomeUnitType.HOME_IAQ -> {
                        updateValueMinMax(unitValue.iaq, updateTime, lastTriggerSource)
                    }
                    HomeUnitType.HOME_STATIC_IAQ -> {
                        updateValueMinMax(unitValue.staticIaq, updateTime, lastTriggerSource)
                    }
                    HomeUnitType.HOME_CO2 -> {
                        updateValueMinMax(unitValue.co2Equivalent, updateTime, lastTriggerSource)
                    }
                    HomeUnitType.HOME_BREATH_VOC -> {
                        updateValueMinMax(
                            unitValue.breathVocEquivalent,
                            updateTime,
                            lastTriggerSource
                        )
                    }
                    else -> {
                        // do nothing, no supported sensor
                        this
                    }
                }
            }
            else -> {
                updateValueMinMax(unitValue, updateTime, lastTriggerSource)
            }
        }
    }


    private fun updateValueMinMax(
        unitValue: Any?,
        updateTime: Long,
        lastTriggerSource: String,
    ): HomeUnit<T> {
        when (unitValue) {
            is Float -> {
                return if (unitValue <= ((min.takeIf { it is Number? } as Number?)?.toFloat()
                        ?: Float.MAX_VALUE)) {
                    copy(
                        value = unitValue as T?,
                        lastUpdateTime = updateTime,
                        min = unitValue,
                        minLastUpdateTime = updateTime,
                        lastTriggerSource = lastTriggerSource
                    )
                } else if (unitValue >= ((max.takeIf { it is Number? } as Number?)?.toFloat()
                        ?: Float.MIN_VALUE)) {
                    copy(
                        value = unitValue as T?,
                        lastUpdateTime = updateTime,
                        max = unitValue,
                        maxLastUpdateTime = updateTime,
                        lastTriggerSource = lastTriggerSource
                    )
                } else {
                    copy(
                        value = unitValue as T?,
                        lastUpdateTime = updateTime,
                        lastTriggerSource = lastTriggerSource
                    )
                }
            }
        }
        return copy(
            value = unitValue as T?,
            lastUpdateTime = updateTime,
            lastTriggerSource = lastTriggerSource
        )
    }
}