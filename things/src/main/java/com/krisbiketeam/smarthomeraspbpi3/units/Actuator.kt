package com.krisbiketeam.smarthomeraspbpi3.units

interface Actuator<T> : BaseHwUnit<T> {

    suspend fun setValue(value: T)

}