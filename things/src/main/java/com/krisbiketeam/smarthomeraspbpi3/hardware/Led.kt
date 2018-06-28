package com.krisbiketeam.smarthomeraspbpi3.hardware

import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager

class Led : Actuator<Boolean> {
    private var ledGpio: Gpio? = null

    override fun start() {
        val service = PeripheralManager.getInstance()
        ledGpio = service.openGpio(gpioForLED).apply {
            setActiveType(Gpio.ACTIVE_HIGH)
            setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        }
    }

    override fun stop() {
        ledGpio?.close().also {
            ledGpio = null
        }
    }

    override fun setState(state: Boolean) {
        ledGpio?.value = state
    }
}







