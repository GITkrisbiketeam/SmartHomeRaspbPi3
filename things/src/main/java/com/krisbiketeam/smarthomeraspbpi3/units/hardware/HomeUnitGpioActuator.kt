package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.google.android.things.pio.Gpio
import com.krisbiketeam.data.storage.ConnectionType
import com.krisbiketeam.data.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.data.storage.dto.HomeUnitLog
import com.krisbiketeam.smarthomeraspbpi3.units.HomeUnitGpio
import com.krisbiketeam.smarthomeraspbpi3.utils.Logger
import com.krisbiketeam.smarthomeraspbpi3.utils.Utils
import java.io.IOException
import java.util.*

private val TAG = Utils.getLogTag(HomeUnitGpioActuator::class.java)

class HomeUnitGpioActuator(name: String,
                           location: String,
                           pinName: String,
                           private val activeType: Int,
                           override var gpio: Gpio? = null) : HomeUnitGpio<Boolean>, Actuator<Boolean> {

    override val homeUnit: HomeUnit = HomeUnit(name, location, pinName, ConnectionType.GPIO)
    override var unitValue: Boolean? = null
    override var valueUpdateTime: String = ""

    override fun setValue(value: Boolean?) {
        if (value is Boolean) {
            unitValue = value
            try {
                gpio?.value = value
            } catch (e: IOException) {
                Logger.e(TAG, "Error updating GPIO value on $homeUnit", e)
            }
        } else {
            Logger.w(TAG, "setValue value not instance of Boolean $value")
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
                Logger.e(TAG, "Error initializing PeripheralIO API on: $homeUnit", e)
            }
        }
    }


}
