package com.krisbiketeam.smarthomeraspbpi3.units

import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import java.util.*

data class HwUnitValue<T>(val unitValue: T?, val valueUpdateTime: Long){
    override fun toString(): String {
        return "[unitValue:$unitValue, valueUpdateTime$valueUpdateTime(${Date(valueUpdateTime)})]"
    }
}

interface BaseHwUnit<T> {
    val hwUnit: HwUnit
    var hwUnitValue: HwUnitValue<T?>

    suspend fun connect(): Result<Unit>
    suspend fun close(): Result<Unit>
}