package com.krisbiketeam.smarthomeraspbpi3.units;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import com.krisbiketeam.smarthomeraspbpi3.ConnectionType;
import com.krisbiketeam.smarthomeraspbpi3.utils.Logger;
import com.krisbiketeam.smarthomeraspbpi3.utils.Utils;

import java.io.IOException;

public abstract class HomeUnitGpio extends HomeUnit {
    private static final String TAG = Utils.getLogTag(HomeUnitGpio.class);

    protected Gpio mGpio;

    public HomeUnitGpio(String name, String location, String pinName, int activeType) {
        super(name, ConnectionType.GPIO, location, pinName, null);
        PeripheralManager peripheralManager = PeripheralManager.getInstance();
        try {
            Gpio gpio = peripheralManager.openGpio(pinName);
            connect(gpio, activeType);
        } catch (IOException e) {
            close();
        }
    }

    /**
     * Constructor invoked from unit tests.
     */
    @VisibleForTesting
    /*package*/ HomeUnitGpio(Gpio buttonGpio, int activeType) {
        connect(buttonGpio, activeType);
    }

    public abstract void connect(Gpio gpio, int activeType);

    @Override
    @Nullable
    public Object readValue() {
        return readValue(mGpio);
    }

    public Object readValue(Gpio gpio) {
        if (gpio != null) {
            try {
                // Read the active high pin state
                if (gpio.getValue()) {
                    // Pin is HIGH
                    return Boolean.TRUE;
                } else {
                    // Pin is LOW
                    return Boolean.FALSE;
                }
            } catch (IOException e) {
                Logger.e(TAG, "Error getting Value PeripheralIO API on:" + this.toString(), e);
            }
        }
        return null;
    }

    @Override
    public void close() {
        if (mGpio != null) {
            try {
                mGpio.close();
            } catch (IOException e) {
                Logger.e(TAG, "Error closing PeripheralIO API on:" + this.toString(), e);
            } finally {
                mGpio = null;
            }
        }
    }
}
