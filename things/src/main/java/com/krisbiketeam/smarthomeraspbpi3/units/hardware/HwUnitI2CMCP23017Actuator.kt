package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017Pin.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class HwUnitI2CMCP23017Actuator(name: String, location: String, private val pinName: String,
                                private val address: Int, private val ioPin: Pin,
                                private val inverse: Boolean = false,
                                override var device: AutoCloseable? = null) : HwUnitI2C<Boolean>,
        Actuator<Boolean> {

    override val hwUnit: HwUnit =
            HwUnit(name, location, BoardConfig.IO_EXTENDER_MCP23017_OUTPUT, pinName,
                    ConnectionType.I2C, address, null, ioPin.name, inverse = inverse)
    override var unitValue: Boolean? = null
    override var valueUpdateTime: Long = System.currentTimeMillis()

    @Throws(Exception::class)
    override suspend fun connect() {
        withContext(Dispatchers.Main) {
            device = HwUnitI2CMCP23017.getMcp23017Instance(pinName, address).apply {
                setMode(ioPin, PinMode.DIGITAL_OUTPUT)
            }
            HwUnitI2CMCP23017.increaseUseCount(pinName, address)
        }
    }

    @Throws(Exception::class)
    override suspend fun close() {
        withContext(Dispatchers.Main) {
            // We do not want to close this device if it is used by another instance of this class
            // decreaseUseCount will close HwUnitI2C when count reaches 0
            val refCount = HwUnitI2CMCP23017.decreaseUseCount(pinName, address)
            Timber.d("close refCount:$refCount i2c:$address")
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
            (device as MCP23017?)?.setState(ioPin, if (inverse xor value) PinState.HIGH else PinState.LOW)
            valueUpdateTime = System.currentTimeMillis()
            Timber.v("setValue finished")
        }
    }
}
