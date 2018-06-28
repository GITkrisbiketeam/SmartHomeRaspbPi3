package com.krisbiketeam.smarthomeraspbpi3.units;

import android.support.annotation.NonNull;

import com.google.android.things.pio.Gpio;
import com.krisbiketeam.smarthomeraspbpi3.utils.Logger;
import com.krisbiketeam.smarthomeraspbpi3.utils.Utils;

import java.io.IOException;

public class ActuatorGpio extends HomeUnitGpio {
    private static final String TAG = Utils.getLogTag(ActuatorGpio.class);

    public ActuatorGpio(String name, String location, String pinName, int activeType) {
        super(name, location, pinName, activeType);
    }

    ActuatorGpio(Gpio buttonGpio, int activeType) {
        super(buttonGpio, activeType);
    }

    @Override
    public void connect(Gpio gpio, int activeType) {
        try {
            mGpio = gpio;
            mGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mGpio.setEdgeTriggerType(Gpio.EDGE_NONE);
            mGpio.setActiveType(activeType);
        } catch (IOException e) {
            Logger.e(TAG, "Error initializing PeripheralIO API on:" + this.toString(), e);
        }
    }
    public void setValue(@NonNull Boolean value) {
        try {
            mGpio.setValue(value);
        } catch (IOException e) {
            Logger.e(TAG, "Error updating GPIO value", e);
        }
    }

    @Override
    public void registerListener(@NonNull HomeUnitListener listener) {
        // empty implementation
    }

    @Override
    public void unregisterListener() {
        // empty implementation
    }
}
