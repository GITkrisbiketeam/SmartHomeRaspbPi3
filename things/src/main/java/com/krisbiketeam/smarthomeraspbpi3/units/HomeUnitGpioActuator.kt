package com.krisbiketeam.smarthomeraspbpi3.units

import com.google.android.things.pio.Gpio
import com.krisbiketeam.smarthomeraspbpi3.utils.Logger
import com.krisbiketeam.smarthomeraspbpi3.utils.Utils
import java.io.IOException

class HomeUnitGpioActuator(override val homeUnit: HomeUnit, override val activeType: Int, override var gpio: Gpio?) : HomeUnitGpio, Actuator {
    companion object {
        private val TAG = Utils.getLogTag(HomeUnitGpioActuator::class.java)
    }

    init {
        //We can safely connect from constructor as this does not block other HomeUnit peripherals
        connect()
    }

    override fun setValue(value: Any?) {
        if (value is Boolean) {
            homeUnit.value = value
            try {
                gpio?.setValue(value)
            } catch (e: IOException) {
                Logger.e(TAG, "Error updating GPIO value on $homeUnit", e)
            }
        } else {
            Logger.w(TAG, "setValue value not instance of Boolean $value")
            homeUnit.value = null
        }
    }
}
