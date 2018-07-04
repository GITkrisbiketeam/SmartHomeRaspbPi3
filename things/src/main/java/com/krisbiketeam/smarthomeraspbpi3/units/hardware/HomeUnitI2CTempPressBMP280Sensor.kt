package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.google.android.things.contrib.driver.bmx280.Bmx280
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat
import com.krisbiketeam.data.storage.ConnectionType
import com.krisbiketeam.data.storage.dto.HomeUnitLog
import com.krisbiketeam.smarthomeraspbpi3.units.HomeUnitI2C
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import com.krisbiketeam.smarthomeraspbpi3.utils.Logger
import com.krisbiketeam.smarthomeraspbpi3.utils.Utils
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import java.util.*

data class TemperatureAndPressure(val temperature: Float,
                                  val pressure: Float)

class HomeUnitI2CTempPressBMP280Sensor(name: String,
                                       location: String,
                                       pinName: String,
                                       softAddress: Int,
                                       override var device: AutoCloseable? = null) : HomeUnitI2C<TemperatureAndPressure>, Sensor<TemperatureAndPressure> {
    companion object {
        private val TAG = Utils.getLogTag(HomeUnitI2CTempPressBMP280Sensor::class.java)
        private const val REFRESH_RATE = 10000L // ten seconds
    }


    override val homeUnit: HomeUnitLog<TemperatureAndPressure> = HomeUnitLog(name, location, pinName, ConnectionType.I2C, softAddress)

    private var job: Job? = null
    private var homeUnitListener: Sensor.HomeUnitListener<TemperatureAndPressure>? = null

    override fun connect() {
        // Do noting we o not want to block I2C device so it will be opened while setting the value
        // and then immediately closed to release resources
    }


    override fun registerListener(listener: Sensor.HomeUnitListener<TemperatureAndPressure>) {
        Logger.d(TAG, "registerListener")
        homeUnitListener = listener
        job?.cancel()
        listener.let { startJob() }
    }

    override fun unregisterListener() {
        Logger.d(TAG, "unregisterListener")
        job?.cancel()
        homeUnitListener = null
    }

    private fun startJob() {
        job = launch(CommonPool) {
            while (true) {
                val temperature = readValue()

                homeUnitListener?.onUnitChanged(homeUnit, temperature)
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
            homeUnit.value = TemperatureAndPressure(it.readTemperature(), it.readPressure())
            homeUnit.localtime = Date().toString()
            Logger.d(TAG, "temperature:${homeUnit.value}")
        }
        return homeUnit.value
    }
}
