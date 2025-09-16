package com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver

import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import com.google.android.things.pio.I2cDevice
import com.google.android.things.pio.PeripheralManager
import timber.log.Timber
import kotlin.experimental.and

// region CTRL_REG1

/**
 * ODR2 - D6; ODR9 - D4  bits of Control register
 * Read/Write
 *
 * ODR2- ODR1 - ODR0 bits allow to change the output data rates of pressure and temperature samples.
 * The default value is “000” which corresponds to “one shot configuration” for both pressure and temperature output.
 * ODR2, ODR1 and ODR0 bits can be configured as described in Table 18.
 *
 * Measurement Data Rate:
 *
 *          Press       Temp
 *  000:    One shot    One shot
 *  001:    1Hz         1Hz
 *  010:    7Hz         1Hz
 *  011:    12.5Hz      1H
 *  100:    25Hz        1Hz
 *  101:    7Hz         7Hz
 *  110:    12.5Hz      12.5H
 *  111:    25Hz        25Hz
 */
private const val CONTROL_REG1_DATA_RATE_0 = 0          // OneShot Default
private const val CONTROL_REG1_DATA_RATE_1 = 1          // P 1Hz    T 1Hz
private const val CONTROL_REG1_DATA_RATE_2 = 2          // P 7Hz    T 1Hz
private const val CONTROL_REG1_DATA_RATE_3 = 3          // P 12.5Hz T 1Hz
private const val CONTROL_REG1_DATA_RATE_4 = 4          // P 25Hz   T 1Hz
private const val CONTROL_REG1_DATA_RATE_5 = 5          // P 7Hz    T 7Hz
private const val CONTROL_REG1_DATA_RATE_6 = 6          // P 12.5Hz T 12.5Hz
private const val CONTROL_REG1_DATA_RATE_7 = 7          // P 25Hz   T 25Hz
private const val CONTROL_REG1_DATA_RATE_MASK = 0x70
private const val CONTROL_REG1_DATA_SHIFT = 4

private const val CONTROL_REG1_SHUTDOWN_MODE_BIT_SHIFT = 7

private const val CONTROL_REG1_DIFF_INT_ENABLE_BIT_SHIFT = 3

private const val CONTROL_REG1_BDU_ENABLE_BIT_SHIFT = 2

// endregion

// region CTRL_REG2

private const val CONTROL_REG2_BOOT_MODE_BIT_SHIFT = 7

private const val CONTROL_REG2_SW_RESET_MODE_BIT_SHIFT = 2

private const val CONTROL_REG2_AUTO_ZERO_MODE_BIT_SHIFT = 1

private const val CONTROL_REG2_ONE_SHOT_MODE_BIT_SHIFT = 0

// endregion

// region STATUS_REG

private const val STATUS_REG_P_DA = 1

private const val STATUS_REG_T_DA = 0

private const val STATUS_REG_P_OR = 5

private const val STATUS_REG_T_OR = 4

// endregion

// region Registers
private const val REF_P_XL = 0x08                       // Default 00000000 RW
private const val REF_P_L = 0x09                        // Default 00000000 RW
private const val REF_P_H = 0x0A                        // Default 00000000 RW
private const val WHO_AM_I = 0x0F                       // Default 10111011 R Dummy register
private const val RES_CONF = 0x10                       // Default 011111010 RW
private const val CTRL_REG1 = 0x20                      // Default 00000000 RW
private const val CTRL_REG2 = 0x21                      // Default 00000000 RW
private const val CTRL_REG3 = 0x22                      // Default 00000000 RW
private const val INT_CFG_REG = 0x23                    // Default 00000000 RW
private const val INT_SOURCE_REG = 0x24                 // Default 00000000 R
private const val THS_P_LOW_REG = 0x25                  // Default 00000000 RW
private const val THS_P_HIGH_REG = 0x26                 // Default 00000000 RW
private const val STATUS_REG = 0x27                     // Default 00000000 R
private const val AMP_CTRL = 0x30                       // RW Partially reserved

private const val PRESS_OUT_XL = 0x28                   // R
private const val PRESS_OUT_L = 0x29                    // R
private const val PRESS_OUT_H = 0x2A                    // R
private const val TEMP_OUT_L = 0x2B                     // R
private const val TEMP_OUT_H = 0x2C                     // R

