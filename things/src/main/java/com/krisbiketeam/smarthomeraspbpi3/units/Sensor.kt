package com.krisbiketeam.smarthomeraspbpi3.units

import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit

interface Sensor<T> : BaseUnit<T> {

    fun registerListener(listener: HwUnitListener<T>)

    fun unregisterListener()

    fun readValue(): T?

    /**
     * Interface definition for a callback to be invoked when a Sensor event occurs.
     */
    interface HwUnitListener<in T> {
        /**
         * Called when a HwUnitLog event occurs
         *
         * @param hwUnit the HwUnitLog for which the event occurred
         */
        fun onHwUnitChanged(hwUnit: HwUnit, unitValue: T?, updateTime: String)
    }
}