package com.krisbiketeam.smarthomeraspbpi3.units

interface BaseUnit<T> : AutoCloseable {
    val homeUnit: HomeUnit<T>

    fun connect()

}