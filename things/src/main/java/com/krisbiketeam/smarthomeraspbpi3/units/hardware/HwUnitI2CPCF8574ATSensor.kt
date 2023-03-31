package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.PCF8574AT
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.PCF8574ATPin.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitValue
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

open class HwUnitI2CPCF8574ATSensor(
    name: String,
    location: String,
    private val pinName: String,
    private val address: Int,
    private val pinInterrupt: String,
    private val ioPin: Pin,
    override var device: AutoCloseable? = null
) : HwUnitI2C<Boolean>, Sensor<Boolean> {

    override val hwUnit: HwUnit = HwUnit(
        name,
        location,
        BoardConfig.IO_EXTENDER_PCF8474AT_INPUT,
        pinName,
        ConnectionType.I2C,
        address,
        pinInterrupt,
        ioPin.name
    )
    override var hwUnitValue: HwUnitValue<Boolean?> = HwUnitValue(null, System.currentTimeMillis())

    private var hwUnitListener: Sensor.HwUnitListener<Boolean>? = null

    private val mPCF8574ATCallback = object : PCF8574ATPinStateChangeListener {
        override suspend fun onPinStateChanged(pin: Pin, state: PinState) {
            Timber.d("onPinStateChanged pin: ${pin.name} state: $state")
            val value = state == PinState.HIGH
            hwUnitValue = HwUnitValue(value, System.currentTimeMillis())
            hwUnitListener?.onHwUnitChanged(hwUnit, Result.success(hwUnitValue))
        }
    }

    override suspend fun connect(): Result<Unit> {
        return withContext(Dispatchers.Main) {
            runCatching {
                device = HwUnitI2CPCF8574AT.getPcf8574AtInstance(pinName, address).apply {
                    intGpio = pinInterrupt
                    setMode(ioPin, PinMode.DIGITAL_INPUT)
                }
                HwUnitI2CPCF8574AT.increaseUseCount(pinName, address)
                Unit
            }
        }
    }

    override suspend fun close(): Result<Unit> {
        unregisterListener()
        // We do not want to close this device if it is used by another instance of this class
        // decreaseUseCount will close HwUnitI2C when count reaches 0
        return withContext(Dispatchers.Main) {
            val refCountResult = runCatching {
                HwUnitI2CPCF8574AT.decreaseUseCount(pinName, address).also {
                    Timber.d("close refCount:$it")
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
            (device as PCF8574AT?)?.registerPinListener(ioPin, mPCF8574ATCallback)
                ?: throw Exception("registerListener MCP23017 device is null")
        }.onSuccess {
            Timber.d("registerListener registered")
        }
    }

    override suspend fun unregisterListener(): Result<Unit> {
        Timber.d("unregisterListener")
        val result =
            (device as PCF8574AT?)?.unRegisterPinListener(ioPin, mPCF8574ATCallback) ?: false
        Timber.d("registerListener unRegisterPinListener?: $result")
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
                val value = (device as PCF8574AT?)?.getState(ioPin) == PinState.HIGH
                if (value != hwUnitValue.unitValue) {
                    hwUnitValue = HwUnitValue(value, System.currentTimeMillis())
                }
                hwUnitValue
            }
        }
    }
}
