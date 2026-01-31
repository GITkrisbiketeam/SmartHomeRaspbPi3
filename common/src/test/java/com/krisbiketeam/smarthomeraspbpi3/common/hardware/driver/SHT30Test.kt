package com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver

import com.google.android.things.pio.I2cDevice
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Matchers.any
import org.mockito.Matchers.anyInt
import org.mockito.Matchers.eq
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer

private const val DEF_Si7021_CONFIG = 0x64A0

class SHT30Test {

    private val mI2c: I2cDevice = mockk()

    //@Rule
    //var mMockitoRule: MockitoRule = MockitoJUnit.rule()

    @Test
    fun calculateCRC() {
        val result = sht30Crc8(ubyteArrayOf(0xBE.toUByte(), 0xEF.toUByte()))
        val expected = 0x92.toUByte()

        assertEquals(expected, result)
    }

    @Test
    fun calculateTemperature() {
        val sht30 = SHT30(mI2c)
        val actual = sht30.calculateTemperature(0xffff) ?: 0f
        val expectedValue = 25.0f
        assertEquals(expectedValue, actual, 0f)
    }

    @Test
    fun calculateTemperature_Negative() {
        val sht30 = SHT30(mI2c)
        val actual = sht30.calculateTemperature(0x1FC0) ?: 0f
        val expectedValue = -4.0f
        assertEquals(expectedValue, actual, 0f)
    }

    @Test
    fun readSample16_1() {
        readSample16_prevTemp(-88, 22, 45)
    }

    @Test
    fun readSample16_2() {
        readSample16_prevTemp(-88, 32, -24)
    }

    @Test
    fun readSample16_3() {
        readSample16_prevTemp(-87, -86, 27)
    }

    @Test
    fun readSample16_4() {
        readSample16_prevTemp(-88, -66, 104)
    }

    @Test
    fun readSample16_5() {
        readSample16_prevTemp(-88, -106, 87)
    }

    @Test
    fun readSample16_6() {
        readSample16_prevTemp(-102, -78, -102)
    }

    @Test
    fun readSample16_7() {
        readSample16_prevTemp(-88, 42, -107)
    }

    @Test
    fun readSample16_8() {
        readSample16_prevTemp(-102, -68, -102)
    }

    private fun readSample16_prevTemp(msb: Byte, lsb: Byte, crc: Byte) {
        doAnswer { invocation ->
            val arg0: Any = invocation.arguments[0]
            val arg1: Any = invocation.arguments[1]
            val arg2: Any = invocation.arguments[2]

            assertEquals(0xE0, arg0)
            assertEquals(3, arg2)
            assertTrue(arg1 is ByteArray)
            val buffer: ByteArray = arg1 as ByteArray
            buffer[0] = msb //-88
            buffer[1] = lsb //22
            buffer[2] = crc //45
        }.`when`(mI2c).readRegBuffer(anyInt(), any(ByteArray::class.java), anyInt())

        val sht30 = SHT30(mI2c)
val value: Float? = sht30.calculateTemperature(0)
        Mockito.verify(mI2c).readRegBuffer(eq(0xE0), any(ByteArray::class.java), eq(3))
        assertTrue(value != null)
        //assertEquals(68.52524f, value)
    }


    // mBuffer -88 22 45
    // msb 168
    // lsb 22
    // crc 45

    // mBuffer -88 32 -24
    // msb 168
    // lsb 38
    // crc 232

    // mBuffer -87 -86 27
    // msb 169
    // lsb 170
    // crc 27

    // mBuffer -88 -66 104
    // msb 168
    // lsb 190
    // crc 104

    // mBuffer -88 -106 87
    // msb 168
    // lsb 150
    // crc 87

    // mBuffer -102 -78 -102
    // msb 102
    // lsb 180
    // crc 102

    // mBuffer -88 42 -107
    // msb 168
    // lsb 42
    // crc 149

    // mBuffer -102 -68 -102
    // msb 102
    // lsb 188
    // crc 102
}
