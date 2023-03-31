package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.google.android.things.pio.Gpio
import com.google.android.things.pio.GpioCallback
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitGpio
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitValue
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import kotlinx.coroutines.*
import timber.log.Timber

open class HwUnitGpioSensor(
    name: String,
    location: String,
    pinName: String,
    private val activeType: Int = Gpio.ACTIVE_HIGH,
    override var gpio: Gpio? = null,
) : HwUnitGpio<Boolean>, Sensor<Boolean> {

    override val hwUnit: HwUnit =
        HwUnit(name, location, BoardConfig.GPIO_INPUT, pinName, ConnectionType.GPIO)
    override var hwUnitValue: HwUnitValue<Boolean?> = HwUnitValue(null, System.currentTimeMillis())

    var hwUnitListener: Sensor.HwUnitListener<Boolean>? = null

    open val mGpioCallback = object : GpioCallback {
        override fun onGpioEdge(callbackGpio: Gpio): Boolean {
            if (gpio?.name == callbackGpio.name) {
                CoroutineScope(Dispatchers.IO).launch {
                    val result = readValue()
                    Timber.v("onGpioEdge gpio.readValue(): $result.value on: $hwUnit")
                    hwUnitListener?.onHwUnitChanged(hwUnit, result)
                }

            }
            // Continue listening for more interrupts
            return true
        }

        override fun onGpioError(gpio: Gpio?, error: Int) {
            Timber.w("${gpio.toString()} : Error event $error on: $hwUnit")
            hwUnitListener?.onHwUnitChanged(
                hwUnit,
                Result.failure(Exception("onGpioError on:$gpio error:$error"))
            )
        }
    }

    @Throws(Exception::class)
    override suspend fun connect(): Result<Unit> {
        return withContext(Dispatchers.Main) {
            super.connect().mapCatching {
                gpio?.apply {
                    setDirection(Gpio.DIRECTION_IN)
                    setEdgeTriggerType(Gpio.EDGE_BOTH)
                    setActiveType(activeType)
                }
                Unit
            }
        }
    }

    @Throws(Exception::class)
    override suspend fun close(): Result<Unit> {
        unregisterListener()
        return super.close()

    }

    @Throws(Exception::class)
    override suspend fun registerListener(listener: Sensor.HwUnitListener<Boolean>): Result<Unit> {
        Timber.d("registerListener")
        hwUnitListener = listener
        return withContext(Dispatchers.Main) {
            runCatching {
                gpio?.registerGpioCallback(mGpioCallback) ?: Unit
            }
        }
    }

    override suspend fun unregisterListener(): Result<Unit> {
        Timber.d("unregisterListener")
        hwUnitListener = null
        return withContext(Dispatchers.Main) {
            runCatching {
                gpio?.unregisterGpioCallback(mGpioCallback) ?: Unit
            }
        }
    }


    override suspend fun readValue(): Result<HwUnitValue<Boolean?>> {
        return readValue(gpio)
    }

    @Throws(Exception::class)
    private suspend fun readValue(gpio: Gpio?): Result<HwUnitValue<Boolean?>> {
        return withContext(Dispatchers.Main) {
            runCatching {
                val unitValue = gpio?.value
                hwUnitValue = HwUnitValue(unitValue, System.currentTimeMillis())
                hwUnitValue
            }
        }
    }
}
