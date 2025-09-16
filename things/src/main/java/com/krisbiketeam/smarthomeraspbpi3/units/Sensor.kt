package com.krisbiketeam.smarthomeraspbpi3.units

import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit

interface Sensor<T> : BaseHwUnit<T> {

    suspend fun registerListener(listener: HwUnitListener<T>): Result<Unit>

    suspend fun unregisterListener(): Result<Unit>

    suspend fun readValue(): Result<HwUnitValue<T?>>

    /**
     * Interface definition for a callback to be invoked when a Sensor event occurs.
     */
    interface HwUnitListener<T> {
        /**
         * Called when a HwUnitLog event occurs
         *
         * @param hwUnit the HwUnitLog for which the event occurred
         */
        fun onHwUnitChanged(hwUnit: HwUnit, result: Result<HwUnitValue<T?>>)
    }
}