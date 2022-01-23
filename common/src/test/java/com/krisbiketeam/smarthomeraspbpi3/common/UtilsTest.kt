package com.krisbiketeam.smarthomeraspbpi3.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class UtilsTest {
    @Test
    fun toHex_test() {
        val byteArray = ByteArray(5)
        byteArray[0] = -128
        byteArray[1] = -64
        byteArray[2] = 0
        byteArray[3] = 64
        byteArray[4] = 127

        val string = byteArray.toHex()
        assertEquals("80c000407f", string)

    }

    @Test
    fun decodeHex_test() {
        val string = "80c000407f"

        val byteArray = ByteArray(5)
        byteArray[0] = -128
        byteArray[1] = -64
        byteArray[2] = 0
        byteArray[3] = 64
        byteArray[4] = 127

        val bytes = string.decodeHex()
        assertTrue(byteArray.contentEquals(bytes))
    }
}