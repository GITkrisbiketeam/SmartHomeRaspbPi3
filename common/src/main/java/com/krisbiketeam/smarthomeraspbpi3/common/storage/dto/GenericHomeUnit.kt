package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import kotlinx.coroutines.Job
import timber.log.Timber

data class GenericHomeUnit<T : Any>(
    override val name: String = "", // Name should be unique for all units
    override val type: HomeUnitType = HomeUnitType.HOME_TEMPERATURES,
    override val room: String = "",
    override val hwUnitName: String? = "",
    override var value: T? = null,
    override var lastUpdateTime: Long? = null,
    var min: T? = null,
    var minLastUpdateTime: Long? = null,
    var max: T? = null,
    var maxLastUpdateTime: Long? = null,
    override var lastTriggerSource: String? = null,
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
        return unitsTasks == other.unitsTasks
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

    override fun isUnitAffected(hwUnit: HwUnit): Boolean {
        return hwUnitName == hwUnit.name
    }

    override fun isHomeUnitChanged(other: HomeUnit<T>?): Boolean {
        if (other == null) return true
        if (other !is GenericHomeUnit<*>) return true

        if (name != other.name) return true
        if (type != other.type) return true
        if (room != other.room) return true
        if (hwUnitName != other.hwUnitName) return true
        //if (value != other.value) return false
        //if (lastUpdateTime != other.lastUpdateTime) return false
        //if (min != other.min) return false
        //if (minLastUpdateTime != other.minLastUpdateTime) return false
        //if (max != other.max) return false
        //if (maxLastUpdateTime != other.maxLastUpdateTime) return false
        //if (lastTriggerSource != other.lastTriggerSource) return false
        if (firebaseNotify != other.firebaseNotify) return true
        if (firebaseNotifyTrigger != other.firebaseNotifyTrigger) return true
        if (showInTaskList != other.showInTaskList) return true
        return unitsTasks != other.unitsTasks
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
    ) {
        // We need to handle differently values of non Basic Types
        when (unitValue) {
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
                        Unit
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
                        Unit
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
                        Unit
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
        valueUpdateTime: Long,
        valueLastTriggerSource: String,
    ) {
        if (unitValue is Float) {
            if (unitValue <= ((min.takeIf { it is Number? } as Number?)?.toFloat()
                    ?: Float.MAX_VALUE)) {
                value = unitValue as T?
                lastUpdateTime = valueUpdateTime
                min = unitValue
                minLastUpdateTime = valueUpdateTime
                lastTriggerSource = valueLastTriggerSource
            } else if (unitValue >= ((max.takeIf { it is Number? } as Number?)?.toFloat()
                    ?: Float.MIN_VALUE)) {
                value = unitValue as T?
                lastUpdateTime = valueUpdateTime
                max = unitValue
                maxLastUpdateTime = valueUpdateTime
                lastTriggerSource = valueLastTriggerSource

            } else {
                value = unitValue as T?
                lastUpdateTime = valueUpdateTime
                lastTriggerSource = valueLastTriggerSource

            }
        } else {
            value = unitValue as T?
            lastUpdateTime = valueUpdateTime
            lastTriggerSource = valueLastTriggerSource
        }
    }
}