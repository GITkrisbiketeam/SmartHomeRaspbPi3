package com.krisbiketeam.smarthomeraspbpi3.units

import com.krisbiketeam.data.storage.dto.HomeUnitLog

interface BaseUnit<T> : AutoCloseable {
    val homeUnit: HomeUnitLog<T>

    fun connect()

}