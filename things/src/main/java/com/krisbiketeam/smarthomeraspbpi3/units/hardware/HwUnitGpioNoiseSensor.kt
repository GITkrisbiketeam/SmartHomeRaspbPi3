package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import android.os.Handler
import android.view.ViewConfiguration
import androidx.annotation.VisibleForTesting
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.GpioCallback
import timber.log.Timber
import java.util.*

class HwUnitGpioNoiseSensor(name: String, location: String, pinName: String, activeType: Int,
                            gpio: Gpio? = null) :
        HwUnitGpioSensor(name, location, pinName, activeType, gpio) {

    private var mDebounceHandler: Handler = Handler()
    private var mPendingCheckDebounce: CheckDebounce? = null

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
                mPendingCheckDebounce = CheckDebounce(value)
                mDebounceHandler.postDelayed(mPendingCheckDebounce, debounceDelay)
            }
            // Continue listening for more interrupts
            return true
        }

        override fun onGpioError(gpio: Gpio?, error: Int) {
            Timber.w("${gpio.toString()} : Error event $error on: $hwUnit")
        }
    }

    @Throws(Exception::class)
    override fun close() {
        removeDebounceCallback()
        super.close()
    }

    /**
     * Invoke button event callback
     */
    @VisibleForTesting
    internal fun performSensorEvent(event: Boolean?) {
        unitValue = event
        valueUpdateTime = Date().toString()
        Timber.d("performSensorEvent event: $event on: $hwUnit")
        hwUnitListener?.onHwUnitChanged(hwUnit, unitValue, valueUpdateTime) ?: Timber.w(
                "listener not registered on: $hwUnit")

    }

    /**
     * Clear pending debounce check
     */
    private fun removeDebounceCallback() {
        if (mPendingCheckDebounce != null) {
            mDebounceHandler.removeCallbacks(mPendingCheckDebounce)
            mPendingCheckDebounce = null
        }
    }

    /**
     * Pending check to delay input events from the initial
     * trigger edge.
     */
    private inner class CheckDebounce(private val mTriggerState: Any?) : Runnable {

        override fun run() {
            // Final check that state hasn't changed
            try {
                if (readValue() == mTriggerState) {
                    performSensorEvent(mTriggerState)
                }
                removeDebounceCallback()
            } catch (e: Exception) {
                Timber.e(e, "Error readValue on $hwUnit")
            }
        }
    }
}
