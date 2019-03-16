package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.PCF8574AT
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.PCF8574ATPin.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnitLog
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import timber.log.Timber
import java.util.*

class HwUnitI2CPCF8574ATActuator(name: String,
                                 location: String,
                                 private val pinName: String,
                                 private val address: Int,
                                 private val pinInterrupt: String,
                                 private val ioPin: Pin,
                                 override var device: AutoCloseable? = null) : HwUnitI2C<Boolean>, Actuator<Boolean> {

    override val hwUnit: HwUnit = HwUnit(name, location, BoardConfig.IO_EXTENDER_PCF8474AT_OUTPUT, pinName, ConnectionType.I2C, address, pinInterrupt, ioPin.name)
    override var unitValue: Boolean? = null
    override var valueUpdateTime: String = ""

    override fun connect() {
        try {
            device = HwUnitI2CPCF8574AT.getPcf8574AtInstance(pinName, address)
            (device as PCF8574AT).run {
                intGpio = pinInterrupt
                setMode(ioPin, PinMode.DIGITAL_OUTPUT)
            }
            HwUnitI2CPCF8574AT.increaseUseCount(pinName, address)
        } catch (e: Exception) {
            FirebaseHomeInformationRepository.hwUnitErrorEvent(HwUnitLog(hwUnit, unitValue, Date().toString().plus(e.message)))
            Timber.e(e, "Error connect HwUnitI2CPCF8574ATActuator")
        }
    }

    override fun close() {
        // We do not want to close this device if it is used by another instance of this class
        val refCount = HwUnitI2CPCF8574AT.decreaseUseCount(pinName, address)
        if (refCount == 0) {
            try {
                super.close()
            } catch (e: Exception) {
                FirebaseHomeInformationRepository.hwUnitErrorEvent(HwUnitLog(hwUnit, unitValue, Date().toString().plus(e.message)))
                Timber.e(e, "Error close HwUnitI2CPCF8574ATActuator")
            }
        }
    }

    override fun setValue(value: Boolean?) {
        if (value is Boolean) {
            unitValue = value
            try{
                (device as PCF8574AT).setState(ioPin,
                    if (value) PinState.LOW else PinState.HIGH)
            } catch(e: Exception){
                FirebaseHomeInformationRepository.hwUnitErrorEvent(HwUnitLog(hwUnit, unitValue, Date().toString().plus(e.message)))
                Timber.e(e, "Error setValue HwUnitI2CPCF8574ATActuator")
            }
        } else {
            Timber.w("setValue value not instance of Boolean $value")
            unitValue = null
        }
        valueUpdateTime = Date().toString()
    }
}
