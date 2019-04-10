package com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import android.view.ViewConfiguration
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.GpioCallback
import com.google.android.things.pio.I2cDevice
import com.google.android.things.pio.PeripheralManager
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017Pin.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.IOException
import java.util.*

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

/**
 * Driver for the MCP23017 16 bit I/O Expander.
 */
class MCP23017(bus: String? = null,
               address: Int = DEFAULT_I2C_000_ADDRESS,
               var intGpio: String? = null,
               private var pollingTime: Int = NO_POLLING_TIME,
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

    private var mDevice: I2cDevice? = null
    private var mGpioInt: Gpio? = null

    private val mListeners = HashMap<Pin, MutableList<MCP23017PinStateChangeListener>>()

    private var monitorJob: Job? = null

    private var debounceIntCallbackJob: Job? = null

    private val mIntCallback = GpioCallback{ gpio: Gpio ->
        if (debounceDelay != NO_DEBOUNCE_DELAY) {
            Timber.d("mIntCallback onGpioEdge ${gpio.value}")
            debounceIntCallbackJob?.cancel()
            debounceIntCallbackJob = GlobalScope.launch {
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
            } catch (e: IOException) {
                Timber.e(e,"init error connecting I2C")
                close()
                throw (Exception("Error init MCP23017", e))
            }
        }
    }

    @VisibleForTesting
    internal constructor(device: I2cDevice) : this() {
        mDevice = device
    }

    @Throws(IOException::class)
    private fun connectI2c(device: I2cDevice?) {
        mDevice = device

        // read initial GPIO pin states
        currentStatesA = readRegister(REGISTER_GPIO_A) ?: -1
        currentStatesB = readRegister(REGISTER_GPIO_B) ?: -1

        currentConf = readRegister(REGISTER_IOCON) ?: -1

        Timber.d("connect currentStatesA: $currentStatesA currentStatesB: $currentStatesB currentConf: $currentConf")
        resetToDefaults()
    }

    @Throws(IOException::class)
    private fun connectGpio() {
        if (intGpio != null && mGpioInt == null) {
            val manager = PeripheralManager.getInstance()
            //Mirror IntA and IntB pins to single Interrupt from either A or B ports
            currentConf = currentConf.or(MIRROR_INT_A_INT_B)
            writeRegister(REGISTER_IOCON, currentConf)
            Timber.d("connectGpio currentConf: $currentConf")

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
            monitorJob = GlobalScope.launch {
                // We could also check for true as suspending delay() method is cancellable
                while (isActive) {
                    try {
                        checkInterrupt()
                        delay(pollingTime.toLong())
                    } catch (e: Exception) {
                        stopMonitor()
                        throw (Exception("Error startMonitor checkInterrupt MCP23017", e))
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
        } catch (e: IOException){
            Timber.e("close i2c exception: $e")
            throw (Exception("Error closing MCP23017", e))
        } finally {
            mDevice = null
        }

        mGpioInt?.unregisterGpioCallback(mIntCallback)
        try {
            mGpioInt?.close()
        } catch (e: IOException){
            Timber.e("close mGpioInt exception: $e")
            throw (Exception("Error closing MCP23017 mGpioInt", e))
        } finally {
            mGpioInt = null
        }
    }


    @Throws(IOException::class)
    private fun readRegister(reg: Int): Int? = mDevice?.readRegByte(reg)?.toInt()?.and(0xff)

    @Throws(IOException::class)
    private fun writeRegister(reg: Int, regVal: Int) = mDevice?.writeRegByte(reg, regVal.toByte())

    @Throws(IOException::class)
    private fun resetToDefaults() {
        // set all default pins directions
        writeRegister(REGISTER_IODIR_A, 0)
        writeRegister(REGISTER_IODIR_B, 0)

        // set all default pin interrupts
        writeRegister(REGISTER_GPINTEN_A, 0)
        writeRegister(REGISTER_GPINTEN_B, 0)

        // set all default pin interrupt default values
        writeRegister(REGISTER_DEFVAL_A, 0)
        writeRegister(REGISTER_DEFVAL_B, 0)

        // set all default pin interrupt comparison behaviors
        writeRegister(REGISTER_INTCON_A, 0)
        writeRegister(REGISTER_INTCON_B, 0)

        // set all default pin states
        writeRegister(REGISTER_GPIO_A, 0)
        writeRegister(REGISTER_GPIO_B, 0)

        // set all default pin pull up resistors
        writeRegister(REGISTER_GPPU_A, 0)
        writeRegister(REGISTER_GPPU_B, 0)

        writeRegister(REGISTER_IOCON, 0)
    }

    // Set Input or output mode functions
    @Throws(Exception::class)
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

    @Throws(IOException::class)
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

        // next update direction value
        writeRegister(REGISTER_IODIR_A, currentDirectionA)

        // enable interrupts; interrupt on any change from previous state
        writeRegister(REGISTER_GPINTEN_A, currentDirectionA)
    }

    @Throws(IOException::class)
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

        // next update direction (mode) value
        writeRegister(REGISTER_IODIR_B, currentDirectionB)

        // enable interrupts; interrupt on any change from previous state
        writeRegister(REGISTER_GPINTEN_B, currentDirectionB)
    }

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

    // Set Output state functions
    @Throws(IOException::class)
    fun setState(pin: Pin, state: PinState) {
        // determine A or B port based on pin address
        if (pin.address < GPIO_B_OFFSET) {
            setStateA(pin, state)
        } else {
            setStateB(pin, state)
        }
    }

    @Throws(IOException::class)
    private fun setStateA(pin: Pin, state: PinState) {
        // determine pin address
        val pinAddress = pin.address - GPIO_A_OFFSET

        // determine state value for pin bit
        currentStatesA = if (state == PinState.HIGH) {
            currentStatesA or pinAddress
        } else {
            currentStatesA and pinAddress.inv()
        }

        // update state value
        writeRegister(REGISTER_GPIO_A, currentStatesA)
    }

    @Throws(IOException::class)
    private fun setStateB(pin: Pin, state: PinState) {
        // determine pin address
        val pinAddress = pin.address - GPIO_B_OFFSET

        // determine state value for pin bit
        currentStatesB = if (state == PinState.HIGH) {
            currentStatesB or pinAddress
        } else {
            currentStatesB and pinAddress.inv()
        }

        // update state value
        writeRegister(REGISTER_GPIO_B, currentStatesB)
    }

    // Get Input state functions
    @Throws(IOException::class)
    fun getState(pin: Pin): PinState {
        // determine A or B port based on pin address
        return if (pin.address < GPIO_B_OFFSET) {
            getStateA(pin) // get pin state
        } else {
            getStateB(pin) // get pin state
        }
    }

    @Throws(IOException::class)
    private fun getStateA(pin: Pin): PinState {
        currentStatesA = readRegister(REGISTER_GPIO_A) ?: currentStatesA

        // determine pin address
        val pinAddress = pin.address - GPIO_A_OFFSET

        // determine pin state

        return if (currentStatesA and pinAddress == pinAddress) PinState.HIGH else PinState.LOW
    }

    @Throws(IOException::class)
    private fun getStateB(pin: Pin): PinState {
        currentStatesB = readRegister(REGISTER_GPIO_B) ?: currentStatesB

        // determine pin address
        val pinAddress = pin.address - GPIO_B_OFFSET

        // determine pin state
        return if (currentStatesB and pinAddress == pinAddress) PinState.HIGH else PinState.LOW
    }

    // PullUps resistors mode functions for input Pins
    @Throws(IOException::class)
    fun setPullResistance(pin: Pin, resistance: PinPullResistance) {
        // determine A or B port based on pin address
        if (pin.address < GPIO_B_OFFSET) {
            setPullResistanceA(pin, resistance)
        } else {
            setPullResistanceB(pin, resistance)
        }
    }

    @Throws(IOException::class)
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

        // next update pull up resistor value
        writeRegister(REGISTER_GPPU_A, currentPullupA)
    }

    @Throws(IOException::class)
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

        // next update pull up resistor value
        writeRegister(REGISTER_GPPU_B, currentPullupB)
    }

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

    // Suppress NewApi for computeIfAbsent this is only used on Things that are Android 8.0+
    @SuppressLint("NewApi")
    fun registerPinListener(pin: Pin, listener: MCP23017PinStateChangeListener): Boolean {
        return if (getMode(pin) == PinMode.DIGITAL_INPUT) {
            val pinListeners = mListeners.computeIfAbsent(pin) { ArrayList(1)}
            pinListeners.add(listener)
            true
        } else {
            // Given pin not set for input
            false
        }
    }

    fun unRegisterPinListener(pin: Pin, listener: MCP23017PinStateChangeListener): Boolean {
        val pinListeners = mListeners[pin]
        return pinListeners?.remove(listener) ?: false
    }

    @Throws(IOException::class)
    private fun checkInterrupt() {
        Timber.v("checkInterrupt")

        // only process for interrupts if a pin on port A is configured as an
        // input pin
        if (currentDirectionA > 0) {
            // process interrupts for port A
            val pinInterruptA = readRegister(REGISTER_INTF_A) ?: -1
            Timber.v("checkInterrupt pinInterruptA:$pinInterruptA")

            // validate that there is at least one interrupt active on port A
            if (pinInterruptA > 0) {
                //TODO: We should think of reading INTCAP instead of GPIO to eliminate noise on
                // input and not loose other interrupt while reading GPIO
                // read the current pin states on port A
                val pinInterruptCapState = readRegister(REGISTER_INTCAP_A)
                Timber.v("checkInterrupt pinInterruptCapState:$pinInterruptCapState")
                val pinInterruptRegState = readRegister(REGISTER_GPIO_A)
                Timber.v("checkInterrupt pinInterruptRegState:$pinInterruptRegState")

                // loop over the available pins on port B
                for (pin in MCP23017Pin.ALL_A_PINS) {
                    evaluatePinForChangeA(pin, pinInterruptRegState)
                }
            }
        }

        // only process for interrupts if a pin on port B is configured as an
        // input pin
        if (currentDirectionB > 0) {
            // process interrupts for port B
            val pinInterruptB = readRegister(REGISTER_INTF_B) ?: -1
            Timber.v("checkInterrupt pinInterruptB:$pinInterruptB")

            // validate that there is at least one interrupt active on port B
            if (pinInterruptB > 0) {
                // read the current pin states on port B
                val pinInterruptCapState = readRegister(REGISTER_INTCAP_B)
                Timber.v("checkInterrupt pinInterruptCapState:$pinInterruptCapState")
                val pinInterruptRegState = readRegister(REGISTER_GPIO_B)
                Timber.v("checkInterrupt pinInterruptRegState:$pinInterruptRegState")

                // loop over the available pins on port B
                for (pin in MCP23017Pin.ALL_B_PINS) {
                    evaluatePinForChangeB(pin, pinInterruptRegState)
                }
            }
        }
    }

    private fun evaluatePinForChangeA(pin: Pin, state: Int?) {
        // determine pin address
        val pinAddress = pin.address - GPIO_A_OFFSET

        if (state != null && state and pinAddress != currentStatesA and pinAddress) {
            val newState = if (state and pinAddress == pinAddress) PinState.HIGH else PinState.LOW

            // determine and cache state value for pin bit
            currentStatesA = if (newState == PinState.HIGH) {
                currentStatesA or pinAddress
            } else {
                currentStatesA and pinAddress.inv()
            }

            dispatchPinChangeEvent(pin, newState)
        }
    }

    private fun evaluatePinForChangeB(pin: Pin, state: Int?) {
        // determine pin address
        val pinAddress = pin.address - GPIO_B_OFFSET

        if (state != null && state and pinAddress != currentStatesB and pinAddress) {
            val newState = if (state and pinAddress == pinAddress) PinState.HIGH else PinState.LOW

            // determine and cache state value for pin bit
            currentStatesB = if (newState == PinState.HIGH) {
                currentStatesB or pinAddress
            } else {
                currentStatesB and pinAddress.inv()
            }

            // change detected for INPUT PIN
            // System.out.println("<<< CHANGE >>> " + pin.getName() + " : " + state);
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