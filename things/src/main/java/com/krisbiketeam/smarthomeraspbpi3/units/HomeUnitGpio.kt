package com.krisbiketeam.smarthomeraspbpi3.units

import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import com.krisbiketeam.smarthomeraspbpi3.utils.Logger
import com.krisbiketeam.smarthomeraspbpi3.utils.Utils

import java.io.IOException

interface HomeUnitGpio : Unit {
    val homeUnit: HomeUnit
    val activeType: Int
    var gpio: Gpio?

    companion object {
        private val TAG = Utils.getLogTag(HomeUnitGpio::class.java)
    }

    override fun connect() {
        if (gpio == null) {
            val peripheralManager = PeripheralManager.getInstance()
            try {
                gpio = peripheralManager.openGpio(homeUnit.pinName)
            } catch (e: IOException) {
                close()
            }
        }
        gpio?.run {
            try {
                setDirection(Gpio.DIRECTION_IN)
                setEdgeTriggerType(Gpio.EDGE_BOTH)
                setActiveType(activeType)
            } catch (e: IOException) {
                Logger.e(TAG, "Error initializing PeripheralIO API on: $homeUnit", e)
            }
        }
    }

    override fun close() {
        try {
            gpio?.close()
        } catch (e: IOException) {
            Logger.e(TAG, "Error closing PeripheralIO API on: $homeUnit", e)
        } finally {
            gpio = null
        }
    }

    override fun readValue(): Any? {
        return readValue(gpio)
    }

    fun readValue(gpio: Gpio?): Any? {
        homeUnit.value = try {
            gpio?.value
        } catch (e: IOException) {
            Logger.e(TAG, "Error getting Value PeripheralIO API on: $homeUnit", e)
        }
        return homeUnit.value
    }
}
