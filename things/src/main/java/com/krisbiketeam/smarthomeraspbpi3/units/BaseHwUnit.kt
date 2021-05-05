package com.krisbiketeam.smarthomeraspbpi3.units

import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit

interface BaseHwUnit<T> {
    val hwUnit: HwUnit
    var unitValue: T?
    var valueUpdateTime: Long

    suspend fun connect()
    suspend fun close()
}