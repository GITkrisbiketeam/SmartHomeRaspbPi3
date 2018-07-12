package com.krisbiketeam.smarthomeraspbpi3.units

import com.krisbiketeam.smarthomeraspbpi3.units.hardware.HomeUnitI2CMCP23017
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.*

class HomeUnitI2CMCP23017Test {

    @Before
    fun setUp(){
        HomeUnitI2CMCP23017.mcpUseCountMap = HashMap()
    }

    @Test
    fun increaseUseCount_by_two() {
        var actual = HomeUnitI2CMCP23017.increaseUseCount("I2C1", 0x48)
        actual = HomeUnitI2CMCP23017.increaseUseCount("I2C1", 0x48)
        Assert.assertEquals(2, actual)
    }

    @Test
    fun decreaseUseCount_by_one() {
        HomeUnitI2CMCP23017.mcpUseCountMap[Objects.hash("I2C1", 0x48)] = 5
        val actual = HomeUnitI2CMCP23017.decreaseUseCount("I2C1", 0x48)
        Assert.assertEquals(4, actual)
    }

    @Test
    fun decreaseUseCount_from_empty() {
        val actual = HomeUnitI2CMCP23017.decreaseUseCount("I2C1", 0x48)
        Assert.assertEquals(0, actual)
    }
}