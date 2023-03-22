package com.krisbiketeam.smarthomeraspbpi3.common.storage.dto

import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import kotlinx.coroutines.*
import timber.log.Timber

const val DEFAULT_WATCH_DOG_TIMEOUT = 2 * 1000L  // 2 seconds
const val DEFAULT_WATCH_DOG_DELAY = 15 * 60 * 1000L  // 15 minutes
private const val WATCH_DOG_DELAY_TASK_KEY = "watch_dog_delay_task_key"
private const val WATCH_DOG_TIMEOUT_TASK_KEY = "watch_dog_timeout_task_key"

data class MCP23017WatchDogHomeUnit<T : Any>(
    override var name: String = "", // Name should be unique for all units
    override var type: HomeUnitType = HomeUnitType.HOME_MCP23017_WATCH_DOG,
    override var room: String = "",
    override var hwUnitName: String? = "",
    override var value: T? = null,
    override var lastUpdateTime: Long? = null,
    var inputHwUnitName: String? = null,
    var inputValue: T? = null,
    var inputLastUpdateTime: Long? = null,
    var watchDogTimeout: Long = DEFAULT_WATCH_DOG_TIMEOUT,
    var watchDogDelay: Long = DEFAULT_WATCH_DOG_DELAY,

    override var lastTriggerSource: String? = null,
    override var firebaseNotify: Boolean = false,
    @TriggerType override var firebaseNotifyTrigger: String? = null,
    override var showInTaskList: Boolean = false,
    override var unitsTasks: Map<String, UnitTask> = HashMap(),
    override var unitJobs: MutableMap<String, Job> = mutableMapOf(),
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

    override fun copy(): HomeUnit<T> {
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
            lastTriggerSource,
            firebaseNotify,
            firebaseNotifyTrigger,
            showInTaskList,
            unitsTasks
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
        booleanApplyAction: suspend HomeUnit<T>.(actionVal: Boolean, taskHomeUnitType: HomeUnitType, taskHomeUnitName: String, taskName: String, periodicallyOnlyHw: Boolean) -> Unit
    ) {
        // We set Switch and normal value as updateHomeUnitValuesAndTimes is only called by HwUnit
        inputValue = unitValue as T?
        inputLastUpdateTime = updateTime
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
                    booleanApplyAction(!unitValue, type, name, name, false)
                }
            }
        }
    }
}