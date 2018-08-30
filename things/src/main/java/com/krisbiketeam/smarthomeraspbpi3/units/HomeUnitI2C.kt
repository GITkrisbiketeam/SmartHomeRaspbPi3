package com.krisbiketeam.smarthomeraspbpi3.units

import timber.log.Timber
import java.io.IOException

interface HomeUnitI2C <T> : BaseUnit<T> {
    var device: AutoCloseable?

    override fun close() {
        try {
            device?.close()
        } catch (e: IOException) {
            Timber.e("Error closing PeripheralIO API on: $homeUnit", e)
        } finally {
            device = null
        }
    }

}