// endregion


private const val TEMP_REG_FACTOR = 0.0625f


/**
 * Driver for the Lps331 pressure and temperature sensor.
 *
 * !!! IMPORTANT !!!
 * Must be called on MainThread with all its methods
 */
@MainThread
class Lps331(bus: String? = null) : AutoCloseable {


    companion object {

        // Sensor constants from the datasheet.
        // https://www.st.com/en/mems-and-sensors/lps331ap.html

        /**
         * Minimum temperature in Celsius the sensor can measure.
         */
        const val MIN_TEMP_C = 0f


        /**
         * Maximum temperature in Celsius the sensor can measure.
         */
        const val MAX_TEMP_C = 80f


        /**
         * Minimum pressure in mbar the sensor can measure.
         */
        const val MIN_PRES = 260f


        /**
         * Maximum temperature in Celsius the sensor can measure.
         */
        const val MAX_PRES = 1260f


        /**
         * Maximum power consumption in micro-amperes when measuring temperature.
         */
        const val MAX_POWER_CONSUMPTION_TEMP_UA = 30f


        /**
         * Maximum frequency of the measurements.
         */
        const val MAX_FREQ_HZ = 25f


        /**
         * Minimum frequency of the measurements.
         */
        const val MIN_FREQ_HZ = 1f


        const val POWER_ON_CONVERSION_DELAY = 80L

        /**
         * I2C address for the Sensor.
         */
        const val DEFAULT_I2C_0_ADDRESS = 0x5C
        const val DEFAULT_I2C_1_ADDRESS = 0x5D

        const val I2C_ADDRESS = DEFAULT_I2C_1_ADDRESS

    }

    private var mDevice: I2cDevice? = null
    private val mBuffer = ByteArray(3) // for reading sensor values

    @VisibleForTesting
    internal var mConfigReg1: Int = 0
    internal var mConfigReg2: Int = 0
    internal var mConfigReg3: Int = 0

    internal var statusReg: Int = 0

    // region CTRL_REG1

    /**
     * [7] CTRL_REG1 PD: Power Down control.
     * Default value: 0
     * (0: power-down mode; 1: active mode)
     *
     * PD bit allows to turn on the device.
     * The device is in power-down mode when PD = ‘0’ (default value after boot).
     * The device is active when PD is set to ‘1’.
     */
    var powerDownMode: Boolean
        /**
         * Check if Lps331 is in Power Down mode where device gets into sleep mode after press/temp read
         *
         * @return true if sensor is in PD (PowerDown Mode) mode
         */
        @MainThread
        get() {
            return 1 shl CONTROL_REG1_SHUTDOWN_MODE_BIT_SHIFT and mConfigReg1 == 0
        }
        /**
         * Set Lps331 in ShutDown Mode where device gets into sleep mode after temp read
         *
         * @param powerDown true if we want to switch to SD (ShutDown) mode
         * @throws Exception
         */
        @MainThread
        set(powerDown) {
            mConfigReg1 = if (powerDown) {
                mConfigReg1 and (1 shl CONTROL_REG1_SHUTDOWN_MODE_BIT_SHIFT).inv()
            } else {
                mConfigReg1 or (1 shl CONTROL_REG1_SHUTDOWN_MODE_BIT_SHIFT)
            }
            writeRegister(CTRL_REG1, mConfigReg1)
        }


