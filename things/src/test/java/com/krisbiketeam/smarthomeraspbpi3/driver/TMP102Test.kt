package com.krisbiketeam.smarthomeraspbpi3.driver

import android.util.Log
import com.google.android.things.pio.I2cDevice
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.Matchers.eq
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

const val DEF_TMP102_CONFIG = 0x60A0

@RunWith(PowerMockRunner::class)
@PrepareForTest(Log::class)
class TMP102Test {

    @Mock
    private lateinit var mI2c: I2cDevice

    @Rule
    var mMockitoRule = MockitoJUnit.rule()


    @Test
    fun readSample16() {
        PowerMockito.mockStatic(Log::class.java)
        val tmp102 = TMP102(mI2c, DEF_TMP102_CONFIG)
        val value: Float? = tmp102.readTemperature()
        Mockito.verify(mI2c).readRegBuffer(eq(0x00), any(ByteArray::class.java), eq(2))
    }

    @Test
    fun calculateTemperature_NormalMode() {
        PowerMockito.mockStatic(Log::class.java)
        val tmp102 = TMP102(mI2c, DEF_TMP102_CONFIG)
        val actual = tmp102.calculateTemperature(0x1900)?: 0f
        val expectedValue = 25.0f
        assertEquals(expectedValue, actual, 0f)
    }

    @Test
    fun calculateTemperature_NegativeNormalMode() {
        PowerMockito.mockStatic(Log::class.java)
        val tmp102 = TMP102(mI2c, DEF_TMP102_CONFIG)
        val actual = tmp102.calculateTemperature(0xFFC0)?: 0f
        val expectedValue = -0.25f
        assertEquals(expectedValue, actual, 0f)
    }

    @Test
    fun calculateTemperature_ExtendedMode() {
        PowerMockito.mockStatic(Log::class.java)
        val tmp102 = TMP102(mI2c, DEF_TMP102_CONFIG)
        val actual = tmp102.calculateTemperature(0xC81)?: 0f
        val expectedValue = 25.0f
        assertEquals(expectedValue, actual, 0f)
    }

    @Test
    fun calculateTemperature_NegativeExtendedMode() {
        PowerMockito.mockStatic(Log::class.java)
        val tmp102 = TMP102(mI2c, DEF_TMP102_CONFIG)
        val actual = tmp102.calculateTemperature(0xFFC0)?: 0f
        val expectedValue = -0.25f
        assertEquals(expectedValue, actual, 0f)
    }

    @Test
    fun isExtendedMode_Extended() {
        PowerMockito.mockStatic(Log::class.java)
        val tmp102 = TMP102(mI2c,0x60B0)

        assertTrue(tmp102.extendedMode)
    }

    @Test
    fun isExtendedMode_Normal() {
        PowerMockito.mockStatic(Log::class.java)
        val tmp102 = TMP102(mI2c, 0x60A0)
        assertFalse(tmp102.extendedMode)
    }

    @Test
    fun setExtendedMode_set() {
        PowerMockito.mockStatic(Log::class.java)
        val tmp102 = TMP102(mI2c, 0x60A0)
        tmp102.extendedMode = true
        val expectedValue = 0x60B0
        assertEquals(expectedValue, tmp102.mConfig)
    }

    @Test
    fun setExtendedMode_clear() {
        PowerMockito.mockStatic(Log::class.java)
        val tmp102 = TMP102(mI2c, 0x60B0)
        tmp102.extendedMode = false
        val expectedValue = 0x60A0
        assertEquals(expectedValue, tmp102.mConfig)
    }

    @Test
    fun getConversionRateMode_rate4() {
        PowerMockito.mockStatic(Log::class.java)
        val tmp102 = TMP102(mI2c,DEF_TMP102_CONFIG)
        assertEquals(TMP102.ConversionRate.CONVERSION_RATE4, tmp102.conversionRateMode)
    }

    @Test
    fun setConversionRateMode_rate1() {
        PowerMockito.mockStatic(Log::class.java)
        val tmp102 = TMP102(mI2c,DEF_TMP102_CONFIG)
        tmp102.conversionRateMode = TMP102.ConversionRate.CONVERSION_RATE1
        val expectedValue = 0x6060
        assertEquals(expectedValue, tmp102.mConfig)
    }

    @Test
    fun isShutdownMode_ShutDown() {
        PowerMockito.mockStatic(Log::class.java)
        val tmp102 = TMP102(mI2c,0x61B0)
        assertTrue(tmp102.shutdownMode)
    }

    @Test
    fun isShutdownMode_Continous() {
        PowerMockito.mockStatic(Log::class.java)
        val tmp102 = TMP102(mI2c, DEF_TMP102_CONFIG)
        assertFalse(tmp102.shutdownMode)
    }

    @Test
    fun setShutdownMode_set() {
        PowerMockito.mockStatic(Log::class.java)
        val tmp102 = TMP102(mI2c, DEF_TMP102_CONFIG)
        tmp102.shutdownMode = true
        val expectedValue = 0x61A0
        assertEquals(expectedValue, tmp102.mConfig)
    }

    @Test
    fun setShutdownMode_clear() {
        PowerMockito.mockStatic(Log::class.java)
        val tmp102 = TMP102(mI2c, 0x61A0)
        tmp102.shutdownMode = false
        val expectedValue = 0x60A0
        assertEquals(expectedValue, tmp102.mConfig)
    }


}
