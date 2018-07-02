package com.krisbiketeam.smarthomeraspbpi3.units

import com.google.android.things.pio.Gpio
import com.google.android.things.pio.GpioCallback
import com.krisbiketeam.data.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.utils.Logger
import com.krisbiketeam.smarthomeraspbpi3.utils.Utils

import java.io.IOException

open class HomeUnitGpioSensor(name: String,
                              location: String,
                              pinName: String,
                              override val activeType: Int = Gpio.ACTIVE_HIGH,
                              override var gpio: Gpio? = null) : HomeUnitGpio<Boolean>, Sensor<Boolean> {

    companion object {
        private val TAG = Utils.getLogTag(HomeUnitGpioSensor::class.java)
    }

    override val homeUnit: HomeUnit<Boolean> = HomeUnit(name, location, pinName, ConnectionType.GPIO)

    var homeUnitListener: Sensor.HomeUnitListener<Boolean>? = null

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

    override fun registerListener(listener: Sensor.HomeUnitListener<Boolean>) {
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


    override fun readValue(): Boolean? {
        return readValue(gpio)
    }

    fun readValue(gpio: Gpio?): Boolean? {
        homeUnit.value = try {
            gpio?.value
        } catch (e: IOException) {
            Logger.e(TAG, "Error getting Value PeripheralIO API on: $homeUnit", e)
            // Set null value on error
            null
        }
        return homeUnit.value
    }
}