    /**
     * [6:4] CTRL_REG1 ODR2,ODR1,ODR0: Output Data Rate selection.
     * Default value: 00
     *
     * Measurement Data Rate:
     *          Press       Temp
     *  000:    One shot    One shot
     *  001:    1Hz         1Hz
     *  010:    7Hz         1Hz
     *  011:    12.5Hz      1H
     *  100:    25Hz        1Hz
     *  101:    7Hz         7Hz
     *  110:    12.5Hz      12.5H
     *  111:    25Hz        25Hz
     *
     *    ODR2- ODR1 - ODR0 bits allow to change the output data rates of pressure and temperature samples.
     *    The default value is “000” which corresponds to “one shot configuration” for both pressure and temperature output.
     *    ODR2, ODR1 and ODR0 bits can be configured as described in Table 18.
     *
     *    NOTE!!! Before changing the ODR it is necessary to power down the device (CTRL_REG1[7]).
     */
    enum class MeasurementDataRate(var value: Int) {
        ONE_SHOT(CONTROL_REG1_DATA_RATE_0),
        PRESS_1_TEMP_1_HZ(CONTROL_REG1_DATA_RATE_1),
        PRESS_7_TEMP_1_HZ(CONTROL_REG1_DATA_RATE_2),
        PRESS_12_5_TEMP_1_HZ(CONTROL_REG1_DATA_RATE_3),
        PRESS_25_TEMP_1_HZ(CONTROL_REG1_DATA_RATE_4),
        PRESS_7_TEMP_7_HZ(CONTROL_REG1_DATA_RATE_5),
        PRESS_12_5_TEMP_12_5_HZ(CONTROL_REG1_DATA_RATE_6),
        PRESS_25_TEMP_25_HZ(CONTROL_REG1_DATA_RATE_7),
    }

    var measurementResolution: MeasurementDataRate
        /**
         * Get the Data Rate.
         *
         * @return data rate val.
         */
        @MainThread
        get() {
            return when ((mConfigReg1 and CONTROL_REG1_DATA_RATE_MASK) shr CONTROL_REG1_DATA_SHIFT) {
                CONTROL_REG1_DATA_RATE_1 -> MeasurementDataRate.PRESS_1_TEMP_1_HZ
                CONTROL_REG1_DATA_RATE_2 -> MeasurementDataRate.PRESS_7_TEMP_1_HZ
                CONTROL_REG1_DATA_RATE_3 -> MeasurementDataRate.PRESS_12_5_TEMP_1_HZ
                CONTROL_REG1_DATA_RATE_4 -> MeasurementDataRate.PRESS_25_TEMP_1_HZ
                CONTROL_REG1_DATA_RATE_5 -> MeasurementDataRate.PRESS_7_TEMP_7_HZ
                CONTROL_REG1_DATA_RATE_6 -> MeasurementDataRate.PRESS_12_5_TEMP_12_5_HZ
                CONTROL_REG1_DATA_RATE_7 -> MeasurementDataRate.PRESS_25_TEMP_25_HZ
                else -> MeasurementDataRate.ONE_SHOT
            }
        }
        /**
         * Set Data Rate
         *
         * NOTE!! Before changing the ODR it is necessary to power down the device (CTRL_REG1[7]).
         *
         * @param resolution rate val.
         */
        @MainThread
        set(resolution) {
            mConfigReg1 = mConfigReg1 and CONTROL_REG1_DATA_RATE_MASK.inv()
            mConfigReg1 = mConfigReg1 or (resolution.value shl CONTROL_REG1_DATA_SHIFT)
            writeRegister(CTRL_REG1, mConfigReg1)
        }


    /**
     * [3] CTRL_REG1 DIFF_EN: Interrupt Circuit Enable.
     * Default value: 0
     * (0: interrupt generation disabled; 1: interrupt circuit enabled)
     *
     * DIFF_EN bit is used to enable the circuitry for the computing of differential pressure output.
     * In default mode (DIF_EN=’0’) the circuitry is turned off.
     * It is suggested to turn on the circuitry only after the configuration of REF_P_x and THS_P_x.
     */
    var diffInterruptEnable: Boolean
        /**
         * Check if diff Interrupt is enabled
         *
         * @return true if diff interrupt is enabled
         */
        @MainThread
        get() {
            return 1 shl CONTROL_REG1_DIFF_INT_ENABLE_BIT_SHIFT and mConfigReg1 > 0
        }
        /**
         * Set diff Interrupt enabled state
         *
         * @param enabled true diff Interrupt is enabled
         * @throws Exception
         */
        @MainThread
        set(enabled) {
            mConfigReg1 = if (enabled) {
                mConfigReg1 or (1 shl CONTROL_REG1_DIFF_INT_ENABLE_BIT_SHIFT)
            } else {
                mConfigReg1 and (1 shl CONTROL_REG1_DIFF_INT_ENABLE_BIT_SHIFT).inv()
            }
            writeRegister(CTRL_REG1, mConfigReg1)
        }

