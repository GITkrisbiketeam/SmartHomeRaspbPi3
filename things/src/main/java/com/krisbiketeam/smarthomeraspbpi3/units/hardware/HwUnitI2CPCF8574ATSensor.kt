package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.PCF8574AT
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.PCF8574ATPin.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnitLog
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitI2C
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import timber.log.Timber
import java.util.*

open class HwUnitI2CPCF8574ATSensor(name: String,
                                    location: String,
                                    private val pinName: String,
                                    private val address: Int,
                                    private val pinInterrupt: String,
                                    private val ioPin: Pin,
                                    override var device: AutoCloseable? = null) : HwUnitI2C<Boolean>, Sensor<Boolean> {

    override val hwUnit: HwUnit = HwUnit(name, location, BoardConfig.IO_EXTENDER_PCF8474AT_INPUT, pinName, ConnectionType.I2C, address, pinInterrupt, ioPin.name)
    override var unitValue: Boolean? = null
    override var valueUpdateTime: String = ""

    var hwUnitListener: Sensor.HwUnitListener<Boolean>? = null

    open val mPCF8574ATCallback = object : PCF8574ATPinStateChangeListener {
        override fun onPinStateChanged(pin: Pin, state: PinState) {
            Timber.d("onPinStateChanged pin: ${pin.name} state: $state")
            unitValue = state == PinState.HIGH
            valueUpdateTime = Date().toString()
            hwUnitListener?.onUnitChanged(hwUnit, unitValue, valueUpdateTime)

        }
    }

    override fun connect() {
        try {
            device = HwUnitI2CPCF8574AT.getPcf8574AtInstance(pinName, address)
            (device as PCF8574AT).run {
                intGpio = pinInterrupt
                setMode(ioPin, PinMode.DIGITAL_INPUT)
            }
            HwUnitI2CPCF8574AT.increaseUseCount(pinName, address)
        } catch (e: Exception) {
            FirebaseHomeInformationRepository.hwUnitErrorEvent(HwUnitLog(hwUnit, unitValue, Date().toString().plus(e.message)))
            Timber.e(e, "Error connect HwUnitI2CPCF8574ATSensor")
        }
    }

    override fun close() {
        unregisterListener()
        // We do not want to close this device if it is used by another instance of this class
        val refCount = HwUnitI2CPCF8574AT.decreaseUseCount(pinName, address)
        if (refCount == 0) {
            try {
                super.close()
            } catch (e: Exception) {
                FirebaseHomeInformationRepository.hwUnitErrorEvent(HwUnitLog(hwUnit, unitValue, Date().toString().plus(e.message)))
                Timber.e(e, "Error close HwUnitI2CPCF8574ATSensor")
            }
        }
    }

    override fun registerListener(listener: Sensor.HwUnitListener<Boolean>) {
        Timber.d("registerListener")
        hwUnitListener = listener
        (device as PCF8574AT).run {
            val result = registerPinListener(ioPin, mPCF8574ATCallback)
            Timber.d("registerListener registerPinListener?: $result")
        }
    }

    override fun unregisterListener() {
        Timber.d("unregisterListener")
        (device as PCF8574AT).run {
            val result = unRegisterPinListener(ioPin, mPCF8574ATCallback)
            Timber.d("registerListener unRegisterPinListener?: $result")
        }
        hwUnitListener = null
    }

    override fun readValue(): Boolean? {
        unitValue = (device as PCF8574AT).run {
            try {
                getState(ioPin) == PinState.HIGH
            } catch (e: Exception) {
                FirebaseHomeInformationRepository.hwUnitErrorEvent(HwUnitLog(hwUnit, unitValue, Date().toString().plus(e.message)))
                Timber.e(e, "Error readValue HwUnitI2CPCF8574ATSensor")
                null
            }
        }

        return unitValue
    }
}
