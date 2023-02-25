package com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver

import android.util.Log
import com.google.android.things.pio.I2cDevice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.Matchers.eq
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

private const val DEF_MCP9808_CONFIG = 0x64A0

@RunWith(PowerMockRunner::class)
@PowerMockIgnore("jdk.internal.reflect.*")
@PrepareForTest(Log::class)
class MCP9808Test {

    @Mock
    private lateinit var mI2c: I2cDevice

    @Rule
    var mMockitoRule: MockitoRule = MockitoJUnit.rule()


    @Test
    fun readSample16() {
        PowerMockito.mockStatic(Log::class.java)
        val mcp9808 = MCP9808(mI2c, DEF_MCP9808_CONFIG)
        val value: Float? = mcp9808.readTemperature()
        Mockito.verify(mI2c).readRegBuffer(eq(0x05), any(ByteArray::class.java), eq(2))
    }

    @Test
    fun readOneShotTemperature() {
        runBlocking {
            PowerMockito.mockStatic(Log::class.java)
            val mcp9808 = MCP9808(mI2c, DEF_MCP9808_CONFIG)
            val value: Float? = mcp9808.readOneShotTemperature()
            Mockito.verify(mI2c, times(3)).readRegBuffer(eq(0x05), any(ByteArray::class.java), eq(2))
        }
    }

    @Test
    fun calculateTemperature() {
        PowerMockito.mockStatic(Log::class.java)
        val mcp9808 = MCP9808(mI2c, DEF_MCP9808_CONFIG)
        val actual = mcp9808.calculateTemperature(0x0190)?: 0f
        val expectedValue = 25.0f
        assertEquals(expectedValue, actual, 0f)
    }

    @Test
    fun calculateTemperature_Negative() {
        PowerMockito.mockStatic(Log::class.java)
        val mcp9808 = MCP9808(mI2c, DEF_MCP9808_CONFIG)
        val actual = mcp9808.calculateTemperature(0x1FC0)?: 0f
        val expectedValue = -4.0f
        assertEquals(expectedValue, actual, 0f)
    }

    @Test
    fun getTemperatureHysteresis_rate4() {
        PowerMockito.mockStatic(Log::class.java)
        val mcp9808 = MCP9808(mI2c, DEF_MCP9808_CONFIG)
        assertEquals(MCP9808.TemperatureHysteresis.HYST_3_0C, mcp9808.temperatureHysteresis)
    }

    @Test
    fun setTemperatureHysteresis_rate1() {
        PowerMockito.mockStatic(Log::class.java)
        val mcp9808 = MCP9808(mI2c, DEF_MCP9808_CONFIG)
        mcp9808.temperatureHysteresis = MCP9808.TemperatureHysteresis.HYST_1_5C
        val expectedValue = 0x62A0
        assertEquals(expectedValue, mcp9808.mConfig)
    }

    @Test
    fun isShutdownMode_ShutDown() {
        PowerMockito.mockStatic(Log::class.java)
        val mcp9808 = MCP9808(mI2c, 0x61B0)
        assertTrue(mcp9808.shutdownMode)
    }

    @Test
    fun isShutdownMode_Continuous() {
        PowerMockito.mockStatic(Log::class.java)
        val mcp9808 = MCP9808(mI2c, DEF_MCP9808_CONFIG)
        assertFalse(mcp9808.shutdownMode)
    }

    @Test
    fun setShutdownMode_set() {
        PowerMockito.mockStatic(Log::class.java)
        val mcp9808 = MCP9808(mI2c, DEF_MCP9808_CONFIG)
        mcp9808.shutdownMode = true
        val expectedValue = 0x65A0
        assertEquals(expectedValue, mcp9808.mConfig)
    }

    @Test
    fun setShutdownMode_clear() {
        PowerMockito.mockStatic(Log::class.java)
        val mcp9808 = MCP9808(mI2c, 0x61A0)
        mcp9808.shutdownMode = false
        val expectedValue = 0x60A0
        assertEquals(expectedValue, mcp9808.mConfig)
    }


}
