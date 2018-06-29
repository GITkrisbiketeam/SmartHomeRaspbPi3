package com.krisbiketeam.smarthomeraspbpi3;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.things.contrib.driver.apa102.Apa102;
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;
import com.google.android.things.contrib.driver.ht16k33.Ht16k33;
import com.google.android.things.contrib.driver.pwmspeaker.Speaker;
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.krisbiketeam.smarthomeraspbpi3.driver.TMP102;
import com.krisbiketeam.smarthomeraspbpi3.units.HomeUnit;
import com.krisbiketeam.smarthomeraspbpi3.units.HomeUnitGpioActuator;
import com.krisbiketeam.smarthomeraspbpi3.units.HomeUnitGpioNoiseSensor;
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor;
import com.krisbiketeam.smarthomeraspbpi3.units.Unit;
import com.krisbiketeam.smarthomeraspbpi3.utils.Logger;
import com.krisbiketeam.smarthomeraspbpi3.utils.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github
 * .com/androidthings/contrib-drivers#readme</a>
 */
public class HomeActivity extends Activity implements Sensor.HomeUnitListener {
    private static final String TAG = Utils.getLogTag(HomeActivity.class);
    private static final String BUTTON_PIN_NAME = "BCM21"; // GPIO port wired to the button
    private static final int INTERVAL_BETWEEN_BLINKS_MS = 1000;
    private static final String LED_PIN_NAME = "BCM6"; // GPIO port wired to the LED

    final Map<String, Unit> mUnitList = new HashMap<>();

    private final Handler mHandler = new Handler();

    private Gpio mLedGpio;

    private Gpio mButtonGpio;

    private boolean mLedBlinking;

    private Speaker mBuzzer = null;

    private final int[] mRainbowLeds = new int[RainbowHat.LEDSTRIP_LENGTH];

    private DatabaseManager mDatabaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Logger.getInstance().setLogConsole(this);

        initializeFirebase();

        PeripheralManager periManager = PeripheralManager.getInstance();
        Logger.d(TAG, "Available GPIO: " + periManager.getGpioList());
        Logger.d(TAG, "Available I2C: " + periManager.getI2cBusList());

        try {
            // Step 1. Create GPIO connection.
            mButtonGpio = periManager.openGpio(BUTTON_PIN_NAME);
            // Step 2. Configure as an input.
            mButtonGpio.setDirection(Gpio.DIRECTION_IN);
            // Step 3. Enable edge trigger events.
            mButtonGpio.setEdgeTriggerType(Gpio.EDGE_FALLING);
            // Step 4. Register an event callback.
            mButtonGpio.registerGpioCallback(mCallback);

            mLedGpio = periManager.openGpio(LED_PIN_NAME);
            mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

        } catch (IOException e) {
            Logger.e(TAG, "Error on PeripheralIO API", e);
        }

        try {
            mBuzzer = RainbowHat.openPiezo();
        } catch (IOException e) {
            e.printStackTrace();
        }
        lightTheRainbow(false);

        HomeUnitGpioActuator ledB = new HomeUnitGpioActuator(new HomeUnit(BoardConfig.LED_B, ConnectionType.GPIO, "Raspberry Pi", BoardConfig
                .LED_B_PIN, null, null), Gpio.ACTIVE_HIGH, null);
        mUnitList.put(BoardConfig.LED_B, ledB);
        HomeUnitGpioActuator ledC = new HomeUnitGpioActuator(new HomeUnit(BoardConfig.LED_C, ConnectionType.GPIO, "Raspberry Pi", BoardConfig
                .LED_C_PIN, null, null), Gpio.ACTIVE_HIGH, null);
        mUnitList.put(BoardConfig.LED_C, ledC);

        Sensor buttonB = new HomeUnitGpioNoiseSensor(new HomeUnit(BoardConfig.BUTTON_B, ConnectionType.GPIO, "Raspberry Pi", BoardConfig
                .BUTTON_B_PIN, null, null), Gpio.ACTIVE_LOW, null);
        mUnitList.put(BoardConfig.BUTTON_B, buttonB);
        buttonB.registerListener(this);

        Sensor buttonC = new HomeUnitGpioNoiseSensor(new HomeUnit(BoardConfig.BUTTON_C, ConnectionType.GPIO, "Raspberry Pi", BoardConfig
                .BUTTON_C_PIN, null, null), Gpio.ACTIVE_LOW, null);
        mUnitList.put(BoardConfig.BUTTON_C, buttonC);
        buttonC.registerListener(this);

        Sensor motion = new HomeUnitGpioNoiseSensor(new HomeUnit(BoardConfig.MOTION_1, ConnectionType.GPIO, "Raspberry Pi", BoardConfig
                .MOTION_1_PIN, null, null), Gpio.ACTIVE_HIGH, null);
        mUnitList.put(BoardConfig.MOTION_1, motion);
        motion.registerListener(this);

