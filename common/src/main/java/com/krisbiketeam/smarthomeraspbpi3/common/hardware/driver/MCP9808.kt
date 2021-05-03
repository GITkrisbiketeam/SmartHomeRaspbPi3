package com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver

import androidx.annotation.VisibleForTesting
import com.google.android.things.pio.I2cDevice
import com.google.android.things.pio.PeripheralManager
import kotlinx.coroutines.*

import timber.log.Timber

/**
 * bit 0 - Alert Mod.: Alert Output Mode bit; R/W-0
 * 0 = Comparator output (power-up default)
 * 1 = Interrupt output
 * This bit cannot be altered when either of the Lock bits are set (bit 6 and bit 7).
 * This bit can be programmed in Shutdown mode, but the Alert output will not assert or deassert.
 */
private const val MCO9808_CONF_ALERT_MODE_COMPARATOR =  0b11111110        //Default
private const val MCO9808_CONF_ALERT_MODE_INTERRUPT =   0b00000001

/**
 * bit 1 - Alert Pol.: Alert Output Polarity bit; R/W-0
 * 0 = Active-low (power-up default; pull-up resistor required)
 * 1 = Active-high
 * This bit cannot be altered when either of the Lock bits are set (bit 6 and bit 7).
 * This bit can be programmed in Shutdown mode, but the Alert output will not assert or deassert.
 */
private const val MCO9808_CONF_ALERT_POL_ACT_LOW =      0b11111101        //Default
private const val MCO9808_CONF_ALERT_POL_INTERRUPT =    0b00000010

/**
 * bit 2 - Alert Sel.: Alert Output Select bit; R/W-0
 * 0 = Alert output for TUPPER, TLOWER and TCRIT (power-up default)
 * 1 = TA > TCRIT only (TUPPER and TLOWER temperature boundaries are disabled)
 * When the Alarm Window Lock bit is set, this bit cannot be altered until unlocked (bit 6).
 * This bit can be programmed in Shutdown mode, but the Alert output will not assert or deassert.
 */
private const val MCO9808_CONF_ALERT_SEL_ALL =          0b11111011        //Default
private const val MCO9808_CONF_ALERT_SEL_CRIT =         0b00000100

/**
 * bit 3 - Alert Cnt.: Alert Output Control bit; R/W-0
 * 0 = Disabled (power-up default)
 * 1 = Enabled
 * This bit can not be altered when either of the Lock bits are set (bit 6 and bit 7).
 * This bit can be programmed in Shutdown mode, but the Alert output will not assert or deassert.
 */
private const val MCO9808_CONF_ALERT_CNT_DISABLED =     0b11110111        //Default
private const val MCO9808_CONF_ALERT_CNT_ENABLED =      0b00001000

/**
 * bit 4 - Alert Stat.: Alert Output Status bit; R-0
 * 0 = Alert output is not asserted by the device (power-up default)
 * 1 = Alert output is asserted as a comparator/Interrupt or critical temperature output
 * This bit can not be set to ‘1’ or cleared to ‘0’ in Shutdown mode. However, if the Alert output
 * is configured as Interrupt mode, and if the host controller clears to ‘0’, the interrupt, using
 * bit 5 while the device is in Shutdown mode, then this bit will also be cleared ‘0’.
 */
private const val MCO9808_CONF_ALERT_STAT_DISABLED =    0b11101111        //Default
private const val MCO9808_CONF_ALERT_STAT_ENABLED =     0b00010000

/**
 * bit 5 - Int. Clear: Interrupt Clear bit; R/W-0
 * 0 = No effect (power-up default)
 * 1 = Clear interrupt output; when read, this bit returns to ‘0’
 * This bit can not be set to ‘1’ in Shutdown mode, but it can be cleared after the device enters
 * Shutdown mode.
 */
private const val MCO9808_CONF_CLEAR_INTERRUPT =        0b00100000

