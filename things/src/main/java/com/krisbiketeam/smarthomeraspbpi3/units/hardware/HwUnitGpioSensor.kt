package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.google.android.things.pio.Gpio
import com.google.android.things.pio.GpioCallback
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnitLog
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitGpio
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import timber.log.Timber
import java.util.*

open class HwUnitGpioSensor(name: String,
                            location: String,
                            pinName: String,
                            private val activeType: Int = Gpio.ACTIVE_HIGH,
                            override var gpio: Gpio? = null) : HwUnitGpio<Boolean>, Sensor<Boolean> {

    override val hwUnit: HwUnit = HwUnit(name, location, BoardConfig.GPIO_INPUT, pinName, ConnectionType.GPIO)
    override var unitValue: Boolean? = null
    override var valueUpdateTime: String = ""

    var hwUnitListener: Sensor.HwUnitListener<Boolean>? = null

    open val mGpioCallback = object : GpioCallback {
        override fun onGpioEdge(gpio: Gpio): Boolean {
            readValue(gpio)
            Timber.v("onGpioEdge gpio.readValue(): $hwUnit.value on: $hwUnit")
            hwUnitListener?.onUnitChanged(hwUnit, unitValue, valueUpdateTime)

            // Continue listening for more interrupts
            return true
        }

        override fun onGpioError(gpio: Gpio?, error: Int) {
            Timber.w("${gpio.toString()} : Error event $error on: $hwUnit")
        }
    }

    override fun connect() {
        super.connect()

        gpio?.run {
            try {
                setDirection(Gpio.DIRECTION_IN)
                setEdgeTriggerType(Gpio.EDGE_BOTH)
                setActiveType(activeType)
            } catch (e: Exception) {
                FirebaseHomeInformationRepository.addHwUnitErrorEvent(HwUnitLog(hwUnit, unitValue, e.message, Date().toString()))
                Timber.e(e,"Error initializing PeripheralIO API on: $hwUnit")
            }
        }
    }

    override fun close() {
        unregisterListener()
        super.close()
    }

    override fun registerListener(listener: Sensor.HwUnitListener<Boolean>) {
        Timber.d( "registerListener")
        hwUnitListener = listener
        try {
            gpio?.registerGpioCallback(mGpioCallback)
        } catch (e: Exception) {
            FirebaseHomeInformationRepository.addHwUnitErrorEvent(HwUnitLog(hwUnit, unitValue, e.message, Date().toString()))
            Timber.e(e,"Error registerListener PeripheralIO API on: $hwUnit")
        }
    }

    override fun unregisterListener() {
        Timber.d( "unregisterListener")
        gpio?.unregisterGpioCallback(mGpioCallback)
        hwUnitListener = null
    }


    override fun readValue(): Boolean? {
        return readValue(gpio)
    }

    fun readValue(gpio: Gpio?): Boolean? {
        unitValue = try {
            gpio?.value
        } catch (e: Exception) {
            FirebaseHomeInformationRepository.addHwUnitErrorEvent(HwUnitLog(hwUnit, unitValue, e.message, Date().toString()))
            Timber.e(e,"Error getting Value PeripheralIO API on: $hwUnit")
            // Set null value on error
            null
        }
        valueUpdateTime = Date().toString()

        return unitValue
    }
}
