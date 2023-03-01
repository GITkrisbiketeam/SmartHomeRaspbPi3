package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.google.android.things.pio.Gpio
import com.google.android.things.pio.GpioCallback
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitGpio
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import kotlinx.coroutines.*
import timber.log.Timber

open class HwUnitGpioSensor(name: String, location: String, pinName: String,
                            private val activeType: Int = Gpio.ACTIVE_HIGH,
                            override var gpio: Gpio? = null) : HwUnitGpio<Boolean>,
        Sensor<Boolean> {

    override val hwUnit: HwUnit =
            HwUnit(name, location, BoardConfig.GPIO_INPUT, pinName, ConnectionType.GPIO)
    override var unitValue: Boolean? = null
    override var valueUpdateTime: Long = System.currentTimeMillis()

    var hwUnitListener: Sensor.HwUnitListener<Boolean>? = null

    open val mGpioCallback = object : GpioCallback {
        override fun onGpioEdge(callbackGpio: Gpio): Boolean {
            if (gpio?.name == callbackGpio.name) {
                CoroutineScope(Dispatchers.IO).launch {
                    readValue()
                    Timber.v("onGpioEdge gpio.readValue(): $hwUnit.value on: $hwUnit")
                    hwUnitListener?.onHwUnitChanged(hwUnit, unitValue, valueUpdateTime)
                }

            }
            // Continue listening for more interrupts
            return true
        }

        override fun onGpioError(gpio: Gpio?, error: Int) {
            Timber.w("${gpio.toString()} : Error event $error on: $hwUnit")
        }
    }

    @Throws(Exception::class)
    override suspend fun connect() {
        super.connect()

        withContext(Dispatchers.Main) {
            gpio?.run {
                setDirection(Gpio.DIRECTION_IN)
                setEdgeTriggerType(Gpio.EDGE_BOTH)
                setActiveType(activeType)
            }
        }
    }

    @Throws(Exception::class)
    override suspend fun close() {
        unregisterListener()
        super.close()
    }

    @Throws(Exception::class)
    override suspend fun registerListener(listener: Sensor.HwUnitListener<Boolean>,
                                          exceptionHandler: CoroutineExceptionHandler) {
        Timber.d("registerListener")
        hwUnitListener = listener
        withContext(Dispatchers.Main) {
            gpio?.registerGpioCallback(mGpioCallback)
        }
    }

    override suspend fun unregisterListener() {
        Timber.d("unregisterListener")
        withContext(Dispatchers.Main) {
            gpio?.unregisterGpioCallback(mGpioCallback)
        }
        hwUnitListener = null
    }


    @Throws(Exception::class)
    override suspend fun readValue(): Boolean? {
        return readValue(gpio)
    }

    @Throws(Exception::class)
    private suspend fun readValue(gpio: Gpio?): Boolean? {
        withContext(Dispatchers.Main) {
            unitValue = gpio?.value
            valueUpdateTime = System.currentTimeMillis()
        }

        return unitValue
    }
}
