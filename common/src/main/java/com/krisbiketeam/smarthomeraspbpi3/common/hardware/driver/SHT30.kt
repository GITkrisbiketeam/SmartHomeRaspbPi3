package com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver

import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import com.google.android.things.pio.I2cDevice
import com.google.android.things.pio.PeripheralManager
import kotlinx.coroutines.delay
import timber.log.Timber

private const val STATUS_REG = 0xF32D
private const val CLEAR_STATUS_REG = 0x3041


/**
 * 15 Alert pending status
 *     '0': no pending alerts
 *     '1': at least one pending alert
 *
 *     Default value - ‘1’
 *
 * 14 Reserved ‘0’
 *
 * 13 Heater status
 *     ‘0’ : Heater OFF
 *     ‘1’ : Heater ON
 *
 *     Default value - ‘0’
 *
 * 12 Reserved ‘0’
 *
 * 11 RH tracking alert
 *     ‘0’ : no alert
 *     ‘1’ . alert
 *
 *     Default value - ‘0
 *
 * 10 T tracking alert
 *     ‘0’ : no alert
 *     ‘1’ . alert
 *
 *     Default value - ‘0’
 *
 * 9:5 Reserved ‘xxxxx’
 *
 * 4 System reset detected
 *     '0': no reset detected since last ‘clear status register’ command
 *     '1': reset detected (hard reset, soft reset command or supply fail)
 *
 *     Default value - ‘1’
 *
 * 3:2 Reserved ‘00’
 *
 * 1 Command status
 *     '0': last command executed successfully
 *     '1': last command not processed. It was either invalid, failed the integrated command checksum
 *
 *     Default value - ‘0’
 *
 * 0 Write data checksum status
 *     '0': checksum of last write transfer was correct
 *     '1': checksum of last write transfer failed
 *
 *     Default value - ‘0’
 */
private const val STATUS_REGISTER_ALERT_PENDING_STATUS_NO = 0b00000000
private const val STATUS_REGISTER_ALERT_PENDING_STATUS_PENDING = 0b1000000000000000
private const val STATUS_REGISTER_ALERT_PENDING_STATUS_MASK = 0x8000
private const val STATUS_REGISTER_HEATER_STATUS_OFF = 0b00000000
private const val STATUS_REGISTER_HEATER_STATUS_ON = 0b10000000000000
private const val STATUS_REGISTER_HEATER_STATUS_MASK = 0x1000
private const val STATUS_REGISTER_RH_TRACK_ALERT_STATUS_NO_ALERT = 0b00000000
private const val STATUS_REGISTER_RH_TRACK_ALERT_STATUS_ALERT = 0b100000000000
private const val STATUS_REGISTER_RH_TRACK_ALERT_STATUS_MASK = 0x300
private const val STATUS_REGISTER_T_TRACK_ALERT_STATUS_NO_ALERT = 0b0000000000
private const val STATUS_REGISTER_T_TRACK_ALERT_STATUS_ALERT = 0b10000000000
private const val STATUS_REGISTER_T_TRACK_ALERT_STATUS_MASK = 0x200
private const val STATUS_REGISTER_RESET_DETECTED_STATUS_OK = 0b00000000
private const val STATUS_REGISTER_RESET_DETECTED_STATUS_ERROR = 0b00010000
private const val STATUS_REGISTER_RESET_DETECTED_STATUS_MASK = 0x10
private const val STATUS_REGISTER_COMMAND_EXEC_STATUS_OK = 0b00000000
private const val STATUS_REGISTER_COMMAND_EXEC_STATUS_ERROR = 0b00000010
private const val STATUS_REGISTER_COMMAND_EXEC_STATUS_MASK = 0x02
private const val STATUS_REGISTER_WRITE_DATA_CHECKSUM_STATUS_OK = 0b00000000
private const val STATUS_REGISTER_WRITE_DATA_CHECKSUM_STATUS_ERROR = 0b00000001
private const val STATUS_REGISTER_WRITE_DATA_CHECKSUM_STATUS_MASK = 0x01

/**
 * Heater
 */
private const val CONTROL_REG_HEATER_ON = 0x306D
private const val CONTROL_REG_HEATER_OFF = 0x3066


// Registers
private const val MEASURE_TEMP_RH_CLK_STRETCHING_HIGH_REPEATABILITY_MODE = 0x2C06
private const val MEASURE_TEMP_RH_CLK_STRETCHING_MED_REPEATABILITY_MODE = 0x2C0D
private const val MEASURE_TEMP_RH_CLK_STRETCHING_LOW_REPEATABILITY_MODE = 0x2C10

private const val MEASURE_TEMP_RH_CLK_STRETCHING_OFF_HIGH_REPEATABILITY_MODE = 0x2400
private const val MEASURE_TEMP_RH_CLK_STRETCHING_OFF_MED_REPEATABILITY_MODE = 0x240B
private const val MEASURE_TEMP_RH_CLK_STRETCHING_OFF_LOW_REPEATABILITY_MODE = 0x2416

