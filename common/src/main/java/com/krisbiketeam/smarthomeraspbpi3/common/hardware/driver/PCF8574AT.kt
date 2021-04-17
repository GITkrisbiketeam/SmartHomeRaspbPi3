package com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import android.view.ViewConfiguration
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.GpioCallback
import com.google.android.things.pio.I2cDevice
import com.google.android.things.pio.PeripheralManager
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.PCF8574ATPin.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*

/**
 * Driver for the MCP23017 16 bit I/O Expander.
 */
class PCF8574AT(bus: String? = null,
               address: Int = DEFAULT_I2C_000_ADDRESS,
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

    private var mDevice: I2cDevice? = null
    private var mGpioInt: Gpio? = null

    private val mListeners = HashMap<Pin, MutableList<PCF8574ATPinStateChangeListener>>()

    private var monitorJob: Job? = null

    private var debounceIntCallbackJob: Job? = null

    private val mIntCallback = GpioCallback{ gpio: Gpio ->
        if (debounceDelay != NO_DEBOUNCE_DELAY) {
            Timber.d("mIntCallback onGpioEdge ${gpio.value}")
            debounceIntCallbackJob?.cancel()
            debounceIntCallbackJob = GlobalScope.launch(Dispatchers.IO) {
                delay(debounceDelay.toLong())
                try {
                    checkInterrupt()
                } finally {
                    debounceIntCallbackJob?.cancel()
                }
            }
        } else {
            debounceIntCallbackJob?.cancel()
            checkInterrupt()
        }

        // Return true to keep callback active.
        true
    }

    init {
        if (bus != null) {
            try {
                connectI2c(PeripheralManager.getInstance()?.openI2cDevice(bus, address))
            } catch (e: Exception) {
                Timber.e(e,"init error connecting I2C")
                close()
                throw (Exception("Error init PCF8574AT", e))
            }
        }
    }

    @VisibleForTesting
    internal constructor(device: I2cDevice) : this() {
        mDevice = device
    }

    @Throws(Exception::class)
    private fun connectI2c(device: I2cDevice?) {
        mDevice = device

        // read initial GPIO pin states
        currentStates = readRegister() ?: -1

        Timber.d("connectI2c currentStates: $currentStates")
        resetToDefaults()
    }

    @Throws(Exception::class)
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
                        checkInterrupt()
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

    /**
     * Close the driver and the underlying device.
     */
    @Throws(Exception::class)
    override fun close() {
        // if a monitor is running, then shut it down now
        stopMonitor()

        try {
            mDevice?.close()
        } catch (e: Exception){
            Timber.e("close i2c exception: $e")
            throw (Exception("Error closing PCF8574AT", e))
        } finally {
            mDevice = null
        }

        mGpioInt?.unregisterGpioCallback(mIntCallback)
        try {
            mGpioInt?.close()
        } catch (e: Exception){
            Timber.e("close mGpioInt exception: $e")
            throw (Exception("Error closing PCF8574AT mGpioInt", e))
        } finally {
            mGpioInt = null
        }
    }


    @Throws(Exception::class)
    private fun readRegister(): Int {
        val byteArray = ByteArray(1)
        mDevice?.read(byteArray, 1)
        return byteArray[0].toInt().and(0xff)
    }

    @Throws(Exception::class)
    private fun writeRegister(regVal: Int) {
        val buffer = ByteArray(1)
        buffer[0] = regVal.toByte()
        mDevice?.write(buffer, 1)
    }

    @Throws(Exception::class)
    private fun resetToDefaults() {
        writeRegister( 0xFF)
    }

    // Set Input or output mode functions
    @Throws(Exception::class)
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
        writeRegister(currentDirections or currentStates)

        // if any pins are configured as input pins, then we need to start the interrupt monitoring
        // thread
        if ((currentDirections > 0)) {
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

    // Set Output state functions
    @Throws(Exception::class)
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
            writeRegister(currentDirections or currentStates)
        } else {
            Timber.e("Cannot set state on INPUT Pin")
        }
    }

    // Get Input state functions
    @Throws(Exception::class)
    fun getState(pin: Pin): PinState {
        currentStates = readRegister() ?: currentStates

        // determine pin address
        val pinAddress = pin.address

        // determine pin state

        return if (currentStates and pinAddress == pinAddress) PinState.HIGH else PinState.LOW
    }

    // Suppress NewApi for computeIfAbsent this is only used on Things that are Android 8.0+
    @SuppressLint("NewApi")
    fun registerPinListener(pin: Pin, listener: PCF8574ATPinStateChangeListener): Boolean {
        return if (getMode(pin) == PinMode.DIGITAL_INPUT) {
            val pinListeners = mListeners.computeIfAbsent(pin) { ArrayList(1)}
            pinListeners.add(listener)
            true
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
    private fun checkInterrupt() {
        Timber.v("checkInterrupt")

        // only process for interrupts if a pin on port A is configured as an
        // input pin
        if (currentDirections > 0) {
            // process interrupts for port A
            readRegister()?.let{pinInterruptStates ->
                Timber.v("checkInterrupt pinInterruptStates:$pinInterruptStates")

                // validate that there is at least one interrupt active on port A
                if (pinInterruptStates != currentStates) {
                    // loop over the available pins
                    for (pin in Pin.values()) {
                        evaluatePinForChange(pin, pinInterruptStates)
                    }
                    currentStates = pinInterruptStates
                }
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
        Timber.d("dispatchPinChangeEvent pin: ${pin.name} pinState: $state")

        val listeners = mListeners[pin]
        if (listeners != null) {
            // iterate over the pin listeners list
            for (listener in listeners) {
                listener.onPinStateChanged(pin, state)
            }
        }
    }

}