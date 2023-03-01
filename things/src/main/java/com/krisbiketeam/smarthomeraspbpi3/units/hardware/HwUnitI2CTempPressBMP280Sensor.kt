package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.google.android.things.contrib.driver.bmx280.Bmx280
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.PressureAndTemperature
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import kotlinx.coroutines.*
import timber.log.Timber

private const val REFRESH_RATE = 300000L // 5 min

class HwUnitI2CTempPressBMP280Sensor(name: String, location: String, pinName: String,
                                     softAddress: Int,
                                     private val refreshRate: Long? = REFRESH_RATE,
                                     override var device: AutoCloseable? = null) :
        HwUnitI2C<PressureAndTemperature>, Sensor<PressureAndTemperature> {

    override val hwUnit: HwUnit =
            HwUnit(name, location, BoardConfig.TEMP_PRESS_SENSOR_BMP280, pinName,
                   ConnectionType.I2C, softAddress, refreshRate = refreshRate)
    override var unitValue: PressureAndTemperature? = null
    override var valueUpdateTime: Long = System.currentTimeMillis()

    private var job: Job? = null
    private var hwUnitListener: Sensor.HwUnitListener<PressureAndTemperature>? = null

    override suspend fun connect() {
        // Do noting we o not want to block I2C device so it will be opened while setting the value
        // and then immediately closed to release resources
    }

    @Throws(Exception::class)
    // TODO use Flow here
    override suspend fun registerListener(listener: Sensor.HwUnitListener<PressureAndTemperature>,
                                          exceptionHandler: CoroutineExceptionHandler) {
        Timber.d("registerListener")
        hwUnitListener = listener
        job?.cancel()
        job = supervisorScope {
            launch(Dispatchers.IO + exceptionHandler) {
                // We could also check for true as suspending delay() method is cancellable
                while (isActive) {
                    try {
                        delay(refreshRate ?: REFRESH_RATE)
                        readValue()
                        hwUnitListener?.onHwUnitChanged(hwUnit, unitValue, valueUpdateTime)
                    } catch (e: Exception) {
                        Timber.e(e, "Error readValue on $hwUnit")
                    }
                }
            }
        }
    }

    override suspend fun unregisterListener() {
        Timber.d("unregisterListener")
        job?.cancel()
        hwUnitListener = null
    }

    @Throws(Exception::class)
    override suspend fun close() {
        job?.cancel()
        super.close()
    }

    @Throws(Exception::class)
    override suspend fun readValue(): PressureAndTemperature? {
        return withContext(Dispatchers.Main) {
            // We do not want to block I2C buss so open device to only display some data and then immediately close it.
            // use block automatically closes resources referenced to tmp102
            // We do not want to block I2C buss so open device to only get some data and then immediately close it.
            RainbowHat.openSensor().use {
                it.temperatureOversampling = Bmx280.OVERSAMPLING_1X
                it.pressureOversampling = Bmx280.OVERSAMPLING_1X
                it.setMode(Bmx280.MODE_NORMAL)
                unitValue = PressureAndTemperature(it.readPressure(), it.readTemperature())
                valueUpdateTime = System.currentTimeMillis()
                Timber.d("temperature:$unitValue")
            }
            unitValue
        }
    }
}
