package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017Pin.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class HwUnitI2CMCP23017Actuator(
    name: String,
    location: String,
    private val pinName: String,
    private val address: Int,
    private val ioPin: Pin,
    private val inverse: Boolean = false,
    override var device: AutoCloseable? = null
) : HwUnitI2C<Boolean>, Actuator<Boolean> {

    override val hwUnit: HwUnit = HwUnit(
        name,
        location,
        BoardConfig.IO_EXTENDER_MCP23017_OUTPUT,
        pinName,
        ConnectionType.I2C,
        address,
        null,
        ioPin.name,
        inverse = inverse
    )
    override var hwUnitValue: HwUnitValue<Boolean?> = HwUnitValue(null, System.currentTimeMillis())

    override suspend fun connect(): Result<Unit> {
        return withContext(Dispatchers.Main) {
            runCatching {
                device = HwUnitI2CMCP23017.getMcp23017Instance(pinName, address).apply {
                    setMode(ioPin, PinMode.DIGITAL_OUTPUT)
                }
                HwUnitI2CMCP23017.increaseUseCount(pinName, address)
                Unit
            }
        }
    }

    override suspend fun close(): Result<Unit> {
        // We do not want to close this device if it is used by another instance of this class
        // decreaseUseCount will close HwUnitI2C when count reaches 0
        return withContext(Dispatchers.Main) {
            val refCountResult = runCatching {
                HwUnitI2CMCP23017.decreaseUseCount(pinName, address).also {
                    Timber.d("close refCount:$it i2c:$address")
                }
            }
            // this will nullify HwUnitI2C#device instance
            if (refCountResult.getOrNull() == 0) {
                super.close()
            } else {
                refCountResult.map { }
            }
        }
    }

    override suspend fun setValue(value: Boolean): Result<Unit> {
        return withContext(Dispatchers.Main) {
            runCatching {
                (device as MCP23017?)?.setState(
                    ioPin,
                    if (inverse xor value) PinState.HIGH else PinState.LOW
                ) ?: Unit
            }.onSuccess {
                hwUnitValue = HwUnitValue(value, System.currentTimeMillis())
                Timber.v("setValue finished")
            }
        }
    }
}
