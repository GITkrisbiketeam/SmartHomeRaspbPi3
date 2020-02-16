package com.krisbiketeam.smarthomeraspbpi3.units.hardware

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.PCF8574AT
import java.util.*

object HwUnitI2CPCF8574AT {

    @SuppressLint("UseSparseArrays")
    private var pcfMap = HashMap<Int, PCF8574AT>()
    @SuppressLint("UseSparseArrays")
    @VisibleForTesting
    internal var mcpUseCountMap = HashMap<Int, Int>()

    @Throws(Exception::class)
    fun getPcf8574AtInstance(bus: String, i2cAddr: Int): PCF8574AT {
        return pcfMap.computeIfAbsent(getKeyHash(bus, i2cAddr)) { PCF8574AT(bus, i2cAddr) }
    }

    fun increaseUseCount(bus: String, i2cAddr: Int): Int? {
        return mcpUseCountMap.compute(getKeyHash(bus, i2cAddr)) { _, v -> v?.inc() ?: 1 }
    }

    @Throws(Exception::class)
    fun decreaseUseCount(bus: String, i2cAddr: Int): Int? {
        return mcpUseCountMap.compute(getKeyHash(bus, i2cAddr)) { k, v ->
            (v?.dec() ?: 0).also {
                if (it == 0) {
                    pcfMap.remove(k)?.close()
                }
            }
        }
    }

    private fun getKeyHash(bus: String, i2cAddr: Int): Int {
        return Objects.hash(bus, i2cAddr)
    }
}
