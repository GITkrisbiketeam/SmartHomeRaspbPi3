package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.PCF8574AT
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.PCF8574ATPin.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

open class HwUnitI2CPCF8574ATSensor(name: String, location: String, private val pinName: String,
                                    private val address: Int, private val pinInterrupt: String,
                                    private val ioPin: Pin,
                                    override var device: AutoCloseable? = null) :
        HwUnitI2C<Boolean>, Sensor<Boolean> {

    override val hwUnit: HwUnit =
            HwUnit(name, location, BoardConfig.IO_EXTENDER_PCF8474AT_INPUT, pinName,
                    ConnectionType.I2C, address, pinInterrupt, ioPin.name)
    override var unitValue: Boolean? = null
    override var valueUpdateTime: Long = System.currentTimeMillis()

    private var hwUnitListener: Sensor.HwUnitListener<Boolean>? = null

    private val mPCF8574ATCallback = object : PCF8574ATPinStateChangeListener {
        override suspend fun onPinStateChanged(pin: Pin, state: PinState) {
            Timber.d("onPinStateChanged pin: ${pin.name} state: $state")
            unitValue = state == PinState.HIGH
            valueUpdateTime = System.currentTimeMillis()
            hwUnitListener?.onHwUnitChanged(hwUnit, unitValue, valueUpdateTime)
        }
    }

    @Throws(Exception::class)
    override suspend fun connect() {
        withContext(Dispatchers.Main) {
            device = HwUnitI2CPCF8574AT.getPcf8574AtInstance(pinName, address).apply {
                intGpio = pinInterrupt
                setMode(ioPin, PinMode.DIGITAL_INPUT)
            }
            HwUnitI2CPCF8574AT.increaseUseCount(pinName, address)
        }
    }

    @Throws(Exception::class)
    override suspend fun close() {
        unregisterListener()
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

    override suspend fun registerListener(listener: Sensor.HwUnitListener<Boolean>,
                                          exceptionHandler: CoroutineExceptionHandler) {
        Timber.d("registerListener")
        hwUnitListener = listener
        (device as PCF8574AT?)?.run {
            val result = registerPinListener(ioPin, mPCF8574ATCallback)
            Timber.d("registerListener registerPinListener?: $result")
        }
    }

    override suspend fun unregisterListener() {
        Timber.d("unregisterListener")
        (device as PCF8574AT?)?.run {
            val result = unRegisterPinListener(ioPin, mPCF8574ATCallback)
            Timber.d("registerListener unRegisterPinListener?: $result")
        }
        hwUnitListener = null
    }

    @Throws(Exception::class)
    override suspend fun readValue(): Boolean? {
        return withContext(Dispatchers.Main) {
            val value = (device as PCF8574AT).run {
                getState(ioPin) == PinState.HIGH
            }
            if (value != unitValue) {
                unitValue = value
                valueUpdateTime = System.currentTimeMillis()
            }
            unitValue
        }
    }
}
