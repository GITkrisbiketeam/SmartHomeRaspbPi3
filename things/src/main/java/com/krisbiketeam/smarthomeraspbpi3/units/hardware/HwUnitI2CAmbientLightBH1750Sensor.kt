package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.BH1750
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import kotlinx.coroutines.*
import timber.log.Timber

private const val REFRESH_RATE = 300000L // 5 min

class HwUnitI2CAmbientLightBH1750Sensor(name: String, location: String, private val pinName: String,
                                        softAddress: Int, private val refreshRate: Long? = REFRESH_RATE,
                                        override var device: AutoCloseable? = null) :
        HwUnitI2C<Float>, Sensor<Float> {

    override val hwUnit: HwUnit =
            HwUnit(name, location, BoardConfig.LIGHT_SENSOR_BH1750, pinName, ConnectionType.I2C,
                    softAddress, refreshRate = refreshRate)
    override var unitValue: Float? = null
    override var valueUpdateTime: Long = System.currentTimeMillis()

    private var job: Job? = null

    override suspend fun connect() {
        // Do noting we do not want to block I2C device so it will be opened while setting the value
        // and then immediately closed to release resources
    }

    @Throws(Exception::class)
    // TODO use Flow here
    override suspend fun registerListener(scope: CoroutineScope, listener: Sensor.HwUnitListener<Float>,
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
            BH1750(pinName).use {
                unitValue = it.readOneShotLight()
                valueUpdateTime = System.currentTimeMillis()
                Timber.d("light:$unitValue")
            }
        }
    }

    @Throws(Exception::class)
    override suspend fun readValue(): Float? {
        // We do not want to block I2C buss so open device to only display some data and then immediately close it.
        // use block automatically closes resources referenced to tmp102
        withContext(Dispatchers.Main) {
            BH1750(pinName).use {
                unitValue = it.readLight()
                valueUpdateTime = System.currentTimeMillis()
                Timber.d("Light:$unitValue")
            }
        }
        return unitValue
    }
}
