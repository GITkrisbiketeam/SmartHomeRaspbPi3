package com.krisbiketeam.smarthomeraspbpi3.hardware

interface Actuator<in T> {
    fun start()
    fun stop()
    fun setState(state: T)
}
