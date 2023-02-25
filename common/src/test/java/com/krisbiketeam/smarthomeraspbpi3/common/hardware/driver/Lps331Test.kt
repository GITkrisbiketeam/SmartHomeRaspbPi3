package com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import timber.log.Timber
import java.math.BigInteger
import kotlin.experimental.and

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class Lps331Test {

    @Test
    fun decodeHex_test() {
        val byteArray = ByteArray(4)
        byteArray[0] = -42
        byteArray[1] = -50
        byteArray[2] = 0
        byteArray[3] = 0

        val int = BigInteger(byteArray).intValueExact()

        val int2 = int shr 16

        val lsb = byteArray[0].toInt().and(0xff)
        val shortLsb:Short = byteArray[0].toShort().and(0xff)
        val lsbByte:Byte = byteArray[0]

        val msb = byteArray[1].toInt().and(0xff)
        val shortMsb:Short = byteArray[1].toShort().and(0xff)
        val msbByte:Byte = byteArray[1]

        Timber.e("msbByte:$msbByte lsbByte:$lsbByte")
        Timber.e("shortMsb:$shortMsb shortLsb:$shortLsb")
        Timber.e("msb:$msb lsb:$lsb  all: ${msb shl 8 or lsb}")
        val shortMsbShift:Short = byteArray[1].toShort() shl 8
        val shortMsbShift2:Short = shortMsb shl 8
        val shortAll: Short = shortMsbShift.plus(shortLsb).toShort()
        Timber.e("shortMsbShift:$shortMsbShift shortMsbShift2:$shortMsbShift2 shortAll:$shortAll")


        val temp = calculateTemperature(shortAll.toInt())

        assertTrue(temp?.equals(20)?: false)
    }

    @Test
    fun calculateTemperature_test() {



        val short:Short = -10800
        val ushort:Short = 0b0010101000110000

        //val short2:UShort = 0b1101010111001110

        val byte: Byte = short.toByte()

        val temp = calculateTemperature(short.toInt())

        assertEquals(temp, 20.0f)
    }

    internal fun calculateTemperature(rawTemp: Int?): Float? {
        if (rawTemp == null || rawTemp == 0) return null
        Timber.d("calculateTemperature rawTemp:$rawTemp")
        val temp: Float = 42.5f + rawTemp / 480f

        Timber.d("calculateTemperature temp:$temp")
        return temp
    }
}