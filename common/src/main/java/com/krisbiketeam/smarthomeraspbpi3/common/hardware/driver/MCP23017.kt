package com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver

import android.annotation.SuppressLint
import android.view.ViewConfiguration
import androidx.annotation.MainThread
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.GpioCallback
import com.google.android.things.pio.I2cDevice
import com.google.android.things.pio.PeripheralManager
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017Pin.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/// Registers all are 8 bit long
// Direction registers
// I/O DIRECTION REGISTER: Input - 1 or output - 0; def 0
private const val REGISTER_IODIR_A = 0x00
private const val REGISTER_IODIR_B = 0x01

// INPUT POLARITY PORT REGISTER; GPIO bit will be inverted LOW/HIGH; def 0
private const val REGISTER_IPOL_A = 0x02
private const val REGISTER_IPOL_B = 0x03

// INTERRUPT-ON-CHANGE PINS; enable interrupts - 1 or disable interrupts - 0; def 0; see
// INTCON and DEFVAL; def 0
private const val REGISTER_GPINTEN_A = 0x04
private const val REGISTER_GPINTEN_B = 0x05

// DEFAULT VALUE REGISTER; if GPIO differs from DEFVAL interrupt is triggered; def 0
private const val REGISTER_DEFVAL_A = 0x06
private const val REGISTER_DEFVAL_B = 0x07

// INTERRUPT-ON-CHANGE CONTROL REGISTER; if set - 1 GPIO is compared with DEFVAL or if cleared
// - 0 interrupt is triggered when GPIO changed compared to its previous state; def 0
private const val REGISTER_INTCON_A = 0x08
private const val REGISTER_INTCON_B = 0x09

// I/O EXPANDER CONFIGURATION REGISTER; MIRROR 1 INTA and INTB are internally connected to
// i=one INT def not connected; INTPOL: 0 active Low (DEFAULT) 1 active high; etc; def 0
private const val REGISTER_IOCON = 0x0A //0x0B

// GPIO PULL-UP RESISTOR REGISTER only for IODIR input pins 0 disabled, 1 enabled with 100k;
// def 0
private const val REGISTER_GPPU_A = 0x0C
private const val REGISTER_GPPU_B = 0x0D

// Read only INTERRUPT FLAG REGISTER; if 1 interrupt was on that pin
private const val REGISTER_INTF_A = 0x0E
private const val REGISTER_INTF_B = 0x0F

// Read only INTERRUPT CAPTURED VALUE FOR PORT REGISTER; These bits reflect the logic level
// on the port pins at the time of interrupt due to pin change cleared on read of INTCAP or GPIO
private const val REGISTER_INTCAP_A = 0x10
private const val REGISTER_INTCAP_B = 0x11

// GENERAL PURPOSE I/O PORT REGISTER; The GPIO register reflects the value on the port.
//Reading from this register reads the port. Writing to this
//register modifies the Output Latch (OLAT) register
private const val REGISTER_GPIO_A = 0x12
private const val REGISTER_GPIO_B = 0x13

private const val GPIO_A_OFFSET = 0
private const val GPIO_B_OFFSET = 1000

private const val MIRROR_INT_A_INT_B = 0x40

private const val RECHECK_INT_DELAY = 5000L

/**
 * Driver for the MCP23017 16 bit I/O Expander.
 *
 * !!! IPORTANT !!!
 * Must be called on MainThread with all its methods
 */
