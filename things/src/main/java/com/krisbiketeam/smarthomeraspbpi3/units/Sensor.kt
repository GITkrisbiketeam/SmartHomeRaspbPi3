package com.krisbiketeam.smarthomeraspbpi3.units

import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope

interface Sensor<T> : BaseHwUnit<T> {

    suspend fun registerListener(listener: HwUnitListener<T>, exceptionHandler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, _ -> })

    suspend fun unregisterListener()

    suspend fun readValue(): T?

    /**
     * Interface definition for a callback to be invoked when a Sensor event occurs.
     */
    interface HwUnitListener<in T> {
        /**
         * Called when a HwUnitLog event occurs
         *
         * @param hwUnit the HwUnitLog for which the event occurred
         */
        suspend fun onHwUnitChanged(hwUnit: HwUnit, unitValue: T?, updateTime: Long)

        suspend fun onHwUnitError(hwUnit: HwUnit, errorMsg: String, updateTime: Long) {}
    }
}