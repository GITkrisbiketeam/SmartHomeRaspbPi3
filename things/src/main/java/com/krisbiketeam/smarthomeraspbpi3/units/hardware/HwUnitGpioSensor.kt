package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.google.android.things.pio.Gpio
import com.google.android.things.pio.GpioCallback
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitGpio
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import timber.log.Timber
import java.util.*

open class HwUnitGpioSensor(name: String, location: String, pinName: String,
                            private val activeType: Int = Gpio.ACTIVE_HIGH,
                            override var gpio: Gpio? = null) : HwUnitGpio<Boolean>,
        Sensor<Boolean> {

    override val hwUnit: HwUnit =
            HwUnit(name, location, BoardConfig.GPIO_INPUT, pinName, ConnectionType.GPIO)
    override var unitValue: Boolean? = null
    override var valueUpdateTime: String = ""

    var hwUnitListener: Sensor.HwUnitListener<Boolean>? = null

    open val mGpioCallback = object : GpioCallback {
        override fun onGpioEdge(gpio: Gpio): Boolean {
            readValue(gpio)
            Timber.v("onGpioEdge gpio.readValue(): $hwUnit.value on: $hwUnit")
            hwUnitListener?.onHwUnitChanged(hwUnit, unitValue, valueUpdateTime)

            // Continue listening for more interrupts
            return true
        }

        override fun onGpioError(gpio: Gpio?, error: Int) {
            Timber.w("${gpio.toString()} : Error event $error on: $hwUnit")
        }
    }

    @Throws(Exception::class)
    override fun connect() {
        super.connect()

        gpio?.run {
            setDirection(Gpio.DIRECTION_IN)
            setEdgeTriggerType(Gpio.EDGE_BOTH)
            setActiveType(activeType)
        }
    }

    @Throws(Exception::class)
    override fun close() {
        unregisterListener()
        super.close()
    }

    @Throws(Exception::class)
    override fun registerListener(listener: Sensor.HwUnitListener<Boolean>) {
        Timber.d("registerListener")
        hwUnitListener = listener
        gpio?.registerGpioCallback(mGpioCallback)
    }

    override fun unregisterListener() {
        Timber.d("unregisterListener")
        gpio?.unregisterGpioCallback(mGpioCallback)
        hwUnitListener = null
    }


    @Throws(Exception::class)
    override fun readValue(): Boolean? {
        return readValue(gpio)
    }

    @Throws(Exception::class)
    fun readValue(gpio: Gpio?): Boolean? {
        unitValue = gpio?.value
        valueUpdateTime = Date().toString()

        return unitValue
    }
}