/**
 * bit 6 - Win. Lock: TUPPER and TLOWER Window Lock bit; R/W-0
 * 0 = Unlocked; TUPPER and TLOWER registers can be written (power-up default)
 * 1 = Locked; TUPPER and TLOWER registers can not be written
 * When enabled, this bit remains set to ‘1’ or locked until cleared by a Power-on Reset (Section 5.3 “Summary of Power-on Default”).
 * This bit can be programmed in Shutdown mode.
 */
private const val MCO9808_CONF_WIN_LOCK_UNLOCKED =      0b10111111        //Default
private const val MCO9808_CONF_WIN_LOCK_LOCKED =        0b01000000

/**
 * bit 7 - Crit. Lock: TCRIT Lock bit; R/W-0
 * 0 = Unlocked. TCRIT register can be written (power-up default)
 * 1 = Locked. TCRIT register can not be written
 * When enabled, this bit remains set to ‘1’ or locked until cleared by an internal Reset (Section
 * 5.3 “Summary of Power-on Default”).
 * This bit can be programmed in Shutdown mode.
 */
private const val MCO9808_CONF_CRIT_LOCK_UNLOCKED =     0b01111111        //Default
private const val MCO9808_CONF_CRIT_LOCK_LOCKED =       0b10000000

/**
 * bit 8 - SHDN: Shutdown Mode bit; R/W-0
 * 0 = Continuous conversion (power-up default)
 * 1 = Shutdown (Low-Power mode)
 * In shutdown, all power-consuming activities are disabled, though all registers can be written to
 * or read.
 * This bit cannot be set to ‘1’ when either of the Lock bits is set (bit 6 and bit 7). However, it
 * can be cleared to ‘0’ for continuous conversion while locked (refer to Section 5.2.1 “Shutdown
 * Mode”).
 */
private const val MCO9808_CONF_SHDN_CONTINOUS =         0b011111111        //Default
private const val MCO9808_CONF_SHDN_SHUTDOWN =          0b100000000
private const val MCO9808_CONF_SHDN_BIT_SHIFT =         8

/**
 * bit 10-9 - THYST: TUPPER and TLOWER Limit Hysteresis bits; R/W-0
 * 00 = 0°C (power-up default)
 * 01 = +1.5°C
 * 10 = +3.0°C
 * 11 = +6.0°C
 * (Refer to Section 5.2.3 “Alert Output Configuration”.)
 * This bit can not be altered when either of the Lock bits are set (bit 6 and bit 7).
 * This bit can be programmed in Shutdown mode.
 */
private const val MCO9808_CONF_HYST_BIT_SHIFT = 9
private const val MCO9808_CONF_HYST_MASK = 0x600

private const val MCO9808_REG_SIGNED_TEMP_MASK = 0x1FFF
private const val MCO9808_REG_TEMP_MASK = 0x0FFF
private const val MCO9808_REG_TEMP_SIGN_BIT = 0x1000
private const val MCO9808_REG_TEMP_SIGN_BIT_SHIFT = 0x0C

// Registers
private const val MCO9808_REG_CONF = 0x01
private const val MCO9808_REG_T_UPPER = 0x02
private const val MCO9808_REG_T_LOWER = 0x03
private const val MCO9808_REG_T_CRITICAL = 0x04
private const val MCO9808_REG_TEMP = 0x05
private const val MAO9808_REG_MANUFACTURER = 0x06
private const val MCO9808_REG_ID_REV = 0x07
private const val MCO9808_REG_RESOLUTION = 0x08

private const val TEMP_REG_FACTOR = 0.0625f
/**
 * Driver for the MCP9808 temperature sensor.
 */
class MCP9808(bus: String? = null, address: Int = DEFAULT_I2C_000_ADDRESS) : AutoCloseable {


