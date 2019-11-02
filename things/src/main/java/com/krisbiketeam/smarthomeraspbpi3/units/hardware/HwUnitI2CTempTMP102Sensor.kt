package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.TMP102
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnitLog
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.Exception
import java.util.*

private const val REFRESH_RATE = 300000L // 5 min

class HwUnitI2CTempTMP102Sensor(name: String,
                                location: String,
                                private val pinName: String,
                                private val softAddress: Int,
                                private val refreshRate: Long? = REFRESH_RATE,
                                override var device: AutoCloseable? = null) : HwUnitI2C<Float>, Sensor<Float> {

    override val hwUnit: HwUnit = HwUnit(name, location, BoardConfig.TEMP_SENSOR_TMP102, pinName, ConnectionType.I2C, softAddress, refreshRate = refreshRate)
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
        job = GlobalScope.launch {
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

    override fun close() {
        Timber.d("close")
        job?.cancel()
        super.close()
    }

    private fun oneShotReadValue() {
        try {
            // We do not want to block I2C buss so open device to only display some data and then immediately close it.
            // use block automatically closes resources referenced to tmp102
            val tmp102 = TMP102(pinName, softAddress)
            tmp102.shutdownMode = true
            tmp102.readOneShotTemperature { value ->
                unitValue = value
                valueUpdateTime = Date().toString()
                Timber.d("temperature:$unitValue")
                hwUnitListener?.onUnitChanged(hwUnit, unitValue, valueUpdateTime)
                tmp102.close()
            }
        } catch (e: Exception){
            FirebaseHomeInformationRepository.addHwUnitErrorEvent(HwUnitLog(hwUnit, unitValue, e.message, Date().toString()))
            Timber.e(e,"Error oneShotReadValue HwUnitI2CTempTMP102Sensor on: $hwUnit")
            unitValue = Float.MAX_VALUE
        }
    }

    override fun readValue(): Float? {
        try {
            // We do not want to block I2C buss so open device to only display some data and then immediately close it.
            // use block automatically closes resources referenced to tmp102
            val tmp102 = TMP102(pinName, softAddress)
            tmp102.use {
                unitValue = it.readTemperature()
                valueUpdateTime = Date().toString()
                Timber.d("temperature:$unitValue")
            }
        } catch (e: Exception){
            FirebaseHomeInformationRepository.addHwUnitErrorEvent(HwUnitLog(hwUnit, unitValue, e.message, Date().toString()))
            Timber.e(e,"Error readValue HwUnitI2CTempTMP102Sensor on: $hwUnit")
            return Float.MAX_VALUE
        }
        return unitValue
    }
}
