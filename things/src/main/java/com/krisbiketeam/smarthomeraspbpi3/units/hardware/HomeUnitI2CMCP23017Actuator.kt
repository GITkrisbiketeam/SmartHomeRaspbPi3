package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.data.storage.ConnectionType
import com.krisbiketeam.data.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.driver.MCP23017
import com.krisbiketeam.smarthomeraspbpi3.driver.MCP23017Pin.*
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.HomeUnitI2C
import timber.log.Timber
import java.util.*

class HomeUnitI2CMCP23017Actuator(name: String,
                                  location: String,
                                  private val pinName: String,
                                  private val address: Int,
                                  private val pinInterrupt: String,
                                  private val ioPin: Pin,
                                  override var device: AutoCloseable? = null) : HomeUnitI2C<Boolean>, Actuator<Boolean> {

    override val homeUnit: HomeUnit = HomeUnit(name, location, pinName, ConnectionType.I2C, address, pinInterrupt, ioPin.name)
    override var unitValue: Boolean? = null
    override var valueUpdateTime: String = ""

    override fun connect() {
        device = HomeUnitI2CMCP23017.getMcp23017Instance(pinName, address)
        (device as MCP23017).run {
            intGpio = pinInterrupt
            setMode(ioPin, PinMode.DIGITAL_OUTPUT)
        }
        HomeUnitI2CMCP23017.increaseUseCount(pinName, address)
    }

    override fun close() {
        // We do not want to close this device if it is used by another instance of this class
        val refCount = HomeUnitI2CMCP23017.decreaseUseCount(pinName, address)
        if (refCount == 0) {
            super.close()
        }
    }

    override fun setValue(value: Boolean?) {
        if (value is Boolean) {
            unitValue = value
            (device as MCP23017).setState(ioPin,
                    if (value) PinState.HIGH else PinState.LOW)
        } else {
            Timber.w("setValue value not instance of Boolean $value")
            unitValue = null
        }
        valueUpdateTime = Date().toString()
    }
}
