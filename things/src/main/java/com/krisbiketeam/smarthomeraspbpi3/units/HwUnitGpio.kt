package com.krisbiketeam.smarthomeraspbpi3.units

import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import timber.log.Timber

interface HwUnitGpio<T> : BaseUnit<T> {
    var gpio: Gpio?

    override fun connect() {
        Timber.e("connect on: $hwUnit")
        if (gpio == null) {
            try {
                gpio = PeripheralManager.getInstance()?.openGpio(hwUnit.pinName)
            } catch (e: Exception) {
                Timber.e(e,"Error connecting device")
                try {
                    close()
                } catch (e: Exception) {
                    Timber.e(e,"Error closing device")
                }
            }
        }
    }

    override fun close() {
        Timber.e("close on: $hwUnit")
        try {
            gpio?.close()
        } catch (e: Exception) {
            Timber.e(e, "Error closing PeripheralIO API on: $hwUnit")
        } finally {
            gpio = null
        }
    }
}
