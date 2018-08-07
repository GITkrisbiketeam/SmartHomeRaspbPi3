package com.krisbiketeam.smarthomeraspbpi3.units

import com.krisbiketeam.data.storage.dto.HomeUnit

interface Sensor<T> : BaseUnit<T> {

    fun registerListener(listener: HomeUnitListener<T>)

    fun unregisterListener()

    fun readValue(): T?

    /**
     * Interface definition for a callback to be invoked when a Sensor event occurs.
     */
    interface HomeUnitListener<in T> {
        /**
         * Called when a HomeUnitLog event occurs
         *
         * @param homeUnit the HomeUnitLog for which the event occurred
         */
        fun onUnitChanged(homeUnit: HomeUnit, unitValue: T?, updateTime: String)
    }
}