package com.krisbiketeam.smarthomeraspbpi3.units

import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit

interface BaseUnit<T> : AutoCloseable {
    val hwUnit: HwUnit
    var unitValue: T?
    var valueUpdateTime: String

    fun connect()
}