package com.krisbiketeam.smarthomeraspbpi3.units

import androidx.annotation.Keep
import androidx.annotation.MainThread
import com.google.android.things.pio.I2cDevice
import com.google.android.things.pio.PeripheralManager
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Bme680Data
import com.krisbiketeam.smarthomeraspbpi3.common.toHex
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

class Bme680BsecJNI(private val scope: CoroutineScope, bus: String? = null,
                    address: Int = BoardConfig.AIR_QUALITY_SENSOR_BME680_ADDR, shortDelay: Boolean,
                    private val resultCallback: suspend (Bme680Data) -> Unit) : AutoCloseable {

    private var mDevice: I2cDevice? = null
    private val mDeviceMutex = Mutex()

    init {
        if (bus != null) {
            Timber.d("connect init")
            try {
                // Currently i2c device is managed by native library
                mDevice = PeripheralManager.getInstance()?.openI2cDevice(bus, address)
                initBme680JNI(shortDelay)
                Timber.d("connected")
            } catch (e: Exception) {
                throw Exception("Error openining i2c device $address", e)
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
        runBlocking(scope.coroutineContext) {
            closeBme680JNI()
            mDeviceMutex.withLock {
                try {
                    mDevice?.close()
                } catch (e: Exception) {
                    throw Exception("Error closing Si7021", e)
                } finally {
                    mDevice = null
                    Timber.d("close finished")
                }
            }
        }
    }


    private external fun initBme680JNI(short_delay: Boolean)

    private external fun closeBme680JNI()

    @ExperimentalUnsignedTypes
    @Keep
    fun readRegister(register: Int, regDataBuffer: ByteArray, dataLen: Int): Int {
        Timber.w("readRegister ${regDataBuffer.toHex()}")
        return runBlocking(scope.coroutineContext) {
            withContext(Dispatchers.Main) {
                mDeviceMutex.withLock {
                    mDevice?.run {
                        readRegBuffer(register, regDataBuffer, dataLen)
                        Timber.w("readRegister register:0x${Integer.toHexString(register)} dataLen:$dataLen ${regDataBuffer.toHex()}")
                        0 // OK
                    } ?: run {
                        Timber.w("Sensor I2C already closed, no readRegister")
                        -1
                    }
                }
            }
        }
    }

    @Keep
    fun writeRegister(register: Int, regDataBuffer: ByteArray, dataLen: Int): Int {
        return runBlocking(scope.coroutineContext) {
            withContext(Dispatchers.Main) {
                mDeviceMutex.withLock {
                    mDevice?.run {
                        Timber.w("writeRegister register:0x${Integer.toHexString(register)} dataLen:$dataLen ${regDataBuffer.toHex()}")
                        mDevice?.writeRegBuffer(register, regDataBuffer, dataLen)
                        0 // OK
                    } ?: run {
                        Timber.w("Sensor I2C already closed, no writeRegister")
                        -1
                    }
                }
            }
        }
    }

    @Keep
    fun sleep(delay: Int) {
        Timber.d("sleep delay:$delay")
        if (mDevice != null) {
            runBlocking(scope.coroutineContext) {
                delay(delay.toLong())
            }
        } else {
            Timber.w("Sensor I2C already closed, no sleep")
        }
    }

    @Keep
    fun outputReady(timestamp: Long, iaq: Float, iaq_accuracy: Int, temperature: Float, humidity: Float,
                    pressure: Float, rawTemperature: Float, rawHumidity: Float, gas: Float,
                    bsec_status: Int,
                    staticIaq: Float, co2Equivalent: Float, breathVocEquivalent: Float) {
        Timber.w("outputReady timestamp:$timestamp\n" +
                "    iaq:$iaq\n" +
                "    iaq_accuracy:$iaq_accuracy\n" +
                "    temperature:$temperature\n" +
                "    humidity:$humidity\n" +
                "    pressure:$pressure\n" +
                "    rawTemperature:$rawTemperature\n" +
                "    rawHumidity:$rawHumidity\n" +
                "    gas:$gas\n" +
                "        bsec_status:$bsec_status\n" +
                "    staticIaq:$staticIaq\n" +
                "    co2Equivalent:$co2Equivalent\n" +
                "    breathVocEquivalent:$breathVocEquivalent\n")

        CoroutineScope(scope.coroutineContext).launch {
            resultCallback(Bme680Data(timestamp, iaq, iaq_accuracy, temperature, humidity,
                    pressure, rawTemperature, rawHumidity, gas,
                    bsec_status,
                    staticIaq, co2Equivalent, breathVocEquivalent))
        }
    }

    @Keep
    fun bsecInitResult(result: Int) {
        Timber.w("bsecInitResult result:$result")
    }

    @Keep
    fun bsecFinished() {
        Timber.w("bsecFinished")
    }
}