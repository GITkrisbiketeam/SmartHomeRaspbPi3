package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import kotlinx.coroutines.*
import timber.log.Timber

const val DEFAULT_WATCH_DOG_TIMEOUT = 2 * 1000L  // 2 seconds
const val DEFAULT_WATCH_DOG_DELAY = 15 * 60 * 1000L  // 15 minutes
private const val WATCH_DOG_DELAY_TASK_KEY = "watch_dog_delay_task_key"
private const val WATCH_DOG_TIMEOUT_TASK_KEY = "watch_dog_timeout_task_key"

data class MCP23017WatchDogHomeUnit<T : Any>(
    override val name: String = "", // Name should be unique for all units
    override val type: HomeUnitType = HomeUnitType.HOME_MCP23017_WATCH_DOG,
    override val room: String = "",
    override val hwUnitName: String? = "",
    override val value: T? = null,
    override val lastUpdateTime: Long? = null,
    val inputHwUnitName: String? = null,
    val inputValue: T? = null,
    val inputLastUpdateTime: Long? = null,
    val watchDogTimeout: Long = DEFAULT_WATCH_DOG_TIMEOUT,
    val watchDogDelay: Long = DEFAULT_WATCH_DOG_DELAY,

    override val lastTriggerSource: String? = null,
    override val firebaseNotify: Boolean = false,
    @TriggerType override val firebaseNotifyTrigger: String? = null,
    override val showInTaskList: Boolean = false,
    override val unitsTasks: Map<String, UnitTask> = HashMap(),
    override val unitJobs: MutableMap<String, Job> = mutableMapOf(),
) : HomeUnit<T> {

    override fun makeNotification(): MCP23017WatchDogHomeUnit<T> {
        return MCP23017WatchDogHomeUnit(
            name,
            type,
            room,
            hwUnitName,
            value,
            lastUpdateTime,
            inputHwUnitName,
            inputValue,
            inputLastUpdateTime,
            watchDogTimeout,
            watchDogDelay,
            lastTriggerSource = lastTriggerSource
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MCP23017WatchDogHomeUnit<*>

        if (name != other.name) return false
        if (type != other.type) return false
        if (room != other.room) return false
        if (hwUnitName != other.hwUnitName) return false
        if (value != other.value) return false
        if (lastUpdateTime != other.lastUpdateTime) return false
        if (inputHwUnitName != other.inputHwUnitName) return false
        if (inputValue != other.inputValue) return false
        if (inputLastUpdateTime != other.inputLastUpdateTime) return false
        if (watchDogTimeout != other.watchDogTimeout) return false
        if (watchDogDelay != other.watchDogDelay) return false
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
        result = 31 * result + (inputHwUnitName?.hashCode() ?: 0)
        result = 31 * result + (inputValue?.hashCode() ?: 0)
        result = 31 * result + (inputLastUpdateTime?.hashCode() ?: 0)
        result = 31 * result + (watchDogTimeout.hashCode())
        result = 31 * result + (watchDogDelay.hashCode())
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
        return inputHwUnitName == hwUnit.name
    }

    override fun unitValue(): T? {
        return inputValue
    }

    override suspend fun updateHomeUnitValuesAndTimes(
        hwUnit: HwUnit,
        unitValue: Any?,
        updateTime: Long,
        lastTriggerSource: String,
        booleanApplyAction: suspend (applyData: BooleanApplyActionData) -> HomeUnit<T>?
    ): HomeUnit<T> {
        // We set Switch and normal value as updateHomeUnitValuesAndTimes is only called by HwUnit
        supervisorScope {
            unitJobs[WATCH_DOG_TIMEOUT_TASK_KEY]?.cancel()
            unitJobs[WATCH_DOG_TIMEOUT_TASK_KEY] = launch(Dispatchers.IO) {
                delay(watchDogTimeout)
                if (value != unitValue) {
                    Timber.e("Watch Dog Timeout, Throw Error")
                    throw Exception("Watch Dog Timeout Error")
                }
            }
            unitJobs[WATCH_DOG_DELAY_TASK_KEY]?.cancel()
            unitJobs[WATCH_DOG_DELAY_TASK_KEY] = launch(Dispatchers.IO) {
                delay(watchDogDelay)
                if (unitValue is Boolean) {
                    booleanApplyAction(BooleanApplyActionData(
                        newActionVal = !unitValue,
                        taskHomeUnitType = type,
                        taskHomeUnitName = name,
                        taskName = name,
                        sourceHomeUnitName = name,
                        periodicallyOnlyHw = false
                    ))
                }
            }
        }
        return copy(inputValue = unitValue as T?, inputLastUpdateTime = updateTime)
    }
}