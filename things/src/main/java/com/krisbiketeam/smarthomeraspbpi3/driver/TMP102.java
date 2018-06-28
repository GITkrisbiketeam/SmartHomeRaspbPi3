package com.krisbiketeam.smarthomeraspbpi3.driver;

import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import timber.log.Timber;

/**
 * Driver for the TMP102 temperature sensor.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class TMP102 implements AutoCloseable {

    /**
     * Default I2C address for the sensor.
     */
    public static final int DEFAULT_I2C_GND_ADDRESS = 0x48;
    public static final int DEFAULT_I2C_VCC_ADDRESS = 0x49;
    public static final int DEFAULT_I2C_SDA_ADDRESS = 0x4A;
    public static final int DEFAULT_I2C_SCL_ADDRESS = 0x4B;
    @Deprecated
    public static final int I2C_ADDRESS = DEFAULT_I2C_GND_ADDRESS;

    // Sensor constants from the datasheet.
    // https://cdn-shop.adafruit.com/datasheets/BST-BMP280-DS001-11.pdf
    /**
     * Minimum temperature in Celsius the sensor can measure.
     */
    public static final float MIN_TEMP_C = -25f;
    /**
     * Maximum temperature in Celsius the sensor can measure.
     */
    public static final float MAX_TEMP_C = 85f;
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

    /**
     * Extended mode EM.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NOT_EXTENDED_MODE, EXTENDED_MODE,})
    public @interface ExtendedMode {
    }

    public static final int NOT_EXTENDED_MODE = 0;      // default
    public static final int EXTENDED_MODE = 1;

    private static final int TMP102_EXTENDED_MODE_BIT_SHIFT = 4;

    /**
     * Extended mode CR1 and CR0.
     * //0 1 CONVERSION_RATE4 is default
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CONVERSION_RATE025, CONVERSION_RATE1, CONVERSION_RATE4, CONVERSION_RATE8,})
    public @interface ConversionRate {
    }

    public static final int CONVERSION_RATE025 = 0;
    public static final int CONVERSION_RATE1 = 1;
    public static final int CONVERSION_RATE4 = 2;
    public static final int CONVERSION_RATE8 = 3;

    private static final int TMP102_CONVERSION_RATE_BIT_SHIFT = 6;
    private static final int TMP102_CONVERSION_RATE_MASK = 0b11000000;

    /**
     * Shutdown mode SD.
     * The Shutdown mode bit saves maximum power by Figure 10. Output Transfer Function Diagrams
     * shutting down all device circuitry other than the serial interface, reducing current
     * consumption to typically less than 0.5mA. Shutdown mode is enabled when the SD bit is '1';
     * the device shuts down when current conversion is completed. When SD is equal to '0', the A
     * fault condition exists when the measured device maintains a continuous conversion state.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NOT_SHUTDOWN_MODE, SHUTDOWN_MODE,})
    public @interface ShutdownMode {
    }

    public static final int NOT_SHUTDOWN_MODE = 0;
    public static final int SHUTDOWN_MODE = 1;

    private static final int TMP102_SHUTDOWN_MODE_BIT_SHIFT = 8;


    // Registers
    private static final int TMP102_REG_TEMP = 0x00;
    @VisibleForTesting
    static final int TMP102_REG_CONF = 0x01;
    private static final int TMP102_REG_T_LOW = 0x02;
    private static final int TMP102_REG_T_HIGH = 0x03;

    private static final float TEMP_REG_FACTOR = 0.0625f;

    private static final int BMX280_OVERSAMPLING_PRESSURE_MASK = 0b00011100;
    private static final int BMX280_OVERSAMPLING_PRESSURE_BITSHIFT = 2;
    private static final int BMX280_OVERSAMPLING_TEMPERATURE_MASK = 0b11100000;
    private static final int BMX280_OVERSAMPLING_TEMPERATURE_BITSHIFT = 5;

    private I2cDevice mDevice;
    private final byte[] mBuffer = new byte[2]; // for reading sensor values
    private boolean mEnabled;
    private int mConfig;

    /**
     * Create a new TMP102 sensor driver connected on the given bus.
     *
     * @param bus I2C bus the sensor is connected to.
     * @throws IOException
     */
    public TMP102(String bus) throws IOException {
        this(bus, DEFAULT_I2C_GND_ADDRESS);
    }

    /**
     * Create a new TMP102 sensor driver connected on the given bus and address.
     *
     * @param bus     I2C bus the sensor is connected to.
     * @param address I2C address of the sensor.
     * @throws IOException
     */
    public TMP102(String bus, int address) throws IOException {
        PeripheralManager pioService = PeripheralManager.getInstance();
        I2cDevice device = pioService.openI2cDevice(bus, address);
        try {
            connect(device);
        } catch (IOException | RuntimeException e) {
            try {
                close();
            } catch (IOException | RuntimeException ignored) {
            }
            throw e;
        }
    }

    /**
     * Create a new TMP102 sensor driver connected to the given I2c device.
     *
     * @param device I2C device of the sensor.
     * @throws IOException
     */
    /*package*/  TMP102(I2cDevice device) throws IOException {
        connect(device);
    }

    private void connect(I2cDevice device) throws IOException {
        mDevice = device;
        mConfig = readSample16(TMP102_REG_CONF);
        Timber.d("connect mConfig: " + mConfig);
    }

    /**
     * Close the driver and the underlying device.
     */
    @Override
    public void close() throws IOException {
        if (mDevice != null) {
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    /**
     * Check if TMP102 is in Extended Mode where 13-bits of Temperature register are read
     *
     * @return true if sensor is in EM (Extended Mode) mode
     * @throws IOException
     */
    public boolean isExtendedMode() throws IOException {
        // is this really needed?
        mConfig = readSample16(TMP102_REG_CONF);
        return isExtendedMode(mConfig);
    }

    @VisibleForTesting
    boolean isExtendedMode(int config) {
        return (1 << TMP102_EXTENDED_MODE_BIT_SHIFT & config) > 0;
    }

    /**
     * Set TMP102  in Extended Mode where 13-bits of Temperature register are read.
     *
     * @param extended true if we want to read 13 bit temperature register (Extended Mode)
     * @throws IOException
     */
    public void setExtendedMode(boolean extended) throws IOException {
        // is this really needed?
        mConfig = readSample16(TMP102_REG_CONF);
        setExtendedMode(mConfig, extended);
    }

    @VisibleForTesting
    int setExtendedMode(int config, boolean extended) throws IOException {
        if (extended) {
            config |= EXTENDED_MODE << TMP102_EXTENDED_MODE_BIT_SHIFT;
        } else {
            config &= ~(EXTENDED_MODE << TMP102_EXTENDED_MODE_BIT_SHIFT);
        }
        writeSample16(TMP102_REG_CONF, config);
        return config;
    }

    /**
     * Set the power mode of the sensor.
     *
     * @param mode power mode.
     * @throws IOException
     */
    public void setConversionRateMode(@ConversionRate int mode) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        mConfig = readSample16(TMP102_REG_CONF);
        switch (mode) {
            case CONVERSION_RATE025:
            case CONVERSION_RATE1:
            case CONVERSION_RATE4:
            case CONVERSION_RATE8:
                mConfig &= ~TMP102_CONVERSION_RATE_MASK;
                mConfig |= mode << TMP102_CONVERSION_RATE_BIT_SHIFT;
                break;
            default:
                Timber.w("setConversionRateMode wrong Conversion mode");
        }
        writeSample16(TMP102_REG_CONF, mConfig);
    }

    /**
     * Read the current temperature.
     *
     * @return the current temperature in degrees Celsius
     */
    public float readTemperature() throws IOException {
        int rawTemp = readSample16(TMP102_REG_TEMP);
        return calculateTemperature(rawTemp);
    }

    /**
     * Reads 16 bits from the given address.
     *
     * @throws IOException
     */
    private int readSample16(int address) throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device is already closed");
        }
        synchronized (mBuffer) {
            // Reading a byte buffer instead of a short to avoid having to deal with
            // platform-specific endianness.
            mDevice.readRegBuffer(address, mBuffer, 2);
            // msb[7:0] lsb[7:0]

            int msb = mBuffer[0] & 0xff;
            int lsb = mBuffer[1] & 0xff;
            return msb << 8 | lsb;
        }
    }

    private void writeSample16(int address, int data) throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device is already closed");
        }
        synchronized (mBuffer) {
            //msb
            mBuffer[0] = (byte) (data >> 8);
            mBuffer[1] = (byte) (data);
            // Reading a byte buffer instead of a short to avoid having to deal with
            // platform-specific endianness.
            mDevice.writeRegBuffer(address, mBuffer, 2);
        }
    }

    /**
     * Calculate real temperature in Celsius degree from Raw temp value
     *
     * @param rawTemp Raw temperature returned from TMP102 Sensor
     * @return
     */
    @VisibleForTesting
    float calculateTemperature(int rawTemp) {
        if (rawTemp < 0x8000) {
            // Check if raw Data is not in extended EM 13 bit mode
            if ((rawTemp & 0x01) > 0) {
                return (rawTemp >> 3) * TEMP_REG_FACTOR;
            } else {
                return (rawTemp >> 4) * TEMP_REG_FACTOR;
            }
        } else {    // Negative number of 2's compliment
            // Check if raw Data is not in extended EM 13 bit mode
            if ((rawTemp & 0x01f) > 0) {
                rawTemp = ~(rawTemp - 0b1000);
                rawTemp = (rawTemp & 0xFFFF);
                return (rawTemp >> 3) * -TEMP_REG_FACTOR;
            } else {
                rawTemp = ~(rawTemp - 0b10000);
                rawTemp = (rawTemp & 0xFFFF);
                return (rawTemp >> 4) * -TEMP_REG_FACTOR;
            }
        }
    }
}

