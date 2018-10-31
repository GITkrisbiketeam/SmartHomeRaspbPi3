package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.google.android.things.contrib.driver.bmx280.Bmx280
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

private const val REFRESH_RATE = 300000L // 5 min

data class TemperatureAndPressure(val temperature: Float, val pressure: Float)

class HwUnitI2CTempPressBMP280Sensor(name: String,
                                     location: String,
                                     pinName: String,
                                     softAddress: Int,
                                     override var device: AutoCloseable? = null) : HwUnitI2C<TemperatureAndPressure>, Sensor<TemperatureAndPressure> {

    override val hwUnit: HwUnit = HwUnit(name, location, BoardConfig.TEMP_PRESS_SENSOR_BMP280, pinName, ConnectionType.I2C, softAddress)
    override var unitValue: TemperatureAndPressure? = null
    override var valueUpdateTime: String = ""

    private var job: Job? = null
    private var hwUnitListener: Sensor.HwUnitListener<TemperatureAndPressure>? = null

    override fun connect() {
        // Do noting we o not want to block I2C device so it will be opened while setting the value
        // and then immediately closed to release resources
    }


    override fun registerListener(listener: Sensor.HwUnitListener<TemperatureAndPressure>) {
        Timber.d("registerListener")
        hwUnitListener = listener
        job?.cancel()
        startJob()
    }

    override fun unregisterListener() {
        Timber.d("unregisterListener")
        job?.cancel()
        hwUnitListener = null
    }

    private fun startJob() {
        job = GlobalScope.launch {
            while (true) {
                readValue()

                hwUnitListener?.onUnitChanged(hwUnit, unitValue, valueUpdateTime)
                Thread.sleep(REFRESH_RATE)
            }
        }
    }

    override fun readValue(): TemperatureAndPressure? {
        // We do not want to block I2C buss so open device to only display some data and then immediately close it.
        // use block automatically closes resources referenced to tmp102
        // We do not want to block I2C buss so open device to only get some data and then immediately close it.
        val bmx280 = RainbowHat.openSensor()
        bmx280.use {
            it.temperatureOversampling = Bmx280.OVERSAMPLING_1X
            it.pressureOversampling = Bmx280.OVERSAMPLING_1X
            it.setMode(Bmx280.MODE_NORMAL)
            unitValue = TemperatureAndPressure(it.readTemperature(), it.readPressure())
            valueUpdateTime = Date().toString()
            Timber.d("temperature:$unitValue")
        }
        return unitValue
    }
}