    /**
     * [2] CTRL_REG1 BDU: Block Data Update.
     * Default value: 0
     *
     * BDU bit is used to inhibit the output registers update between the reading of upper and lower register parts.
     * In default mode (BDU = ‘0’), the lower and upper register parts are updated continuously.
     * If it is not sure to read faster than output data rate, it is recommended to set BDU bit to ‘1’.
     * In this way, after the reading of the lower (upper) register part,
     * the content of that output registers is not updated until the upper (lower) part is read too.
     * This feature avoids reading LSB and MSB related to different samples.
     */
    var blockDataUpdateEnabled: Boolean
        /**
         * Check if block data update is enabled
         *
         * @return true if block data update is enabled
         */
        @MainThread
        get() {
            return 1 shl CONTROL_REG1_BDU_ENABLE_BIT_SHIFT and mConfigReg1 > 0
        }
        /**
         * Set block data update state
         *
         * @param enable true if we want enable block data update
         * @throws Exception
         */
        @MainThread
        set(enable) {
            mConfigReg1 = if (enable) {
                mConfigReg1 or (1 shl CONTROL_REG1_BDU_ENABLE_BIT_SHIFT)
            } else {
                mConfigReg1 and (1 shl CONTROL_REG1_BDU_ENABLE_BIT_SHIFT).inv()
            }
            writeRegister(CTRL_REG1, mConfigReg1)
        }

    // endregion

    // region CTRL_REG2

    /**
     * [7] CTRL_REG2 BOOT: Reboot memory content.
     * Default value:0
     * (0: normal mode; 1: reboot memory content)
     *
     * BOOT bit is used to refresh the content of the internal registers stored in the Flash memory block.
     * At the device power-up the content of the Flash memory block is transferred to the internal
     * registers related to trimming functions to permit a good behavior of the device itself.
     * If for any reason, the content of the trimming registers is modified,
     * it is sufficient to use this bit to restore the correct values.
     * When BOOT bit is set to ‘1’ the content of the internal Flash is copied inside
     * the corresponding internal registers and is used to calibrate the device.
     * These values are factory trimmed and they are different for every device.
     * They permit good behavior of the device and normally they should not be changed.
     * At the end of the boot process the BOOT bit is set again to ‘0’.
     * BOOT bit takes effect after one ODR clock cycle.
     */
    var bootMode: Boolean
        /**
         * Check if boot is enabled
         *
         * @return true if boot is enabled
         * @throws Exception
         */
        @MainThread
        get() {
            mConfigReg2 = readRegister(CTRL_REG2) ?: 0
            return 1 shl CONTROL_REG2_BOOT_MODE_BIT_SHIFT and mConfigReg2 > 0
        }
        /**
         * Set boot mode
         *
         * @param enable true if we want enable boot
         * @throws Exception
         */
        @MainThread
        set(enable) {
            mConfigReg2 = if (enable) {
                mConfigReg2 or (1 shl CONTROL_REG2_BOOT_MODE_BIT_SHIFT)
            } else {
                mConfigReg2 and (1 shl CONTROL_REG2_BOOT_MODE_BIT_SHIFT).inv()
            }
            writeRegister(CTRL_REG2, mConfigReg2)
        }

    /**
     * [2] CTRL_REG2 Software reset.
     * Default value:0
     * (0: normal mode; 1: software reset)
     *
     * SWRESET is the software reset bit.
     * The device is reset to the power on configuration if the SWRESET bit is set to ‘1’ and BOOT is set to ‘1’.
     */
    var swReset: Boolean
        /**
         * Check if sw reset is set
         *
         * @return true if sw reset is enabled
         * @throws Exception
         */
        @MainThread
        get() {
            mConfigReg2 = readRegister(CTRL_REG2) ?: 0
            return 1 shl CONTROL_REG2_SW_RESET_MODE_BIT_SHIFT and mConfigReg2 > 0
        }
        /**
         * Set sw reset mode
         *
         * @param enable true if sw reset is enabled
         * @throws Exception
         */
        @MainThread
        set(enable) {
            mConfigReg2 = if (enable) {
                mConfigReg2 or (1 shl CONTROL_REG2_SW_RESET_MODE_BIT_SHIFT)
            } else {
                mConfigReg2 and (1 shl CONTROL_REG2_SW_RESET_MODE_BIT_SHIFT).inv()
            }
            writeRegister(CTRL_REG2, mConfigReg2)
        }

