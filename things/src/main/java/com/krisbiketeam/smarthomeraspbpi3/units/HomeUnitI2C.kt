package com.krisbiketeam.smarthomeraspbpi3.units

import com.krisbiketeam.smarthomeraspbpi3.utils.Logger
import com.krisbiketeam.smarthomeraspbpi3.utils.Utils
import java.io.IOException

private val TAG = Utils.getLogTag(HomeUnitGpio::class.java)

interface HomeUnitI2C <T> : BaseUnit<T> {
    var device: AutoCloseable?

    override fun close() {
        try {
            device?.close()
        } catch (e: IOException) {
            Logger.e(TAG, "Error closing PeripheralIO API on: $homeUnit", e)
        } finally {
            device = null
        }
    }

}
