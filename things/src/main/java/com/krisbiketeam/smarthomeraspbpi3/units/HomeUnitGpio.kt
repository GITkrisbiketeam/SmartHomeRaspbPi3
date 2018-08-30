package com.krisbiketeam.smarthomeraspbpi3.units

import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import timber.log.Timber

import java.io.IOException

interface HomeUnitGpio<T> : BaseUnit<T> {
    var gpio: Gpio?

    override fun connect() {
        Timber.e("connect on: $homeUnit")
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
        Timber.e("close on: $homeUnit")
        try {
            gpio?.close()
        } catch (e: IOException) {
            Timber.e("Error closing PeripheralIO API on: $homeUnit", e)
        } finally {
            gpio = null
        }
    }
}
