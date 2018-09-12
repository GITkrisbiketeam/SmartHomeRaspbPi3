package com.krisbiketeam.smarthomeraspbpi3.units

import timber.log.Timber
import java.io.IOException

interface HwUnitI2C <T> : BaseUnit<T> {
    var device: AutoCloseable?

    override fun close() {
        try {
            device?.close()
        } catch (e: IOException) {
            Timber.e(e,"Error closing PeripheralIO API on: $hwUnit")
        } finally {
            device = null
        }
    }

}
