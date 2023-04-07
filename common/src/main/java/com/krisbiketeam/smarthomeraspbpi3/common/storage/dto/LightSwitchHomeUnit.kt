package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

import com.google.firebase.database.Exclude
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import kotlinx.coroutines.Job

data class LightSwitchHomeUnit<T : Any>(
    override val name: String = "", // Name should be unique for all units
    override val type: HomeUnitType = HomeUnitType.HOME_LIGHT_SWITCHES,
    override val room: String = "",
    override val hwUnitName: String? = "",
    override val value: T? = null,
    override val lastUpdateTime: Long? = null,
    val switchHwUnitName: String? = null,
    val switchValue: T? = null,
    val switchLastUpdateTime: Long? = null,

    override val lastTriggerSource: String? = null,
    override val firebaseNotify: Boolean = false,
    @TriggerType override val firebaseNotifyTrigger: String? = null,
    override val showInTaskList: Boolean = false,
    override val unitsTasks: Map<String, UnitTask> = HashMap(),
    // should it also be in equals/hashCode
    @Exclude
    @get:Exclude
    override val unitJobs: MutableMap<String, Job> = mutableMapOf(),
) : HomeUnit<T> {

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
        return switchHwUnitName == hwUnit.name
    }

    override fun unitValue(): T? {
        return switchValue
    }

    override suspend fun updateHomeUnitValuesAndTimes(
        hwUnit: HwUnit,
        unitValue: Any?,
        updateTime: Long,
        lastTriggerSource: String,
        booleanApplyAction: suspend (applyData: BooleanApplyActionData) -> Unit
    ): HomeUnit<T> {
        // We set Switch and normal value as updateHomeUnitValuesAndTimes is only called by HwUnit
        return copy(switchValue= unitValue as T?, switchLastUpdateTime = updateTime).also {
            if (unitValue is Boolean) {
                booleanApplyAction(BooleanApplyActionData(unitValue, type, name, name, name, false))
            }
        }
    }
}