        Sensor contactron = new HomeUnitGpioNoiseSensor(new HomeUnit(BoardConfig.CONTACT_1, ConnectionType.GPIO, "Raspberry Pi", BoardConfig
                .CONTACT_1_PIN, null, null), Gpio.ACTIVE_LOW, null);
        mUnitList.put(BoardConfig.CONTACT_1, contactron);
        contactron.registerListener(this);

    }
    private void initializeFirebase() {
        FirebaseApp.initializeApp(this);
        mDatabaseManager = new DatabaseManager();
        FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseAuth.signInAnonymously().addOnCompleteListener(this, task -> {
            if (task.isSuccessful()){
                Log.e(TAG, "FirebaseAuth:successful");
            } else {
                Log.e(TAG, "FirebaseAuth:failed", task.getException());
            }
        });
    }
    private void lightTheRainbow(boolean light) {
        // Light up the rainbow
        try {
            Apa102 ledstrip = RainbowHat.openLedStrip();
            ledstrip.setBrightness(1);
            for (int i = 0; i < mRainbowLeds.length; i++) {
                mRainbowLeds[i] = light? Color.HSVToColor(255, new float[]{i * 360.f / mRainbowLeds
                        .length, 1.0f, 1.0f}) : 0;
            }
            ledstrip.write(mRainbowLeds);
            // Close the device when done.
            ledstrip.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void lightOnOffOneRainbowLed(int led, boolean on) {
        // Light up the rainbow
        try {
            Apa102 ledstrip = RainbowHat.openLedStrip();
            ledstrip.setBrightness(1);
            mRainbowLeds[led] = on? Color.HSVToColor(255, new float[]{led * 360.f / mRainbowLeds
                        .length, 1.0f, 1.0f}) : 0;
            ledstrip.write(mRainbowLeds);
            // Close the device when done.
            ledstrip.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void displayTemperature(boolean show) {
        try {
            /*Bmx280 bmx280 = new Bmx280(BoardConfig.I2C);
            bmx280.setTemperatureOversampling(Bmx280.OVERSAMPLING_1X);
            float temperature = bmx280.readTemperature();
            bmx280.setPressureOversampling(Bmx280.OVERSAMPLING_1X);
            float pressure = bmx280.readPressure();
            bmx280.close();
            Logger.d(TAG, "temperature:" + temperature + " pressure: " + pressure);*/
            TMP102 tmp102 = new TMP102(BoardConfig.I2C);
            float temperature = tmp102.readTemperature();
            Logger.d(TAG, "temperature:" + temperature);
            tmp102.close();
            AlphanumericDisplay display = RainbowHat.openDisplay();
            display.setBrightness(Ht16k33.HT16K33_BRIGHTNESS_MAX);
            display.display(temperature);
            display.setEnabled(show);
            // Close the device when done.
            display.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Step 4. Register an event callback.
    private final GpioCallback mCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            Logger.i(TAG, "GPIO changed, button pressed");

            //Turn On Blinking
            if (mLedBlinking) {
                mHandler.removeCallbacks(mBlinkRunnable);
                mLedBlinking = false;
            } else {
                mHandler.post(mBlinkRunnable);
                mLedBlinking = true;
            }

            // Step 5. Return true to keep callback active.
            return true;
        }
    };

    private final Runnable mBlinkRunnable = new Runnable() {
        @Override
        public void run() {
            // Exit if the GPIO is already closed
            if (mLedGpio == null) {
                mLedBlinking = false;
                return;
            }

            try {
                // Step 3. Toggle the LED state
                mLedGpio.setValue(!mLedGpio.getValue());
                Logger.d(TAG, "run mLedGpio.readValue(): " + mLedGpio.getValue());
                displayTemperature(mLedGpio.getValue());
                // Step 4. Schedule another event after delay.
                mHandler.postDelayed(mBlinkRunnable, INTERVAL_BETWEEN_BLINKS_MS);
            } catch (IOException e) {
                Logger.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Step 6. Close the resource
        if (mButtonGpio != null) {
            mButtonGpio.unregisterGpioCallback(mCallback);
            try {
                mButtonGpio.close();
            } catch (IOException e) {
                Logger.e(TAG, "Error on PeripheralIO API", e);
            }
        }

        // Step 4. Remove handler events on close.
        mHandler.removeCallbacks(mBlinkRunnable);

        // Step 5. Close the resource.
        if (mLedGpio != null) {
            try {
                mLedGpio.close();
            } catch (IOException e) {
                Logger.e(TAG, "Error on PeripheralIO API", e);
            }
        }

        if (mBuzzer != null) {
            try {
                mBuzzer.close();
            } catch (IOException e) {
                Logger.e(TAG, "Error on PeripheralIO API", e);
            }
        }

        for (Unit unit : mUnitList.values()) {
            try {
                unit.close();
            } catch (Exception e) {
                Logger.e(TAG, "Error on PeripheralIO API", e);
            }
        }
        mUnitList.clear();
    }

    @Override
    public void onUnitChanged(@NonNull HomeUnit homeUnit, @Nullable Object value) {
        Logger.d(TAG, "onUnitChanged unit: " + homeUnit + " value: " + value);
        mDatabaseManager.addButtonPress(homeUnit);
        Unit unit;
        switch (homeUnit.getName()) {
            case BoardConfig.BUTTON_A:
                break;
            case BoardConfig.BUTTON_B:
                unit = mUnitList.get(BoardConfig.LED_B);
                if (unit instanceof HomeUnitGpioActuator && value != null) {
                    ((HomeUnitGpioActuator) unit).setValue(value);
                }
                try {
                    mBuzzer.play(440);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case BoardConfig.BUTTON_C:
                unit = mUnitList.get(BoardConfig.LED_C);
                if (unit instanceof HomeUnitGpioActuator && value != null) {
                    ((HomeUnitGpioActuator) unit).setValue(value);
                }
                try {
                    // Stop the buzzer.
                    mBuzzer.stop();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case BoardConfig.MOTION_1:
                if (value != null) {
                    lightOnOffOneRainbowLed(0, (Boolean) value);
                }
                break;
            case BoardConfig.CONTACT_1:
                if (value != null) {
                    lightOnOffOneRainbowLed(1, (Boolean) value);
                }
                break;
        }
    }
}