    /**
     * [1] CTRL_REG2 Auto zero enable.
     * Default value:0
     * (0: normal mode; 1: auto zero enable)
     *
     * AUTO_ZERO, when set to ‘1’, the actual pressure output is copied in the REF_P_H & REF_P_L & REF_P_XL
     * and kept as reference and the PRESS_OUT_H & PRESS_OUT_L & PRESS _OUT_XL is the difference
     * between this reference and the pressure sensor value.
     */
    var autoZero: Boolean
        /**
         * Check if auto zero is set
         *
         * @return true if auto zero is enabled
         * @throws Exception
         */
        @MainThread
        get() {
            mConfigReg2 = readRegister(CTRL_REG2) ?: 0
            return 1 shl CONTROL_REG2_AUTO_ZERO_MODE_BIT_SHIFT and mConfigReg2 > 0
        }
        /**
         * Set auto zero mode
         *
         * @param enable true if auto zero is enabled
         * @throws Exception
         */
        @MainThread
        set(enable) {
            mConfigReg2 = if (enable) {
                mConfigReg2 or (1 shl CONTROL_REG2_AUTO_ZERO_MODE_BIT_SHIFT)
            } else {
                mConfigReg2 and (1 shl CONTROL_REG2_AUTO_ZERO_MODE_BIT_SHIFT).inv()
            }
            writeRegister(CTRL_REG2, mConfigReg2)
        }

    /**
     * [0] CTRL_REG2 One shot enable.
     * Default value:0
     * (0: waiting for start of conversion; 1: start for a new dataset)
     *
     * ONE_SHOT bit is used to start a new conversion when ODR1-ODR0 bits in CTRL_REG1 are set to “000”.
     * In this situation a single acquisition of temperature and pressure is started when ONE_SHOT bit is set to ‘1’.
     * At the end of conversion the new data are available in the output registers,
     * the STAUS_REG[0] and STAUS_REG[1] bits are set to ‘1’ and the ONE_SHOT bit comes back to ‘0’ by hardware.
     */
    var oneShotTrigger: Boolean
        /**
         * Check if one shot is set
         *
         * @return true if one shot is enabled
         * @throws Exception
         */
        @MainThread
        get() {
            mConfigReg2 = readRegister(CTRL_REG2) ?: 0
            return 1 shl CONTROL_REG2_ONE_SHOT_MODE_BIT_SHIFT and mConfigReg2 > 0
        }
        /**
         * Set one shot trigger
         *
         * @param enable true if one shot read value should be triggered
         * @throws Exception
         */
        @MainThread
        set(enable) {
            mConfigReg2 = if (enable) {
                mConfigReg2 or (1 shl CONTROL_REG2_ONE_SHOT_MODE_BIT_SHIFT)
            } else {
                mConfigReg2 and (1 shl CONTROL_REG2_ONE_SHOT_MODE_BIT_SHIFT).inv()
            }
            writeRegister(CTRL_REG2, mConfigReg2)
        }


    // endregion

    // region STATUS_REG

    /**
     * P_DA is set to 1 whenever a new pressure sample is available.
     * P_DA is cleared anytime PRESS_OUT_H (29h) register is read.
     */
    val pressureReady: Boolean
        @MainThread
        get() {
            return 1 shl STATUS_REG_P_DA and statusReg > 0
        }

    /**
     * T_DA is set to 1 whenever a new temperature sample is available.
     * T_DA is cleared anytime TEMP_OUT_H (2Bh) register is read.
     */
    val temperatureReady: Boolean
        @MainThread
        get() {
            return 1 shl STATUS_REG_T_DA and statusReg > 0
        }

    /**
     * P_OR bit is set to '1' whenever new pressure data is available and P_DA was set in the previous ODR cycle and not cleared.
     * P_OR is cleared anytime PRESS_OUT_H (29h) register is read.
     */
    val pressureOverridden: Boolean
        @MainThread
        get() {
            return 1 shl STATUS_REG_P_OR and statusReg > 0
        }

