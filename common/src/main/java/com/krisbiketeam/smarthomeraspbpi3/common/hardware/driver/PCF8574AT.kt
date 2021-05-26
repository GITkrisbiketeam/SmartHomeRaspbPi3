package com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver

import android.annotation.SuppressLint
import android.view.ViewConfiguration
import androidx.annotation.MainThread
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.GpioCallback
import com.google.android.things.pio.I2cDevice
import com.google.android.things.pio.PeripheralManager
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.PCF8574ATPin.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Driver for the MCP23017 16 bit I/O Expander.
 *
 * !!! IPORTANT !!!
 * Must be called on MainThread with all its methods
 */
@MainThread
class PCF8574AT(private val bus: String? = null,
                private val address: Int = DEFAULT_I2C_000_ADDRESS,
                var intGpio: String? = null,
                private var pollingTime: Int = NO_POLLING_TIME,
                private val debounceDelay: Int = NO_DEBOUNCE_DELAY) : AutoCloseable {


    companion object {

        /**
         * Default I2C address for the Expander.
         */
        const val DEFAULT_I2C_000_ADDRESS = 0x38
        const val DEFAULT_I2C_001_ADDRESS = 0x39
        const val DEFAULT_I2C_010_ADDRESS = 0x3A
        const val DEFAULT_I2C_011_ADDRESS = 0x3B
        const val DEFAULT_I2C_100_ADDRESS = 0x3C
        const val DEFAULT_I2C_101_ADDRESS = 0x3D
        const val DEFAULT_I2C_110_ADDRESS = 0x3E
        const val DEFAULT_I2C_111_ADDRESS = 0x3F

        const val DEFAULT_POLLING_TIME = 50
        const val NO_POLLING_TIME = -1

        val DEBOUNCE_DELAY = ViewConfiguration.getTapTimeout()
        const val NO_DEBOUNCE_DELAY = 0
    }

    private var currentStates = 0
    private var currentDirections = 0

    private var mGpioInt: Gpio? = null

    private val mListeners = ConcurrentHashMap<Pin, MutableList<PCF8574ATPinStateChangeListener>>()

    private var monitorJob: Job? = null

    private var debounceIntCallbackJob: Job? = null

    private val mIntCallback = GpioCallback{ gpio: Gpio ->
        if (debounceDelay != NO_DEBOUNCE_DELAY) {
            Timber.d("mIntCallback onGpioEdge ${gpio.value}")
            debounceIntCallbackJob?.cancel()
            debounceIntCallbackJob = GlobalScope.launch(Dispatchers.IO) {
                delay(debounceDelay.toLong())
                try {
                    if (this.isActive) {
                        withContext(Dispatchers.Main) {
                            checkInterrupt()
                        }
                    }
                } finally {
                    debounceIntCallbackJob?.cancel()
                }
            }
        } else {
            debounceIntCallbackJob?.cancel()
            GlobalScope.launch(Dispatchers.Main) {
                checkInterrupt()
            }
        }

        // Return true to keep callback active.
        true
    }

    init {
        if (bus != null) {
            try {
                lockedI2cOperation {
                    // read initial GPIO pin states
                    currentStates = readRegister(it)

                    Timber.d("connectI2c currentStates: $currentStates")
                    resetToDefaults(it)
                }
            } catch (e: Exception) {
                Timber.e(e,"init error connecting I2C")
                close()
                throw (Exception("Error init PCF8574AT", e))
            }
        }
    }

    /**
     * Close the driver and the underlying device.
     */
    @Throws(Exception::class)
    @MainThread
    override fun close() {
        Timber.d("close started")
        // if a monitor is running, then shut it down now
        stopMonitor()

        mGpioInt?.unregisterGpioCallback(mIntCallback)
        try {
            mGpioInt?.close()
        } catch (e: Exception){
            Timber.e("close mGpioInt exception: $e")
            throw (Exception("Error closing PCF8574AT mGpioInt", e))
        } finally {
            mGpioInt = null
            Timber.d("close finished")
        }
    }

    @MainThread
    private fun lockedI2cOperation(block: (I2cDevice?) -> Unit) {
        synchronized(this) {
            PeripheralManager.getInstance()?.openI2cDevice(bus, address).use {
                block(it)
            }
        }
    }

    @Throws(Exception::class)
    @MainThread
    private fun resetToDefaults(i2cDevice: I2cDevice?) {
        writeRegister(i2cDevice, 0xFF)
    }

    // region Input interrupt handling

    @Throws(Exception::class)
    @MainThread
    private fun connectGpio() {
        if (intGpio != null && mGpioInt == null) {
            val manager = PeripheralManager.getInstance()
            //Mirror IntA and IntB pins to single Interrupt from either A or B ports
            Timber.d("connectGpio intGpio openGpio $intGpio")
            // Step 1. Create GPIO connection.
            mGpioInt = manager.openGpio(intGpio)
            // Step 2. Configure as an input.
            mGpioInt?.setDirection(Gpio.DIRECTION_IN)
            // Step 3. Enable edge trigger events.
            mGpioInt?.setEdgeTriggerType(Gpio.EDGE_FALLING)    // INT active Low
            // Step 4. Register an event callback.
            mGpioInt?.registerGpioCallback(mIntCallback)
        }
    }

    @Throws(Exception::class)
    @MainThread
    private fun disconnectGpio() {
        Timber.d("disconnect int Gpio ")
        mGpioInt = mGpioInt?.run {
            unregisterGpioCallback(mIntCallback)
            close()
            null
        }
    }

    @Throws(Exception::class)
    private fun startMonitor(){
        if (pollingTime != NO_POLLING_TIME) {
            // if the monitor has not been started, then start it now
            monitorJob = GlobalScope.launch(Dispatchers.IO) {
                // We could also check for true as suspending delay() method is cancellable
                while (isActive) {
                    try {
                        withContext(Dispatchers.Main) {
                            checkInterrupt()
                        }
                        delay(pollingTime.toLong())
                    } catch (e: Exception) {
                        stopMonitor()
                        throw (Exception("Error startMonitor checkInterrupt PCF8574AT", e))
                    }
                }
            }
        } else {
            stopMonitor()
        }
    }

    private fun stopMonitor(){
        // cancel and null monitoring Job since there are no input pins configured
        monitorJob?.cancel()
        monitorJob = null
    }

    // endregion

    // region Read/Write Register functions

    @Throws(Exception::class)
    @MainThread
    private fun readRegister(i2cDevice: I2cDevice?): Int {
        val byteArray = ByteArray(1)
        i2cDevice?.read(byteArray, 1)
        return byteArray[0].toInt().and(0xff)
    }

    @Throws(Exception::class)
    @MainThread
    private fun writeRegister(i2cDevice: I2cDevice?, regVal: Int) {
        val buffer = ByteArray(1)
        buffer[0] = regVal.toByte()
        i2cDevice?.write(buffer, 1)
    }

    // endregion

    // region Set Input or output mode functions

    @Throws(Exception::class)
    @MainThread
    fun setMode(pin: Pin, mode: PinMode) {
        // determine register and pin address
        val pinAddress = pin.address

        // determine update direction value based on mode
        currentDirections = if (mode == PinMode.DIGITAL_INPUT) {
            currentDirections or pinAddress
        } else {
            currentDirections and pinAddress.inv()
        }
        Timber.d("setMode currentDirections: $currentDirections")

        // next update direction value
        lockedI2cOperation{
            writeRegister(it, currentDirections or currentStates)
        }

        // if any pins are configured as input pins, then we need to start the interrupt monitoring
        // thread
        if (currentDirections > 0) {
            startMonitor()
            connectGpio()
        } else {
            stopMonitor()
            disconnectGpio()
        }
    }

    fun getMode(pin: Pin): PinMode {
        return if (currentDirections and pin.address != 0) {
            PinMode.DIGITAL_INPUT
        } else PinMode.DIGITAL_OUTPUT
    }

    // endregion

    // region Set Output state functions

    @Throws(Exception::class)
    @MainThread
    fun setState(pin: Pin, state: PinState) {
        // determine pin address
        val pinAddress = pin.address

        if (getMode(pin) == PinMode.DIGITAL_OUTPUT) {
            // determine state value for pin bit
            currentStates = if (state == PinState.HIGH) {
                currentStates or pinAddress
            } else {
                currentStates and pinAddress.inv()
            }

            // update state value
            lockedI2cOperation {
                writeRegister(it, currentDirections or currentStates)
            }
        } else {
            Timber.e("Cannot set state on INPUT Pin")
        }
    }

    // Get Input state functions
    @Throws(Exception::class)
    @MainThread
    fun getState(pin: Pin): PinState {
        lockedI2cOperation {
            currentStates = readRegister(it)
        }

        // determine pin address
        val pinAddress = pin.address

        // determine pin state

        return if (currentStates and pinAddress == pinAddress) PinState.HIGH else PinState.LOW
    }

    // endregion

    // Suppress NewApi for computeIfAbsent this is only used on Things that are Android 8.0+
    @SuppressLint("NewApi")
    @MainThread
    fun registerPinListener(pin: Pin, listener: PCF8574ATPinStateChangeListener): Boolean {
        return if (getMode(pin) == PinMode.DIGITAL_INPUT) {
            val pinListeners = mListeners.computeIfAbsent(pin) { ArrayList(1)}
            pinListeners.add(listener)
            try {
                checkInterrupt()
                true
            } catch (e: Exception) {
                Timber.e(e, "registerPinListener Error")
                false
            }
        } else {
            // Given pin not set for input
            false
        }
    }

    fun unRegisterPinListener(pin: Pin, listener: PCF8574ATPinStateChangeListener): Boolean {
        val pinListeners = mListeners[pin]
        return pinListeners?.remove(listener) ?: false
    }

    @Throws(Exception::class)
    @MainThread
    private fun checkInterrupt() {
        Timber.v("checkInterrupt")

        // only process for interrupts if a pin is configured as an input pin
        if (currentDirections > 0) {
            var pinInterruptStates : Int = currentStates
            lockedI2cOperation {
                pinInterruptStates = readRegister(it)
                Timber.v("checkInterrupt pinInterruptStates:$pinInterruptStates")
            }
            if (pinInterruptStates != currentStates) {
                // loop over the available pins
                for (pin in Pin.values()) {
                    evaluatePinForChange(pin, pinInterruptStates)
                }
                currentStates = pinInterruptStates
            }
        }
    }

    private fun evaluatePinForChange(pin: Pin, state: Int) {
        // determine pin address
        val pinAddress = pin.address

        if (state and pinAddress != currentStates and pinAddress) {
            val newState = if (state and pinAddress == pinAddress) PinState.HIGH else PinState.LOW

            dispatchPinChangeEvent(pin, newState)
        }
    }

    private fun dispatchPinChangeEvent(pin: Pin, state: PinState) {
        mListeners[pin]?.forEach { listener ->
            Timber.d("dispatchPinChangeEvent pin: ${pin.name} pinState: $state")
            listener.onPinStateChanged(pin, state)
        }
    }
}