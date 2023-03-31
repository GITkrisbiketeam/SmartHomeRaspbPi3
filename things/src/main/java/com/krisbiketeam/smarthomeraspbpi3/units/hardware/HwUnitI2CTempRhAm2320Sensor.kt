package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.AM2320
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.TemperatureAndHumidity
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitValue
import kotlinx.coroutines.*
import timber.log.Timber

private const val REFRESH_RATE = 300000L // 5 min

class HwUnitI2CTempRhAm2320Sensor(
    name: String,
    location: String,
    private val pinName: String,
    softAddress: Int,
    private val refreshRate: Long? = REFRESH_RATE,
    override var device: AutoCloseable? = null
) : HwUnitI2C<TemperatureAndHumidity>, Sensor<TemperatureAndHumidity> {

    override val hwUnit: HwUnit = HwUnit(
        name,
        location,
        BoardConfig.TEMP_RH_SENSOR_AM2320,
        pinName,
        ConnectionType.I2C,
        softAddress,
        refreshRate = refreshRate
    )
    override var hwUnitValue: HwUnitValue<TemperatureAndHumidity?> =
        HwUnitValue(null, System.currentTimeMillis())

    private var job: Job? = null

    override suspend fun connect(): Result<Unit> {
        // Do noting we do not want to block I2C device so it will be opened while setting the value
        // and then immediately closed to release resources
        return Result.success(Unit)
    }

    // TODO use Flow here
    override suspend fun registerListener(listener: Sensor.HwUnitListener<TemperatureAndHumidity>): Result<Unit> {
        Timber.d("registerListener")
        job?.cancel()
        job = supervisorScope {
            launch(Dispatchers.IO) {
                // We could also check for true as suspending delay() method is cancellable
                while (isActive) {
                    delay(refreshRate ?: REFRESH_RATE)
                    // Cancel will not stop non suspending oneShotReadValue function
                    val result = readValue()
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

    override suspend fun readValue(): Result<HwUnitValue<TemperatureAndHumidity?>> {
        // We do not want to block I2C buss so open device to only display some data and then immediately close it.
        // use block automatically closes resources referenced to tmp102
        return withContext(Dispatchers.Main) {
            runCatching {
                hwUnitValue = HwUnitValue(AM2320(pinName).use {
                    val (temperature, rh) = it.readOneShotTempAndRh()
                    TemperatureAndHumidity(temperature, rh)
                }, System.currentTimeMillis())
                Timber.d("temperatureAndHumidity:$hwUnitValue")
                hwUnitValue
            }
        }
    }
}
