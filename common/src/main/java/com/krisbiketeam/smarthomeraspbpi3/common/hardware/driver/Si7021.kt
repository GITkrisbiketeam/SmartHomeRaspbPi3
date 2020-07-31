package com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver

import androidx.annotation.VisibleForTesting
import com.google.android.things.pio.I2cDevice
import com.google.android.things.pio.PeripheralManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * RES1 - D7; RES0 - D0  bits of Control register
 * Read/Write
 * Measurement Resolution:
 *      RH      Temp
 *  00: 12 bit  14 bit
 *  01: 8 bit   12 bit
 *  10: 10 bit  13 bit
 *  11: 11 bit  11 bit
 */
private const val CONTROL_REG_RES_12_14_BIT = 0b00000000        //Default
private const val CONTROL_REG_RES_8_11_BIT = 0b00000001
private const val CONTROL_REG_RES_10_13_BIT = 0b10000000
private const val CONTROL_REG_RES_11_11_BIT = 0b10000001
private const val CONTROL_REG_RES_MASK = 0x81

/**
 * VDDS - D6
 * Read only
 * VDD Status:
 * 0: VDD OK
 * 1: VDD Low
 */
private const val CONTROL_REG_VDD_STATUS_OK = 0b00000000        //Default
private const val CONTROL_REG_VDD_STATUS_LOW = 0b01000000
private const val CONTROL_REG_VDD_STATUS_MASK = 0b01000000

/**
 * HTRE - D2
 * Read/Write
 * 1 = On-chip Heater Enable
 * 0 = On-chip Heater Disable
 */
private const val CONTROL_REG_HEATER_ON = 0b00000100        //Default
private const val CONTROL_REG_HEATER_OFF = 0b00000000
private const val CONTROL_REG_HEATER_MASK = 0b00000100

/**
 * RES1 - D7; RES0 - D0  bits of Control register
 * Read/Write
 * Measurement Resolution:
 *      RH      Temp
 *  00: 12 bit  14 bit
 *  01: 8 bit   12 bit
 *  10: 10 bit  13 bit
 *  11: 11 bit  11 bit
 */
private const val HEATER_REG_RES_3_MA = 0b00000000        //Default
private const val HEATER_REG_RES_9_MA = 0b00000001
private const val HEATER_REG_RES_15_MA = 0b10000010
private const val HEATER_REG_RES_27_MA = 0b10000100
private const val HEATER_REG_RES_52_MA = 0b10001000
private const val HEATER_REG_RES_94_MA = 0b00001111
private const val HEATER_REG_MASK = 0b00001111

private const val MCO9808_REG_SIGNED_TEMP_MASK = 0x1FFF
private const val MCO9808_REG_TEMP_MASK = 0x0FFF
private const val MCO9808_REG_TEMP_SIGN_BIT = 0x1000
private const val MCO9808_REG_TEMP_SIGN_BIT_SHIFT = 0x0C

// Registers
private const val MEASURE_RH_HOLD_MASTER_MODE = 0xE5
private const val MEASURE_RH_NO_HOLD_MASTER_MODE = 0xF5
private const val MEASURE_TEMP_HOLD_MASTER_MODE = 0xE3
private const val MEASURE_TEMP_NO_HOLD_MASTER_MODE = 0xF3
private const val READ_TEMP_FROM_PREV_RH = 0xE0
private const val RESET = 0xFE
private const val WRITE_RH_T_USER_REG = 0xE6
private const val READ_RH_T_USER_REG = 0xE7
private const val WRITE_HEATER_REG = 0x51
private const val READ_HEATER_REG = 0x11
private const val READ_ID_1 = 0xFA // 0x0F
private const val READ_ID_2 = 0xFC // 0xC9
private const val READ_FW = 0x84 // 0xB8

private const val TEMP_REG_FACTOR = 0.0625f


/**
 * Driver for the Si7021 temperature sensor.
 */
class Si7021(bus: String? = null) : AutoCloseable {


