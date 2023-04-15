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
import kotlin.math.abs

private const val REFRESH_RATE = 10000L // 10 sec
private const val MAX_READ_VALUE_RETRY = 3 // 10 sec

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

    private val maxTempDiffInTime: Long = ((refreshRate ?: REFRESH_RATE) / 60_000).let {
        // 60_000     1 minute - 1 degree
        // 600_000     10 minute - 10 degree
        if (it < 1) 1 else if (it > 10) 10 else it
    }

    override suspend fun connect(): Result<Unit> {
        // Do noting we do not want to block I2C device so it will be opened while setting the value
        // and then immediately closed to release resources
        return Result.success(Unit)
    }

    // TODO use Flow here
    override suspend fun registerListener(listener: Sensor.HwUnitListener<Float>): Result<Unit> {
        Timber.d("registerListener")
        job?.cancel()
        supervisorScope {
            job = launch(Dispatchers.IO) {
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
                hwUnitValue = MCP9808(pinName, softAddress).use {
                    Timber.d("MCP9808 use")
                    var i = 0
                    val lastTemp = hwUnitValue.unitValue
                    var temp: Float?
                    var retry: Boolean
                    val valueList:MutableList<Float> = mutableListOf()
                    do {
                        temp = it.readOneShotTemperature()
                        Timber.d("MCP9808 lastTemp$lastTemp temp:$temp i:$i")

                        retry = i++ < MAX_READ_VALUE_RETRY && checkOutOfRange(temp, lastTemp)

                        if (temp != null) valueList.add(temp)

                        if (retry) delay((refreshRate?:REFRESH_RATE)/10L)
                    } while (retry)
                    val newValue = if (checkOutOfRange(temp, lastTemp)) {
                        valueList.sorted().let { sortedList ->
                            if (sortedList.isNotEmpty()) {
                                sortedList[sortedList.size / 2]
                            } else {
                                null
                            }
                        }
                    } else {
                        temp
                    }
                    HwUnitValue(
                        newValue,
                        if (newValue != null) System.currentTimeMillis() else hwUnitValue.valueUpdateTime
                    )
                }
                Timber.d("temperature:$hwUnitValue")
                hwUnitValue
            }
        }
    }

    private fun checkOutOfRange(temp: Float?, lastTemp: Float?): Boolean {
        return (temp == null || (lastTemp != null && abs(temp - lastTemp) > maxTempDiffInTime))
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