@MainThread
class MCP23017(private val bus: String? = null, private val address: Int = DEFAULT_I2C_000_ADDRESS,
               var intGpio: String? = null, private var pollingTime: Int = NO_POLLING_TIME,
               private val debounceDelay: Int = DEBOUNCE_DELAY) : AutoCloseable {


    companion object {

        /**
         * Default I2C address for the Expander.
         */
        const val DEFAULT_I2C_000_ADDRESS = 0x20
        const val DEFAULT_I2C_001_ADDRESS = 0x21
        const val DEFAULT_I2C_010_ADDRESS = 0x22
        const val DEFAULT_I2C_011_ADDRESS = 0x23
        const val DEFAULT_I2C_100_ADDRESS = 0x24
        const val DEFAULT_I2C_101_ADDRESS = 0x25
        const val DEFAULT_I2C_110_ADDRESS = 0x26
        const val DEFAULT_I2C_111_ADDRESS = 0x27

        const val DEFAULT_POLLING_TIME = 50
        const val NO_POLLING_TIME = -1

        val DEBOUNCE_DELAY = ViewConfiguration.getTapTimeout()
        const val NO_DEBOUNCE_DELAY = 0
    }

    private var currentStatesA = 0
    private var currentStatesB = 0
    private var currentDirectionA = 0
    private var currentDirectionB = 0
    private var currentPullupA = 0
    private var currentPullupB = 0
    private var currentConf = 0

    private var mGpioInt: Gpio? = null

    private val mListeners = ConcurrentHashMap<Pin, MutableList<MCP23017PinStateChangeListener>>()

    private var monitorJob: Job? = null

    private var debounceIntCallbackJob: Job? = null

    private var recheckIntCallbackJob: Job? = null

    private val mIntCallback = object : GpioCallback {
        override fun onGpioEdge(gpio: Gpio): Boolean {
            if (debounceDelay != NO_DEBOUNCE_DELAY) {
                Timber.d("mIntCallback addr:$address onGpioEdge ${gpio.value}")
                debounceIntCallbackJob?.cancel()
                debounceIntCallbackJob = GlobalScope.launch(Dispatchers.IO) {
                    delay(debounceDelay.toLong())
                    try {
                        if (this.isActive) {
                            checkInterrupt()
                        }
                    } catch (e: Exception) {
                        dispatchCheckInterruptError(e)
                    } finally {
                        debounceIntCallbackJob?.cancel()
                    }
                }
            } else {
                debounceIntCallbackJob?.cancel()
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        checkInterrupt()
                    } catch (e: Exception) {
                        dispatchCheckInterruptError(e)
                    }
                }
            }

            recheckIntCallbackJob?.cancel()
            recheckIntCallbackJob = GlobalScope.launch(Dispatchers.IO) {
                delay(RECHECK_INT_DELAY)
                try {
                    Timber.e("mIntCallback addr:$address recheckIntCallbackJob mGpioInt: ${mGpioInt?.value}")
                    if (this.isActive) {
                        checkInterrupt()
                    }
                } catch (e: Exception) {
                    dispatchCheckInterruptError(e)
                } finally {
                    recheckIntCallbackJob?.cancel()
                }
            }
            // Return true to keep callback active.
            return true
        }

        override fun onGpioError(gpio: Gpio?, error: Int) {
            GlobalScope.launch(Dispatchers.IO) {
                mListeners.flatMap { it.value }.forEach {
                    Timber.e("mIntCallback addr:$address ${gpio.toString()} : Error event $error on: $this")
                    it.onError("\"mIntCallback ${gpio.toString()} : Error event $error on: $this\"")
                }
            }
        }
    }

    init {
        if (bus != null) {
            try {
                lockedI2cOperation {
                    // read initial GPIO pin states
                    val initCurrentStatesA = readRegister(it, REGISTER_GPIO_A) ?: -1
                    val initCurrentStatesB = readRegister(it, REGISTER_GPIO_B) ?: -1

                    val initCurrentConf = readRegister(it, REGISTER_IOCON) ?: -1

                    Timber.d(
                            "connect addr:$address initCurrentStatesA: $initCurrentStatesA initCurrentStatesB: $initCurrentStatesB initCurrentConf: $initCurrentConf")
                    resetToDefaults(it)
                }
            } catch (e: Exception) {
                Timber.e(e, "init error connecting I2C addr:$address")
                close()
                throw (Exception("Error init MCP23017", e))
            }
        }
    }

    /**
     * Close the driver and the underlying device.
     */
    @Throws(Exception::class)
    @MainThread
    override fun close() {
        Timber.d("close started i2c:$address inGpio:$intGpio mGpioInt:$mGpioInt")
        // if a monitor is running, then shut it down now
        stopMonitor()

        mGpioInt?.unregisterGpioCallback(mIntCallback)
        try {
            mGpioInt?.close()
        } catch (e: Exception) {
            Timber.e("close mGpioInt exception: $e")
            throw (Exception("Error closing MCP23017 mGpioInt", e))
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
        Timber.e("resetToDefaults addr:$address")
        // set all default pins directions
        writeRegister(i2cDevice, REGISTER_IODIR_A, 0)
        writeRegister(i2cDevice, REGISTER_IODIR_B, 0)

        // set all default pin interrupts
        writeRegister(i2cDevice, REGISTER_GPINTEN_A, 0)
        writeRegister(i2cDevice, REGISTER_GPINTEN_B, 0)

        // set all default pin interrupt default values
        writeRegister(i2cDevice, REGISTER_DEFVAL_A, 0)
        writeRegister(i2cDevice, REGISTER_DEFVAL_B, 0)

        // set all default pin interrupt comparison behaviors
        writeRegister(i2cDevice, REGISTER_INTCON_A, 0)
        writeRegister(i2cDevice, REGISTER_INTCON_B, 0)

        // set all default pin states
        writeRegister(i2cDevice, REGISTER_GPIO_A, 0)
        writeRegister(i2cDevice, REGISTER_GPIO_B, 0)

        // set all default pin pull up resistors
        writeRegister(i2cDevice, REGISTER_GPPU_A, 0)
        writeRegister(i2cDevice, REGISTER_GPPU_B, 0)

        writeRegister(i2cDevice, REGISTER_IOCON, 0)
    }

    // region Input interrupt handling

    @Throws(Exception::class)
    @MainThread
    private fun connectGpio() {
        if (intGpio != null && mGpioInt == null) {
            val manager = PeripheralManager.getInstance()
            //Mirror IntA and IntB pins to single Interrupt from either A or B ports
            currentConf = currentConf.or(MIRROR_INT_A_INT_B)
            lockedI2cOperation {
                writeRegister(it, REGISTER_IOCON, currentConf)
            }
            Timber.d("connectGpio addr:$address currentConf: $currentConf")

            Timber.d("connectGpio addr:$address intGpio openGpio $intGpio")
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
    private fun startMonitor() {
        if (pollingTime != NO_POLLING_TIME) {
            // if the monitor has not been started, then start it now
            monitorJob = GlobalScope.launch(Dispatchers.IO) {
                // We could also check for true as suspending delay() method is cancellable
                while (isActive) {
                    try {
                        checkInterrupt()
                        delay(pollingTime.toLong())
                    } catch (e: Exception) {
                        dispatchCheckInterruptError(e)
                        stopMonitor()
                        throw (Exception("Error startMonitor checkInterrupt MCP23017", e))
                    }
                }
            }
        } else {
            stopMonitor()
        }
    }

    private fun stopMonitor() {
        // cancel and null monitoring Job since there are no input pins configured
        monitorJob?.cancel()
        monitorJob = null
    }

    // endregion

    // region Read/Write Register functions

    @Throws(Exception::class)
    @MainThread
    private fun readRegister(i2cDevice: I2cDevice?, reg: Int): Int? =
            i2cDevice?.readRegByte(reg)?.toInt()?.and(0xff)

    @Throws(Exception::class)
    @MainThread
    private fun writeRegister(i2cDevice: I2cDevice?, reg: Int, regVal: Int) =
            i2cDevice?.writeRegByte(reg, regVal.toByte())

    // endregion

    // region Set Input or output mode functions

    @Throws(Exception::class)
    @MainThread
    fun setMode(pin: Pin, mode: PinMode) {
        // determine A or B port based on pin address
        if (pin.address < GPIO_B_OFFSET) {
            setModeA(pin, mode)
        } else {
            setModeB(pin, mode)
        }

        // if any pins are configured as input pins, then we need to start the interrupt monitoring
        // thread
        if ((currentDirectionA > 0 || currentDirectionB > 0)) {
            startMonitor()
            connectGpio()
        } else {
            stopMonitor()
            disconnectGpio()
        }
    }

    @Throws(Exception::class)
    @MainThread
    private fun setModeA(pin: Pin, mode: PinMode) {
        // determine register and pin address
        val pinAddress = pin.address - GPIO_A_OFFSET

        // determine update direction value based on mode
        currentDirectionA = if (mode == PinMode.DIGITAL_INPUT) {
            currentDirectionA or pinAddress
        } else {
            currentDirectionA and pinAddress.inv()
        }
        Timber.d("setModeA currentDirectionA: $currentDirectionA")

        lockedI2cOperation {
            // next update direction value
            writeRegister(it, REGISTER_IODIR_A, currentDirectionA)

            // enable interrupts; interrupt on any change from previous state
            writeRegister(it, REGISTER_GPINTEN_A, currentDirectionA)
        }
    }

    @Throws(Exception::class)
    @MainThread
    private fun setModeB(pin: Pin, mode: PinMode) {
        // determine register and pin address
        val pinAddress = pin.address - GPIO_B_OFFSET

        // determine update direction value based on mode
        currentDirectionB = if (mode == PinMode.DIGITAL_INPUT) {
            currentDirectionB or pinAddress
        } else {
            currentDirectionB and pinAddress.inv()
        }

        Timber.d("setModeB currentDirectionB: $currentDirectionB")

        lockedI2cOperation {
            // next update direction (mode) value
            writeRegister(it, REGISTER_IODIR_B, currentDirectionB)

            // enable interrupts; interrupt on any change from previous state
            writeRegister(it, REGISTER_GPINTEN_B, currentDirectionB)
        }
    }

    @MainThread
    fun getMode(pin: Pin): PinMode {
        var address = pin.address
        if (address < GPIO_B_OFFSET) {
            if (currentDirectionA and address != 0) {
                return PinMode.DIGITAL_INPUT
            }
        } else {
            address -= GPIO_B_OFFSET
            if (currentDirectionB and address != 0) {
                return PinMode.DIGITAL_INPUT
            }
        }
        return PinMode.DIGITAL_OUTPUT
    }

    // endregion

    // region Set Output state functions

    @Throws(Exception::class)
    @MainThread
    fun setState(pin: Pin, state: PinState) {
        // determine A or B port based on pin address
        if (pin.address < GPIO_B_OFFSET) {
            setStateA(pin, state)
        } else {
            setStateB(pin, state)
        }
    }

    @Throws(Exception::class)
    @MainThread
    private fun setStateA(pin: Pin, state: PinState) {
        // determine pin address
        val pinAddress = pin.address - GPIO_A_OFFSET

        // determine state value for pin bit
        currentStatesA = if (state == PinState.HIGH) {
            currentStatesA or pinAddress
        } else {
            currentStatesA and pinAddress.inv()
        }

        lockedI2cOperation {
            // update state value
            writeRegister(it, REGISTER_GPIO_A, currentStatesA)
        }
    }

    @Throws(Exception::class)
    @MainThread
    private fun setStateB(pin: Pin, state: PinState) {
        // determine pin address
        val pinAddress = pin.address - GPIO_B_OFFSET

        // determine state value for pin bit
        currentStatesB = if (state == PinState.HIGH) {
            currentStatesB or pinAddress
        } else {
            currentStatesB and pinAddress.inv()
        }

        lockedI2cOperation {
            // update state value
            writeRegister(it, REGISTER_GPIO_B, currentStatesB)
        }
    }

    // Get Input state functions
    @Throws(Exception::class)
    @MainThread
    fun getState(pin: Pin): PinState {
        // determine A or B port based on pin address
        return if (pin.address < GPIO_B_OFFSET) {
            getStateA(pin) // get pin state
        } else {
            getStateB(pin) // get pin state
        }
    }

    @Throws(Exception::class)
    @MainThread
    private fun getStateA(pin: Pin): PinState {
        lockedI2cOperation {
            currentStatesA = readRegister(it, REGISTER_GPIO_A) ?: currentStatesA
        }

        // determine pin address
        val pinAddress = pin.address - GPIO_A_OFFSET

        // determine pin state

        return if (currentStatesA and pinAddress == pinAddress) PinState.HIGH else PinState.LOW
    }

    @Throws(Exception::class)
    @MainThread
    private fun getStateB(pin: Pin): PinState {
        lockedI2cOperation {
            currentStatesB = readRegister(it, REGISTER_GPIO_B) ?: currentStatesB
        }

        // determine pin address
        val pinAddress = pin.address - GPIO_B_OFFSET

        // determine pin state
        return if (currentStatesB and pinAddress == pinAddress) PinState.HIGH else PinState.LOW
    }

    // endregion

    // region PullUps resistors mode functions for input Pins

    @Throws(Exception::class)
    @MainThread
    fun setPullResistance(pin: Pin, resistance: PinPullResistance) {
        // determine A or B port based on pin address
        if (pin.address < GPIO_B_OFFSET) {
            setPullResistanceA(pin, resistance)
        } else {
            setPullResistanceB(pin, resistance)
        }
    }

    @Throws(Exception::class)
    @MainThread
    private fun setPullResistanceA(pin: Pin, resistance: PinPullResistance) {
        // determine pin address
        val pinAddress = pin.address - GPIO_A_OFFSET

        // determine pull up value for pin bit
        currentPullupA = if (resistance == PinPullResistance.PULL_UP) {
            currentPullupA or pinAddress
        } else {
            currentPullupA and pinAddress.inv()
        }
        Timber.d("setPullResistanceA currentPullupA: $currentPullupA")

        lockedI2cOperation {
            // next update pull up resistor value
            writeRegister(it, REGISTER_GPPU_A, currentPullupA)
        }
    }

    @Throws(Exception::class)
    @MainThread
    private fun setPullResistanceB(pin: Pin, resistance: PinPullResistance) {
        // determine pin address
        val pinAddress = pin.address - GPIO_B_OFFSET

        // determine pull up value for pin bit
        currentPullupB = if (resistance == PinPullResistance.PULL_UP) {
            currentPullupB or pinAddress
        } else {
            currentPullupB and pinAddress.inv()
        }
        Timber.d("setPullResistanceB currentPullupB: $currentPullupB")

        lockedI2cOperation {
            // next update pull up resistor value
            writeRegister(it, REGISTER_GPPU_B, currentPullupB)
        }
    }

    @MainThread
    fun getPullResistance(pin: Pin): PinPullResistance {
        var address = pin.address
        if (address < GPIO_B_OFFSET) {
            if (currentPullupA and address != 0) {
                return PinPullResistance.PULL_UP
            }
        } else {
            address -= GPIO_B_OFFSET
            if (currentPullupB and address != 0) {
                return PinPullResistance.PULL_UP
            }
        }
        return PinPullResistance.OFF
    }

    // endregion

    // Suppress NewApi for computeIfAbsent this is only used on Things that are Android 8.0+
    @Throws(Exception::class)
    @SuppressLint("NewApi")
    suspend fun registerPinListener(pin: Pin, listener: MCP23017PinStateChangeListener) {
        if (getMode(pin) == PinMode.DIGITAL_INPUT) {
            val pinListeners = mListeners.computeIfAbsent(pin) { ArrayList(1) }
            pinListeners.add(listener)
            checkInterrupt()
        } else {
            // Given pin not set for input
            throw Exception("Given pin not set for input")
        }
    }

    fun unRegisterPinListener(pin: Pin, listener: MCP23017PinStateChangeListener): Boolean {
        val pinListeners = mListeners[pin]
        return pinListeners?.remove(listener) ?: false
    }

    @Throws(Exception::class)
    private suspend fun checkInterrupt() {
        Timber.v("checkInterrupt addr:$address")

        var pinInterruptRegStateA: Int? = null
        var pinInterruptRegStateB: Int? = null
        withContext(Dispatchers.Main) {
            lockedI2cOperation {
                // only process for interrupts if a pin on port A is configured as an
                // input pin
                if (currentDirectionA > 0) {
                    pinInterruptRegStateA = readRegister(it, REGISTER_GPIO_A)
                    Timber.v("checkInterrupt addr:$address pinInterruptRegStateA:$pinInterruptRegStateA")
                }
                // only process for interrupts if a pin on port B is configured as an
                // input pin
                if (currentDirectionB > 0) {
                    pinInterruptRegStateB = readRegister(it, REGISTER_GPIO_B)
                    Timber.v("checkInterrupt addr:$address pinInterruptRegStateB:$pinInterruptRegStateB")
                }
            }
        }
        pinInterruptRegStateA?.let { evaluatePinForChangeA(it) }
        pinInterruptRegStateB?.let { evaluatePinForChangeB(it) }
    }

    private suspend fun evaluatePinForChangeA(state: Int) {
        Timber.v("evaluatePinForChangeA addr:$address currentStatesA:$currentStatesA")
        var xor = state.xor(currentStatesA)
        for (element in MCP23017Pin.ALL_A_PINS) {
            if (xor.and(1) > 0) {
                val newState =
                        if (state.and(element.address - GPIO_A_OFFSET) > 0) PinState.HIGH else PinState.LOW
                dispatchPinChangeEvent(element, newState)
            }
            xor = xor.shr(1)
        }
        currentStatesA = state
    }

    private suspend fun evaluatePinForChangeB(state: Int) {
        Timber.v("evaluatePinForChangeA addr:$address currentStatesA:$currentStatesA")
        var xor = state.xor(currentStatesB)
        for (element in MCP23017Pin.ALL_B_PINS) {
            if (xor.and(1) > 0) {
                val newState =
                        if (state.and(element.address - GPIO_B_OFFSET) > 0) PinState.HIGH else PinState.LOW
                dispatchPinChangeEvent(element, newState)
            }
            xor = xor.shr(1)
        }
        currentStatesB = state
    }

    private suspend fun dispatchPinChangeEvent(pin: Pin, state: PinState) {
        mListeners[pin]?.forEach {
            Timber.d("dispatchPinChangeEvent addr:$address pin: ${pin.name} pinState: $state")
            it.onPinStateChanged(pin, state)
        }
    }

    private suspend fun dispatchCheckInterruptError(e: Exception){
        Timber.e(e, "dispatchCheckInterruptError")
        mListeners.flatMap { it.value }.forEach {
            Timber.d("dispatchCheckInterruptError dispatch onError to: $it")
            it.onError("dispatchCheckInterruptError Exception: $e")
        }
    }
}