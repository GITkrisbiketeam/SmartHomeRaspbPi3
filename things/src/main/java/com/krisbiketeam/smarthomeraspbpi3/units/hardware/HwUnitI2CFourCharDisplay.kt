package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay
import com.google.android.things.contrib.driver.ht16k33.Ht16k33
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import java.util.*

class HwUnitI2CFourCharDisplay(name: String,
                               location: String,
                               pinName: String,
                               override var device: AutoCloseable? = null) : HwUnitI2C<String>, Actuator<String> {

    override fun connect() {
        // Do noting we o not want to block I2C device so it will be opened while setting the value
        // and then immediately closed to release resources
    }

    override val hwUnit: HwUnit = HwUnit(name, location, BoardConfig.FOUR_CHAR_DISP, pinName, ConnectionType.I2C, AlphanumericDisplay.I2C_ADDRESS)
    override var unitValue: String? = null
    override var valueUpdateTime: String = ""

    override fun setValue(value: String?) {
        // We do not want to block I2C buss so open device to only display some data and then immediately close it.
        val display: AlphanumericDisplay = RainbowHat.openDisplay().apply {
            setEnabled(false)
            setBrightness(Ht16k33.HT16K33_BRIGHTNESS_MAX)
        }
        unitValue = value
        if (value?.isNotEmpty() == true) {
            display.display(value)
            display.setEnabled(true)
        } else {
            display.setEnabled(false)
        }
        valueUpdateTime = Date().toString()
        display.close()
    }
}