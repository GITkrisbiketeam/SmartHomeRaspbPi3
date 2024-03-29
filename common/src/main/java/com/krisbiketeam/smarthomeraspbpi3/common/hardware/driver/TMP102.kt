package com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver

import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import com.google.android.things.pio.I2cDevice
import com.google.android.things.pio.PeripheralManager
import kotlinx.coroutines.*
import timber.log.Timber

private const val TMP102_EXTENDED_MODE_BIT_SHIFT = 4

private const val TMP102_CONVERSION_RATE_BIT_SHIFT = 6
private const val TMP102_CONVERSION_RATE_MASK = 192

private const val TMP102_SHUTDOWN_MODE_BIT_SHIFT = 8
private const val TMP102_ONE_SHOT_BIT_SHIFT = 15

// Registers
private const val TMP102_REG_TEMP = 0x00
private const val TMP102_REG_CONF = 0x01
private const val TMP102_REG_T_LOW = 0x02
private const val TMP102_REG_T_HIGH = 0x03

private const val TEMP_REG_FACTOR = 0.0625f
/**
 * Driver for the TMP102 temperature sensor.
 */
@MainThread
class TMP102(bus: String? = null, address: Int = DEFAULT_I2C_GND_ADDRESS) : AutoCloseable {


    companion object {

        /**
         * Default I2C address for the sensor.
         */
        const val DEFAULT_I2C_GND_ADDRESS = 0x48
        const val DEFAULT_I2C_VCC_ADDRESS = 0x49
        const val DEFAULT_I2C_SDA_ADDRESS = 0x4A
        const val DEFAULT_I2C_SCL_ADDRESS = 0x4B

        @Deprecated("")
        const val I2C_ADDRESS = DEFAULT_I2C_GND_ADDRESS

        // Sensor constants from the datasheet.
        // https://cdn-shop.adafruit.com/datasheets/BST-BMP280-DS001-11.pdf
        /**
         * Minimum temperature in Celsius the sensor can measure.
         */
        const val MIN_TEMP_C = -25f
        /**
         * Maximum temperature in Celsius the sensor can measure.
         */
        const val MAX_TEMP_C = 85f
        /**
         * Maximum power consumption in micro-amperes when measuring temperature.
         */
        const val MAX_POWER_CONSUMPTION_TEMP_UA = 10f
        /**
         * Maximum frequency of the measurements.
         */
        const val MAX_FREQ_HZ = 8f
        /**
         * Minimum frequency of the measurements.
         */
        const val MIN_FREQ_HZ = 0.25f

        /**
         * Maximum frequency of the measurements.
         */
        const val POWER_ON_CONVERSION_DELAY = 26L
    }

    private var mDevice: I2cDevice? = null
    private val mBuffer = ByteArray(2) // for reading sensor values
    @VisibleForTesting
    internal var mConfig: Int = 0

    /**
     * Extended mode EM.
     */
    enum class ExtendedMode(var value: Int) {
        NOT_EXTENDED_MODE(0),      // default
        EXTENDED_MODE(1);
    }

    var extendedMode: Boolean
        /**
         * Check if TMP102 is in Extended Mode where 13-bits of Temperature register are read
         *
         * @return true if sensor is in EM (Extended Mode) mode
         */
        @MainThread
        get() {
            return 1 shl TMP102_EXTENDED_MODE_BIT_SHIFT and mConfig > 0
        }
        /**
         * Set TMP102  in Extended Mode where 13-bits of Temperature register are read.
         *
         * @param extended true if we want to read 13 bit temperature register (Extended Mode)
         * @throws Exception
         */
        @MainThread
        set(extended) {
            mConfig = if (extended) {
                mConfig or (1 shl TMP102_EXTENDED_MODE_BIT_SHIFT)
            } else {
                mConfig and (1 shl TMP102_EXTENDED_MODE_BIT_SHIFT).inv()
            }
            writeSample16(TMP102_REG_CONF, mConfig)
        }

    /**
     * Conversion mode CR1 and CR0.
     * "01" CONVERSION_RATE4 is default
     */
    enum class ConversionRate(var value: Int) {
        CONVERSION_RATE025(0),
        CONVERSION_RATE1(1),
        CONVERSION_RATE4(2),
        CONVERSION_RATE8(3);
    }

    var conversionRateMode: ConversionRate
        /**
         * Get the power mode of the sensor.
         *
         * @return power mode.
         */
        @MainThread
        get() {
            var tmp = mConfig and TMP102_CONVERSION_RATE_MASK
            tmp = (tmp shr TMP102_CONVERSION_RATE_BIT_SHIFT)
            return when (tmp) {
                3 -> ConversionRate.CONVERSION_RATE8
                2 -> ConversionRate.CONVERSION_RATE4
                1 -> ConversionRate.CONVERSION_RATE1
                else -> ConversionRate.CONVERSION_RATE025
            }
        }
        /**
         * Set the power mode of the sensor.
         *
         * @param mode power mode.
         */
        @MainThread
        set(mode) {
            mConfig = mConfig and TMP102_CONVERSION_RATE_MASK.inv()
            mConfig = mConfig or (mode.value shl TMP102_CONVERSION_RATE_BIT_SHIFT)
            writeSample16(TMP102_REG_CONF, mConfig)
        }

