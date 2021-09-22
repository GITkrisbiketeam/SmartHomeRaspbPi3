package com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver

import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import com.google.android.things.pio.I2cDevice
import com.google.android.things.pio.PeripheralManager
import kotlinx.coroutines.delay
import timber.log.Timber

// Registers
private const val HUMID_HIGH: Byte = 0x00
private const val HUMID_LOW: Byte = 0x01
private const val TEMP_HIGH: Byte = 0x02
private const val TEMP_LOW: Byte = 0x03
private const val MODEL_HIGH: Byte = 0x08
private const val MODEL_LOW: Byte = 0x09
private const val VERSION_NUMBER: Byte = 0x0A
private const val DEVICE_ID_0_7: Byte = 0x0E
private const val DEVICE_ID_8_15: Byte = 0x0D
private const val DEVICE_ID_16_23: Byte = 0x0C
private const val DEVICE_ID_24_31: Byte = 0x0B

private const val STATUS_REGISTER = 0x0F

private const val USER_REGISTER_A_HIGH: Byte = 0x10
private const val USER_REGISTER_A_LOW: Byte = 0x11
private const val USER_REGISTER_2_HIGH: Byte = 0x12
private const val USER_REGISTER_2_LOW: Byte = 0x13


private const val READ_REG_CODE: Byte = 0x03


/**
 * Driver for the AM2320 temperature sensor.
 *
 * !!! IMPORTANT !!!
 * Must be called on MainThread with all its methods
 */
@MainThread
class AM2320(bus: String? = null) : AutoCloseable {


    companion object {

        // Sensor constants from the datasheet.
        // https://cdn-shop.adafruit.com/product-files/3721/AM2320.pdf
        /**
         * Minimum temperature in Celsius the sensor can measure.
         */
        const val MIN_TEMP_C = -40f

        /**
         * Maximum temperature in Celsius the sensor can measure.
         */
        const val MAX_TEMP_C = 80f

        /**
         * Minimum temperature in Celsius the sensor can measure.
         */
        const val MIN_RH = 0f

        /**
         * Maximum temperature in Celsius the sensor can measure.
         */
        const val MAX_RH = 100f

        /**
         * Maximum power consumption in micro-amperes when measuring temperature.
         */
        const val MAX_POWER_CONSUMPTION_TEMP_UA = 950f

        /**
         * Maximum frequency of the measurements.
         */
        const val MAX_FREQ_HZ = 40f

        /**
         * Minimum frequency of the measurements.
         */
        const val MIN_FREQ_HZ = 100f

        /**
         * Maximum frequency of the measurements.
         */
        const val POWER_ON_CONVERSION_DELAY = 80L

        /**
         * I2C address for the Sensor.
         */
        const val I2C_ADDRESS = 0x5C

    }

    private var mDevice: I2cDevice? = null
    private val mBuffer = ByteArray(8) // for reading sensor values

    init {
        if (bus != null) {
            Timber.d("connect init")
            try {
                mDevice = PeripheralManager.getInstance()?.openI2cDevice(bus, I2C_ADDRESS)
                Timber.d("connect")
            } catch (e: Exception) {
                close()
                throw Exception("Error Initializing Am2320", e)
            }
        }
    }

    /**
     * Create a new Am2320 sensor driver connected to the given I2c device.
     *
     * @param device I2C device of the sensor.
     */
    @VisibleForTesting
    internal constructor(device: I2cDevice, config: Int) : this() {
        mDevice = device
    }

