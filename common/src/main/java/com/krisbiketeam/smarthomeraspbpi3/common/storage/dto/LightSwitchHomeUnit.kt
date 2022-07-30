package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

import com.google.firebase.database.Exclude
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType

data class LightSwitchHomeUnit<T : Any>(
    override var name: String = "", // Name should be unique for all units
    override var type: HomeUnitType = HomeUnitType.HOME_LIGHT_SWITCHES,
    override var room: String = "",
    override var hwUnitName: String? = "",
    override var value: T? = null,
    override var lastUpdateTime: Long? = null,
    var switchHwUnitName: String? = null,
    var switchValue: T? = null,
    var switchLastUpdateTime: Long? = null,

    override var lastTriggerSource: String? = null,
    override var firebaseNotify: Boolean = false,
    @TriggerType override var firebaseNotifyTrigger: String? = null,
    override var showInTaskList: Boolean = false,
    override var unitsTasks: Map<String, UnitTask> = HashMap(),
) : HomeUnit<T> {

    @Exclude
    @set:Exclude
    @get:Exclude
    override var applyFunction: suspend HomeUnit<in Any>.(Any) -> Unit = { }

    override fun makeNotification(): LightSwitchHomeUnit<T> {
        return LightSwitchHomeUnit(
            name,
            type,
            room,
            hwUnitName,
            value,
            lastUpdateTime,
            switchHwUnitName,
            switchValue,
            switchLastUpdateTime,
            lastTriggerSource = lastTriggerSource
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LightSwitchHomeUnit<*>

        if (name != other.name) return false
        if (type != other.type) return false
        if (room != other.room) return false
        if (hwUnitName != other.hwUnitName) return false
        if (value != other.value) return false
        if (lastUpdateTime != other.lastUpdateTime) return false
        if (switchHwUnitName != other.switchHwUnitName) return false
        if (switchValue != other.switchValue) return false
        if (switchLastUpdateTime != other.switchLastUpdateTime) return false
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
        result = 31 * result + (switchHwUnitName?.hashCode() ?: 0)
        result = 31 * result + (switchValue?.hashCode() ?: 0)
        result = 31 * result + (switchLastUpdateTime?.hashCode() ?: 0)
        result = 31 * result + (lastTriggerSource?.hashCode() ?: 0)
        result = 31 * result + firebaseNotify.hashCode()
        result = 31 * result + (firebaseNotifyTrigger?.hashCode() ?: 0)
        result = 31 * result + showInTaskList.hashCode()
        result = 31 * result + unitsTasks.hashCode()
        return result
    }

    override fun copy(): HomeUnit<T> {
        return LightSwitchHomeUnit(
            name,
            type,
            room,
            hwUnitName,
            value,
            lastUpdateTime,
            switchHwUnitName,
            switchValue,
            switchLastUpdateTime,
            lastTriggerSource,
            firebaseNotify,
            firebaseNotifyTrigger,
            showInTaskList,
            unitsTasks
        )
    }

    override fun isUnitAffected(hwUnit: HwUnit): Boolean {
        return switchHwUnitName == hwUnit.name
    }

    override fun getHomeUnitValue(): T? {
        return switchValue
    }

    override fun updateHomeUnitValuesAndTimes(hwUnit: HwUnit, unitValue: Any?, updateTime: Long) {
        // We set Switch and normal value as updateHomeUnitValuesAndTimes is only called by HwUnit
        switchValue = unitValue as T?
        switchLastUpdateTime = updateTime
    }
}