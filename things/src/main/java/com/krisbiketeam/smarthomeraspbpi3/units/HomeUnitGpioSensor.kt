package com.krisbiketeam.smarthomeraspbpi3.units

import com.google.android.things.pio.Gpio
import com.google.android.things.pio.GpioCallback
import com.krisbiketeam.smarthomeraspbpi3.utils.Logger
import com.krisbiketeam.smarthomeraspbpi3.utils.Utils

import java.io.IOException

open class HomeUnitGpioSensor(override val homeUnit: HomeUnit, override val activeType: Int, override var gpio: Gpio?) : HomeUnitGpio, Sensor {
    companion object {
        private val TAG = Utils.getLogTag(HomeUnitGpioSensor::class.java)
    }

    var homeUnitListener: Sensor.HomeUnitListener? = null

    init {
        //We can safely connect from constructor as this does not block other HomeUnit peripherals
        connect()
    }

    open val mGpioCallback = object : GpioCallback {
        override fun onGpioEdge(gpio: Gpio): Boolean {
            val value = readValue(gpio)
            Logger.v(TAG, "onGpioEdge gpio.readValue(): $value on: $homeUnit")
            homeUnitListener?.onUnitChanged(homeUnit, value)

            // Continue listening for more interrupts
            return true
        }

        override fun onGpioError(gpio: Gpio?, error: Int) {
            Logger.w(TAG, gpio.toString() + ": Error event $error on: $homeUnit")
        }
    }

    override fun close() {
        unregisterListener()
        super.close()
    }

    override fun registerListener(listener: Sensor.HomeUnitListener) {
        Logger.d(TAG, "registerListener")
        homeUnitListener = listener
        try {
            gpio?.registerGpioCallback(mGpioCallback)
        } catch (e: IOException) {
            Logger.e(TAG, "Error registerListener PeripheralIO API on: $homeUnit", e)
        }
    }

    override fun unregisterListener() {
        Logger.d(TAG, "unregisterListener")
        gpio?.unregisterGpioCallback(mGpioCallback)
        homeUnitListener = null
    }
}
