package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP9808
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitValue
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import kotlinx.coroutines.*
import timber.log.Timber

private const val REFRESH_RATE = 10000L // 10 sec

class HwUnitI2CTempMCP9808Sensor(
    name: String, location: String,
    private val pinName: String,
    private val softAddress: Int,
    private val refreshRate: Long? = REFRESH_RATE,
    override var device: AutoCloseable? = null
) : HwUnitI2C<Float>, Sensor<Float> {

    override val hwUnit: HwUnit = HwUnit(
        name,
        location,
        BoardConfig.TEMP_SENSOR_MCP9808,
        pinName,
        ConnectionType.I2C,
        softAddress,
        refreshRate = refreshRate
    )
    override var hwUnitValue: HwUnitValue<Float?> = HwUnitValue(null, System.currentTimeMillis())

    private var job: Job? = null

    override suspend fun connect(): Result<Unit> {
        // Do noting we do not want to block I2C device so it will be opened while setting the value
        // and then immediately closed to release resources
        return Result.success(Unit)
    }

    // TODO use Flow here
    override suspend fun registerListener(listener: Sensor.HwUnitListener<Float>): Result<Unit> {
        Timber.d("registerListener")
        job?.cancel()
        job = supervisorScope {
            launch(Dispatchers.IO) {
                // We could also check for true as suspending delay() method is cancellable
                while (isActive) {
                    delay(refreshRate ?: REFRESH_RATE)
                    val result = oneShotReadValue()
                    // all data should be updated by suspending oneShotReadValue() method
                    listener.onHwUnitChanged(hwUnit, result)
                }
            }
        }
        return Result.success(Unit)
    }

    override suspend fun unregisterListener(): Result<Unit> {
        Timber.d("unregisterListener")
        job?.cancel()
        return Result.success(Unit)
    }

    override suspend fun close(): Result<Unit> {
        Timber.d("close")
        job?.cancel()
        return super.close()
    }

    private suspend fun oneShotReadValue(): Result<HwUnitValue<Float?>> {
        // We do not want to block I2C buss so open device to only display some data and then immediately close it.
        // use block automatically closes resources referenced to mcp9808
        return withContext(Dispatchers.Main) {
            runCatching {
                hwUnitValue = HwUnitValue(MCP9808(pinName, softAddress).use {
                    it.readOneShotTemperature()
                }, System.currentTimeMillis())
                Timber.d("temperature:$hwUnitValue")
                hwUnitValue
            }
        }
    }

    override suspend fun readValue(): Result<HwUnitValue<Float?>> {
        // We do not want to block I2C buss so open device to only display some data and then immediately close it.
        // use block automatically closes resources referenced to tmp102
        return withContext(Dispatchers.Main) {
            runCatching {
                hwUnitValue = HwUnitValue(MCP9808(pinName, softAddress).use {
                    it.readTemperature()
                }, System.currentTimeMillis())
                Timber.d("temperature:$hwUnitValue")

                hwUnitValue
            }
        }
    }
}
