package com.krisbiketeam.smarthomeraspbpi3.hardware

import com.google.android.things.contrib.driver.bmx280.Bmx280
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch

class HatTemperatureAndPressureSensor : Sensor<TemperatureAndPressure> {
    companion object {
        private const val REFRESH_RATE = 1000L // One second
    }

    private var job: Job? = null
    private var listener: Sensor.OnStateChangeListener<TemperatureAndPressure>? = null

    init {
        // No initialization required
    }

    override fun start(onStateChangeListener: Sensor.OnStateChangeListener<TemperatureAndPressure>) {
        this.listener = onStateChangeListener
        job?.cancel()
        listener?.let { startJob() }
    }

    override fun stop() {
        job?.cancel()
        listener = null
    }

    private fun startJob() {
        job = launch(CommonPool) {
            while (true) {
                // We do not want to block I2C buss so open device to only get some data and then immediately close it.
                val sensor: Bmx280 = RainbowHat.openSensor().apply {
                    temperatureOversampling = Bmx280.OVERSAMPLING_1X
                    pressureOversampling = Bmx280.OVERSAMPLING_1X
                    setMode(Bmx280.MODE_NORMAL)
                }
                val temperature = sensor.readTemperature()
                val pressure = sensor.readPressure()

                sensor.close()

                listener?.onStateChanged(TemperatureAndPressure(temperature, pressure))
                Thread.sleep(REFRESH_RATE)
            }
        }
    }
}

data class TemperatureAndPressure(val temperature: Float,
                                  val pressure: Float)
