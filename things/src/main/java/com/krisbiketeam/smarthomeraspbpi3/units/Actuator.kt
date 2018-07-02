package com.krisbiketeam.smarthomeraspbpi3.units

interface Actuator<T> : BaseUnit<T> {

    fun setValue(value: T?)

}