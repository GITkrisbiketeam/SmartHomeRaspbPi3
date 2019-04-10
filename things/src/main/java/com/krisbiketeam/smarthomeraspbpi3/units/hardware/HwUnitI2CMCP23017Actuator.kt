package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017Pin.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnitLog
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import timber.log.Timber
import java.util.*

class HwUnitI2CMCP23017Actuator(name: String,
                                location: String,
                                private val pinName: String,
                                private val address: Int,
                                private val pinInterrupt: String,
                                private val ioPin: Pin,
                                override var device: AutoCloseable? = null) : HwUnitI2C<Boolean>, Actuator<Boolean> {

    override val hwUnit: HwUnit = HwUnit(name, location, BoardConfig.IO_EXTENDER_MCP23017_OUTPUT, pinName, ConnectionType.I2C, address, pinInterrupt, ioPin.name)
    override var unitValue: Boolean? = null
    override var valueUpdateTime: String = ""

    override fun connect() {
        try {
            device = HwUnitI2CMCP23017.getMcp23017Instance(pinName, address).apply {
                intGpio = pinInterrupt
                setMode(ioPin, PinMode.DIGITAL_OUTPUT)
            }
            HwUnitI2CMCP23017.increaseUseCount(pinName, address)
        } catch (e: Exception) {
            FirebaseHomeInformationRepository.hwUnitErrorEvent(HwUnitLog(hwUnit, unitValue, Date().toString().plus(e.message)))
            Timber.e(e, "Error connect HwUnitI2CMCP23017Actuator")
        }
    }

    override fun close() {
        // We do not want to close this device if it is used by another instance of this class
        // decreaseUseCount will close HwUnitI2C when count reaches 0
        try {
           HwUnitI2CMCP23017.decreaseUseCount(pinName, address)
        } catch (e: Exception) {
            FirebaseHomeInformationRepository.hwUnitErrorEvent(HwUnitLog(hwUnit, unitValue, Date().toString().plus(e.message)))
            Timber.e(e, "Error close HwUnitI2CMCP23017Actuator")
        }
    }

    override fun setValue(value: Boolean?) {
        if (value is Boolean) {
            unitValue = value
            try{
                (device as MCP23017).setState(ioPin,
                    if (value) PinState.HIGH else PinState.LOW)
            } catch(e: Exception){
                FirebaseHomeInformationRepository.hwUnitErrorEvent(HwUnitLog(hwUnit, unitValue, Date().toString().plus(e.message)))
                Timber.e(e, "Error setValue HwUnitI2CMCP23017Actuator")
            }
        } else {
            Timber.w("setValue value not instance of Boolean $value")
            unitValue = null
        }
        valueUpdateTime = Date().toString()
    }
}
