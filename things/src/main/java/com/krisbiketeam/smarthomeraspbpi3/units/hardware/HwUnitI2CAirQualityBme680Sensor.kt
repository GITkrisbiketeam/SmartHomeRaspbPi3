package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Bme680Data
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.Bme680BsecJNI
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import kotlinx.coroutines.*
import timber.log.Timber

// Refresh rate is currently hardcoded in native library set to 3 sec or default 300 sec (5 minutes)
private const val REFRESH_RATE = 300000L // 5 min

class HwUnitI2CAirQualityBme680Sensor(private val secureStorage: SecureStorage, name: String, location: String, private val pinName: String,
                                      private val softAddress: Int, private val refreshRate: Long? = REFRESH_RATE,
                                      override var device: AutoCloseable? = null) :
        HwUnitI2C<Bme680Data>, Sensor<Bme680Data> {

    override val hwUnit: HwUnit =
            HwUnit(name, location, BoardConfig.AIR_QUALITY_SENSOR_BME680, pinName, ConnectionType.I2C,
                    softAddress, refreshRate = refreshRate)
    override var unitValue: Bme680Data? = null
    override var valueUpdateTime: Long = System.currentTimeMillis()

    private var job: Job? = null

    override suspend fun connect() {
        // Do noting we do not want to block I2C device so it will be opened while setting the value
        // and then immediately closed to release resources
    }

    @Throws(Exception::class)
    // TODO use Flow here
    override suspend fun registerListener(scope: CoroutineScope, listener: Sensor.HwUnitListener<Bme680Data>,
                                          exceptionHandler: CoroutineExceptionHandler) {
        Timber.d("registerListener")
        job?.cancel()
        job = scope.launch(Dispatchers.IO + exceptionHandler) {
            val bme680BsecJNI =  Bme680BsecJNI(this, secureStorage, pinName, softAddress) {
                unitValue = it
                valueUpdateTime = System.currentTimeMillis()
                Timber.d("Bme680Data:$unitValue")
                listener.onHwUnitChanged(hwUnit, unitValue, valueUpdateTime)
            }
            try {
                bme680BsecJNI.initBme680JNI(refreshRate ?: REFRESH_RATE < REFRESH_RATE)
            } catch (e: Exception) {
                Timber.i("Bme680 Job Canceled $e")
                //TODO: should we call that
                //throw e
            } finally {
                Timber.i("Bme680 finally close")
                bme680BsecJNI.close()
            }
        }
        Timber.i("registerListener FINSHED")
    }

    override suspend fun unregisterListener() {
        Timber.d("unregisterListener")
        job?.cancelAndJoin()
    }

    @Throws(Exception::class)
    override suspend fun close() {
        Timber.d("close")
        job?.cancelAndJoin()
        unitValue = null
        super.close()
    }

    override suspend fun readValue(): Bme680Data? {
        return unitValue
    }
}
