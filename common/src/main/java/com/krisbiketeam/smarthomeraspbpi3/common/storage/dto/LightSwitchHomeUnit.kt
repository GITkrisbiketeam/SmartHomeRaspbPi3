package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

import com.google.firebase.database.Exclude

data class LightSwitchHomeUnit<T : Any>(
    override var name: String = "", // Name should be unique for all units
    override var type: String = "",
    override var room: String = "",
    override var hwUnitName: String? = "",
    override var value: T? = null,
    override var lastUpdateTime: Long? = null,
    override var secondHwUnitName: String? = null,
    override var secondValue: T? = null,
    override var secondLastUpdateTime: Long? = null,

    override var min: T? = null,
    override var minLastUpdateTime: Long? = null,
    override var max: T? = null,
    override var maxLastUpdateTime: Long? = null,

    override var lastTriggerSource: String? = null,
    override var firebaseNotify: Boolean = false,
    @TriggerType override var firebaseNotifyTrigger: String? = null,
    override var showInTaskList: Boolean = false,
    override var unitsTasks: Map<String, UnitTask> = HashMap(),
) : HomeUnit<T> {
    constructor(homeUnit: HomeUnit<T>) : this(
        homeUnit.name,
        homeUnit.type,
        homeUnit.room,
        homeUnit.hwUnitName,
        homeUnit.value,
        homeUnit.lastUpdateTime,
        homeUnit.secondHwUnitName,
        homeUnit.secondValue,
        homeUnit.secondLastUpdateTime,
        lastTriggerSource = homeUnit.lastTriggerSource,
        firebaseNotify = homeUnit.firebaseNotify,
        firebaseNotifyTrigger = homeUnit.firebaseNotifyTrigger,
        showInTaskList = homeUnit.showInTaskList,
        unitsTasks = homeUnit.unitsTasks
    )

    @Exclude
    @set:Exclude
    @get:Exclude
    override var applyFunction: suspend HomeUnit<in Any>.(Any) -> Unit = { }

    override fun makeInvariant(): LightSwitchHomeUnit<Any> {
        return LightSwitchHomeUnit(
            name,
            type,
            room,
            hwUnitName,
            value,
            lastUpdateTime,
            secondHwUnitName,
            secondValue,
            secondLastUpdateTime,
            lastTriggerSource,
            firebaseNotify = firebaseNotify,
            firebaseNotifyTrigger = firebaseNotifyTrigger,
            showInTaskList = showInTaskList,
            unitsTasks = unitsTasks
        )
    }

    override fun makeNotification(): LightSwitchHomeUnit<T> {
        return LightSwitchHomeUnit(
            name,
            type,
            room,
            hwUnitName,
            value,
            lastUpdateTime,
            secondHwUnitName,
            secondValue,
            secondLastUpdateTime,
            lastTriggerSource = lastTriggerSource
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HomeUnit<*>

        if (name != other.name) return false
        if (type != other.type) return false
        if (room != other.room) return false
        if (hwUnitName != other.hwUnitName) return false
        if (value != other.value) return false
        if (lastUpdateTime != other.lastUpdateTime) return false
        if (secondHwUnitName != other.secondHwUnitName) return false
        if (secondValue != other.secondValue) return false
        if (secondLastUpdateTime != other.secondLastUpdateTime) return false
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
        result = 31 * result + (secondHwUnitName?.hashCode() ?: 0)
        result = 31 * result + (secondValue?.hashCode() ?: 0)
        result = 31 * result + (secondLastUpdateTime?.hashCode() ?: 0)
        result = 31 * result + (lastTriggerSource?.hashCode() ?: 0)
        result = 31 * result + firebaseNotify.hashCode()
        result = 31 * result + (firebaseNotifyTrigger?.hashCode() ?: 0)
        result = 31 * result + showInTaskList.hashCode()
        result = 31 * result + unitsTasks.hashCode()
        return result
    }
}