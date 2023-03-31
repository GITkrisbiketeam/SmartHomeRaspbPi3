package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import com.google.android.things.pio.Gpio
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitGpio
import com.krisbiketeam.smarthomeraspbpi3.units.HwUnitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HwUnitGpioActuator(
    name: String,
    location: String,
    pinName: String,
    private val activeType: Int,
    override var gpio: Gpio? = null
) : HwUnitGpio<Boolean>, Actuator<Boolean> {

    override val hwUnit: HwUnit =
        HwUnit(name, location, BoardConfig.GPIO_OUTPUT, pinName, ConnectionType.GPIO)
    override var hwUnitValue: HwUnitValue<Boolean?> = HwUnitValue(null, System.currentTimeMillis())

    override suspend fun connect(): Result<Unit> {
        return withContext(Dispatchers.Main) {
            super.connect().mapCatching {
                gpio?.run {
                    setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
                    setActiveType(activeType)
                } ?: Unit
            }
        }
    }

    override suspend fun setValue(value: Boolean): Result<Unit> {
        return withContext(Dispatchers.Main) {
            runCatching {
                gpio?.value = value
            }.onSuccess {
                hwUnitValue = HwUnitValue(value, System.currentTimeMillis())
            }
        }
    }
}