    companion object {

        // Sensor constants from the datasheet.
        // https://www.silabs.com/documents/public/data-sheets/Si7021-A20.pdf
        /**
         * Minimum temperature in Celsius the sensor can measure.
         */
        const val MIN_TEMP_C = -40f

        /**
         * Maximum temperature in Celsius the sensor can measure.
         */
        const val MAX_TEMP_C = 85f

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
        const val MAX_POWER_CONSUMPTION_TEMP_UA = 200f

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
        const val I2C_ADDRESS = 0x40

    }

    private var mDevice: I2cDevice? = null
    private val mBuffer = ByteArray(2) // for reading sensor values

    @VisibleForTesting
    internal var mConfig: Int = 0

    /**
     * Measurement Resolution:
     *      RH      Temp
     *  00: 12 bit  14 bit
     *  01: 8 bit   12 bit
     *  10: 10 bit  13 bit
     *  11: 11 bit  11 bit
     */
    enum class MeasurementResolution(var value: Int) {
        RH_12_TEMP_14_BIT(CONTROL_REG_RES_12_14_BIT),
        RH_8_TEMP_11_BIT(CONTROL_REG_RES_8_11_BIT),
        RH_10_TEMP_13_BIT(CONTROL_REG_RES_10_13_BIT),
        RH_11_TEMP11_BIT(CONTROL_REG_RES_11_11_BIT);
    }

    var measurementResolution: MeasurementResolution
        /**
         * Get the resolution.
         *
         * @return resolution val.
         */
        get() {
            return when (mConfig and CONTROL_REG_RES_MASK) {
                CONTROL_REG_RES_11_11_BIT -> MeasurementResolution.RH_11_TEMP11_BIT
                CONTROL_REG_RES_10_13_BIT -> MeasurementResolution.RH_10_TEMP_13_BIT
                CONTROL_REG_RES_8_11_BIT -> MeasurementResolution.RH_8_TEMP_11_BIT
                else -> MeasurementResolution.RH_12_TEMP_14_BIT
            }
        }
        /**
         * Set measurement resolution
         *
         * @param resolution val.
         */
        set(resolution) {
            mConfig = mConfig and CONTROL_REG_RES_MASK.inv()
            mConfig = mConfig or resolution.value
            writeRegister(WRITE_RH_T_USER_REG, mConfig)
        }

    /**
     * VDD Status
     * */
    enum class VddStatus(var value: Int) {
        OK(CONTROL_REG_VDD_STATUS_OK),
        LOW(CONTROL_REG_VDD_STATUS_LOW)
    }

    val vddStatus: VddStatus
        /**
         * Check VddStatus , this perfoms reading control register from i2c sensor
         *
         * @return VddStatus
         */
        get() {
            mConfig = readRegister(READ_RH_T_USER_REG) ?: 0
            return when (mConfig and CONTROL_REG_VDD_STATUS_MASK) {
                CONTROL_REG_VDD_STATUS_LOW -> VddStatus.LOW
                else -> VddStatus.LOW
            }
        }


    /**
     * Heater Mode and value
     */
    var heater: Int?
        /**
         * Get the heater status value, if null then heater is off.
         *
         * @return heater val.
         */
        get() {
            return if((mConfig and CONTROL_REG_HEATER_MASK) == CONTROL_REG_HEATER_ON){
                readRegister(READ_HEATER_REG)
            } else {
                null
            }
        }
        /**
         * Set heater value if null heater will be off
         *
         * @param value  heater val.
         */
        set(value) {
            if (value == null) {
                mConfig = mConfig and CONTROL_REG_HEATER_MASK.inv()
                mConfig = mConfig or CONTROL_REG_HEATER_OFF
                writeRegister(WRITE_RH_T_USER_REG, mConfig)
            } else if (value and HEATER_REG_MASK.inv() == 0) {
                writeRegister(READ_HEATER_REG, value)
                mConfig = mConfig and CONTROL_REG_HEATER_MASK.inv()
                mConfig = mConfig or CONTROL_REG_HEATER_ON
                writeRegister(WRITE_RH_T_USER_REG, mConfig)
            }
        }


