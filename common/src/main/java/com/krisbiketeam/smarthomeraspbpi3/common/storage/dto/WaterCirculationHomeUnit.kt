package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import kotlinx.coroutines.Job
import timber.log.Timber

data class WaterCirculationHomeUnit<T : Any>(
    override val name: String = "", // Name should be unique for all units
    override val type: HomeUnitType = HomeUnitType.HOME_WATER_CIRCULATION,
    override val room: String = "",
    override val hwUnitName: String? = "",
    override var value: T? = null,
    override var lastUpdateTime: Long? = null,
    val temperatureHwUnitName: String? = null,
    var temperatureValue: TemperatureType? = null,
    var temperatureLastUpdateTime: Long? = null,
    var temperatureMin: TemperatureType? = null,
    var temperatureMinLastUpdateTime: Long? = null,
    var temperatureMax: TemperatureType? = null,
    var temperatureMaxLastUpdateTime: Long? = null,
    val temperatureThreshold: TemperatureType? = null,
    val motionHwUnitName: String? = null,
    var motionValue: MotionType? = null,
    var motionLastUpdateTime: Long? = null,
    val actionTimeout: Long? = null,
    val enabled: Boolean = true,             // should this be here?
    override var lastTriggerSource: String? = null,
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


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WaterCirculationHomeUnit<*>

        if (name != other.name) return false
        if (type != other.type) return false
        if (room != other.room) return false
        if (hwUnitName != other.hwUnitName) return false
        if (value != other.value) return false
        if (lastUpdateTime != other.lastUpdateTime) return false
        if (temperatureHwUnitName != other.temperatureHwUnitName) return false
        if (temperatureValue != other.temperatureValue) return false
        if (temperatureLastUpdateTime != other.temperatureLastUpdateTime) return false
        if (temperatureMin != other.temperatureMin) return false
        if (temperatureMinLastUpdateTime != other.temperatureMinLastUpdateTime) return false
        if (temperatureMax != other.temperatureMax) return false
        if (temperatureMaxLastUpdateTime != other.temperatureMaxLastUpdateTime) return false
        if (temperatureThreshold != other.temperatureThreshold) return false
        if (motionHwUnitName != other.motionHwUnitName) return false
        if (motionValue != other.motionValue) return false
        if (motionLastUpdateTime != other.motionLastUpdateTime) return false
        if (actionTimeout != other.actionTimeout) return false
        if (enabled != other.enabled) return false
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
        result = 31 * result + (temperatureHwUnitName?.hashCode() ?: 0)
        result = 31 * result + (temperatureValue?.hashCode() ?: 0)
        result = 31 * result + (temperatureLastUpdateTime?.hashCode() ?: 0)
        result = 31 * result + (temperatureMin?.hashCode() ?: 0)
        result = 31 * result + (temperatureMinLastUpdateTime?.hashCode() ?: 0)
        result = 31 * result + (temperatureMax?.hashCode() ?: 0)
        result = 31 * result + (temperatureMaxLastUpdateTime?.hashCode() ?: 0)
        result = 31 * result + (temperatureThreshold?.hashCode() ?: 0)
        result = 31 * result + (motionHwUnitName?.hashCode() ?: 0)
        result = 31 * result + (motionValue?.hashCode() ?: 0)
        result = 31 * result + (motionLastUpdateTime?.hashCode() ?: 0)
        result = 31 * result + (actionTimeout?.hashCode() ?: 0)
        result = 31 * result + enabled.hashCode()
        result = 31 * result + (lastTriggerSource?.hashCode() ?: 0)
        result = 31 * result + firebaseNotify.hashCode()
        result = 31 * result + (firebaseNotifyTrigger?.hashCode() ?: 0)
        result = 31 * result + showInTaskList.hashCode()
        result = 31 * result + unitsTasks.hashCode()
        return result
    }

    override fun isUnitAffected(hwUnit: HwUnit): Boolean {
        return temperatureHwUnitName == hwUnit.name || motionHwUnitName == hwUnit.name
    }

    override fun isHomeUnitChanged(other: HomeUnit<T>?): Boolean {
        if (other == null) return true
        if (other !is WaterCirculationHomeUnit<*>) return true

        if (name != other.name) return true
        if (type != other.type) return true
        if (room != other.room) return true
        if (hwUnitName != other.hwUnitName) return true
        //if (value != other.value) return false
        //if (lastUpdateTime != other.lastUpdateTime) return false
        if (temperatureHwUnitName != other.temperatureHwUnitName) return true
        //if (temperatureValue != other.temperatureValue) return false
        //if (temperatureLastUpdateTime != other.temperatureLastUpdateTime) return false
        //if (temperatureMin != other.temperatureMin) return false
        //if (temperatureMinLastUpdateTime != other.temperatureMinLastUpdateTime) return false
        //if (temperatureMax != other.temperatureMax) return false
        //if (temperatureMaxLastUpdateTime != other.temperatureMaxLastUpdateTime) return false
        if (temperatureThreshold != other.temperatureThreshold) return true
        if (motionHwUnitName != other.motionHwUnitName) return true
        //if (motionValue != other.motionValue) return false
        //if (motionLastUpdateTime != other.motionLastUpdateTime) return false
        if (actionTimeout != other.actionTimeout) return true
        if (enabled != other.enabled) return true
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
        Timber.d("updateHomeUnitValuesAndTimes hwUnit:$hwUnit unitValue:$unitValue")
        // We set Switch and normal value as updateHomeUnitValuesAndTimes is only called by HwUnit
        when (hwUnit.name) {
            temperatureHwUnitName -> {
                updateTemperatureValueMinMax(
                    unitValue as TemperatureType?,
                    updateTime,
                    lastTriggerSource
                )
                temperatureThreshold?.let { threshold ->
                    temperatureValue?.let { temperature ->
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
                            )
                            //}
                            //}
                        }
                    }
                }
            }

            motionHwUnitName -> {
                motionValue = unitValue as MotionType?
                motionLastUpdateTime = updateTime
                this.lastTriggerSource = lastTriggerSource

                // TODO: Should we also turn off circulation while no more motion???
                if (motionValue == true &&
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
                    )
                    //}
                    //}
                }
            }
        }
    }

    private fun updateTemperatureValueMinMax(
        newTemperatureValue: Any?,
        newTemperatureLastUpdateTime: Long,
        newLastTriggerSource: String,
    ) {
        when (newTemperatureValue) {
            is Float -> {
                if (newTemperatureValue <= (temperatureMin ?: Float.MAX_VALUE)) {
                    temperatureValue = newTemperatureValue
                    temperatureLastUpdateTime = newTemperatureLastUpdateTime
                    temperatureMin = newTemperatureValue
                    temperatureMinLastUpdateTime = newTemperatureLastUpdateTime
                    lastTriggerSource = newLastTriggerSource
                } else if (newTemperatureValue >= (temperatureMax ?: Float.MIN_VALUE)) {
                    temperatureValue = newTemperatureValue
                    temperatureLastUpdateTime = newTemperatureLastUpdateTime
                    temperatureMax = newTemperatureValue
                    temperatureMaxLastUpdateTime = newTemperatureLastUpdateTime
                    lastTriggerSource = newLastTriggerSource
                } else {
                    temperatureValue = newTemperatureValue
                    temperatureLastUpdateTime = newTemperatureLastUpdateTime
                    lastTriggerSource = newLastTriggerSource
                }
            }
        }
        temperatureValue = newTemperatureValue as TemperatureType?
        temperatureLastUpdateTime = newTemperatureLastUpdateTime
        lastTriggerSource = newLastTriggerSource

    }
}