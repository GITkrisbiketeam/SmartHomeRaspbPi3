package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.google.android.things.pio.Gpio
import com.google.android.things.pio.GpioCallback
import com.krisbiketeam.data.storage.ConnectionType
import com.krisbiketeam.data.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.units.HomeUnitGpio
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import com.krisbiketeam.smarthomeraspbpi3.utils.Logger
import com.krisbiketeam.smarthomeraspbpi3.utils.Utils
import java.io.IOException
import java.util.*

private val TAG = Utils.getLogTag(HomeUnitGpioSensor::class.java)

open class HomeUnitGpioSensor(name: String,
                              location: String,
                              pinName: String,
                              private val activeType: Int = Gpio.ACTIVE_HIGH,
                              override var gpio: Gpio? = null) : HomeUnitGpio<Boolean>, Sensor<Boolean> {

    override val homeUnit: HomeUnit = HomeUnit(name, location, pinName, ConnectionType.GPIO)
    override var unitValue: Boolean? = null
    override var valueUpdateTime: String = ""

    var homeUnitListener: Sensor.HomeUnitListener<Boolean>? = null

    open val mGpioCallback = object : GpioCallback {
        override fun onGpioEdge(gpio: Gpio): Boolean {
            readValue(gpio)
            Logger.v(TAG, "onGpioEdge gpio.readValue(): $homeUnit.value on: $homeUnit")
            homeUnitListener?.onUnitChanged(homeUnit, unitValue, valueUpdateTime)

            // Continue listening for more interrupts
            return true
        }

        override fun onGpioError(gpio: Gpio?, error: Int) {
            Logger.w(TAG, gpio.toString() + ": Error event $error on: $homeUnit")
        }
    }

    override fun connect() {
        super.connect()

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
        unitValue = try {
            gpio?.value
        } catch (e: IOException) {
            Logger.e(TAG, "Error getting Value PeripheralIO API on: $homeUnit", e)
            // Set null value on error
            null
        }
        valueUpdateTime = Date().toString()

        return unitValue
    }
}
