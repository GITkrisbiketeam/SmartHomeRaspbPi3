package com.krisbiketeam.smarthomeraspbpi3.units

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

interface HwUnitI2C <T> : BaseHwUnit<T> {
    var device: AutoCloseable?

    @Throws(Exception::class)
    override suspend fun close():Result<Unit> {
        Timber.i("close on: $hwUnit")
        return withContext(Dispatchers.Main) {
            kotlin.runCatching {
                try {
                    device?.close()
                    device = null
                } catch (e: Exception) {
                    device = null
                    Timber.e(e, "Error closing PeripheralIO API on: $hwUnit")
                    throw (Exception("Error close HwUnitI2C", e))
                }
            }
        }
    }

}
