package com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver

import android.util.Log
import android.view.ViewConfiguration

import com.google.android.things.pio.I2cDevice
import com.google.android.things.pio.PeripheralManager
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.ViewConfigurationMock

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

import org.junit.Assert.assertEquals
import org.junit.Before
import org.mockito.Matchers.anyInt
import org.mockito.Matchers.anyString
import org.mockito.junit.MockitoRule
import org.powermock.core.classloader.annotations.PowerMockIgnore

@RunWith(PowerMockRunner::class)
@PowerMockIgnore("jdk.internal.reflect.*")
@PrepareForTest(Log::class, ViewConfiguration::class, PeripheralManager::class)
class MCP23017Test {

    @Mock
    private lateinit var mI2c: I2cDevice

    @Mock
    private lateinit var peripheralManager: PeripheralManager

    @Rule
    var mMockitoRule: MockitoRule = MockitoJUnit.rule()

    @Before
    @Throws(Exception::class)
    fun setup() {
        ViewConfigurationMock.mockStatic()

        PowerMockito.mockStatic(PeripheralManager::class.java)
        PowerMockito.`when`(PeripheralManager.getInstance()).thenReturn(peripheralManager)
        PowerMockito.`when`(peripheralManager.openI2cDevice(anyString(), anyInt())).thenReturn(mI2c)

    }

    @Test
    @Throws(Exception::class)
    fun getMode_gpio_A() {
        PowerMockito.mockStatic(Log::class.java)
        PowerMockito.`when`(mI2c.readRegByte(anyInt())).thenReturn(0b10000000.toByte())
        val mcp23017 = MCP23017()
        mcp23017.setMode(MCP23017Pin.Pin.GPIO_A0, MCP23017Pin.PinMode.DIGITAL_INPUT)
        val pinMode = mcp23017.getMode(MCP23017Pin.Pin.GPIO_A0)
        assertEquals(MCP23017Pin.PinMode.DIGITAL_INPUT, pinMode)
    }

    @Test
    @Throws(Exception::class)
    fun getMode_gpio_B() {
        PowerMockito.mockStatic(Log::class.java)
        PowerMockito.`when`(mI2c.readRegByte(anyInt())).thenReturn(0.toByte())
        val mcp23017 = MCP23017()
        mcp23017.setMode(MCP23017Pin.Pin.GPIO_B2, MCP23017Pin.PinMode.DIGITAL_INPUT)
        val pinMode = mcp23017.getMode(MCP23017Pin.Pin.GPIO_B2)
        assertEquals(MCP23017Pin.PinMode.DIGITAL_INPUT, pinMode)
    }

    @Test
    @Throws(Exception::class)
    fun getMode_notSet() {
        PowerMockito.mockStatic(Log::class.java)
        PowerMockito.`when`(mI2c.readRegByte(anyInt())).thenReturn(0.toByte())
        val mcp23017 = MCP23017()
        mcp23017.setMode(MCP23017Pin.Pin.GPIO_A4, MCP23017Pin.PinMode.DIGITAL_OUTPUT)
        val pinMode = mcp23017.getMode(MCP23017Pin.Pin.GPIO_A4)
        assertEquals(MCP23017Pin.PinMode.DIGITAL_OUTPUT, pinMode)
    }

    @Test
    @Throws(Exception::class)
    fun getPullResistance_gpio_A() {
        PowerMockito.mockStatic(Log::class.java)
        PowerMockito.`when`(mI2c.readRegByte(anyInt())).thenReturn(0.toByte())
        val mcp23017 = MCP23017()
        mcp23017.setPullResistance(MCP23017Pin.Pin.GPIO_A1, MCP23017Pin.PinPullResistance.PULL_UP)
        val pullRes = mcp23017.getPullResistance(MCP23017Pin.Pin.GPIO_A1)
        assertEquals(MCP23017Pin.PinPullResistance.PULL_UP, pullRes)
    }

    @Test
    @Throws(Exception::class)
    fun getPullResistance_gpio_B() {
        PowerMockito.mockStatic(Log::class.java)
        PowerMockito.`when`(mI2c.readRegByte(anyInt())).thenReturn(0.toByte())
        val mcp23017 = MCP23017()
        mcp23017.setPullResistance(MCP23017Pin.Pin.GPIO_B2, MCP23017Pin.PinPullResistance.PULL_UP)
        val pullRes = mcp23017.getPullResistance(MCP23017Pin.Pin.GPIO_B2)
        assertEquals(MCP23017Pin.PinPullResistance.PULL_UP, pullRes)
    }

    @Test
    @Throws(Exception::class)
    fun getPullResistance_notSet() {
        PowerMockito.mockStatic(Log::class.java)
        PowerMockito.`when`(mI2c.readRegByte(anyInt())).thenReturn(0.toByte())
        val mcp23017 = MCP23017()
        mcp23017.setPullResistance(MCP23017Pin.Pin.GPIO_A1, MCP23017Pin.PinPullResistance.OFF)
        val pullRes = mcp23017.getPullResistance(MCP23017Pin.Pin.GPIO_A1)
        assertEquals(MCP23017Pin.PinPullResistance.OFF, pullRes)
    }
}
