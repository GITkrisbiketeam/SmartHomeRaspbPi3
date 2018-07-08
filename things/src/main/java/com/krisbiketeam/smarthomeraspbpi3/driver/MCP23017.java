package com.krisbiketeam.smarthomeraspbpi3.driver;

import android.support.annotation.Nullable;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;
import com.krisbiketeam.smarthomeraspbpi3.driver.MCP23017Pin.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Driver for the MCP23017 16 bit I/O Expander.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class MCP23017 implements AutoCloseable {

    /**
     * Default I2C address for the Expander.
     */
    public static final int DEFAULT_I2C_000_ADDRESS = 0x20;
    public static final int DEFAULT_I2C_001_ADDRESS = 0x21;
    public static final int DEFAULT_I2C_010_ADDRESS = 0x22;
    public static final int DEFAULT_I2C_011_ADDRESS = 0x23;
    public static final int DEFAULT_I2C_100_ADDRESS = 0x24;
    public static final int DEFAULT_I2C_101_ADDRESS = 0x25;
    public static final int DEFAULT_I2C_110_ADDRESS = 0x26;
    public static final int DEFAULT_I2C_111_ADDRESS = 0x27;

    @Deprecated
    public static final int I2C_ADDRESS = DEFAULT_I2C_000_ADDRESS;

    /**
     * Maximum power consumption in micro-amperes when measuring temperature.
     */
    public static final float MAX_POWER_CONSUMPTION_TEMP_UA = 85f;
    /**
     * Maximum frequency of the measurements.
     */
    public static final float MAX_FREQ_HZ = 8f;
    /**
     * Minimum frequency of the measurements.
     */
    public static final float MIN_FREQ_HZ = 1f;

    // Sensor constants from the datasheet.
    // https://cdn-shop.adafruit.com/datasheets/BST-BMP280-DS001-11.pdf
    public static final int DEFAULT_POLLING_TIME = 50;
    public static final int NO_POLLING_TIME = -1;

    /// Registers all are 8 bit long
    // Direction registers
    // I/O DIRECTION REGISTER: Input - 1 or output - 0; def 0
    private static final int REGISTER_IODIR_A = 0x00;
    private static final int REGISTER_IODIR_B = 0x01;
    // INPUT POLARITY PORT REGISTER; GPIO bit will be inverted LOW/HIGH; def 0
    private static final int REGISTER_IPOL_A = 0x02;
    private static final int REGISTER_IPOL_B = 0x03;
    // INTERRUPT-ON-CHANGE PINS; enable interrupts - 1 or disable interrupts - 0; def 0; see
    // INTCON and DEFVAL; def 0
    private static final int REGISTER_GPINTEN_A = 0x04;
    private static final int REGISTER_GPINTEN_B = 0x05;
    // DEFAULT VALUE REGISTER; if GPIO differs from DEFVAL interrupt is triggered; def 0
    private static final int REGISTER_DEFVAL_A = 0x06;
    private static final int REGISTER_DEFVAL_B = 0x07;
    // INTERRUPT-ON-CHANGE CONTROL REGISTER; if set - 1 GPIO is compared with DEFVAL or if cleard
    // - 0 interrupt is triggered when GPIO changed compared to its previous state; def 0
    private static final int REGISTER_INTCON_A = 0x08;
    private static final int REGISTER_INTCON_B = 0x09;
    // I/O EXPANDER CONFIGURATION REGISTER; MIRROR 1 INTA and INTB are internally connected to
    // i=one INT def not connected; INTPOL: 0 active Low (DEFAULT) 1 active high; etc; def 0
    private static final int REGISTER_IOCON = 0x0A; //0x0B
    // GPIO PULL-UP RESISTOR REGISTER only for IODIR input pins 0 disabled, 1 enabled with 100k;
    // def 0
    private static final int REGISTER_GPPU_A = 0x0C;
    private static final int REGISTER_GPPU_B = 0x0D;
    // Read only INTERRUPT FLAG REGISTER; if 1 interrupt was on that pin
    private static final int REGISTER_INTF_A = 0x0E;
    private static final int REGISTER_INTF_B = 0x0F;
    // Read only INTERRUPT CAPTURED VALUE FOR PORT REGISTER; These bits reflect the logic level
    // on the port pins at the time of interrupt due to pin change cleared on read of INTCAP or GPIO
    private static final int REGISTER_INTCAP_A = 0x10;
    private static final int REGISTER_INTCAP_B = 0x11;
    // GENERAL PURPOSE I/O PORT REGISTER; The GPIO register reflects the value on the port.
    //Reading from this register reads the port. Writing to this
    //register modifies the Output Latch (OLAT) register
    private static final int REGISTER_GPIO_A = 0x12;
    private static final int REGISTER_GPIO_B = 0x13;

    private static final int GPIO_A_OFFSET = 0;
    private static final int GPIO_B_OFFSET = 1000;

    private int currentStatesA = 0;
    private int currentStatesB = 0;
    private int currentDirectionA = 0;
    private int currentDirectionB = 0;
    private int currentPullupA = 0;
    private int currentPullupB = 0;

    private int pollingTime = DEFAULT_POLLING_TIME;

    private boolean i2cBusOwner = false;
    private GpioStateMonitor monitor = null;

    private final String mBus;
    private final int mAddress;

    private I2cDevice mDevice;
    private Gpio mIntAGpio;
    private Gpio mIntBGpio;

    private final Map<MCP23017Pin, List<MCP23017PinStateChangeListener>> mListeners = new
            HashMap<>();

    /**
     * Create a new MCP23017  driver connected on the given bus.
     *
     * @param bus I2C bus the sensor is connected to.
     * @throws IOException
     */
    public MCP23017(String bus) throws IOException {
        this(bus, DEFAULT_I2C_000_ADDRESS, DEFAULT_POLLING_TIME, null, null);
    }

    public MCP23017(String bus, String intAGpio) throws IOException {
        this(bus, DEFAULT_I2C_000_ADDRESS, NO_POLLING_TIME, intAGpio, null);
    }

    public MCP23017(String bus, String intAGpio, String intBGpio) throws IOException {
        this(bus, DEFAULT_I2C_000_ADDRESS, NO_POLLING_TIME, intAGpio, intBGpio);
    }

    /**
     * Create a new TMP102 sensor driver connected on the given bus and address.
     *
     * @param bus     I2C bus the sensor is connected to.
     * @param address I2C address of the sensor.
     * @throws IOException
     */
    public MCP23017(String bus, int address) throws IOException {
        this(bus, address, DEFAULT_POLLING_TIME, null, null);
    }

    public MCP23017(String bus, int address, String intAGpio) throws IOException {
        this(bus, address, NO_POLLING_TIME, intAGpio, null);
    }

    public MCP23017(String bus, int address, String intAGpio, String intBGpio) throws IOException {
        this(bus, address, NO_POLLING_TIME, intAGpio, intBGpio);
    }


    public MCP23017(String bus, int address, int pollingTime, String intAGpio, String intBGpio)
            throws IOException {
        mBus = bus;
        mAddress = address;
        try {
            connectI2c(null);
            if (intAGpio != null || intBGpio != null) {
                connectGpio(intAGpio, intBGpio);
            }
        } catch (IOException | RuntimeException e) {
            try {
                close();
            } catch (IOException | RuntimeException ignored) {
            }
            throw e;
        }
        this.pollingTime = pollingTime;
    }

    /**
     * Create a new TMP102 sensor driver connected to the given I2c device.
     *
     * @param device I2C device of the sensor.
     * @throws IOException
     */
    /*package*/  MCP23017(I2cDevice device) throws IOException {
        mBus = "";
        mAddress = 0;
        connectI2c(device);
    }


    private void connectI2c(@Nullable I2cDevice device) throws IOException {
        if (device == null) {
            reconnectI2c();
        } else {
            mDevice = device;
        }

        if (mDevice == null) {
            throw new IllegalStateException("I2C device could not be open");
        }

        // read initial GPIO pin states
        currentStatesA = mDevice.readRegByte(REGISTER_GPIO_A) & 0xff;
        currentStatesB = mDevice.readRegByte(REGISTER_GPIO_B) & 0xff;

        int currentConf = mDevice.readRegByte(REGISTER_IOCON) & 0xff;

        Timber.d("connect currentStatesA: " + currentStatesA + " currentStatesB: " +
                currentStatesA + " currentConf: " + currentConf);
        resetToDefaults();
    }

    private void connectGpio(String intAGpio, String intBGpio) throws IOException {
        PeripheralManager manager = PeripheralManager.getInstance();
        if (intAGpio != null) {
            Timber.d("connectGpio intAGpio openGpio " + intAGpio);
            // Step 1. Create GPIO connection.
            mIntAGpio = manager.openGpio(intAGpio);
            // Step 2. Configure as an input.
            mIntAGpio.setDirection(Gpio.DIRECTION_IN);
            // Step 3. Enable edge trigger events.
            mIntAGpio.setEdgeTriggerType(Gpio.EDGE_RISING);    // INT active Low
            // Step 4. Register an event callback.
            mIntAGpio.registerGpioCallback(mIntACallback);
        }
        if (intBGpio != null) {
            Timber.d("connectGpio intBGpio openGpio " + intBGpio);
            // Step 1. Create GPIO connection.
            mIntBGpio = manager.openGpio(intBGpio);
            // Step 2. Configure as an input.
            mIntBGpio.setDirection(Gpio.DIRECTION_IN);
            // Step 3. Enable edge trigger events.
            mIntBGpio.setEdgeTriggerType(Gpio.EDGE_RISING);    // INT active Low
            // Step 4. Register an event callback.
            mIntBGpio.registerGpioCallback(mIntBCallback);
        }
    }

    private void reconnectI2c() throws IOException {
        mDevice = PeripheralManager.getInstance().openI2cDevice(mBus, mAddress);
    }

    private void closeI2c() throws IOException {
        if (mDevice != null) {
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    /**
     * Close the driver and the underlying device.
     */
    @Override
    public void close() throws IOException {
        // if a monitor is running, then shut it down now
        if (monitor != null) {
            // shutdown monitoring thread
            monitor.shutdown();
            monitor = null;
        }

        closeI2c();

        if (mIntAGpio != null) {
            mIntAGpio.unregisterGpioCallback(mIntACallback);
            try {
                mIntAGpio.close();
            } finally {
                mIntAGpio = null;
            }
        }
        if (mIntBGpio != null) {
            mIntBGpio.unregisterGpioCallback(mIntBCallback);
            try {
                mIntBGpio.close();
            } finally {
                mIntBGpio = null;
            }
        }
    }

    private final GpioCallback mIntACallback = gpio -> {
        try {
            Timber.d("mIntACallback onGpioEdge " + gpio.getValue());
            checkInterrupt();
        } catch (IOException e) {
            Timber.e("mIntACallback onGpioEdge exception", e);
        }
        // Return true to keep callback active.
        return true;
    };

    private final GpioCallback mIntBCallback = gpio -> {
        try {
            Timber.d("mIntBCallback onGpioEdge " + gpio.getValue());
            checkInterrupt();
        } catch (IOException e) {
            Timber.e("mIntBCallback onGpioEdge exception", e);
        }
        // Return true to keep callback active.
        return true;
    };

    public void resetToDefaults() throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device is already closed");
        }

        // set all default pins directions
        mDevice.writeRegByte(REGISTER_IODIR_A, (byte) currentDirectionA);
        mDevice.writeRegByte(REGISTER_IODIR_B, (byte) currentDirectionB);

        // set all default pin interrupts
        mDevice.writeRegByte(REGISTER_GPINTEN_A, (byte) currentDirectionA);
        mDevice.writeRegByte(REGISTER_GPINTEN_B, (byte) currentDirectionB);

        // set all default pin interrupt default values
        mDevice.writeRegByte(REGISTER_DEFVAL_A, (byte) 0x00);
        mDevice.writeRegByte(REGISTER_DEFVAL_B, (byte) 0x00);

        // set all default pin interrupt comparison behaviors
        mDevice.writeRegByte(REGISTER_INTCON_A, (byte) 0x00);
        mDevice.writeRegByte(REGISTER_INTCON_B, (byte) 0x00);

        // set all default pin states
        mDevice.writeRegByte(REGISTER_GPIO_A, (byte) currentStatesA);
        mDevice.writeRegByte(REGISTER_GPIO_B, (byte) currentStatesB);

        // set all default pin pull up resistors
        mDevice.writeRegByte(REGISTER_GPPU_A, (byte) currentPullupA);
        mDevice.writeRegByte(REGISTER_GPPU_B, (byte) currentPullupB);

    }

    // Set Input or output mode functions
    public void setMode(MCP23017Pin pin, PinMode mode) {
        // determine A or B port based on pin address
        try {
            if (pin.getAddress() < GPIO_B_OFFSET) {
                setModeA(pin, mode);
            } else {
                setModeB(pin, mode);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        // if any pins are configured as input pins, then we need to start the interrupt monitoring
        // thread
        if ((currentDirectionA > 0 || currentDirectionB > 0) && pollingTime != NO_POLLING_TIME) {
            // if the monitor has not been started, then start it now
            if (monitor == null) {
                // start monitoring thread
                monitor = new GpioStateMonitor(mDevice);
                monitor.start();
            }
        } else {
            // shutdown and destroy monitoring thread since there are no input pins configured
            if (monitor != null) {
                monitor.shutdown();
                monitor = null;
            }
        }
    }

    private void setModeA(MCP23017Pin pin, PinMode mode) throws IOException {
        // determine register and pin address
        int pinAddress = pin.getAddress() - GPIO_A_OFFSET;

        // determine update direction value based on mode
        if (mode == PinMode.DIGITAL_INPUT) {
            currentDirectionA |= pinAddress;
        } else if (mode == PinMode.DIGITAL_OUTPUT) {
            currentDirectionA &= ~pinAddress;
        }

        // next update direction value
        mDevice.writeRegByte(REGISTER_IODIR_A, (byte) currentDirectionA);

        // enable interrupts; interrupt on any change from previous state
        mDevice.writeRegByte(REGISTER_GPINTEN_A, (byte) currentDirectionA);
    }

    private void setModeB(MCP23017Pin pin, PinMode mode) throws IOException {
        // determine register and pin address
        int pinAddress = pin.getAddress() - GPIO_B_OFFSET;

        // determine update direction value based on mode
        if (mode == PinMode.DIGITAL_INPUT) {
            currentDirectionB |= pinAddress;
        } else if (mode == PinMode.DIGITAL_OUTPUT) {
            currentDirectionB &= ~pinAddress;
        }

        // next update direction (mode) value
        mDevice.writeRegByte(REGISTER_IODIR_B, (byte) currentDirectionB);

        // enable interrupts; interrupt on any change from previous state
        mDevice.writeRegByte(REGISTER_GPINTEN_B, (byte) currentDirectionB);
    }

    public PinMode getMode(MCP23017Pin pin) {
        int address = pin.getAddress();
        if (address < GPIO_B_OFFSET) {
            if ((currentDirectionA & address) != 0) {
                return PinMode.DIGITAL_INPUT;
            }
        } else {
            address -= GPIO_B_OFFSET;
            if ((currentDirectionB & address) != 0) {
                return PinMode.DIGITAL_INPUT;
            }
        }
        return PinMode.DIGITAL_OUTPUT;
    }

    // Set Output state functions
    public void setState(MCP23017Pin pin, PinState state) {
        try {
            // determine A or B port based on pin address
            if (pin.getAddress() < GPIO_B_OFFSET) {
                setStateA(pin, state);
            } else {
                setStateB(pin, state);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void setStateA(MCP23017Pin pin, PinState state) throws IOException {
        // determine pin address
        int pinAddress = pin.getAddress() - GPIO_A_OFFSET;

        // determine state value for pin bit
        if (state == PinState.HIGH) {
            currentStatesA |= pinAddress;
        } else {
            currentStatesA &= ~pinAddress;
        }

        // update state value
        mDevice.writeRegByte(REGISTER_GPIO_A, (byte) currentStatesA);
    }

    private void setStateB(MCP23017Pin pin, PinState state) throws IOException {
        // determine pin address
        int pinAddress = pin.getAddress() - GPIO_B_OFFSET;

        // determine state value for pin bit
        if (state == PinState.HIGH) {
            currentStatesB |= pinAddress;
        } else {
            currentStatesB &= ~pinAddress;
        }

        // update state value
        mDevice.writeRegByte(REGISTER_GPIO_B, (byte) currentStatesB);
    }

    // Get Input state functions
    public PinState getState(MCP23017Pin pin) {
        // determine A or B port based on pin address
        if (pin.getAddress() < GPIO_B_OFFSET) {
            return getStateA(pin); // get pin state
        } else {
            return getStateB(pin); // get pin state
        }
    }

    private PinState getStateA(MCP23017Pin pin) {

        try {
            currentStatesA = mDevice.readRegByte(REGISTER_GPIO_A) & 0xff;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // determine pin address
        int pinAddress = pin.getAddress() - GPIO_A_OFFSET;

        // determine pin state

        return (currentStatesA & pinAddress) == pinAddress ? PinState.HIGH : PinState.LOW;
    }

    private PinState getStateB(MCP23017Pin pin) {

        try {
            currentStatesB = mDevice.readRegByte(REGISTER_GPIO_B) & 0xff;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // determine pin address
        int pinAddress = pin.getAddress() - GPIO_B_OFFSET;

        // determine pin state
        return (currentStatesB & pinAddress) == pinAddress ? PinState.HIGH : PinState.LOW;
    }

    // PullUps resistors mode functions for input Pins
    public void setPullResistance(MCP23017Pin pin, PinPullResistance resistance) {
        try {
            // determine A or B port based on pin address
            if (pin.getAddress() < GPIO_B_OFFSET) {
                setPullResistanceA(pin, resistance);
            } else {
                setPullResistanceB(pin, resistance);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void setPullResistanceA(MCP23017Pin pin, PinPullResistance resistance) throws
            IOException {
        // determine pin address
        int pinAddress = pin.getAddress() - GPIO_A_OFFSET;

        // determine pull up value for pin bit
        if (resistance == PinPullResistance.PULL_UP) {
            currentPullupA |= pinAddress;
        } else {
            currentPullupA &= ~pinAddress;
        }

        // next update pull up resistor value
        mDevice.writeRegByte(REGISTER_GPPU_A, (byte) currentPullupA);
    }

    private void setPullResistanceB(MCP23017Pin pin, PinPullResistance resistance) throws
            IOException {
        // determine pin address
        int pinAddress = pin.getAddress() - GPIO_B_OFFSET;

        // determine pull up value for pin bit
        if (resistance == PinPullResistance.PULL_UP) {
            currentPullupB |= pinAddress;
        } else {
            currentPullupB &= ~pinAddress;
        }

        // next update pull up resistor value
        mDevice.writeRegByte(REGISTER_GPPU_B, (byte) currentPullupB);
    }

    public PinPullResistance getPullResistance(MCP23017Pin pin) {
        int address = pin.getAddress();
        if (address < GPIO_B_OFFSET) {
            if ((currentPullupA & address) != 0) {
                return PinPullResistance.PULL_UP;
            }
        } else {
            address -= GPIO_B_OFFSET;
            if ((currentPullupB & address) != 0) {
                return PinPullResistance.PULL_UP;
            }
        }
        return PinPullResistance.OFF;
    }


    public void setPollingTime(int pollingTime) {
        this.pollingTime = pollingTime;
    }

    public boolean registerPinListener(MCP23017Pin pin, MCP23017PinStateChangeListener listener) {
        if (getMode(pin) == PinMode.DIGITAL_INPUT) {
            List<MCP23017PinStateChangeListener> pinListeners = mListeners.computeIfAbsent(pin, k
                    -> new ArrayList<>(1));
            pinListeners.add(listener);
            return true;
        } else {
            // Given pin not set for input
            return false;
        }
    }

    public boolean unRegisterPinListener(MCP23017Pin pin, MCP23017PinStateChangeListener listener) {
        List<MCP23017PinStateChangeListener> pinListeners = mListeners.get(pin);
        return pinListeners != null && pinListeners.remove(listener);
    }

    private void checkInterrupt() throws IOException {
        Timber.v("checkInterrupt");

        // only process for interrupts if a pin on port A is configured as an
        // input pin
        if (currentDirectionA > 0) {
            // process interrupts for port A
            int pinInterruptA = mDevice.readRegByte(REGISTER_INTF_A) & 0xff;
            Timber.v("checkInterrupt pinInterruptA:" + pinInterruptA);

            // validate that there is at least one interrupt active on port A
            if (pinInterruptA > 0) {
                //TODO: We should think of reading INTCAP instead of GPIO to eliminate noise on
                // input and not loose other interrupt while reading GPIO
                // read the current pin states on port A
                int pinInterruptState = mDevice.readRegByte(REGISTER_INTCAP_A) & 0xff;
                Timber.v("checkInterrupt pinInterruptState:" + pinInterruptState);

                // loop over the available pins on port B
                for (MCP23017Pin pin : MCP23017Pin.ALL_A_PINS) {
                    evaluatePinForChangeA(pin, pinInterruptState);
                }
            }
        }

        // only process for interrupts if a pin on port B is configured as an
        // input pin
        if (currentDirectionB > 0) {
            // process interrupts for port B
            int pinInterruptB = mDevice.readRegByte(REGISTER_INTF_B) & 0xff;
            Timber.v("checkInterrupt pinInterruptB:" + pinInterruptB);

            // validate that there is at least one interrupt active on port B
            if (pinInterruptB > 0) {
                // read the current pin states on port B
                int pinInterruptState = mDevice.readRegByte(REGISTER_INTCAP_B) & 0xff;
                Timber.v("checkInterrupt pinInterruptState:" + pinInterruptState);

                // loop over the available pins on port B
                for (MCP23017Pin pin : MCP23017Pin.ALL_B_PINS) {
                    evaluatePinForChangeB(pin, pinInterruptState);
                }
            }
        }
    }

    private void evaluatePinForChangeA(MCP23017Pin pin, int state) {
        // determine pin address
        int pinAddress = pin.getAddress() - GPIO_A_OFFSET;

        if ((state & pinAddress) != (currentStatesA & pinAddress)) {
            PinState newState = (state & pinAddress) == pinAddress ? PinState.HIGH : PinState.LOW;

            // determine and cache state value for pin bit
            if (newState == PinState.HIGH) {
                currentStatesA |= pinAddress;
            } else {
                currentStatesA &= ~pinAddress;
            }

            dispatchPinChangeEvent(pin, newState);
        }
    }

    private void evaluatePinForChangeB(MCP23017Pin pin, int state) {
        // determine pin address
        int pinAddress = pin.getAddress() - GPIO_B_OFFSET;

        if ((state & pinAddress) != (currentStatesB & pinAddress)) {
            PinState newState = (state & pinAddress) == pinAddress ? PinState.HIGH : PinState.LOW;

            // determine and cache state value for pin bit
            if (newState == PinState.HIGH) {
                currentStatesB |= pinAddress;
            } else {
                currentStatesB &= ~pinAddress;
            }

            // change detected for INPUT PIN
            // System.out.println("<<< CHANGE >>> " + pin.getName() + " : " + state);
            dispatchPinChangeEvent(pin, newState);
        }
    }

    private void dispatchPinChangeEvent(MCP23017Pin pin, PinState state) {
        Timber.d("dispatchPinChangeEvent pin: " + pin.getName() + " pinState: " + state);

        List<MCP23017PinStateChangeListener> listeners = mListeners.get(pin);
        if (listeners != null) {
            // iterate over the pin listeners list
            for (MCP23017PinStateChangeListener listener : listeners) {
                listener.onPinStateChanged(pin, state);
            }
        }
    }

    /**
     * This class/thread is used to to actively monitor for GPIO interrupts
     *
     * @author Robert Savage
     */
    private class GpioStateMonitor extends Thread {
        private final I2cDevice device;
        private boolean shuttingDown = false;

        public GpioStateMonitor(I2cDevice device) {
            this.device = device;
        }

        public void shutdown() {
            shuttingDown = true;
        }

        @Override
        public void run() {
            while (!shuttingDown) {
                try {
                    checkInterrupt();

                    // ... lets take a short breather ...
                    Thread.currentThread();
                    Thread.sleep(pollingTime);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }


    }

}