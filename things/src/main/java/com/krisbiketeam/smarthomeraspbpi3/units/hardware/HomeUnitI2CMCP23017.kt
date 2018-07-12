package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import android.support.annotation.VisibleForTesting
import com.krisbiketeam.smarthomeraspbpi3.driver.MCP23017
import java.util.*

object HomeUnitI2CMCP23017 {

    private var mcpMap = HashMap<Int, MCP23017>()
    @VisibleForTesting
    internal var mcpUseCountMap = HashMap<Int, Int>()

    fun getMcp23017Instance(bus: String, i2cAddr: Int): MCP23017 {
        return mcpMap.computeIfAbsent(getKeyHash(bus, i2cAddr)) { MCP23017(bus, i2cAddr) }
    }

    fun increaseUseCount(bus: String, i2cAddr: Int): Int? {
        return mcpUseCountMap.compute(getKeyHash(bus, i2cAddr)){k, v -> v?.inc() ?: 1}
    }

    fun decreaseUseCount(bus: String, i2cAddr: Int): Int? {
        return mcpUseCountMap.compute(getKeyHash(bus, i2cAddr)){k, v -> v?.dec() ?: 0}
    }

    private fun getKeyHash(bus: String, i2cAddr: Int): Int {
        return Objects.hash(bus, i2cAddr)
    }
}
