package com.krisbiketeam.smarthomeraspbpi3.units

import com.krisbiketeam.smarthomeraspbpi3.units.hardware.HwUnitI2CMCP23017
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class HwUnitI2CMCP23017Test {

    @Before
    fun setUp(){
        HwUnitI2CMCP23017.mcpUseCountMap = ConcurrentHashMap()
    }

    @Test
    fun increaseUseCount_by_two() {
        var actual = HwUnitI2CMCP23017.increaseUseCount("I2C1", 0x48)
        actual = HwUnitI2CMCP23017.increaseUseCount("I2C1", 0x48)
        Assert.assertEquals(2, actual)
    }

    @Test
    fun decreaseUseCount_by_one() {
        HwUnitI2CMCP23017.mcpUseCountMap[Objects.hash("I2C1", 0x48)] = 5
        val actual = HwUnitI2CMCP23017.decreaseUseCount("I2C1", 0x48)
        Assert.assertEquals(4, actual)
    }

    @Test
    fun decreaseUseCount_from_empty() {
        val actual = HwUnitI2CMCP23017.decreaseUseCount("I2C1", 0x48)
        Assert.assertEquals(0, actual)
    }
}