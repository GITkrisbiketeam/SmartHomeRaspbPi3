package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.google.android.things.pio.Gpio
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitGpio
import timber.log.Timber
import java.io.IOException
import java.util.*

class HwUnitGpioActuator(name: String,
                         location: String,
                         pinName: String,
                         private val activeType: Int,
                         override var gpio: Gpio? = null) : HwUnitGpio<Boolean>, Actuator<Boolean> {

    override val hwUnit: HwUnit = HwUnit(name, location, BoardConfig.GPIO_OUTPUT, pinName, ConnectionType.GPIO)
    override var unitValue: Boolean? = null
    override var valueUpdateTime: String = ""

    override fun setValue(value: Boolean?) {
        if (value is Boolean) {
            unitValue = value
            try {
                gpio?.value = value
            } catch (e: IOException) {
                Timber.e(e,"Error updating GPIO value on $hwUnit")
            }
        } else {
            Timber.w("setValue value not instance of Boolean $value")
            unitValue = null
        }
        valueUpdateTime = Date().toString()
    }

    override fun connect() {
        super.connect()

        gpio?.run {
            try {
                setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
                setActiveType(activeType)
            } catch (e: IOException) {
                Timber.e(e,"Error initializing PeripheralIO API on: $hwUnit")
            }
        }
    }


}
