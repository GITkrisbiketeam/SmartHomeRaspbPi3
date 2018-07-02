package com.krisbiketeam.smarthomeraspbpi3.units

import com.krisbiketeam.data.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.driver.TMP102
import com.krisbiketeam.smarthomeraspbpi3.utils.Logger
import com.krisbiketeam.smarthomeraspbpi3.utils.Utils
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch

class HomeUnitI2CTemperatureSensor(name: String,
                                   location: String,
                                   pinName: String,
                                   softAddress: Int,
                                   override var device: AutoCloseable? = null) : HomeUnitI2C<Float>, Sensor<Float> {
    companion object {
        private val TAG = Utils.getLogTag(HomeUnitI2CTemperatureSensor::class.java)
        private const val REFRESH_RATE = 1000L // One second
    }


    override val homeUnit: HomeUnit<Float> = HomeUnit(name, location, pinName, ConnectionType.I2C, softAddress)

    private var job: Job? = null
    private var homeUnitListener: Sensor.HomeUnitListener<Float>? = null

    override fun connect() {
        // Do noting we o not want to block I2C device so it will be opened while setting the value
        // and then immediately closed to release resources
    }


    override fun registerListener(listener: Sensor.HomeUnitListener<Float>) {
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
    override fun readValue(): Float? {
        val temperature : Float? = null
        // We do not want to block I2C buss so open device to only display some data and then immediately close it.
        // use block automatically closes resources referenced to tmp102
        val tmp102 = TMP102(homeUnit.pinName)
        tmp102.use {
            val temperature = tmp102.readTemperature()
            Logger.d(TAG, "temperature:$temperature")
        }

        return temperature
    }
}
