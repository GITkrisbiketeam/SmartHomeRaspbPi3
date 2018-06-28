package com.krisbiketeam.smarthomeraspbpi3.hardware

import com.google.android.things.contrib.driver.rainbowhat.RainbowHat
import com.google.android.things.pio.Gpio

class LedHat(private val led: LED) : Actuator<Boolean> {
    enum class LED { RED, GREEN, BLUE }

    private var ledGpio: Gpio? = null
    override fun start() {
        ledGpio = openLedGpio()
    }

    override fun stop() {
        ledGpio?.close().also {
            ledGpio = null
        }
    }

    override fun setState(state: Boolean) {
        ledGpio?.value = state
    }

    private fun openLedGpio(): Gpio? {
        return when (led) {
            LED.RED -> RainbowHat.openLedRed()
            LED.GREEN -> RainbowHat.openLedGreen()
            LED.BLUE -> RainbowHat.openLedBlue()
        }
    }
}
