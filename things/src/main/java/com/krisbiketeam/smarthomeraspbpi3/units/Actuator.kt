package com.krisbiketeam.smarthomeraspbpi3.units

interface Actuator<T> : BaseHwUnit<T> {

    fun setValue(value: T?)

}