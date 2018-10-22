package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.TMP102
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import timber.log.Timber
import java.util.*

private const val REFRESH_RATE = 300000L // 5 min

class HwUnitI2CTempTMP102Sensor(name: String,
                                location: String,
                                pinName: String,
                                softAddress: Int,
                                override var device: AutoCloseable? = null) : HwUnitI2C<Float>, Sensor<Float> {

    override val hwUnit: HwUnit = HwUnit(name, location, BoardConfig. TEMP_SENSOR_TMP102, pinName, ConnectionType.I2C, softAddress)
    override var unitValue: Float? = null
    override var valueUpdateTime: String = ""

    private var job: Job? = null
    private var hwUnitListener: Sensor.HwUnitListener<Float>? = null

    override fun connect() {
        // Do noting we o not want to block I2C device so it will be opened while setting the value
        // and then immediately closed to release resources
    }


    override fun registerListener(listener: Sensor.HwUnitListener<Float>) {
        Timber.d("registerListener")
        hwUnitListener = listener
        job?.cancel()
        job = launch(CommonPool) {
            // We could also check for true as suspending delay() method is cancellable
            while (isActive) {
                // Cancel will not stop non suspending oneShotReadValue function
                oneShotReadValue()
                delay(REFRESH_RATE)
            }
        }
    }

    override fun unregisterListener() {
        Timber.d("unregisterListener")
        job?.cancel()
        hwUnitListener = null
    }

    private fun oneShotReadValue() {
        val tmp102 = TMP102(hwUnit.pinName)
        // We do not want to block I2C buss so open device to only display some data and then immediately close it.
        // use block automatically closes resources referenced to tmp102
        tmp102.shutdownMode = true
        tmp102.readOneShotTemperature {
            unitValue = it
            valueUpdateTime = Date().toString()
            Timber.d("temperature:$unitValue")
            hwUnitListener?.onUnitChanged(hwUnit, unitValue, valueUpdateTime)
            tmp102.close()
        }
    }

    override fun readValue(): Float? {
        // We do not want to block I2C buss so open device to only display some data and then immediately close it.
        // use block automatically closes resources referenced to tmp102
        val tmp102 = TMP102(hwUnit.pinName)
        tmp102.use {
            unitValue = it.readTemperature()
            valueUpdateTime = Date().toString()
            Timber.d("temperature:$unitValue")
        }

        return unitValue
    }
}
