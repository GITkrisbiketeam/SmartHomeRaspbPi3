package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.smarthomeraspbpi3.common.FULL_DAY_IN_MILLIS
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.Si7021
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.TemperatureAndHumidity
import kotlinx.coroutines.*
import timber.log.Timber

private const val REFRESH_RATE = 300000L // 5 min

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

    private var heatOnCounter = 0
    private var heatOnTrigger = FULL_DAY_IN_MILLIS / (refreshRate?:REFRESH_RATE) // Heat on once per day

    override suspend fun connect() {
        // Do noting we do not want to block I2C device so it will be opened while setting the value
        // and then immediately closed to release resources
    }

    @Throws(Exception::class)
    // TODO use Flow here
    override suspend fun registerListener(scope: CoroutineScope, listener: Sensor.HwUnitListener<TemperatureAndHumidity>,
                                          exceptionHandler: CoroutineExceptionHandler) {
        Timber.d("registerListener")
        job?.cancel()
        job = scope.launch(Dispatchers.IO + exceptionHandler) {
            // We could also check for true as suspending delay() method is cancellable
            while (isActive) {
                delay(refreshRate ?: REFRESH_RATE)
                // Cancel will not stop non suspending oneShotReadValue function
                oneShotReadValue()
                // all data should be updated by suspending oneShotReadValue() method
                listener.onHwUnitChanged(hwUnit, unitValue, valueUpdateTime)
                if (heatOnCounter++ >= heatOnTrigger) {
                    // wait for OneShotReadValue to close Si7021
                    delay(5000)
                    heatOnOff(true)
                    heatOnCounter = 0
                    // heat up for 60 sec
                    delay(60000)
                    heatOnOff(false)
                }

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
            Si7021(pinName).use {
                val (temperature, rh) = it.readOneShotTempAndRh()
                unitValue = TemperatureAndHumidity(temperature, rh)
                valueUpdateTime = System.currentTimeMillis()
                Timber.d("temperatureAndHumidity:$unitValue")
            }
        }
    }

    @Throws(Exception::class)
    private suspend fun heatOnOff(on:Boolean) {
        // We do not want to block I2C buss so open device to only display some data and then immediately close it.
        // use block automatically closes resources referenced to mcp9808
        Timber.d("heatOn:$on")
        withContext(Dispatchers.Main) {
            Si7021(pinName).use {
                if (on) {
                    it.heater = Si7021.HeaterAmount.HEATER_15_MA
                } else {
                    it.heater = Si7021.HeaterAmount.HEATER_OFF
                }
            }
        }
    }

    @Throws(Exception::class)
    override suspend fun readValue(): TemperatureAndHumidity? {
        // We do not want to block I2C buss so open device to only display some data and then immediately close it.
        // use block automatically closes resources referenced to tmp102
        withContext(Dispatchers.Main) {
            Si7021(pinName).use {
                // TODO do not use callback method here as it will hold I2C buss
                unitValue = TemperatureAndHumidity(it.readPrevTemperature(), null)
                valueUpdateTime = System.currentTimeMillis()
                Timber.d("temperatureAndHumidity:$unitValue")
            }
        }
        return unitValue
    }
}
