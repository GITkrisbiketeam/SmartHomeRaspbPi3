package com.krisbiketeam.smarthomeraspbpi3.units

import com.krisbiketeam.data.storage.dto.HwUnit

interface BaseUnit<T> : AutoCloseable {
    val hwUnit: HwUnit
    var unitValue: T?
    var valueUpdateTime: String

    fun connect()
}