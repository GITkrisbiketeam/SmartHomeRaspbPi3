package com.krisbiketeam.smarthomeraspbpi3.hardware

import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay
import com.google.android.things.contrib.driver.ht16k33.Ht16k33
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat

class FourCharDisplay : Actuator<String> {
    override fun start() {
    }

    override fun stop() {
    }

    override fun setState(state: String) {
        // We do not want to block I2C buss so open device to only display some data and then immediately close it.
        val display: AlphanumericDisplay = RainbowHat.openDisplay().apply {
            setEnabled(false)
            setBrightness(Ht16k33.HT16K33_BRIGHTNESS_MAX)
        }
        when {
            state.isNotEmpty() -> {
                display.display(state)
                display.setEnabled(true)
            }
            else -> display.setEnabled(false)
        }
        display.close()
    }
}