    /**
     * Shutdown mode SD.
     * The Shutdown mode bit saves maximum power by Figure 10. Output Transfer Function Diagrams
     * shutting down all device circuitry other than the serial interface, reducing current
     * consumption to typically less than 0.5mA. Shutdown mode is enabled when the SD bit is '1';
     * the device shuts down when current conversion is completed. When SD is equal to '0', the A
     * fault condition exists when the measured device maintains a continuous conversion state.
     */
    enum class ShutdownMode(var value: Int) {
        CONTINUOUS_MODE(0),
        SHUTDOWN_MODE(1)
    }

    var shutdownMode: Boolean
        /**
         * Check if TMP102 is in shutdown mode where device gets into sleep mode after temp read
         *
         * @return true if sensor is in SD (ShutDown Mode) mode
         */
        @MainThread
        get() {
            return 1 shl TMP102_SHUTDOWN_MODE_BIT_SHIFT and mConfig > 0
        }
        /**
         * Set TMP102 in ShutDown Mode where device gets into sleep mode after temp read
         *
         * @param shutdown true if we want to switch to SD (ShutDown) mode
         * @throws Exception
         */
        @MainThread
        set(shutdown) {
            mConfig = if (shutdown) {
                mConfig or (1 shl TMP102_SHUTDOWN_MODE_BIT_SHIFT)
            } else {
                mConfig and (1 shl TMP102_SHUTDOWN_MODE_BIT_SHIFT).inv()
            }
            writeSample16(TMP102_REG_CONF, mConfig)
        }


    init {
        if (bus != null) {
            try {
                mDevice = PeripheralManager.getInstance()?.openI2cDevice(bus, address)
                mConfig = readSample16(TMP102_REG_CONF) ?: 0
                Timber.d("connect mConfig: $mConfig")
            } catch (e: Exception) {
                try {
                    close()
                } catch (e: Exception) {
                    Timber.e(e,"Error closing device")
                }
                throw Exception("Error Initializing TMP102", e)
            }
        }
    }

    /**
     * Create a new TMP102 sensor driver connected to the given I2c device.
     *
     * @param device I2C device of the sensor.
     * @throws Exception
     */
    @VisibleForTesting
    internal constructor(device: I2cDevice, config: Int) : this() {
        mDevice = device
        mConfig = config
    }

    /**
     * Close the driver and the underlying device.
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    override fun close() {
        Timber.d("close")
        try {
            mDevice?.close()
        } catch (e: Exception){
            throw Exception("Error closing TMP102", e)
        } finally {
            mDevice = null
        }
    }


    /**
     * Read the current temperature.
     *
     * @return the current temperature in degrees Celsius
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    fun readTemperature(): Float? = calculateTemperature(readSample16(TMP102_REG_TEMP))

    /**
     * Read the current temperature in SD (ShutDown) mode. Callback will be triggered after temp read is completed
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    suspend fun readOneShotTemperature(): Float? {
        return if (shutdownMode) {
            // Write OneShot bit to config to wakeup device for one shot read temp
            mConfig = mConfig or (1 shl TMP102_ONE_SHOT_BIT_SHIFT)
            writeSample16(TMP102_REG_CONF, mConfig)
            // Wait 26 ms for conversion to complete
            delay(POWER_ON_CONVERSION_DELAY)
            // check if conversion finished by reading OS bit to '1'
            if (1.shl(TMP102_ONE_SHOT_BIT_SHIFT).and(readSample16(TMP102_REG_CONF) ?: 0) > 0) {
                val temp = readTemperature()
                Timber.d("readOneShotTemperature conversion finished temp? $temp")
                temp
            } else {
                Timber.d("readOneShotTemperature conversion did not completed")
                null
            }
        } else {
            readTemperature()
        }
    }


    /**
     * Reads 16 bits from the given address.
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    private fun readSample16(address: Int): Int? {
        synchronized(mBuffer) {
            // Reading a byte buffer instead of a short to avoid having to deal with
            // platform-specific endianness.
            mDevice?.readRegBuffer(address, mBuffer, 2) ?: return null

            val msb = mBuffer[0].toInt().and(0xff)
            val lsb = mBuffer[1].toInt().and(0xff)
            return msb shl 8 or lsb
        }
    }

    @Throws(Exception::class)
    @MainThread
    private fun writeSample16(address: Int, data: Int) {
        synchronized(mBuffer) {
            //msb
            mBuffer[0] = (data shr 8).toByte()
            mBuffer[1] = data.toByte()
            // Reading a byte buffer instead of a short to avoid having to deal with
            // platform-specific endianness.
            mDevice?.writeRegBuffer(address, mBuffer, 2)
        }
    }

    /**
     * Calculate real temperature in Celsius degree from Raw temp value
     *
     * @param rawTemp Raw temperature returned from TMP102 Sensor
     * @return
     */
    @VisibleForTesting
    internal fun calculateTemperature(rawTemp: Int?): Float? {
        if (rawTemp == null) return null
        return if (rawTemp < 0x8000) {
            // Check if raw Data is not in extended EM 13 bit mode
            if (rawTemp and 0x01 > 0) {
                (rawTemp shr 3) * TEMP_REG_FACTOR
            } else {
                (rawTemp shr 4) * TEMP_REG_FACTOR
            }
        } else {    // Negative number of 2's compliment
            // Check if raw Data is not in extended EM 13 bit mode
            if (rawTemp and 0x01f > 0) {
                ((((rawTemp - 8).inv()) and 0xFFFF) shr 3) * -TEMP_REG_FACTOR
            } else {
                ((((rawTemp - 16).inv()) and 0xFFFF) shr 4) * -TEMP_REG_FACTOR
            }
        }
    }
}

