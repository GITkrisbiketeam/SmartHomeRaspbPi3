package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP9808
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*

private const val REFRESH_RATE = 10000L // 10 sec

class HwUnitI2CTempMCP9808Sensor(name: String,
                                location: String,
                                val pinName: String,
                                val softAddress: Int,
                                override var device: AutoCloseable? = null) : HwUnitI2C<Float>, Sensor<Float> {

    override val hwUnit: HwUnit = HwUnit(name, location, BoardConfig.TEMP_SENSOR_MCP9808, pinName, ConnectionType.I2C, softAddress)
    override var unitValue: Float? = null
    override var valueUpdateTime: String = ""

    private var job: Job? = null
    private var hwUnitListener: Sensor.HwUnitListener<Float>? = null

    override fun connect() {
        // Do noting we o not want to block I2C device so it will be opened while setting the value
        // and then immediately closed to release resources
    }


    override fun registerListener(listener: Sensor.HwUnitListener<Float>) {
        Timber.d("registerListener")
        hwUnitListener = listener
        job?.cancel()
        job = GlobalScope.launch {
            // We could also check for true as suspending delay() method is cancellable
            while (isActive) {
                // Cancel will not stop non suspending oneShotReadValue function
                //oneShotReadValue()
                readValue()
                hwUnitListener?.onUnitChanged(hwUnit, unitValue, valueUpdateTime)
                delay(REFRESH_RATE)
            }
        }
    }

    override fun unregisterListener() {
        Timber.d("unregisterListener")
        job?.cancel()
        hwUnitListener = null
    }

    private fun oneShotReadValue() {
        val mcp9808 = MCP9808(pinName, softAddress)
        // We do not want to block I2C buss so open device to only display some data and then immediately close it.
        // use block automatically closes resources referenced to tmp102
        mcp9808.shutdownMode = true
        mcp9808.readOneShotTemperature {
            unitValue = it
            valueUpdateTime = Date().toString()
            Timber.d("temperature:$unitValue")
            hwUnitListener?.onUnitChanged(hwUnit, unitValue, valueUpdateTime)
            mcp9808.close()
        }
    }

    override fun readValue(): Float? {
        // We do not want to block I2C buss so open device to only display some data and then immediately close it.
        // use block automatically closes resources referenced to tmp102
        val mcp9808 = MCP9808(pinName, softAddress)
        mcp9808.use {
            unitValue = it.readTemperature()
            valueUpdateTime = Date().toString()
            Timber.d("temperature:$unitValue")
        }

        return unitValue
    }
}