    /**
     * T_OR is set to ‘1’ whenever new temperature data is available and T_DA was set in the previous ODR cycle and not cleared.
     * T_OR is cleared anytime TEMP_OUT_H (2Bh) register is read.
     */
    val temperatureOverridden: Boolean
        @MainThread
        get() {
            return 1 shl STATUS_REG_T_OR and statusReg > 0
        }

    // endregion

    init {
        if (bus != null) {
            Timber.d("connect init")
            try {
                mDevice = PeripheralManager.getInstance()?.openI2cDevice(bus, I2C_ADDRESS)
                mConfigReg1 = readRegister(CTRL_REG1) ?: 0
                Timber.d("connect mConfigReg1: $mConfigReg1")
                mConfigReg2 = readRegister(CTRL_REG2) ?: 0
                Timber.d("connect mConfigReg2: $mConfigReg2")
                mConfigReg3 = readRegister(CTRL_REG3) ?: 0
                Timber.d("connect mConfigReg3: $mConfigReg3")
                if (powerDownMode) {
                    Timber.w("connect Sensor is powered down. Set continuous 12.5 Hz measurement mode")
                    measurementResolution = MeasurementDataRate.PRESS_25_TEMP_1_HZ
                    writeRegister(RES_CONF, 0x7A)
                    Timber.w("connect Sensor is powered down. Power it on")
                    powerDownMode = false
                } else {
                    updateDataAvailableStatus()
                    Timber.d("connect Sensor is powered up, pressureReady:$pressureReady temperatureReady:$temperatureReady pressureOverridden:$pressureOverridden temperatureOverridden:$temperatureOverridden")
                }
            } catch (e: Exception) {
                close()
                throw Exception("Error Initializing Lps331", e)
            }
        }
    }