    /**
     * Close the driver and the underlying device.
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    override fun close() {
        Timber.d("close started")
        try {
            mDevice?.close()
        } catch (e: Exception) {
            throw Exception("Error closing Am2320", e)
        } finally {
            mDevice = null
            Timber.d("close finished")
        }
    }

    /**
     * Read the current temperature . Callback will be triggered after temp
     * read is completed
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    suspend fun readOneShotTemperature(): Float? {
        Timber.d("readOneShotTemperature start")
        val temp = readTemperature()
        Timber.d("readOneShotTemperature conversion finished temp? $temp")
        return temp
    }

    /**
     * Read the current RH . Callback will be triggered after RH measurement is finished
     * read is completed temperature measurement is also done here in the sensor and value can be read by readPrevTemperature
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    suspend fun readOneShotRh(): Float? {
        Timber.d("readOneShotRh start")
        val rh = readRH()
        Timber.d("readOneShotRh conversion finished rh? $rh")
        return rh
    }

    /**
     * Read the current RH and Temperature associated with this RH measurement. Callback will be triggered after RH measurement is finished
     * read is completed temperature measurement is also done here in the sensor
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    suspend fun readOneShotTempAndRh(): Pair<Float?, Float?> {
        Timber.d("readOneShotTempAndRh start")
        val result = readTempAndRH()
        Timber.d("readOneShotTempAndRh conversion finished result? $result")
        return result ?: Pair(null, null)
    }


    /**
     * Read the temperature.
     *
     * @return the current temperature in degrees Celsius
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    private suspend fun readTemperature(): Float? = calculateTemperature(readSample16CRC(TEMP_HIGH))

    /**
     * Read the RH.
     *
     * @return the current temperature in degrees Celsius
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    private suspend fun readRH(): Float? = calculateRh(readSample16CRC(HUMID_HIGH))

    /**
     * Read the RH.
     *
     * @return the current temperature in degrees Celsius
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    private suspend fun readTempAndRH(): Pair<Float?, Float?>? = calculateTempAndRh(readSampleTwo16CRC(HUMID_HIGH))

    /**
     * Reads 16 bits from the given address.
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    private suspend fun readSample16CRC(register: Byte): Int? {
        //synchronized(mBuffer) {
        val mWriteBuffer = ByteArray(3)
        // wakeup
        mDevice?.write(mWriteBuffer, 1)

        delay(1)// or 10)

        mWriteBuffer[0] = READ_REG_CODE
        mWriteBuffer[1] = register
        mWriteBuffer[2] = 0x02

        mDevice?.write(mWriteBuffer, 1)

        delay(2)

        mDevice?.read(mBuffer, 6) ?: return null


        if (mBuffer[0] != READ_REG_CODE) return null // must be 0x03 modbus reply

        if (mBuffer[1].toInt().and(0xff) != 2) return null // must be 2 bytes reply


        var sensorCrc: Int = mBuffer[5].toInt().and(0xff)
        sensorCrc = sensorCrc shl 8
        sensorCrc = sensorCrc or mBuffer[4].toInt().and(0xff)

        val uByteArray = UByteArray(4)
        for (i in 0 until 4) {
            uByteArray[i] = mBuffer[i].toUByte()
        }
        val calcCrc: Int = am2320Crc16(uByteArray, 4) // preamble + data

        // Serial.print("CRC: 0x"); Serial.println(calc_crc, HEX);
        // Serial.print("CRC: 0x"); Serial.println(calc_crc, HEX);
        if (sensorCrc != calcCrc) return null

        // All good!

        // All good!
        var ret: Int = mBuffer[2].toInt().and(0xff)
        ret = ret shl 8
        ret = ret or mBuffer[3].toInt().and(0xff)

        return ret
        //}
    }

    @Throws(Exception::class)
    @MainThread
    private suspend fun readSampleTwo16CRC(register: Byte): Pair<Int, Int>? {
        Timber.w("readSampleTwo16CRC")
        //synchronized(mBuffer) {
        val mWriteBuffer = ByteArray(3)
        // wakeup
        try {
            mDevice?.write(mWriteBuffer, 1)
        } catch (e: Exception){
            // first write command always fails because we wakeup the sensor
            // ignore
        }

        delay(1)// or 10)

        mWriteBuffer[0] = READ_REG_CODE
        mWriteBuffer[1] = register
        mWriteBuffer[2] = 0x04

        mDevice?.write(mWriteBuffer, 3)

        delay(2)

        mDevice?.read(mBuffer, 8) ?: return null

        if (mBuffer[0] != READ_REG_CODE) return null // must be 0x03 modbus reply

        if (mBuffer[1].toInt().and(0xff) != 4) return null // must be 2 bytes reply


        var sensorCrc: Int = mBuffer[7].toInt().and(0xff)
        sensorCrc = sensorCrc shl 8
        sensorCrc = sensorCrc or mBuffer[6].toInt().and(0xff)


        val uByteArray = UByteArray(6)
        for (i in 0 until 6) {
            uByteArray[i] = mBuffer[i].toUByte()
        }

        val calcCrc: Int = am2320Crc16(uByteArray, 6) // preamble + data
        Timber.v("readSampleTwo16CRC sensorCrc:$sensorCrc calcCrc $calcCrc")

        // Serial.print("CRC: 0x"); Serial.println(calc_crc, HEX);
        // Serial.print("CRC: 0x"); Serial.println(calc_crc, HEX);
        if (sensorCrc != calcCrc) return null

        // All good!

        // All good!
        var rh: Int = mBuffer[2].toInt().and(0xff)
        rh = rh shl 8
        rh = rh or mBuffer[3].toInt().and(0xff)

        var temp: Int = mBuffer[4].toInt().and(0xff)
        temp = temp shl 8
        temp = temp or mBuffer[5].toInt().and(0xff)

        return Pair(temp, rh)
    }

    /**
     * Calculate real temperature in Celsius degree from Raw temp value
     *
     * @param rawTemp Raw temperature returned from AM2320 Sensor
     * @return
     */
    @VisibleForTesting
    internal fun calculateTemperature(rawTemp: Int?): Float? {
        if (rawTemp == null || rawTemp == 0) return null
        Timber.d("calculateTemperature rawTemp:$rawTemp")
        val tempRaw = if (rawTemp > 0x8000) { // negative temp
            rawTemp.and(0x7fff) / -10f
        } else {
            rawTemp / 10f
        }

        Timber.d("calculateTemperature tempRaw:$tempRaw")
        return tempRaw
    }

