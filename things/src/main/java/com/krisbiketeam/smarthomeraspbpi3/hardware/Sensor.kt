package com.krisbiketeam.smarthomeraspbpi3.hardware

interface Sensor<out T> {
    fun start(onStateChangeListener: OnStateChangeListener<T>)
    fun stop()

    interface OnStateChangeListener<in T> {
        fun onStateChanged(state: T)
    }
}
