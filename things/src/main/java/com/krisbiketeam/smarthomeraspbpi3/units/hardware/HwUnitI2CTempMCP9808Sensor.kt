package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP9808
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*

private const val REFRESH_RATE = 10000L // 10 sec

class HwUnitI2CTempMCP9808Sensor(name: String, location: String, private val pinName: String,
                                 private val softAddress: Int,
                                 private val refreshRate: Long? = REFRESH_RATE,
                                 override var device: AutoCloseable? = null) : HwUnitI2C<Float>,
        Sensor<Float> {

    override val hwUnit: HwUnit =
            HwUnit(name, location, BoardConfig.TEMP_SENSOR_MCP9808, pinName, ConnectionType.I2C,
                   softAddress, refreshRate = refreshRate)
    override var unitValue: Float? = null
    override var valueUpdateTime: String = ""

    private var job: Job? = null
    private var hwUnitListener: Sensor.HwUnitListener<Float>? = null

    override fun connect() {
        // Do noting we do not want to block I2C device so it will be opened while setting the value
        // and then immediately closed to release resources
    }

    @Throws(Exception::class)
    override fun registerListener(listener: Sensor.HwUnitListener<Float>,
                                  exceptionHandler: CoroutineExceptionHandler) {
        Timber.d("registerListener")
        hwUnitListener = listener
        job?.cancel()
        job = GlobalScope.plus(exceptionHandler).launch(Dispatchers.IO) {
            // We could also check for true as suspending delay() method is cancellable
            while (isActive) {
                // Cancel will not stop non suspending oneShotReadValue function
                oneShotReadValue()
                delay(refreshRate ?: REFRESH_RATE)
            }
        }
    }

    override fun unregisterListener() {
        Timber.d("unregisterListener")
        job?.cancel()
        hwUnitListener = null
    }

    @Throws(Exception::class)
    override fun close() {
        Timber.d("close")
        job?.cancel()
        super.close()
    }

    @Throws(Exception::class)
    private fun oneShotReadValue() {
        // We do not want to block I2C buss so open device to only display some data and then immediately close it.
        // use block automatically closes resources referenced to mcp9808
        MCP9808(pinName, softAddress).use {
            it.readOneShotTemperature { value ->
                unitValue = value
                valueUpdateTime = Date().toString()
                Timber.d("temperature:$unitValue")
                hwUnitListener?.onHwUnitChanged(hwUnit, unitValue, valueUpdateTime)
            }
        }
    }

    @Throws(Exception::class)
    override fun readValue(): Float? {
        // We do not want to block I2C buss so open device to only display some data and then immediately close it.
        // use block automatically closes resources referenced to tmp102
        MCP9808(pinName, softAddress).use {
            unitValue = it.readTemperature()
            valueUpdateTime = Date().toString()
            Timber.d("temperature:$unitValue")
        }
        return unitValue
    }
}
