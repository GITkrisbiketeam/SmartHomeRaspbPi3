package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.data.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.driver.TMP102
import com.krisbiketeam.data.storage.dto.HomeUnitLog
import com.krisbiketeam.smarthomeraspbpi3.units.HomeUnitI2C
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import com.krisbiketeam.smarthomeraspbpi3.utils.Logger
import com.krisbiketeam.smarthomeraspbpi3.utils.Utils
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import java.util.*

class HomeUnitI2CTempTMP102Sensor(name: String,
                                  location: String,
                                  pinName: String,
                                  softAddress: Int,
                                  override var device: AutoCloseable? = null) : HomeUnitI2C<Float>, Sensor<Float> {
    companion object {
        private val TAG = Utils.getLogTag(HomeUnitI2CTempTMP102Sensor::class.java)
        private const val REFRESH_RATE = 10000L // ten seconds
    }


    override val homeUnit: HomeUnitLog<Float> = HomeUnitLog(name, location, pinName, ConnectionType.I2C, softAddress)

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
        // We do not want to block I2C buss so open device to only display some data and then immediately close it.
        // use block automatically closes resources referenced to tmp102
        val tmp102 = TMP102(homeUnit.pinName)
        tmp102.use {
            homeUnit.value = it.readTemperature()
            homeUnit.localtime = Date().toString()
            Logger.d(TAG, "temperature:${homeUnit.value}")
        }

        return homeUnit.value
    }
}
