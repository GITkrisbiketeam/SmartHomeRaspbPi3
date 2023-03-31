package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Bme680Data
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.Bme680BsecJNI
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitValue
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import kotlinx.coroutines.*
import timber.log.Timber

// Refresh rate is currently hardcoded in native library set to 3 sec or default 300 sec (5 minutes)
private const val REFRESH_RATE = 300000L // 5 min

class HwUnitI2CAirQualityBme680Sensor(
    private val secureStorage: SecureStorage,
    name: String,
    location: String,
    private val pinName: String,
    private val softAddress: Int,
    private val refreshRate: Long? = REFRESH_RATE,
    override var device: AutoCloseable? = null
) : HwUnitI2C<Bme680Data>, Sensor<Bme680Data> {

    override val hwUnit: HwUnit = HwUnit(
        name,
        location,
        BoardConfig.AIR_QUALITY_SENSOR_BME680,
        pinName,
        ConnectionType.I2C,
        softAddress,
        refreshRate = refreshRate
    )
    override var hwUnitValue: HwUnitValue<Bme680Data?> =
        HwUnitValue(null, System.currentTimeMillis())

    private var job: Job? = null

    override suspend fun connect(): Result<Unit> {
        // Do noting we do not want to block I2C device so it will be opened while setting the value
        // and then immediately closed to release resources
        return Result.success(Unit)
    }

    // TODO use Flow here
    override suspend fun registerListener(listener: Sensor.HwUnitListener<Bme680Data>): Result<Unit> {
        Timber.d("registerListener")
        job?.cancel()
        // TODO check this more
        job = supervisorScope {
            launch(Dispatchers.IO) {
                runCatching {
                    Bme680BsecJNI(this, secureStorage, pinName, softAddress) {
                        hwUnitValue = HwUnitValue(it, System.currentTimeMillis())
                        Timber.d("Bme680Data:$hwUnitValue")
                        listener.onHwUnitChanged(hwUnit, Result.success(hwUnitValue))
                    }
                }.onSuccess { bme680BsecJNI ->
                    try {
                        bme680BsecJNI.initBme680JNI((refreshRate ?: REFRESH_RATE) < REFRESH_RATE)
                    } catch (e: Exception) {
                        Timber.i("Bme680 Job Canceled $e")
                        //TODO: should we call that
                        //listener.onHwUnitChanged(hwUnit, Result.failure(e))
                    } finally {
                        Timber.i("Bme680 finally close")
                        kotlin.runCatching {
                            bme680BsecJNI.close()
                        }.onFailure {
                            listener.onHwUnitChanged(hwUnit, Result.failure(it))
                        }
                    }
                }.onFailure {
                    listener.onHwUnitChanged(hwUnit, Result.failure(it))
                }
            }
        }
        Timber.i("registerListener FINSHED")
        return Result.success(Unit)
    }

    override suspend fun unregisterListener(): Result<Unit> {
        Timber.d("unregisterListener")
        job?.cancelAndJoin()
        return Result.success(Unit)
    }

    @Throws(Exception::class)
    override suspend fun close(): Result<Unit> {
        Timber.d("close")
        job?.cancelAndJoin()
        hwUnitValue = HwUnitValue(null, System.currentTimeMillis())
        return super.close()
    }

    override suspend fun readValue(): Result<HwUnitValue<Bme680Data?>> {
        return Result.success(hwUnitValue)
    }
}
