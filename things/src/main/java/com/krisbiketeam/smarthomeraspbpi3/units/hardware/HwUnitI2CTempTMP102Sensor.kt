package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.TMP102
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import kotlinx.coroutines.*
import timber.log.Timber

private const val REFRESH_RATE = 300000L // 5 min

class HwUnitI2CTempTMP102Sensor(name: String, location: String, private val pinName: String,
                                private val softAddress: Int,
                                private val refreshRate: Long? = REFRESH_RATE,
                                override var device: AutoCloseable? = null) : HwUnitI2C<Float>,
        Sensor<Float> {

    override val hwUnit: HwUnit =
            HwUnit(name, location, BoardConfig.TEMP_SENSOR_TMP102, pinName, ConnectionType.I2C,
                   softAddress, refreshRate = refreshRate)
    override var unitValue: Float? = null
    override var valueUpdateTime: Long = System.currentTimeMillis()

    private var job: Job? = null
    private var hwUnitListener: Sensor.HwUnitListener<Float>? = null

    override fun connect() {
        // Do noting we o not want to block I2C device so it will be opened while setting the value
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
                delay(refreshRate ?: REFRESH_RATE)
                // Cancel will not stop non suspending oneShotReadValue function
                oneShotReadValue()
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
        // use block automatically closes resources referenced to tmp102
        val tmp102 = TMP102(pinName, softAddress)
        tmp102.shutdownMode = true
        tmp102.readOneShotTemperature { value ->
            unitValue = value
            valueUpdateTime = System.currentTimeMillis()
            Timber.d("temperature:$unitValue")
            hwUnitListener?.onHwUnitChanged(hwUnit, unitValue, valueUpdateTime)
            tmp102.close()
        }
    }

    @Throws(Exception::class)
    override fun readValue(): Float? {
        // We do not want to block I2C buss so open device to only display some data and then immediately close it.
        // use block automatically closes resources referenced to tmp102
        val tmp102 = TMP102(pinName, softAddress)
        tmp102.use {
            unitValue = it.readTemperature()
            valueUpdateTime = System.currentTimeMillis()
            Timber.d("temperature:$unitValue")
        }
        return unitValue
    }
}
