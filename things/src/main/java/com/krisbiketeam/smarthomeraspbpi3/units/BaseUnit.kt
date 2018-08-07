package com.krisbiketeam.smarthomeraspbpi3.units

import com.krisbiketeam.data.storage.dto.HomeUnit

interface BaseUnit<T> : AutoCloseable {
    val homeUnit: HomeUnit
    var unitValue: T?
    var valueUpdateTime: String

    fun connect()
}