private const val MEASURE_TEMP_RH_PERIODIC_HIGH_REPEATABILITY_0_5_MPS_MODE = 0x2032
private const val MEASURE_TEMP_RH_PERIODIC_MED_REPEATABILITY_0_5_MPS_MODE = 0x2024
private const val MEASURE_TEMP_RH_PERIODIC_LOW_REPEATABILITY_0_5_MPS_MODE = 0x202F

private const val MEASURE_TEMP_RH_PERIODIC_HIGH_REPEATABILITY_1_MPS_MODE = 0x2130
private const val MEASURE_TEMP_RH_PERIODIC_MED_REPEATABILITY_1_MPS_MODE = 0x2126
private const val MEASURE_TEMP_RH_PERIODIC_LOW_REPEATABILITY_1_MPS_MODE = 0x212D

private const val MEASURE_TEMP_RH_PERIODIC_HIGH_REPEATABILITY_2_MPS_MODE = 0x2236
private const val MEASURE_TEMP_RH_PERIODIC_MED_REPEATABILITY_2_MPS_MODE = 0x2220
private const val MEASURE_TEMP_RH_PERIODIC_LOW_REPEATABILITY_2_MPS_MODE = 0x222B

private const val MEASURE_TEMP_RH_PERIODIC_HIGH_REPEATABILITY_4_MPS_MODE = 0x2334
private const val MEASURE_TEMP_RH_PERIODIC_MED_REPEATABILITY_4_MPS_MODE = 0x23322
private const val MEASURE_TEMP_RH_PERIODIC_LOW_REPEATABILITY_4_MPS_MODE = 0x2329

private const val MEASURE_TEMP_RH_PERIODIC_HIGH_REPEATABILITY_10_MPS_MODE = 0x237
private const val MEASURE_TEMP_RH_PERIODIC_MED_REPEATABILITY_10_MPS_MODE = 0x2721
private const val MEASURE_TEMP_RH_PERIODIC_LOW_REPEATABILITY_10_MPS_MODE = 0x272A

private const val MEASURE_TEMP_RH_PERIODIC_ART_MODE = 0x2B32

// Stop periodic measurement and switch to single shot mode
private const val MEASURE_TEMP_RH_BREAK_PERIODIC_MODE = 0x2B32

private const val MEASURE_TEMP_RH_PERIODIC_READ_DATA = 0xE000

private const val RESET = 0x30A2


/**
 * Driver for the SHT30 temperature sensor.
 *
 * !!! IPORTANT !!!
 * Must be called on MainThread with all its methods
 */
@MainThread
class SHT30(bus: String? = null, address: Int = I2C_ADDRESS_DEFAULT) : AutoCloseable {


    companion object {

        // Sensor constants from the datasheet.
        // https://
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
        const val I2C_ADDRESS_DEFAULT = 0x44
        const val I2C_ADDRESS_ALTERNATIVE = 0x45

    }

    private var mDevice: I2cDevice? = null
    private val mBuffer = ByteArray(6) // for reading sensor values

    @VisibleForTesting
    internal var mStatus: StatusRegister? = null

    internal data class StatusRegister(
        val alertPendingStatus: Boolean = false,
        val heaterStatus: Boolean = false,
        val rhTrackAlertStatus: Boolean = false,
        val tTrackAlertStatus: Boolean = false,
        val resetDetectedStatus: Boolean = false,
        val commandExecStatus: Boolean = false,
        val writeDataChecksumStatus: Boolean = false,
    )

    init {
        if (bus != null) {
            Timber.d("connect init")
            try {
                mDevice = PeripheralManager.getInstance()?.openI2cDevice(bus, address)
                mStatus = readSample16CRC(STATUS_REG)?.let { status ->
                    parseStatusRegister(status)
                }
                Timber.d("connect mStatus: $mStatus")
            } catch (e: Exception) {
                close()
                throw Exception("Error Initializing SHT30", e)
            }
        }
    }

