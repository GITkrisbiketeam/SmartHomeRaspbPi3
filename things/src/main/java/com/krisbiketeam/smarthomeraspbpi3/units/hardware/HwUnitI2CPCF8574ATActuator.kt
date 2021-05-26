package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.PCF8574AT
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.PCF8574ATPin.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class HwUnitI2CPCF8574ATActuator(name: String, location: String, private val pinName: String,
                                 private val address: Int, private val ioPin: Pin,
                                 override var device: AutoCloseable? = null) : HwUnitI2C<Boolean>,
        Actuator<Boolean> {

    override val hwUnit: HwUnit =
            HwUnit(name, location, BoardConfig.IO_EXTENDER_PCF8474AT_OUTPUT, pinName,
                    ConnectionType.I2C, address, null, ioPin.name)
    override var unitValue: Boolean? = null
    override var valueUpdateTime: Long = System.currentTimeMillis()

    @Throws(Exception::class)
    override suspend fun connect() {
        withContext(Dispatchers.Main) {
            device = HwUnitI2CPCF8574AT.getPcf8574AtInstance(pinName, address).apply {
                setMode(ioPin, PinMode.DIGITAL_OUTPUT)
            }
            HwUnitI2CPCF8574AT.increaseUseCount(pinName, address)
        }
    }

    @Throws(Exception::class)
    override suspend fun close() {
        // We do not want to close this device if it is used by another instance of this class
        // decreaseUseCount will close HwUnitI2C when count reaches 0
        withContext(Dispatchers.Main) {
            val refCount = HwUnitI2CPCF8574AT.decreaseUseCount(pinName, address)
            Timber.d("close refCount:$refCount")
            // this will nullify HwUnitI2C#device instance
            if (refCount == 0) {
                super.close()
            }
        }
    }

    @Throws(Exception::class)
    override suspend fun setValue(value: Boolean) {
        withContext(Dispatchers.Main) {
            unitValue = value
            (device as PCF8574AT).setState(ioPin, if (value) PinState.LOW else PinState.HIGH)
            valueUpdateTime = System.currentTimeMillis()
        }
    }
}
