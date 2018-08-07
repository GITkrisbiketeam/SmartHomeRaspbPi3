package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay
import com.google.android.things.contrib.driver.ht16k33.Ht16k33
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat
import com.krisbiketeam.data.storage.ConnectionType
import com.krisbiketeam.data.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.HomeUnitI2C
import java.util.*

class HomeUnitI2CFourCharDisplay(name: String,
                                 location: String,
                                 pinName: String,
                                 override var device: AutoCloseable? = null) : HomeUnitI2C<String>, Actuator<String> {

    override fun connect() {
        // Do noting we o not want to block I2C device so it will be opened while setting the value
        // and then immediately closed to release resources
    }

    override val homeUnit: HomeUnit = HomeUnit(name, location, pinName, ConnectionType.I2C, AlphanumericDisplay.I2C_ADDRESS)
    override var unitValue: String? = null
    override var valueUpdateTime: String = ""

    override fun setValue(value: String?) {
        // We do not want to block I2C buss so open device to only display some data and then immediately close it.
        val display: AlphanumericDisplay = RainbowHat.openDisplay().apply {
            setEnabled(false)
            setBrightness(Ht16k33.HT16K33_BRIGHTNESS_MAX)
        }
        unitValue = value
        when {
            value?.isNotEmpty()!! -> {
                display.display(value)
                display.setEnabled(true)
            }
            else -> display.setEnabled(false)
        }
        valueUpdateTime = Date().toString()
        display.close()
    }
}
