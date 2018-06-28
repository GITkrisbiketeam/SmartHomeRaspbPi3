package com.krisbiketeam.smarthomeraspbpi3.hardware

import com.google.android.things.pio.Gpio
import com.google.android.things.pio.GpioCallback
import com.google.android.things.pio.PeripheralManager
import timber.log.Timber

class Button : Sensor<Boolean> {
    private lateinit var onStateChangeListener: Sensor.OnStateChangeListener<Boolean>
    private var buttonGpio: Gpio? = null

    private val gpioCallback = object : GpioCallback {
        override fun onGpioEdge(gpio: Gpio?): Boolean {
            onStateChangeListener.onStateChanged(gpio?.value ?: false)
            return true
        }

        override fun onGpioError(gpio: Gpio?, error: Int) {
            Timber.w("$gpio: Error event $error")
        }
    }

    override fun start(onStateChangeListener: Sensor.OnStateChangeListener<Boolean>) {
        this.onStateChangeListener = onStateChangeListener
        val service = PeripheralManager.getInstance()
        buttonGpio = service.openGpio(gpioForButton).apply {
            setDirection(Gpio.DIRECTION_IN)
            setEdgeTriggerType(Gpio.EDGE_BOTH)
            registerGpioCallback(gpioCallback)
        }
    }

    override fun stop() {
        buttonGpio?.close().also {
            buttonGpio = null
        }
    }
}
