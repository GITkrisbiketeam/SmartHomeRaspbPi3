package com.krisbiketeam.smarthomeraspbpi3.units

import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import timber.log.Timber

import java.io.IOException

interface HwUnitGpio<T> : BaseUnit<T> {
    var gpio: Gpio?

    override fun connect() {
        Timber.e("connect on: $hwUnit")
        if (gpio == null) {
            val peripheralManager = PeripheralManager.getInstance()
            try {
                gpio = peripheralManager.openGpio(hwUnit.pinName)
            } catch (e: IOException) {
                close()
            }
        }
    }

    override fun close() {
        Timber.e("close on: $hwUnit")
        try {
            gpio?.close()
        } catch (e: IOException) {
            Timber.e(e, "Error closing PeripheralIO API on: $hwUnit")
        } finally {
            gpio = null
        }
    }
}
