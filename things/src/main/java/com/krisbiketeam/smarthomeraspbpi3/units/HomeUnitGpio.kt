package com.krisbiketeam.smarthomeraspbpi3.units

import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import com.krisbiketeam.smarthomeraspbpi3.utils.Logger
import com.krisbiketeam.smarthomeraspbpi3.utils.Utils

import java.io.IOException

interface HomeUnitGpio<T> : BaseUnit<T> {
    var gpio: Gpio?

    companion object {
        private val TAG = Utils.getLogTag(HomeUnitGpio::class.java)
    }

    override fun connect() {
        Logger.e(TAG, "connect on: $homeUnit")
        if (gpio == null) {
            val peripheralManager = PeripheralManager.getInstance()
            try {
                gpio = peripheralManager.openGpio(homeUnit.pinName)
            } catch (e: IOException) {
                close()
            }
        }
    }

    override fun close() {
        Logger.e(TAG, "close on: $homeUnit")
        try {
            gpio?.close()
        } catch (e: IOException) {
            Logger.e(TAG, "Error closing PeripheralIO API on: $homeUnit", e)
        } finally {
            gpio = null
        }
    }
}
