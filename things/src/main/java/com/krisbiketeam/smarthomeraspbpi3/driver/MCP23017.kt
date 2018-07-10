package com.krisbiketeam.smarthomeraspbpi3.driver

import android.support.annotation.VisibleForTesting
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.I2cDevice
import com.google.android.things.pio.PeripheralManager
import com.krisbiketeam.smarthomeraspbpi3.driver.MCP23017Pin.*
import timber.log.Timber
import java.io.IOException
import java.util.*

/**
 * Driver for the MCP23017 16 bit I/O Expander.
 */
class MCP23017(bus: String? = null, address: Int = DEFAULT_I2C_000_ADDRESS, pollingTime: Int = DEFAULT_POLLING_TIME, intAGpio: String? = null, intBGpio: String? = null) : AutoCloseable {


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

        /**
         * Maximum power consumption in micro-amperes when measuring temperature.
         */
        const val MAX_POWER_CONSUMPTION_TEMP_UA = 85f
        // Sensor constants from the datasheet.
        // https://cdn-shop.adafruit.com/datasheets/BST-BMP280-DS001-11.pdf
        const val DEFAULT_POLLING_TIME = 50
        const val NO_POLLING_TIME = -1

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
        // INTERRUPT-ON-CHANGE CONTROL REGISTER; if set - 1 GPIO is compared with DEFVAL or if cleard
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
    }

    private var currentStatesA = 0
    private var currentStatesB = 0
    private var currentDirectionA = 0
    private var currentDirectionB = 0
    private var currentPullupA = 0
    private var currentPullupB = 0

    private var pollingTime = DEFAULT_POLLING_TIME

    private val i2cBusOwner = false
    private var monitor: GpioStateMonitor? = null

    private var mDevice: I2cDevice? = null
    private var mIntAGpio: Gpio? = null
    private var mIntBGpio: Gpio? = null

    private val mListeners = HashMap<MCP23017Pin, List<MCP23017PinStateChangeListener>>()

    private val mIntACallback = { gpio: Gpio ->
        try {
            Timber.d("mIntACallback onGpioEdge " + gpio.getValue())
            checkInterrupt()
        } catch (e: IOException) {
            Timber.e("mIntACallback onGpioEdge exception", e)
        }

        // Return true to keep callback active.
        true
    }

    private val mIntBCallback = { gpio: Gpio ->
        try {
            Timber.d("mIntBCallback onGpioEdge " + gpio.getValue())
            checkInterrupt()
        } catch (e: IOException) {
            Timber.e("mIntBCallback onGpioEdge exception", e)
        }

        // Return true to keep callback active.
        true
    }


    init {
        if (bus != null) {
            try {
                connectI2c(PeripheralManager.getInstance().openI2cDevice(bus, address))
                if (intAGpio != null || intBGpio != null) {
                    connectGpio(intAGpio, intBGpio)
                }
            } catch (e: IOException) {
                try {
                    close()
                } catch (ignored: IOException) {
                }
            }
        }

        this.pollingTime = pollingTime
    }

    @VisibleForTesting
    internal constructor(device: I2cDevice) : this() {
        mDevice = device
    }


    @Throws(IOException::class)
    private fun connectI2c(device: I2cDevice?) {
        mDevice = device

        if (mDevice == null) {
            throw IllegalStateException("I2C device could not be open")
        }

        // read initial GPIO pin states
        currentStatesA = readRegister(REGISTER_GPIO_A) ?: currentStatesA
        currentStatesB = readRegister(REGISTER_GPIO_B) ?: currentStatesB

        val currentConf = mDevice?.readRegByte(REGISTER_IOCON)?.toInt() ?: -1

        Timber.d("connect currentStatesA: " + currentStatesA + " currentStatesB: " +
                currentStatesA + " currentConf: " + currentConf)
        resetToDefaults()
    }

    @Throws(IOException::class)
    private fun connectGpio(intAGpio: String?, intBGpio: String?) {
        val manager = PeripheralManager.getInstance()
        if (intAGpio != null) {
            Timber.d("connectGpio intAGpio openGpio $intAGpio")
            // Step 1. Create GPIO connection.
            mIntAGpio = manager.openGpio(intAGpio)
            // Step 2. Configure as an input.
            mIntAGpio!!.setDirection(Gpio.DIRECTION_IN)
            // Step 3. Enable edge trigger events.
            mIntAGpio!!.setEdgeTriggerType(Gpio.EDGE_RISING)    // INT active Low
            // Step 4. Register an event callback.
            mIntAGpio!!.registerGpioCallback(mIntACallback)
        }
        if (intBGpio != null) {
            Timber.d("connectGpio intBGpio openGpio $intBGpio")
            // Step 1. Create GPIO connection.
            mIntBGpio = manager.openGpio(intBGpio)
            // Step 2. Configure as an input.
            mIntBGpio!!.setDirection(Gpio.DIRECTION_IN)
            // Step 3. Enable edge trigger events.
            mIntBGpio!!.setEdgeTriggerType(Gpio.EDGE_RISING)    // INT active Low
            // Step 4. Register an event callback.
            mIntBGpio!!.registerGpioCallback(mIntBCallback)
        }
    }

    /**
     * Close the driver and the underlying device.
     */
    @Throws(IOException::class)
    override fun close() {
        // if a monitor is running, then shut it down now
        monitor?.shutdown()
        monitor = null

        try {
            mDevice?.close()
        } finally {
            mDevice = null
        }


        mIntAGpio?.unregisterGpioCallback(mIntACallback)
        try {
            mIntAGpio?.close()
        } finally {
            mIntAGpio = null
        }

        mIntBGpio?.unregisterGpioCallback(mIntBCallback)
        try {
            mIntBGpio?.close()
        } finally {
            mIntBGpio = null
        }
    }


    fun readRegister(reg: Int) = mDevice?.readRegByte(reg)?.toInt()?.and(0xff)

    fun writeRegister(reg: Int, regVal: Int) = mDevice?.writeRegByte(reg, regVal.toByte())

    @Throws(IOException::class, IllegalStateException::class)
    fun resetToDefaults() {
        if (mDevice == null) {
            throw IllegalStateException("I2C device is already closed")
        }

        // set all default pins directions
        writeRegister(REGISTER_IODIR_A, currentDirectionA)
        writeRegister(REGISTER_IODIR_B, currentDirectionB)

        // set all default pin interrupts
        writeRegister(REGISTER_GPINTEN_A, currentDirectionA)
        writeRegister(REGISTER_GPINTEN_B, currentDirectionB)

        // set all default pin interrupt default values
        writeRegister(REGISTER_DEFVAL_A, 0)
        writeRegister(REGISTER_DEFVAL_B, 0)

        // set all default pin interrupt comparison behaviors
        writeRegister(REGISTER_INTCON_A, 0)
        writeRegister(REGISTER_INTCON_B, 0)

        // set all default pin states
        writeRegister(REGISTER_GPIO_A, currentStatesA)
        writeRegister(REGISTER_GPIO_B, currentStatesB)

        // set all default pin pull up resistors
        writeRegister(REGISTER_GPPU_A, currentPullupA)
        writeRegister(REGISTER_GPPU_B, currentPullupB)

    }

    // Set Input or output mode functions
    fun setMode(pin: MCP23017Pin, mode: PinMode) {
        // determine A or B port based on pin address
        try {
            if (pin.address < GPIO_B_OFFSET) {
                setModeA(pin, mode)
            } else {
                setModeB(pin, mode)
            }
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }

        // if any pins are configured as input pins, then we need to start the interrupt monitoring
        // thread
        if ((currentDirectionA > 0 || currentDirectionB > 0) && pollingTime != NO_POLLING_TIME) {
            // if the monitor has not been started, then start it now
            if (monitor == null) {
                // start monitoring thread
                monitor = GpioStateMonitor()
                monitor?.start()
            }
        } else {
            // shutdown and destroy monitoring thread since there are no input pins configured
            monitor?.shutdown()
            monitor = null
        }
    }

    @Throws(IOException::class)
    private fun setModeA(pin: MCP23017Pin, mode: PinMode) {
        // determine register and pin address
        val pinAddress = pin.address - GPIO_A_OFFSET

        // determine update direction value based on mode
        if (mode === PinMode.DIGITAL_INPUT) {
            currentDirectionA = currentDirectionA or pinAddress
        } else if (mode === PinMode.DIGITAL_OUTPUT) {
            currentDirectionA = currentDirectionA and pinAddress.inv()
        }

        // next update direction value
        writeRegister(REGISTER_IODIR_A, currentDirectionA)

        // enable interrupts; interrupt on any change from previous state
        writeRegister(REGISTER_GPINTEN_A, currentDirectionA)
    }

    @Throws(IOException::class)
    private fun setModeB(pin: MCP23017Pin, mode: PinMode) {
        // determine register and pin address
        val pinAddress = pin.address - GPIO_B_OFFSET

        // determine update direction value based on mode
        if (mode === PinMode.DIGITAL_INPUT) {
            currentDirectionB = currentDirectionB or pinAddress
        } else if (mode === PinMode.DIGITAL_OUTPUT) {
            currentDirectionB = currentDirectionB and pinAddress.inv()
        }

        // next update direction (mode) value
        writeRegister(REGISTER_IODIR_B, currentDirectionB)

        // enable interrupts; interrupt on any change from previous state
        writeRegister(REGISTER_GPINTEN_B, currentDirectionB)
    }

    fun getMode(pin: MCP23017Pin): PinMode {
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
    fun setState(pin: MCP23017Pin, state: PinState) {
        try {
            // determine A or B port based on pin address
            if (pin.address < GPIO_B_OFFSET) {
                setStateA(pin, state)
            } else {
                setStateB(pin, state)
            }
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }

    }

    @Throws(IOException::class)
    private fun setStateA(pin: MCP23017Pin, state: PinState) {
        // determine pin address
        val pinAddress = pin.address - GPIO_A_OFFSET

        // determine state value for pin bit
        if (state === PinState.HIGH) {
            currentStatesA = currentStatesA or pinAddress
        } else {
            currentStatesA = currentStatesA and pinAddress.inv()
        }

        // update state value
        writeRegister(REGISTER_GPIO_A, currentStatesA)
    }

    @Throws(IOException::class)
    private fun setStateB(pin: MCP23017Pin, state: PinState) {
        // determine pin address
        val pinAddress = pin.address - GPIO_B_OFFSET

        // determine state value for pin bit
        if (state === PinState.HIGH) {
            currentStatesB = currentStatesB or pinAddress
        } else {
            currentStatesB = currentStatesB and pinAddress.inv()
        }

        // update state value
        writeRegister(REGISTER_GPIO_B, currentStatesB)
    }

    // Get Input state functions
    fun getState(pin: MCP23017Pin): PinState {
        // determine A or B port based on pin address
        return if (pin.address < GPIO_B_OFFSET) {
            getStateA(pin) // get pin state
        } else {
            getStateB(pin) // get pin state
        }
    }

    private fun getStateA(pin: MCP23017Pin): PinState {

        try {
            currentStatesA = mDevice?.readRegByte(REGISTER_GPIO_A)?.toInt() ?: currentStatesA
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        // determine pin address
        val pinAddress = pin.address - GPIO_A_OFFSET

        // determine pin state

        return if (currentStatesA and pinAddress == pinAddress) PinState.HIGH else PinState.LOW
    }

    private fun getStateB(pin: MCP23017Pin): PinState {

        try {
            currentStatesB = mDevice?.readRegByte(REGISTER_GPIO_B)?.toInt() ?: currentStatesB
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        // determine pin address
        val pinAddress = pin.address - GPIO_B_OFFSET

        // determine pin state
        return if (currentStatesB and pinAddress == pinAddress) PinState.HIGH else PinState.LOW
    }

    // PullUps resistors mode functions for input Pins
    fun setPullResistance(pin: MCP23017Pin, resistance: PinPullResistance) {
        try {
            // determine A or B port based on pin address
            if (pin.address < GPIO_B_OFFSET) {
                setPullResistanceA(pin, resistance)
            } else {
                setPullResistanceB(pin, resistance)
            }
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }

    }

    @Throws(IOException::class)
    private fun setPullResistanceA(pin: MCP23017Pin, resistance: PinPullResistance) {
        // determine pin address
        val pinAddress = pin.address - GPIO_A_OFFSET

        // determine pull up value for pin bit
        if (resistance === PinPullResistance.PULL_UP) {
            currentPullupA = currentPullupA or pinAddress
        } else {
            currentPullupA = currentPullupA and pinAddress.inv()
        }

        // next update pull up resistor value
        writeRegister(REGISTER_GPPU_A, currentPullupA)
    }

    @Throws(IOException::class)
    private fun setPullResistanceB(pin: MCP23017Pin, resistance: PinPullResistance) {
        // determine pin address
        val pinAddress = pin.address - GPIO_B_OFFSET

        // determine pull up value for pin bit
        if (resistance === PinPullResistance.PULL_UP) {
            currentPullupB = currentPullupB or pinAddress
        } else {
            currentPullupB = currentPullupB and pinAddress.inv()
        }

        // next update pull up resistor value
        writeRegister(REGISTER_GPPU_B, currentPullupB)
    }

    fun getPullResistance(pin: MCP23017Pin): PinPullResistance {
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


    fun setPollingTime(pollingTime: Int) {
        this.pollingTime = pollingTime
    }

    /*fun registerPinListener(pin: MCP23017Pin, listener: MCP23017PinStateChangeListener): Boolean {
        if (getMode(pin) === PinMode.DIGITAL_INPUT) {
            val pinListeners = (mListeners as java.util.Map<MCP23017Pin, List<MCP23017PinStateChangeListener>>).computeIfAbsent(pin) { k -> ArrayList(1) }
            pinListeners.add(listener)
            return true
        } else {
            // Given pin not set for input
            return false
        }
    }

    fun unRegisterPinListener(pin: MCP23017Pin, listener: MCP23017PinStateChangeListener): Boolean {
        val pinListeners = mListeners[pin]
        return pinListeners != null && pinListeners.remove(listener)
    }*/

    @Throws(IOException::class)
    private fun checkInterrupt() {
        Timber.v("checkInterrupt")

        // only process for interrupts if a pin on port A is configured as an
        // input pin
        if (currentDirectionA > 0) {
            // process interrupts for port A
            val pinInterruptA = mDevice?.readRegByte(REGISTER_INTF_A)?.toInt() ?: -1
            Timber.v("checkInterrupt pinInterruptA:$pinInterruptA")

            // validate that there is at least one interrupt active on port A
            if (pinInterruptA > 0) {
                //TODO: We should think of reading INTCAP instead of GPIO to eliminate noise on
                // input and not loose other interrupt while reading GPIO
                // read the current pin states on port A
                val pinInterruptState = mDevice?.readRegByte(REGISTER_INTCAP_A)?.toInt() ?: -1
                Timber.v("checkInterrupt pinInterruptState:$pinInterruptState")

                // loop over the available pins on port B
                for (pin in MCP23017Pin.ALL_A_PINS) {
                    evaluatePinForChangeA(pin, pinInterruptState)
                }
            }
        }

        // only process for interrupts if a pin on port B is configured as an
        // input pin
        if (currentDirectionB > 0) {
            // process interrupts for port B
            val pinInterruptB = mDevice?.readRegByte(REGISTER_INTF_B)?.toInt() ?: -1
            Timber.v("checkInterrupt pinInterruptB:$pinInterruptB")

            // validate that there is at least one interrupt active on port B
            if (pinInterruptB > 0) {
                // read the current pin states on port B
                val pinInterruptState = mDevice?.readRegByte(REGISTER_INTCAP_B)?.toInt() ?: -1
                Timber.v("checkInterrupt pinInterruptState:$pinInterruptState")

                // loop over the available pins on port B
                for (pin in MCP23017Pin.ALL_B_PINS) {
                    evaluatePinForChangeB(pin, pinInterruptState)
                }
            }
        }
    }

    private fun evaluatePinForChangeA(pin: MCP23017Pin, state: Int) {
        // determine pin address
        val pinAddress = pin.address - GPIO_A_OFFSET

        if (state and pinAddress != currentStatesA and pinAddress) {
            val newState = if (state and pinAddress == pinAddress) PinState.HIGH else PinState.LOW

            // determine and cache state value for pin bit
            if (newState === PinState.HIGH) {
                currentStatesA = currentStatesA or pinAddress
            } else {
                currentStatesA = currentStatesA and pinAddress.inv()
            }

            dispatchPinChangeEvent(pin, newState)
        }
    }

    private fun evaluatePinForChangeB(pin: MCP23017Pin, state: Int) {
        // determine pin address
        val pinAddress = pin.address - GPIO_B_OFFSET

        if (state and pinAddress != currentStatesB and pinAddress) {
            val newState = if (state and pinAddress == pinAddress) PinState.HIGH else PinState.LOW

            // determine and cache state value for pin bit
            if (newState === PinState.HIGH) {
                currentStatesB = currentStatesB or pinAddress
            } else {
                currentStatesB = currentStatesB and pinAddress.inv()
            }

            // change detected for INPUT PIN
            // System.out.println("<<< CHANGE >>> " + pin.getName() + " : " + state);
            dispatchPinChangeEvent(pin, newState)
        }
    }

    private fun dispatchPinChangeEvent(pin: MCP23017Pin, state: PinState) {
        Timber.d("dispatchPinChangeEvent pin: " + pin.name + " pinState: " + state)

        val listeners = mListeners[pin]
        if (listeners != null) {
            // iterate over the pin listeners list
            for (listener in listeners) {
                listener.onPinStateChanged(pin, state)
            }
        }
    }

    /**
     * This class/thread is used to to actively monitor for GPIO interrupts
     *
     * @author Robert Savage
     */
    private inner class GpioStateMonitor : Thread() {
        private var shuttingDown = false

        fun shutdown() {
            shuttingDown = true
        }

        override fun run() {
            while (!shuttingDown) {
                try {
                    checkInterrupt()

                    // ... lets take a short breather ...
                    Thread.currentThread()
                    Thread.sleep(pollingTime.toLong())
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }

            }
        }

    }

}