    init {
        if (bus != null) {
            Timber.d("connect init")
            try {
                mDevice = PeripheralManager.getInstance()?.openI2cDevice(bus, I2C_ADDRESS)
                mConfig = readRegister(READ_RH_T_USER_REG) ?: 0
                Timber.d("connect mConfig: $mConfig")
            } catch (e: Exception) {
                close()
                throw Exception("Error Initializing Si7021", e)
            }
        }
    }

    /**
     * Create a new Si7021 sensor driver connected to the given I2c device.
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
    override fun close() {
        Timber.d("close")
        try {
            mDevice?.close()
        } catch (e: Exception) {
            throw Exception("Error closing Si7021", e)
        } finally {
            mDevice = null
        }
    }

    @Throws(Exception::class)
    fun reset() {
        mDevice?.writeRegByte(RESET, 0)
    }

    /**
     * Read the temperature from previous RH measurement.
     *
     * @return the current temperature in degrees Celsius
     * @throws Exception
     */
    @Throws(Exception::class)
    fun readPrevTemperature(): Float? = calculateTemperature(readSample16(READ_TEMP_FROM_PREV_RH))

    /**
     * Read the current temperature . Callback will be triggered after temp
     * read is completed
     * @throws Exception
     */
    @Throws(Exception::class)
    fun readOneShotTemperature(onResult: (Float?) -> Unit) {
        synchronized(mBuffer) {
            GlobalScope.launch(Dispatchers.IO) {
                Timber.d("readOneShotTemperature start")
                val temp = readTemperature()
                Timber.d("readOneShotTemperature conversion finished temp? $temp")
                onResult(temp)
            }
        }
    }

    /**
     * Read the current RH . Callback will be triggered after RH measurement is finished
     * read is completed temperature measurement is also done here in the sensor and value can be read by readPrevTemperature
     * @throws Exception
     */
    @Throws(Exception::class)
    fun readOneShotRh(onResult: (Float?) -> Unit) {
        synchronized(mBuffer) {
            GlobalScope.launch(Dispatchers.IO) {
                Timber.d("readOneShotRh start")
                val rh = readRH()
                Timber.d("readOneShotRh conversion finished rh? $rh")
                onResult(rh)
            }
        }
    }


    /**
     * Read the temperature.
     *
     * @return the current temperature in degrees Celsius
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun readTemperature(): Float? = calculateTemperature(readSample16(MEASURE_TEMP_HOLD_MASTER_MODE))

    /**
     * Read the RH.
     *
     * @return the current temperature in degrees Celsius
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun readRH(): Float? = calculateRh(readSample16(MEASURE_RH_HOLD_MASTER_MODE))

    @Throws(Exception::class)
    private fun readRegister(reg: Int): Int? = mDevice?.readRegByte(reg)?.toInt()?.and(0xff)

    @Throws(Exception::class)
    private fun writeRegister(reg: Int, regVal: Int) = mDevice?.writeRegByte(reg, regVal.toByte())

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
        var tempRaw: Float = rawTemp * 175.72f
        tempRaw /= 65536
        tempRaw -= 46.85f

        Timber.d("calculateTemperature tempRaw:$tempRaw")
        return tempRaw
    }

    /**
     * Calculate real rh in percentage value from 0 to 100
     *
     * @param rawRh Raw RH returned from Si7021 Sensor
     * @return
     */
    @VisibleForTesting
    internal fun calculateRh(rawRh: Int?): Float? {
        if (rawRh == null) return null
        Timber.d("calculateRh rawRh:$rawRh")
        var rhRaw: Float = rawRh * 125f
        rhRaw /= 65536
        rhRaw -= 6

        Timber.d("calculateRh rhRaw:$rhRaw")
        return if (rhRaw > 100) 100f else rhRaw
    }
}