    companion object {

        /**
         * Default I2C address for the Expander.
         */
        const val DEFAULT_I2C_000_ADDRESS = 0x18
        const val DEFAULT_I2C_001_ADDRESS = 0x19
        const val DEFAULT_I2C_010_ADDRESS = 0x1A
        const val DEFAULT_I2C_011_ADDRESS = 0x1B
        const val DEFAULT_I2C_100_ADDRESS = 0x1C
        const val DEFAULT_I2C_101_ADDRESS = 0x1D
        const val DEFAULT_I2C_110_ADDRESS = 0x1E
        const val DEFAULT_I2C_111_ADDRESS = 0x1F


        @Deprecated("")
        const val I2C_ADDRESS = DEFAULT_I2C_000_ADDRESS

        // Sensor constants from the datasheet.
        // https://cdn-shop.adafruit.com/datasheets/BST-BMP280-DS001-11.pdf
        /**
         * Minimum temperature in Celsius the sensor can measure.
         */
        const val MIN_TEMP_C = -20f
        /**
         * Maximum temperature in Celsius the sensor can measure.
         */
        const val MAX_TEMP_C = 100f
        /**
         * Maximum power consumption in micro-amperes when measuring temperature.
         */
        const val MAX_POWER_CONSUMPTION_TEMP_UA = 200f
        /**
         * Maximum frequency of the measurements.
         */
        const val MAX_FREQ_HZ = 0.03f
        /**
         * Minimum frequency of the measurements.
         */
        const val MIN_FREQ_HZ = 0.25f

        /**
         * Maximum frequency of the measurements.
         */
        const val POWER_ON_CONVERSION_DELAY = 250L
    }

    private var mDevice: I2cDevice? = null
    private val mBuffer = ByteArray(2) // for reading sensor values
    @VisibleForTesting
    internal var mConfig: Int = 0

    /**
     * THYST: TUPPER and TLOWER Limit Hysteresis bits
     * 00 = 0°C (power-up default)
     * 01 = +1.5°C
     * 10 = +3.0°C
     * 11 = +6.0°C
     */
    enum class TemperatureHysteresis(var value: Int) {
        HYST_0_0C(0),
        HYST_1_5C(1),
        HYST_3_0C(2),
        HYST_6_0C(3);
    }

    var temperatureHysteresis: TemperatureHysteresis
        /**
         * Get the hysteresis val UPPER and LOWER limit Hysteresis values.
         *
         * @return hysteresis val.
         */
        get() {
            var tmp = mConfig and MCO9808_CONF_HYST_MASK
            tmp = (tmp shr MCO9808_CONF_HYST_BIT_SHIFT)
            return when (tmp) {
                3 -> TemperatureHysteresis.HYST_6_0C
                2 -> TemperatureHysteresis.HYST_3_0C
                1 -> TemperatureHysteresis.HYST_1_5C
                else -> TemperatureHysteresis.HYST_0_0C
            }
        }
        /**
         * Set the T_HYST T_UPPER and T_LOWER Limit Hysteresis bits
         *
         * @param hyst hysteresis val.
         */
        set(hyst) {
            mConfig = mConfig and MCO9808_CONF_HYST_MASK.inv()
            mConfig = mConfig or (hyst.value shl MCO9808_CONF_HYST_BIT_SHIFT)
            writeSample16(MCO9808_REG_CONF, mConfig)
        }

    /**
     * Shutdown mode disables all power consuming activities (including temperature sampling
     * operations) while leaving the serial interface active. This mode is selected by setting bit 8
     * of CONFIG to ‘1’. In this mode, the device consumes ISHDN. It remains in this mode until bit
     * 8 is cleared to ‘0’ to enable Continuous Conversion mode or until power is recycled.
     * The Shutdown bit (bit 8) cannot be set to ‘1’ while the CONFIG<7:6> bits (Lock bits) are set
     * to ‘1’. However, it can be cleared to ‘0’ or returned to Continuous Conversion mode while
     * locked.
     * In Shutdown mode, all registers can be read or written. However, the serial bus activity
     * increases the shutdown current. In addition, if the device is in shutdown while the Alert pin
     * is asserted, the device will retain the active state during shutdown. This increases the
     * shutdown current due to the additional Alert output current.
     * */
    enum class ShutdownMode(var value: Int) {
        CONTINUOUS_MODE(0),
        SHUTDOWN_MODE(1)
    }

