package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType

data class WaterCirculationHomeUnit<T : Any>(
    override var name: String = "", // Name should be unique for all units
    override var type: HomeUnitType = HomeUnitType.HOME_WATER_CIRCULATION,
    override var room: String = "",
    override var hwUnitName: String? = "",
    override var value: T? = null,
    override var lastUpdateTime: Long? = null,
    var temperatureHwUnitName: String? = null,
    var temperatureValue: TemperatureType? = null,
    var temperatureLastUpdateTime: Long? = null,
    var temperatureThreshold: TemperatureType? = null,
    var motionHwUnitName: String? = null,
    var motionValue: MotionType? = null,
    var motionLastUpdateTime: Long? = null,
    var actionTimeout: Long? = null,
    var enabled: Boolean = true,             // should this be here?
    override var lastTriggerSource: String? = null,
    override var firebaseNotify: Boolean = false,
    @TriggerType override var firebaseNotifyTrigger: String? = null,
    override var showInTaskList: Boolean = false,
    override var unitsTasks: Map<String, UnitTask> = HashMap(),
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

    override fun copy(): HomeUnit<T> {
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
            lastTriggerSource,
            firebaseNotify,
            firebaseNotifyTrigger,
            showInTaskList,
            unitsTasks
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
        booleanApplyAction: suspend HomeUnit<T>.(actionVal: Boolean, taskHomeUnitType: HomeUnitType, taskHomeUnitName: String, taskName: String, periodicallyOnlyHw: Boolean) -> Unit
    ) {
        // We set Switch and normal value as updateHomeUnitValuesAndTimes is only called by HwUnit
        when (hwUnit.name) {
            temperatureHwUnitName -> {
                temperatureValue = unitValue as TemperatureType?
                temperatureLastUpdateTime = updateTime
                temperatureThreshold?.let { threshold ->
                    temperatureValue?.let { temperature ->
                        if (temperature > threshold) {
                            // turn Off circulation
                            booleanApplyAction(false, type, name, name, false)
                        }
                    }
                }

            }
            motionHwUnitName -> {
                motionValue = unitValue as MotionType?
                motionLastUpdateTime = updateTime
                // TODO: Should we also turn off circulation while no more motion???
                if (motionValue == true &&
                    (temperatureValue ?: TemperatureType.MIN_VALUE) <
                    (temperatureThreshold ?: TemperatureType.MAX_VALUE)
                ) {
                    booleanApplyAction(true, type, name, name, false)
                }
            }
        }
    }
}