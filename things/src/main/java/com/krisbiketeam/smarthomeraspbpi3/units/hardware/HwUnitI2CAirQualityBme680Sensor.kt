package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.knobtviker.android.things.contrib.community.driver.bme680.Bme680
import com.knobtviker.android.things.contrib.community.driver.bme680.Bme680.ALTERNATIVE_I2C_ADDRESS
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Bme680Data
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import kotlinx.coroutines.*
import timber.log.Timber

private const val REFRESH_RATE = 300000L // 5 min

class HwUnitI2CAirQualityBme680Sensor(name: String, location: String, private val pinName: String,
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
    override suspend fun registerListener(listener: Sensor.HwUnitListener<Bme680Data>,
                                          exceptionHandler: CoroutineExceptionHandler) {
        Timber.d("registerListener")
        job?.cancel()
        job = GlobalScope.plus(exceptionHandler).launch(Dispatchers.IO) {
            while (isActive) {
                delay(refreshRate ?: REFRESH_RATE)
                // Cancel will not stop non suspending oneShotReadValue function
                oneShotReadValue()
                // all data should be updated by suspending oneShotReadValue() method
                listener.onHwUnitChanged(hwUnit, unitValue, valueUpdateTime)
            }
        }
    }

    override suspend fun unregisterListener() {
        Timber.d("unregisterListener")
        job?.cancel()
    }

    @Throws(Exception::class)
    override suspend fun close() {
        Timber.d("close")
        job?.cancel()
        super.close()
    }

    @Throws(Exception::class)
    private suspend fun oneShotReadValue() {
        // We do not want to block I2C buss so open device to only display some data and then immediately close it.
        // use block automatically closes resources referenced to mcp9808
        withContext(Dispatchers.Main) {
            Bme680(pinName, ALTERNATIVE_I2C_ADDRESS).use {
                // Configure driver oversampling for temperature, humidity or pressure,
                // threshold filter or gas status settings according to your use case
                it.temperatureOversample = Bme680.OVERSAMPLING_1X
                it.humidityOversample = Bme680.OVERSAMPLING_1X
                // Ensure the driver is powered and not sleeping before trying to read from it
                it.powerMode = Bme680.MODE_FORCED

                val delay = it.profileDuration.toLong()
                Timber.d("oneShotReadValue delay for measurement to complete:$delay")
                delay(it.profileDuration.toLong())

                unitValue = Bme680Data(0, 0f, 0, it.readTemperature(), it.readHumidity(), 0f, 0f, 0f, 0f, 0, 0f, 0f, 0f)
                it.powerMode = Bme680.MODE_SLEEP
                valueUpdateTime = System.currentTimeMillis()
                Timber.d("temperatureAndHumidity:$unitValue")
            }
        }
    }

    @Throws(Exception::class)
    override suspend fun readValue(): Bme680Data? {
        // We do not want to block I2C buss so open device to only display some data and then immediately close it.
        // use block automatically closes resources referenced to tmp102
        withContext(Dispatchers.Main) {
            Bme680(pinName, ALTERNATIVE_I2C_ADDRESS).use {
                // Configure driver oversampling for temperature, humidity or pressure,
                // threshold filter or gas status settings according to your use case
                it.temperatureOversample = Bme680.OVERSAMPLING_1X
                it.humidityOversample = Bme680.OVERSAMPLING_1X
                // Ensure the driver is powered and not sleeping before trying to read from it
                it.powerMode = Bme680.MODE_FORCED

                unitValue = Bme680Data(0, 0f, 0, it.readTemperature(), it.readHumidity(), 0f, 0f, 0f, 0f, 0, 0f, 0f, 0f)
                valueUpdateTime = System.currentTimeMillis()
                it.powerMode = Bme680.MODE_SLEEP
                Timber.d("Bme680Data:$unitValue")
            }
        }
        return unitValue
    }
}