    /**
     * Create a new SHT30 sensor driver connected to the given I2c device.
     *
     * @param device I2C device of the sensor.
     */
    @VisibleForTesting
    internal constructor(device: I2cDevice) : this() {
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
            throw Exception("Error closing SHT30", e)
        } finally {
            mDevice = null
            Timber.d("close finished")
        }
    }

    @Throws(Exception::class)
    @MainThread
    suspend fun reset() {
        writeCommand(RESET)

        delay(5)

        mStatus = readSample16CRC(STATUS_REG)?.let { status ->
            parseStatusRegister(status)
        }
        Timber.d("connect mStatus: $mStatus")
    }

    fun heaterOnOff(on: Boolean) {
        Timber.i("heaterOnOff $on")
        if (on) {
            writeCommand(CONTROL_REG_HEATER_ON)
        } else {
            writeCommand(CONTROL_REG_HEATER_OFF)
        }

        //delay(1)

        mStatus = readSample16CRC(STATUS_REG)?.let { status ->
            parseStatusRegister(status)
        }
        Timber.d("connect mStatus: $mStatus")
    }

    fun clearStatusRegister() {
        Timber.d("clearStatusRegister mStatus: $mStatus")
        writeCommand(CLEAR_STATUS_REG)
    }

    /**
     * Turn on/off periodic measurement mode, by default we set 0.5 mps and medium repeatability
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    fun turnOnOfPeriodicMode(on: Boolean) {
        Timber.i("turnOnOfPeriodicMode start")

        if (on) {
            writeCommand(MEASURE_TEMP_RH_PERIODIC_MED_REPEATABILITY_0_5_MPS_MODE)
        } else {
            writeCommand(MEASURE_TEMP_RH_BREAK_PERIODIC_MODE)
        }

        //delay(1)

        mStatus = readSample16CRC(STATUS_REG)?.let { status ->
            parseStatusRegister(status)
        }
        Timber.d("connect mStatus: $mStatus")
    }

    /**
     * Read the current RH and Temperature associated with this RH measurement in Periodic mode
     * Function may throw exception if on measurement is available yet or had been already read.
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    suspend fun readPeriodicTempAndRh(): Pair<Float?, Float?> {
        Timber.i("readPeriodicTempAndRh start")

        // this may fail if periodic mode is not enabled NACK from i2c device
        val (rawTemperature, rawRh) = readSampleTwo16CRC(MEASURE_TEMP_RH_PERIODIC_READ_DATA)?: Pair(null, null)

        Timber.d("readPeriodicTempAndRh conversion finished rawTemperature? $rawTemperature rawRh? $rawRh")

        val temperature = calculateTemperature(rawTemperature)
        val rh = calculateRh(rawRh)

        Timber.d("readPeriodicTempAndRh calculated temperature? $temperature rh? $rh")
        return Pair(temperature, rh)
    }

    /**
     * Read the current RH and Temperature associated with this RH measurement.
     * Function will suspend until measurement is finished.
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    suspend fun readOneShotTempAndRh(): Pair<Float?, Float?> {
        Timber.i("readOneShotTempAndRh start")

        val (rawTemperature, rawRh) = readSampleTwo16CRC(MEASURE_TEMP_RH_CLK_STRETCHING_OFF_MED_REPEATABILITY_MODE)?: Pair(null, null)

        Timber.d("readOneShotTempAndRh conversion finished rawTemperature? $rawTemperature rawRh? $rawRh")

        val temperature = calculateTemperature(rawTemperature)
        val rh = calculateRh(rawRh)

        Timber.d("readOneShotTempAndRh calculated temperature? $temperature rh? $rh")
        return Pair(temperature, rh)
    }

    @Throws(Exception::class)
    @MainThread
    private fun writeCommand(command: Int) {
        val msb = (command shr 8).toByte()
        val lsb = (command shr 0).toByte()
        val crc: Byte = sht30Crc8(ubyteArrayOf(msb.toUByte(), lsb.toUByte())).toByte()

        // wakeup
        mDevice?.write(byteArrayOf(msb, lsb, crc), 3)
    }

    /**
     * Reads 16 bits from the given address.
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    private fun readSample16CRC(command: Int): Int? {
        Timber.d("readSample16CRC")
        val commandMsb = (command shr 8).toByte()
        val commandLsb = (command shr 0).toByte()
        val commandCrc: Byte =
            sht30Crc8(ubyteArrayOf(commandMsb.toUByte(), commandLsb.toUByte())).toByte()

        // wakeup
        mDevice?.write(byteArrayOf(commandMsb, commandLsb, commandCrc), 3)

        mDevice?.read(mBuffer, 3) ?: return null

        val sensorMsb: Int = mBuffer[0].toInt().and(0xff)
        val sensorLsb: Int = mBuffer[1].toInt().and(0xff)
        val sensorCrc: Int = mBuffer[2].toInt().and(0xff)

        val sht30CalcCrc = sht30Crc8(ubyteArrayOf(sensorMsb.toUByte(), sensorLsb.toUByte()))

        return if (sht30CalcCrc == sensorCrc.toUByte()) {
            sensorMsb shl 8 or sensorLsb
        } else {
            null
        }
    }

    @Throws(Exception::class)
    @MainThread
    private suspend fun readSampleTwo16CRC(command: Int): Pair<Int, Int>? {
        Timber.d("readSampleTwo16CRC")
        val commandMsb = (command shr 8).toByte()
        val commandLsb = (command shr 0).toByte()
        val commandCrc: Byte =
            sht30Crc8(ubyteArrayOf(commandMsb.toUByte(), commandLsb.toUByte())).toByte()

        // wakeup
        mDevice?.write(byteArrayOf(commandMsb, commandLsb, commandCrc), 3)

        // The three repeatability modes differ with respect to measurement duration, noise level
        // and energy consumption.
        // Low repeatability    - typ - 2.5 ms max - 4 ms
        // Medium repeatability - typ - 4.5 ms max - 6 ms
        // High repeatability - typ - 12.5 ms max - 15 ms

        when (command) {
            MEASURE_TEMP_RH_CLK_STRETCHING_OFF_HIGH_REPEATABILITY_MODE -> delay(15)
            MEASURE_TEMP_RH_CLK_STRETCHING_OFF_MED_REPEATABILITY_MODE -> delay(6)
            MEASURE_TEMP_RH_CLK_STRETCHING_OFF_LOW_REPEATABILITY_MODE -> delay(4)
            else -> Unit    // Do nothing (stretching(dangerous) or periodic mode)
        }

        mDevice?.read(mBuffer, 6) ?: return null

        val sensorTempMsb: Int = mBuffer[0].toInt().and(0xff)
        val sensorTempLsb: Int = mBuffer[1].toInt().and(0xff)
        val sensorTempCrc: Int = mBuffer[2].toInt().and(0xff)

        val sht30CalcTempCrc =
            sht30Crc8(ubyteArrayOf(sensorTempMsb.toUByte(), sensorTempLsb.toUByte()))

        val sensorRhMsb: Int = mBuffer[0].toInt().and(0xff)
        val sensorRhLsb: Int = mBuffer[1].toInt().and(0xff)
        val sensorRhCrc: Int = mBuffer[2].toInt().and(0xff)

        val sht30CalcRhCrc = sht30Crc8(ubyteArrayOf(sensorRhMsb.toUByte(), sensorRhLsb.toUByte()))

        return if (sht30CalcTempCrc == sensorTempCrc.toUByte() && sht30CalcRhCrc == sensorRhCrc.toUByte()) {
            (sensorTempMsb shl 8 or sensorTempLsb) to (sensorRhMsb shl 8 or sensorRhLsb)
        } else {
            null
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
        if (rawTemp == null || rawTemp == 0) return null
        Timber.d("calculateTemperature rawTemp:$rawTemp")
        var tempRaw: Float = rawTemp * 175.0f
        tempRaw /= 65535
        tempRaw -= 45.0f

        Timber.d("calculateTemperature tempRaw:$tempRaw")
        return tempRaw
    }

    /**
     * Calculate real rh in percentage value from 0 to 100
     *
     * @param rawRh Raw RH returned from SHT30 Sensor
     * @return
     */
    @VisibleForTesting
    internal fun calculateRh(rawRh: Int?): Float? {
        if (rawRh == null || rawRh == 0) return null
        Timber.d("calculateRh rawRh:$rawRh")
        var rhRaw: Float = rawRh * 100f
        rhRaw /= 65535

        Timber.d("calculateRh rhRaw:$rhRaw")
        /*return when {
            rhRaw > 100 -> 100f
            rhRaw < 0 -> 0f
            else -> rhRaw

        }*/
        return rhRaw
    }

    private fun parseStatusRegister(value: Int): StatusRegister {
        return StatusRegister(
            alertPendingStatus = (value and STATUS_REGISTER_ALERT_PENDING_STATUS_MASK) != 0,
            heaterStatus = (value and STATUS_REGISTER_HEATER_STATUS_MASK) != 0,
            rhTrackAlertStatus = (value and STATUS_REGISTER_RH_TRACK_ALERT_STATUS_MASK) != 0,
            tTrackAlertStatus = (value and STATUS_REGISTER_T_TRACK_ALERT_STATUS_MASK) != 0,
            resetDetectedStatus = (value and STATUS_REGISTER_RESET_DETECTED_STATUS_MASK) != 0,
            commandExecStatus = (value and STATUS_REGISTER_COMMAND_EXEC_STATUS_MASK) != 0,
            writeDataChecksumStatus = (value and STATUS_REGISTER_WRITE_DATA_CHECKSUM_STATUS_MASK) != 0,
        )
    }
}

@ExperimentalUnsignedTypes
internal fun sht30Crc8(data: UByteArray): UByte {
    var crc: UShort = 0xffu
    for (i in 0 until data.size) {
        crc = crc xor data[i].toUShort()
        for (j in 8 downTo 1) {
            if ((crc and 0x80u) > 0u) {
                crc = (crc shl 1) xor 0x131u
            } else {
                crc = (crc shl 1)
            }
        }
    }

    return crc.toUByte() //!= check.toUShort()
}

private infix fun UShort.shl(bitCount: Int): UShort =
    (this.toUInt() shl bitCount).toUShort()

