package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.Si7021
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import kotlinx.coroutines.*
import timber.log.Timber

private const val REFRESH_RATE = 300000L // 5 min

data class TemperatureAndHumidity(val temperature: Float?, val humidity: Float?)

class HwUnitI2CTempRhSi7021Sensor(name: String, location: String, private val pinName: String,
                                  softAddress: Int, private val refreshRate: Long? = REFRESH_RATE,
                                  override var device: AutoCloseable? = null) :
        HwUnitI2C<TemperatureAndHumidity>, Sensor<TemperatureAndHumidity> {

    override val hwUnit: HwUnit =
            HwUnit(name, location, BoardConfig.TEMP_RH_SENSOR_SI7021, pinName, ConnectionType.I2C,
                   softAddress, refreshRate = refreshRate)
    override var unitValue: TemperatureAndHumidity? = null
    override var valueUpdateTime: Long = System.currentTimeMillis()

    private var job: Job? = null
    private var hwUnitListener: Sensor.HwUnitListener<TemperatureAndHumidity>? = null

    override fun connect() {
        // Do noting we do not want to block I2C device so it will be opened while setting the value
        // and then immediately closed to release resources
    }

    @Throws(Exception::class)
    override fun registerListener(listener: Sensor.HwUnitListener<TemperatureAndHumidity>,
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
        Si7021(pinName).let {
            it.readOneShotRh { rh ->
                unitValue = TemperatureAndHumidity(it.readPrevTemperature(), rh)
                valueUpdateTime = System.currentTimeMillis()
                Timber.d("temperatureAndHumidity:$unitValue")
                hwUnitListener?.onHwUnitChanged(hwUnit, unitValue, valueUpdateTime)
                it.close()
            }
        }
    }

    @Throws(Exception::class)
    override fun readValue(): TemperatureAndHumidity? {
        // We do not want to block I2C buss so open device to only display some data and then immediately close it.
        // use block automatically closes resources referenced to tmp102
        Si7021(pinName).use {
            // TODO do not use callback method here as it will hold I2C buss
            unitValue = TemperatureAndHumidity(it.readPrevTemperature(), null)
            valueUpdateTime = System.currentTimeMillis()
            Timber.d("temperatureAndHumidity:$unitValue")
        }
        return unitValue
    }
}