    /**
     * Create a new Lps331 sensor driver connected to the given I2c device.
     *
     * @param device I2C device of the sensor.
     */
    @VisibleForTesting
    internal constructor(device: I2cDevice, config1: Int, config2: Int, config3: Int) : this() {
        mDevice = device
        mConfigReg1 = config1
        mConfigReg2 = config2
        mConfigReg3 = config3
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
            throw Exception("Error closing Lps331", e)
        } finally {
            mDevice = null
            Timber.d("close finished")
        }
    }

    @Throws(Exception::class)
    @MainThread
    fun reset() {
        // TODO:
        //mDevice?.writeRegByte(RESET, 0)
    }


    /**
     * Read the current temperature . Callback will be triggered after temp
     * read is completed
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    fun readOneShotTemperature(): Float? {
        Timber.d("readOneShotTemperature start")
        updateDataAvailableStatus()
        Timber.d("readOneShotTemperature, pressureReady:$pressureReady temperatureReady:$temperatureReady pressureOverridden:$pressureOverridden temperatureOverridden:$temperatureOverridden")
        val temp = readTemperature()
        Timber.d("readOneShotTemperature conversion finished temp? $temp")
        return temp
    }


    /**
     * Read the current pressure. Callback will be triggered after RH measurement is finished
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    fun readOneShotPressure(): Float? {
        Timber.d("readOneShotRh start")
        updateDataAvailableStatus()
        Timber.d("readOneShotRh, pressureReady:$pressureReady temperatureReady:$temperatureReady pressureOverridden:$pressureOverridden temperatureOverridden:$temperatureOverridden")
        val rh = readPressure()
        Timber.d("readOneShotRh conversion finished rh? $rh")
        return rh
    }


    /**
     * Read the current Pressure and Temperature. Callback will be triggered after Press measurement is finished
     * read is completed temperature measurement is also done here in the sensor
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    fun readOneShotPressAndTemp(): Pair<Float?, Float?> {
        Timber.d("readOneShotPressAndTemp start")
        updateDataAvailableStatus()
        Timber.d("readOneShotPressAndTemp, pressureReady:$pressureReady temperatureReady:$temperatureReady pressureOverridden:$pressureOverridden temperatureOverridden:$temperatureOverridden")
        val temperature = readTemperature()
        val pressure = readPressure()
        Timber.d("readOneShotPressAndTemp conversion finished pressure? $pressure temperature? $temperature")
        return Pair(pressure, temperature)
    }


    /**
     * Read the temperature.
     *
     * @return the current temperature in degrees Celsius
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    private fun readTemperature(): Float? = calculateTemperature(readRawTemperature())

    /**
     * Read the Pressure.
     *
     * @return the current pressure in mbars
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    private fun readPressure(): Float? = calculatePressure(readRawPressure())

    /**
     * Read the STATUS_REG and update statusReg.
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    private fun updateDataAvailableStatus() {
        statusReg = readRegister(STATUS_REG) ?: 0
    }


    @Throws(Exception::class)
    @MainThread
    private fun writeRegister(reg: Int, regVal: Int) = mDevice?.writeRegByte(reg, regVal.toByte())

    @Throws(Exception::class)
    @MainThread
    private fun readRegister(reg: Int): Int? = mDevice?.readRegByte(reg)?.toInt()?.and(0xff)

    /**
     * Reads 16 bits from the given address.
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    private fun readRawTemperature(): Int? {
        synchronized(mBuffer) {
            // Reading a byte buffer instead of a short to avoid having to deal with
            // platform-specific endianness.
            mDevice?.readRegBuffer(TEMP_OUT_L, mBuffer, 1) ?: return null
            val lsb = mBuffer[0].toInt().and(0xff)
            val shortLsb:Short = mBuffer[0].toShort().and(0xff)
            val lsbByte:Byte = mBuffer[0]

            mDevice?.readRegBuffer(TEMP_OUT_H, mBuffer, 1) ?: return null
            val msb = mBuffer[0].toInt().and(0xff)
            val shortMsb:Short = mBuffer[0].toShort().and(0xff)
            val msbByte:Byte = mBuffer[0]

            Timber.v("msbByte:$msbByte lsbByte:$lsbByte")
            Timber.v("shortMsb:$shortMsb shortLsb:$shortLsb")
            Timber.v("msb:$msb lsb:$lsb  all: ${msb shl 8 or lsb}")
            val shortMsbShift:Short = shortMsb shl 8
            val shortAll: Short = shortMsbShift.plus(shortLsb).toShort()
            Timber.i("shortMsbShift:$shortMsbShift shortAll:$shortAll")
            return shortAll.toInt()
        }
    }

    /**
     * Reads 24 bits from the given address.
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    private fun readRawPressure(): Int? {
        synchronized(mBuffer) {
            // Reading a byte buffer instead of a short to avoid having to deal with
            // platform-specific endianness.
            mDevice?.readRegBuffer(PRESS_OUT_XL, mBuffer, 1) ?: return null
            val lsb = mBuffer[0].toInt().and(0xff)

            mDevice?.readRegBuffer(PRESS_OUT_L, mBuffer, 1) ?: return null
            val data = mBuffer[0].toInt().and(0xff)

            mDevice?.readRegBuffer(PRESS_OUT_H, mBuffer, 1) ?: return null
            val msb = mBuffer[0].toInt().and(0xff)

            Timber.i("msb:$msb data:$data lsb:$lsb  all: ${(msb shl 16) or (data shl 8) or lsb}")
            return (msb shl 16) or (data shl 8) or lsb
        }
    }


    /**
     * Calculate real temperature in Celsius degree from Raw temp value
     *
     * @param rawTemp Raw temperature returned from Lps331 Sensor
     * @return
     */
    @VisibleForTesting
    internal fun calculateTemperature(rawTemp: Int?): Float? {
        if (rawTemp == null || rawTemp == 0) return null
        Timber.d("calculateTemperature rawTemp:$rawTemp")
        val temp: Float = 42.5f + (rawTemp / 480f)

        Timber.d("calculateTemperature temp:$temp")
        return temp
    }

    /**
     * Calculate real pressure in mbar
     *
     * @param rawPress Raw pressure returned from Lps331 Sensor
     * @return
     */
    @VisibleForTesting
    internal fun calculatePressure(rawPress: Int?): Float? {
        if (rawPress == null || rawPress == 0) return null
        Timber.d("calculatePressure rawPress:$rawPress")
        val press: Float = rawPress / 4096f

        Timber.d("calculatePress press:$press")
        return press
    }
}


infix fun Short.shl(b: Int) = (toInt() shl b.toInt()).toShort()


