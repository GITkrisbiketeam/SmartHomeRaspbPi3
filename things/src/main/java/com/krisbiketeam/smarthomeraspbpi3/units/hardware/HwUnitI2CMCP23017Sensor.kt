package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017Pin.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

open class HwUnitI2CMCP23017Sensor(name: String, location: String, private val pinName: String,
                                   private val address: Int, private val pinInterrupt: String,
                                   private val ioPin: Pin,
                                   private val internalPullUp: Boolean = false,
                                   private val inverse: Boolean = false,
                                   override var device: AutoCloseable? = null) : HwUnitI2C<Boolean>,
        Sensor<Boolean> {

    override val hwUnit: HwUnit =
            HwUnit(name, location, BoardConfig.IO_EXTENDER_MCP23017_INPUT, pinName,
                   ConnectionType.I2C, address, pinInterrupt, ioPin.name, internalPullUp, inverse = inverse)
    override var unitValue: Boolean? = null
    override var valueUpdateTime: Long = System.currentTimeMillis()

    private var hwUnitListener: Sensor.HwUnitListener<Boolean>? = null

    private val mMCP23017Callback = object : MCP23017PinStateChangeListener {
        override suspend fun onPinStateChanged(pin: Pin, state: PinState) {
            Timber.d("onPinStateChanged pin: ${pin.name} state: $state")
            unitValue = if(inverse) state != PinState.HIGH else state == PinState.HIGH
            valueUpdateTime = System.currentTimeMillis()
            hwUnitListener?.onHwUnitChanged(hwUnit, unitValue, valueUpdateTime)
        }

        override suspend fun onError(error: String) {
            hwUnitListener?.onHwUnitError(hwUnit, error, System.currentTimeMillis())
        }
    }

    @Throws(Exception::class)
    override suspend fun connect() {
        withContext(Dispatchers.Main) {
            device = HwUnitI2CMCP23017.getMcp23017Instance(pinName, address).apply {
                intGpio = pinInterrupt
                setPullResistance(ioPin,
                        if (internalPullUp) PinPullResistance.PULL_UP else PinPullResistance.OFF)
                setMode(ioPin, PinMode.DIGITAL_INPUT)
            }
            HwUnitI2CMCP23017.increaseUseCount(pinName, address)
        }
    }

    @Throws(Exception::class)
    override suspend fun close() {
        unregisterListener()
        // We do not want to close this device if it is used by another instance of this class
        return withContext(Dispatchers.Main) {
            val refCount = HwUnitI2CMCP23017.decreaseUseCount(pinName, address)
            Timber.d("close refCount:$refCount i2c:$address pinInterrupt: $pinInterrupt")
            // this will nullify HwUnitI2C#device instance
            if (refCount == 0) {
                super.close()
            }
        }
    }

    override suspend fun registerListener(listener: Sensor.HwUnitListener<Boolean>,
                                          exceptionHandler: CoroutineExceptionHandler) {
        Timber.d("registerListener")
        hwUnitListener = listener
        (device as MCP23017?)?.run {
            val result = registerPinListener(ioPin, mMCP23017Callback)
            Timber.d("registerListener registerPinListener?: $result")
        }
    }

    override suspend fun unregisterListener() {
        Timber.d("unregisterListener")
        (device as MCP23017?)?.run {
            val result = unRegisterPinListener(ioPin, mMCP23017Callback)
            Timber.d("unregisterListener unRegisterPinListener?: $result")
        }
        hwUnitListener = null
    }

    @Throws(Exception::class)
    override suspend fun readValue(): Boolean? {
        return withContext(Dispatchers.Main) {
            val value = (device as MCP23017?)?.run {
                if (inverse) getState(ioPin) != PinState.HIGH else getState(ioPin) == PinState.HIGH
            }
            if (value != unitValue) {
                unitValue = value
                valueUpdateTime = System.currentTimeMillis()
            }
            unitValue
        }
    }
}
