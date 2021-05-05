package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import android.view.ViewConfiguration
import androidx.annotation.VisibleForTesting
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.GpioCallback
import kotlinx.coroutines.*
import timber.log.Timber

class HwUnitGpioNoiseSensor(name: String, location: String, pinName: String, activeType: Int,
                            gpio: Gpio? = null) :
        HwUnitGpioSensor(name, location, pinName, activeType, gpio) {

    private var mPendingCheckDebounce: Job? = null

    /**
     * Set the time delay after an edge trigger that the button
     * must remain stable before generating an event. Debounce
     * is enabled by default for 100ms.
     *
     * Setting this value to zero disables debounce and triggers
     * events on all edges immediately.
     */
    @get:VisibleForTesting
    internal// Clear any pending events
    var debounceDelay = ViewConfiguration.getTapTimeout().toLong()
        set(delay) {
            require(delay >= 0) { "Debounce delay cannot be negative." }
            removeDebounceCallback()
            field = delay
        }

    override val mGpioCallback = object : GpioCallback {
        override fun onGpioEdge(gpio: Gpio): Boolean {
            val value = readValue(gpio)
            Timber.v("onGpioEdge gpio.readValue(): $value on: $hwUnit")

            if (debounceDelay == 0L) {
                // Trigger event immediately
                performSensorEvent(value)
            } else {
                // Clear any pending checks
                removeDebounceCallback()
                // Set a new pending check
                mPendingCheckDebounce = CoroutineScope(Dispatchers.IO).launch {
                    delay(debounceDelay)
                    try {
                        if (readValue() == value) {
                            performSensorEvent(value)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error readValue on $hwUnit")
                    }
                }
            }
            // Continue listening for more interrupts
            return true
        }

        override fun onGpioError(gpio: Gpio?, error: Int) {
            Timber.w("${gpio.toString()} : Error event $error on: $hwUnit")
        }
    }

    @Throws(Exception::class)
    override suspend fun close() {
        removeDebounceCallback()
        super.close()
    }

    /**
     * Invoke button event callback
     */
    @VisibleForTesting
    internal fun performSensorEvent(event: Boolean?) {
        unitValue = event
        valueUpdateTime = System.currentTimeMillis()
        Timber.d("performSensorEvent event: $event on: $hwUnit")
        hwUnitListener?.onHwUnitChanged(hwUnit, unitValue, valueUpdateTime) ?: Timber.w(
                "listener not registered on: $hwUnit")

    }

    /**
     * Clear pending debounce check
     */
    private fun removeDebounceCallback() {
        mPendingCheckDebounce?.cancel()
        mPendingCheckDebounce = null
    }

}
