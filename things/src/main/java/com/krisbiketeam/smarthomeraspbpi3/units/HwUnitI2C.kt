package com.krisbiketeam.smarthomeraspbpi3.units

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

interface HwUnitI2C <T> : BaseHwUnit<T> {
    var device: AutoCloseable?

    @Throws(Exception::class)
    override suspend fun close() {
        withContext(Dispatchers.Main) {
            try {
                device?.close()
            } catch (e: Exception) {
                Timber.e(e, "Error closing PeripheralIO API on: $hwUnit")
                throw (Exception("Error close HwUnitI2C", e))
            } finally {
                device = null
            }
        }
    }

}
