package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017Pin.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitValue
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

open class HwUnitI2CMCP23017Sensor(
    name: String,
    location: String,
    private val pinName: String,
    private val address: Int,
    private val pinInterrupt: String,
    private val ioPin: Pin,
    private val internalPullUp: Boolean = false,
    private val inverse: Boolean = false,
    override var device: AutoCloseable? = null
) : HwUnitI2C<Boolean>, Sensor<Boolean> {

    override val hwUnit: HwUnit = HwUnit(
        name,
        location,
        BoardConfig.IO_EXTENDER_MCP23017_INPUT,
        pinName,
        ConnectionType.I2C,
        address,
        pinInterrupt,
        ioPin.name,
        internalPullUp,
        inverse = inverse
    )
    override var hwUnitValue: HwUnitValue<Boolean?> = HwUnitValue(null, System.currentTimeMillis())

    private var hwUnitListener: Sensor.HwUnitListener<Boolean>? = null

    private val mMCP23017Callback = object : MCP23017PinStateChangeListener {
        override suspend fun onPinStateChanged(pin: Pin, state: PinState) {
            Timber.d("onPinStateChanged pin: ${pin.name} state: $state")
            val value = if (inverse) state != PinState.HIGH else state == PinState.HIGH
            hwUnitValue = HwUnitValue(value, System.currentTimeMillis())
            hwUnitListener?.onHwUnitChanged(hwUnit, Result.success(hwUnitValue))
        }

        override suspend fun onError(error: String) {
            hwUnitListener?.onHwUnitChanged(hwUnit, Result.failure(Exception("error")))
        }
    }

    override suspend fun connect(): Result<Unit> {
        return withContext(Dispatchers.Main) {
            runCatching {
                device = HwUnitI2CMCP23017.getMcp23017Instance(pinName, address).apply {
                    intGpio = pinInterrupt
                    setPullResistance(
                        ioPin,
                        if (internalPullUp) PinPullResistance.PULL_UP else PinPullResistance.OFF
                    )
                    setMode(ioPin, PinMode.DIGITAL_INPUT)
                }
                HwUnitI2CMCP23017.increaseUseCount(pinName, address)
                Unit
            }
        }
    }

    override suspend fun close(): Result<Unit> {
        unregisterListener()
        // We do not want to close this device if it is used by another instance of this class
        return withContext(Dispatchers.Main) {
            val refCountResult = runCatching {
                HwUnitI2CMCP23017.decreaseUseCount(pinName, address).also {
                    Timber.d("close refCount:$it i2c:$address pinInterrupt: $pinInterrupt")
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

    override suspend fun registerListener(listener: Sensor.HwUnitListener<Boolean>): Result<Unit> {
        Timber.d("registerListener")
        hwUnitListener = listener
        return runCatching {
            (device as MCP23017?)?.registerPinListener(ioPin, mMCP23017Callback)
                ?: throw Exception("registerListener MCP23017 device is null")
        }.onSuccess {
            Timber.d("registerListener registered")
        }
    }

    override suspend fun unregisterListener(): Result<Unit> {
        Timber.d("unregisterListener")
        val result = (device as MCP23017?)?.unRegisterPinListener(ioPin, mMCP23017Callback) ?: false
        Timber.d("unregisterListener unRegisterPinListener?: $result")
        hwUnitListener = null
        return if (result) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("unregisterListener device or listener was null"))
        }
    }

    override suspend fun readValue(): Result<HwUnitValue<Boolean?>> {
        return withContext(Dispatchers.Main) {
            runCatching {
                val value = (device as MCP23017?)?.run {
                    if (inverse) getState(ioPin) != PinState.HIGH else getState(ioPin) == PinState.HIGH
                }
                if (value != hwUnitValue.unitValue) {
                    hwUnitValue = HwUnitValue(value, System.currentTimeMillis())
                }
                hwUnitValue
            }
        }
    }
}
