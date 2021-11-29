package com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver

import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import com.google.android.things.pio.I2cDevice
import com.google.android.things.pio.PeripheralManager
import kotlinx.coroutines.delay
import timber.log.Timber

// Registers
private const val POWER_DOWN: Byte = 0b00000000           // No active state.
private const val POWER_ON: Byte = 0b00000001             // Waiting for measurement command.

/*
Reset Data register value. Reset command is not acceptable in Power Down mode.
 */
private const val RESET: Byte = 0b00000111

/*
Start measurement at 1lx resolution.
Measurement Time is typically 120ms.
 */
private const val CONT_H_RES_MODE: Byte = 0b00010000

/*
Start measurement at 0.5lx resolution.
Measurement Time is typically 120ms.
 */
private const val CONT_H_RES_MODE_2: Byte = 0b00010001

/*
Start measurement at 4lx resolution.
Measurement Time is typically 16ms.
 */
private const val CONT_L_RES_MODE: Byte = 0b00010011
/*
Start measurement at 1lx resolution.
Measurement Time is typically 120ms.
It is automatically set to Power Down mode after measurement.
 */
private const val ONE_TIME_H_RES_MODE: Byte = 0b00100000

/*
Start measurement at 0.5lx resolution.
Measurement Time is typically 120ms.
It is automatically set to Power Down mode after measurement.
 */
private const val ONE_TIME_H_RES_MODE_2: Byte = 0b00100001

/*
Start measurement at 4lx resolution.
Measurement Time is typically 16ms.
It is automatically set to Power Down mode after measurement.
 */
private const val ONE_TIME_L_RES_MODE: Byte = 0b00100011

private const val LIGHT_REG_FACTOR = 1.2f

private const val H_MODE_WAIT_TIME = 180L // ms

private const val L_MODE_WAIT_TIME = 24L // ms



/**
 * Driver for the BH1750 temperature sensor.
 *
 * !!! IPORTANT !!!
 * Must be called on MainThread with all its methods
 */
@MainThread
class BH1750(bus: String? = null) : AutoCloseable {

    companion object {

        const val DEFAULT_I2C_L_ADDRESS = 0x23
        const val DEFAULT_I2C_H_ADDRESS = 0x5C

        /**
         * I2C address for the Sensor.
         */
        const val I2C_ADDRESS = DEFAULT_I2C_L_ADDRESS

        // Sensor constants from the datasheet.
        /**
         * Minimum Light
         */
        const val MIN_LX = 1

        /**
         * Maximum temperature in Celsius the sensor can measure.
         */
        const val MAX_LX = 65535

        /**
         * Maximum power consumption in micro-amperes when measuring temperature.
         */
        const val MAX_POWER_CONSUMPTION_TEMP_UA = 190f

        /**
         * Maximum frequency of the measurements.
         */
        const val MAX_FREQ_HZ = 63f

        /**
         * Minimum frequency of the measurements.
         */
        const val MIN_FREQ_HZ = 9f

        /**
         * Maximum frequency of the measurements.
         */
        const val POWER_ON_CONVERSION_DELAY = 80L

    }

    private var mDevice: I2cDevice? = null
    private val mBuffer = ByteArray(2) // for reading sensor values

    init {
        if (bus != null) {
            Timber.d("connect init")
            try {
                mDevice = PeripheralManager.getInstance()?.openI2cDevice(bus, I2C_ADDRESS)
                mBuffer[0] = POWER_ON
                mDevice?.write(mBuffer, 1)
                Timber.d("connect")
            } catch (e: Exception) {
                close()
                throw Exception("Error Initializing Bh1750", e)
            }
        }
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
            mBuffer[0] = POWER_DOWN
            mDevice?.write(mBuffer, 1)
            mDevice?.close()
        } catch (e: Exception) {
            throw Exception("Error closing Bh1750", e)
        } finally {
            mDevice = null
            Timber.d("close finished")
        }
    }

    @Throws(Exception::class)
    @MainThread
    fun reset() {
        mBuffer[0] = RESET
        mDevice?.write(mBuffer, 1)
    }

    /**
     * Read the current Ambient Light. Callback will be triggered after temp
     * read is completed
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    suspend fun readOneShotLight(): Float? {
        Timber.d("readOneShotLight start")
        // wakeup
        mBuffer[0] = POWER_ON
        mDevice?.write(mBuffer, 1)

        delay(1)// or 10)

        mBuffer[0] = ONE_TIME_H_RES_MODE

        delay(H_MODE_WAIT_TIME)

        mDevice?.read(mBuffer, 2) ?: return null

        val msb = mBuffer[0].toInt().and(0xff)
        val lsb = mBuffer[1].toInt().and(0xff)
        val light = msb shl 8 or lsb
        Timber.d("readOneShotLight conversion finished light? $light")
        return calculateAmbientLight(light)
    }


    /**
     * Read the Light value.
     *
     * @return the current Light in degrees Celsius
     * @throws Exception
     */
    @Throws(Exception::class)
    @MainThread
    suspend fun readLight(): Float? {
        Timber.d("readLight start")
        mDevice?.write(mBuffer, 1)

        delay(1)// or 10)

        mBuffer[0] = CONT_H_RES_MODE

        delay(H_MODE_WAIT_TIME)

        mDevice?.read(mBuffer, 2) ?: return null

        val msb = mBuffer[0].toInt().and(0xff)
        val lsb = mBuffer[1].toInt().and(0xff)
        val light = msb shl 8 or lsb
        Timber.d("readLight conversion finished light? $light")
        return calculateAmbientLight(light)
    }


    /**
     * Calculate real ambient Light value in Lx from Raw temp value
     *
     * @param rawLight Raw Light returned from BH1750 Sensor
     * @return
     */
    @VisibleForTesting
    internal fun calculateAmbientLight(rawLight: Int?): Float? {
        if (rawLight == null || rawLight == 0) return null
        Timber.d("calculateAmbientLight rawLight:$rawLight")
        val tempLight: Float = rawLight / LIGHT_REG_FACTOR

        Timber.d("calculateAmbientLight tempLight:$tempLight")
        return tempLight
    }
}
