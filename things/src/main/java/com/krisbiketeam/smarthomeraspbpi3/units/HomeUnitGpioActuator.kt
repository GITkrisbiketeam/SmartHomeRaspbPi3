package com.krisbiketeam.smarthomeraspbpi3.units

import com.google.android.things.pio.Gpio
import com.krisbiketeam.data.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.utils.Logger
import com.krisbiketeam.smarthomeraspbpi3.utils.Utils
import java.io.IOException

class HomeUnitGpioActuator(name: String,
                           location: String,
                           pinName: String,
                           override val activeType: Int,
                           override var gpio: Gpio? = null) : HomeUnitGpio<Boolean>, Actuator<Boolean> {

    companion object {
        private val TAG = Utils.getLogTag(HomeUnitGpioActuator::class.java)
    }

    override val homeUnit: HomeUnit<Boolean> = HomeUnit(name, location, pinName, ConnectionType.GPIO)


    override fun setValue(value: Boolean?) {
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
