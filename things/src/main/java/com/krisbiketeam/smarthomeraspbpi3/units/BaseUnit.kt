package com.krisbiketeam.smarthomeraspbpi3.units

import com.krisbiketeam.data.storage.HomeUnit

interface BaseUnit<T> : AutoCloseable {
    val homeUnit: HomeUnit<T>

    fun connect()

}