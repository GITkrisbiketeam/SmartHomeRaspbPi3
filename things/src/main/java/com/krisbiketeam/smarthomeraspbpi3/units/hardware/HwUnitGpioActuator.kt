package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.google.android.things.pio.Gpio
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitGpio

class HwUnitGpioActuator(name: String, location: String, pinName: String,
                         private val activeType: Int, override var gpio: Gpio? = null) :
        HwUnitGpio<Boolean>, Actuator<Boolean> {

    override val hwUnit: HwUnit =
            HwUnit(name, location, BoardConfig.GPIO_OUTPUT, pinName, ConnectionType.GPIO)
    override var unitValue: Boolean? = null
    override var valueUpdateTime: Long = System.currentTimeMillis()

    @Throws(Exception::class)
    override fun setValue(value: Boolean) {
        unitValue = value
        gpio?.value = value
        valueUpdateTime = System.currentTimeMillis()
    }

    @Throws(Exception::class)
    override fun connect() {
        super.connect()

        gpio?.run {
            setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
            setActiveType(activeType)
        }
    }


}
