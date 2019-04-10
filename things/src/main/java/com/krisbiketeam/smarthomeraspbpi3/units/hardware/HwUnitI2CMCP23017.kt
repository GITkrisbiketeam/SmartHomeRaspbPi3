package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017
import java.util.*

object HwUnitI2CMCP23017 {

    @SuppressLint("UseSparseArrays")
    private var mcpMap = HashMap<Int, MCP23017>()
    @SuppressLint("UseSparseArrays")
    @VisibleForTesting
    internal var mcpUseCountMap = HashMap<Int, Int>()

    @Throws(Exception::class)
    fun getMcp23017Instance(bus: String, i2cAddr: Int): MCP23017 {
        return mcpMap.computeIfAbsent(getKeyHash(bus, i2cAddr)) { MCP23017(bus, i2cAddr) }
    }

    fun increaseUseCount(bus: String, i2cAddr: Int): Int? {
        return mcpUseCountMap.compute(getKeyHash(bus, i2cAddr)){_, v -> v?.inc() ?: 1}
    }

    @Throws(Exception::class)
    fun decreaseUseCount(bus: String, i2cAddr: Int): Int? {
        return mcpUseCountMap.compute(getKeyHash(bus, i2cAddr)){k, v ->
            (v?.dec() ?: 0).also {
                if (it ==0 ){
                    mcpMap.remove(k)?.close()
                }
            }
        }
    }

    private fun getKeyHash(bus: String, i2cAddr: Int): Int {
        return Objects.hash(bus, i2cAddr)
    }
}