    /**
     * Calculate real rh in percentage value from 0 to 100
     *
     * @param rawRh Raw RH returned from AM2320 Sensor
     * @return
     */
    @VisibleForTesting
    internal fun calculateRh(rawRh: Int?): Float? {
        if (rawRh == null || rawRh == 0) return null
        Timber.d("calculateRh rawRh:$rawRh")
        val rhRaw: Float = rawRh / 10f

        Timber.d("calculateRh rhRaw:$rhRaw")
        /*return when {
            rhRaw > 100 -> 100f
            rhRaw < 0 -> 0f
            else -> rhRaw

        }*/
        return rhRaw
    }


    /**
     * Calculate real temperature in Celsius degree from Raw temp value
     *
     * @param rawTempAndRh Raw temperature and humidity returned from Am2320 Sensor
     * @return
     */
    @VisibleForTesting
    internal fun calculateTempAndRh(rawTempAndRh: Pair<Int, Int>?): Pair<Float?, Float?>? {
        if (rawTempAndRh == null || rawTempAndRh.first == 0 || rawTempAndRh.second == 0) return null
        val result = Pair(calculateTemperature(rawTempAndRh.first), calculateRh(rawTempAndRh.second))
        Timber.d("calculateTemperature result:$result")
        return result
    }
}

@ExperimentalUnsignedTypes
private fun am2320Crc16(data: UByteArray, count: Int): Int {
    var crc: UShort = 0xffffu
    for (i in 0 until count) {
        crc = crc xor data[i].toUShort()
        for (j in 0 until 8) {
            crc = if ((crc and 0x0001u) > 0u) {
                (crc shr 1) xor 0xA001u
            } else {
                (crc shr 1)
            }
        }
    }

    return crc.toInt()
}

private infix fun UShort.shr(bitCount: Int): UShort =
        (this.toUInt() shr bitCount).toUShort()

