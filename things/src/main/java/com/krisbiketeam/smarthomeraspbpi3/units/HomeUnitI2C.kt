package com.krisbiketeam.smarthomeraspbpi3.units

import com.krisbiketeam.smarthomeraspbpi3.utils.Logger
import com.krisbiketeam.smarthomeraspbpi3.utils.Utils
import java.io.IOException

interface HomeUnitI2C <T> : BaseUnit<T> {
    var device: AutoCloseable?

    companion object {
        private val TAG = Utils.getLogTag(HomeUnitGpio::class.java)
    }

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
