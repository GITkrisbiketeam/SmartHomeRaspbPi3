package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017Pin.*
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import timber.log.Timber
import java.util.*

open class HwUnitI2CMCP23017Sensor(name: String,
                                   location: String,
                                   private val pinName: String,
                                   private val address: Int,
                                   private val pinInterrupt: String,
                                   private val ioPin: Pin,
                                   private val internalPullUp: Boolean = false,
                                   override var device: AutoCloseable? = null) : HwUnitI2C<Boolean>, Sensor<Boolean> {

    override val hwUnit: HwUnit = HwUnit(name, location, BoardConfig.IO_EXTENDER_MCP23017_INPUT, pinName, ConnectionType.I2C, address, pinInterrupt, ioPin.name, internalPullUp)
    override var unitValue: Boolean? = null
    override var valueUpdateTime: String = ""

    var hwUnitListener: Sensor.HwUnitListener<Boolean>? = null

    open val mMCP23017Callback = object : MCP23017PinStateChangeListener {
        override fun onPinStateChanged(pin: Pin, state: PinState) {
            Timber.d("onPinStateChanged pin: ${pin.name} state: $state")
            unitValue = state == PinState.HIGH
            valueUpdateTime = Date().toString()
            hwUnitListener?.onUnitChanged(hwUnit, unitValue, valueUpdateTime)

        }
    }

    override fun connect() {
        device = HwUnitI2CMCP23017.getMcp23017Instance(pinName, address)
        (device as MCP23017).run {
            intGpio = pinInterrupt
            setPullResistance(ioPin,
                    if (internalPullUp) PinPullResistance.PULL_UP else PinPullResistance.OFF)
            setMode(ioPin, PinMode.DIGITAL_INPUT)
        }
        HwUnitI2CMCP23017.increaseUseCount(pinName, address)
    }

    override fun close() {
        unregisterListener()
        // We do not want to close this device if it is used by another instance of this class
        val refCount = HwUnitI2CMCP23017.decreaseUseCount(pinName, address)
        if (refCount == 0) {
            super.close()
        }
    }

    override fun registerListener(listener: Sensor.HwUnitListener<Boolean>) {
        Timber.d("registerListener")
        hwUnitListener = listener
        (device as MCP23017).run {
            val result = registerPinListener(ioPin, mMCP23017Callback)
            Timber.d("registerListener registerPinListener?: $result")
        }
    }

    override fun unregisterListener() {
        Timber.d("unregisterListener")
        (device as MCP23017).run {
            val result = unRegisterPinListener(ioPin, mMCP23017Callback)
            Timber.d("registerListener unRegisterPinListener?: $result")
        }
        hwUnitListener = null
    }

    override fun readValue(): Boolean? {
        unitValue = (device as MCP23017).run {
            getState(ioPin) == PinState.HIGH
        }

        return unitValue
    }
}
