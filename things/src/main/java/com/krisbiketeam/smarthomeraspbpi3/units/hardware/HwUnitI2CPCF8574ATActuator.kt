package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.PCF8574AT
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.PCF8574ATPin.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import timber.log.Timber

class HwUnitI2CPCF8574ATActuator(name: String, location: String, private val pinName: String,
                                 private val address: Int, private val pinInterrupt: String,
                                 private val ioPin: Pin,
                                 override var device: AutoCloseable? = null) : HwUnitI2C<Boolean>,
        Actuator<Boolean> {

    override val hwUnit: HwUnit =
            HwUnit(name, location, BoardConfig.IO_EXTENDER_PCF8474AT_OUTPUT, pinName,
                   ConnectionType.I2C, address, pinInterrupt, ioPin.name)
    override var unitValue: Boolean? = null
    override var valueUpdateTime: Long = System.currentTimeMillis()

    @Throws(Exception::class)
    override fun connect() {
        device = HwUnitI2CPCF8574AT.getPcf8574AtInstance(pinName, address).apply {
            intGpio = pinInterrupt
            setMode(ioPin, PinMode.DIGITAL_OUTPUT)
        }
        HwUnitI2CPCF8574AT.increaseUseCount(pinName, address)
    }

    @Throws(Exception::class)
    override fun close() {
        // We do not want to close this device if it is used by another instance of this class
        // decreaseUseCount will close HwUnitI2C when count reaches 0
        val refCount = HwUnitI2CPCF8574AT.decreaseUseCount(pinName, address)
        Timber.d("close refCount:$refCount")
    }

    @Throws(Exception::class)
    override fun setValue(value: Boolean) {
        unitValue = value
        (device as PCF8574AT).setState(ioPin, if (value) PinState.LOW else PinState.HIGH)
        valueUpdateTime = System.currentTimeMillis()
    }
}
