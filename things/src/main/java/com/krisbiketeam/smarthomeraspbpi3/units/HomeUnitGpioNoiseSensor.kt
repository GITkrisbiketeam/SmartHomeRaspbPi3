package com.krisbiketeam.smarthomeraspbpi3.units

import android.os.Handler
import android.support.annotation.VisibleForTesting
import android.view.ViewConfiguration
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.GpioCallback
import com.krisbiketeam.smarthomeraspbpi3.utils.Logger
import com.krisbiketeam.smarthomeraspbpi3.utils.Utils

class HomeUnitGpioNoiseSensor(homeUnit: HomeUnit, activeType: Int, gpio: Gpio?) : HomeUnitGpioSensor(homeUnit, activeType, gpio) {

    companion object {
        private val TAG = Utils.getLogTag(HomeUnitGpioNoiseSensor::class.java)
    }

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
            if (delay < 0) {
                throw IllegalArgumentException("Debounce delay cannot be negative.")
            }
            removeDebounceCallback()
            field = delay
        }

    override val mGpioCallback = object : GpioCallback {
        override fun onGpioEdge(gpio: Gpio): Boolean {
            val value = readValue(gpio)
            Logger.v(TAG, "onGpioEdge gpio.readValue(): $value on: $homeUnit")

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
            Logger.w(TAG, gpio.toString() + ": Error event $error on: $homeUnit")
        }
    }

    override fun close() {
        removeDebounceCallback()
        super.close()
    }

    /**
     * Invoke button event callback
     */
    @VisibleForTesting
    internal fun performSensorEvent(event: Any?) {
        homeUnit.value = event
        Logger.d(TAG, "performSensorEvent event: $event on: $homeUnit")
        homeUnitListener?.onUnitChanged(homeUnit, event)
                ?: Logger.w(TAG, "listener not registered on: $homeUnit")

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
            if (readValue() == mTriggerState) {
                performSensorEvent(mTriggerState)
            }
            removeDebounceCallback()
        }
    }
}