    var shutdownMode: Boolean
        /**
         * Check if MCP9808 is in shutdown mode where device gets into sleep mode after temp read
         *
         * @return true if sensor is in SDHN (ShutDown Mode) mode
         */
        get() {
            return 1 shl MCO9808_CONF_SHDN_BIT_SHIFT and mConfig > 0
        }
        /**
         * Set TMP102 in ShutDown Mode where device gets into sleep mode after temp read
         *
         * @param shutdown true if we want to switch to SD (ShutDown) mode
         * @throws Exception
         */
        set(shutdown) {
            mConfig = if (shutdown) {
                mConfig or (1 shl MCO9808_CONF_SHDN_BIT_SHIFT)
            } else {
                mConfig and (1 shl MCO9808_CONF_SHDN_BIT_SHIFT).inv()
            }
            writeSample16(MCO9808_REG_CONF, mConfig)
        }


    init {
        if (bus != null) {
            try {
                Timber.d("Init")
                mDevice = PeripheralManager.getInstance()?.openI2cDevice(bus, address)
                mConfig = readSample16(MCO9808_REG_CONF) ?: 0
                Timber.d("Init mConfig: $mConfig")
            } catch (e: Exception) {
                close()
                throw Exception("Error Initializing MCP9808", e)
            }
        }
    }

    /**
     * Create a new MCP9808 sensor driver connected to the given I2c device.
     *
     * @param device I2C device of the sensor.
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
    override fun close(){
        Timber.d("close started")
        try {
            mDevice?.close()
        } catch (e: Exception) {
            throw Exception("Error closing MCP9808", e)
        } finally {
            mDevice = null
            Timber.d("close finished")
        }
    }


    /**
     * Read the current temperature.
     *
     * @return the current temperature in degrees Celsius
     * @throws Exception
     */
    @Throws(Exception::class)
    fun readTemperature(): Float? = calculateTemperature(readSample16(MCO9808_REG_TEMP))

    /**
     * Read the current temperature in SD (ShutDown) mode. Callback will be triggered after temp
     * read is completed
     * @throws Exception
     */
    @Throws(Exception::class)
    fun readOneShotTemperature(onResult: (Float?)-> Unit) {
        if (shutdownMode) {
            synchronized(mBuffer) {
                GlobalScope.launch(Dispatchers.Main) {
                    //disable shutdown
                    shutdownMode = false
                    // Wait 250 ms for conversion to complete
                    delay(POWER_ON_CONVERSION_DELAY)
                    // check if conversion finished by reading OS bit to '1'
                    val temp = readTemperature()
                    Timber.d("readOneShotTemperature conversion finished temp? $temp")
                    //TODO: find better solution as some external action can reset this flag
                    //restore shutdown mode
                    shutdownMode = true
                    onResult(temp)
                }
            }
        } else {
            val temp = readTemperature()
            onResult(temp)
        }
    }


    /**
     * Reads 16 bits from the given address.
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun readSample16(register: Int): Int? {
        synchronized(mBuffer) {
            // Reading a byte buffer instead of a short to avoid having to deal with
            // platform-specific endianness.
            mDevice?.readRegBuffer(register, mBuffer, 2) ?: return null

            val msb = mBuffer[0].toInt().and(0xff)
            val lsb = mBuffer[1].toInt().and(0xff)
            return msb shl 8 or lsb
        }
    }

    @Throws(Exception::class)
    private fun writeSample16(register: Int, data: Int) {
        synchronized(mBuffer) {
            //msb
            mBuffer[0] = (data shr 8).toByte()
            mBuffer[1] = data.toByte()
            // Reading a byte buffer instead of a short to avoid having to deal with
            // platform-specific endianness.
            mDevice?.writeRegBuffer(register, mBuffer, 2)
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
        Timber.d("calculateTemperature rawTemp:$rawTemp")
        val tempRaw = rawTemp and MCO9808_REG_SIGNED_TEMP_MASK
        Timber.d("calculateTemperature tempRaw:$tempRaw")
        return if (rawTemp and MCO9808_REG_TEMP_SIGN_BIT > 0) {
            // Negative Temperature value
            (((rawTemp - 1).inv()) and MCO9808_REG_TEMP_MASK) * -TEMP_REG_FACTOR
        } else {
            // Positive temperature value
            tempRaw * TEMP_REG_FACTOR
        }
    }
}

