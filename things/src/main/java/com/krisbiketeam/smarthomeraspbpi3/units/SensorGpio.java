package com.krisbiketeam.smarthomeraspbpi3.units;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.view.ViewConfiguration;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.krisbiketeam.smarthomeraspbpi3.utils.Logger;
import com.krisbiketeam.smarthomeraspbpi3.utils.Utils;

import java.io.IOException;

public class SensorGpio extends HomeUnitGpio {

    private static final String TAG = Utils.getLogTag(SensorGpio.class);

    private Handler mDebounceHandler;
    private CheckDebounce mPendingCheckDebounce;
    private long mDebounceDelay = ViewConfiguration.getTapTimeout();

    public SensorGpio(String name, String location, String pinName, int activeType) {
        super(name, location, pinName, activeType);
    }

    SensorGpio(Gpio buttonGpio, int activeType) {
        super(buttonGpio, activeType);
    }


    public void connect(Gpio gpio, int activeType) {
        try {
            mGpio = gpio;
            mGpio.setDirection(Gpio.DIRECTION_IN);
            mGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            mGpio.setActiveType(activeType);

        } catch (IOException e) {
            Logger.e(TAG, "Error initializing PeripheralIO API on:" + this.toString(), e);
        }

        mDebounceHandler = new Handler();
    }

    @Override
    public void close() {
        removeDebounceCallback();
        unregisterListener();
        super.close();
    }

    /**
     * Set the time delay after an edge trigger that the button
     * must remain stable before generating an event. Debounce
     * is enabled by default for 100ms.
     *
     * Setting this value to zero disables debounce and triggers
     * events on all edges immediately.
     *
     * @param delay Delay, in milliseconds, or 0 to disable.
     */
    public void setDebounceDelay(long delay) {
        if (delay < 0) {
            throw new IllegalArgumentException("Debounce delay cannot be negative.");
        }
        // Clear any pending events
        removeDebounceCallback();
        mDebounceDelay = delay;
    }

    @VisibleForTesting
    long getDebounceDelay() {
        return mDebounceDelay;
    }

    @Override
    public void registerListener(@NonNull HomeUnitListener listener) {
        Logger.d(TAG, "registerListener");
        mListener = listener;
        if (mGpio != null) {
            try {
                mGpio.registerGpioCallback(mGpioCallback);
            } catch (IOException e) {
                //Logger.e(TAG, "Error registerListener PeripheralIO API on:" + SensorGpio.this.toString(), e);
            }
        }
    }

    @Override
    public void unregisterListener() {
        if (mGpio != null) {
            mGpio.unregisterGpioCallback(mGpioCallback);
            mListener = null;
        }
    }

    private final GpioCallback mGpioCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            Object value = SensorGpio.this.readValue(gpio);
            Logger.v(TAG, "onGpioEdge gpio.readValue(): " + value + " on:" + SensorGpio.this.toString());

            if (mDebounceDelay == 0) {
                // Trigger event immediately
                performSensorEvent(value);
            } else {
                // Clear any pending checks
                removeDebounceCallback();
                // Set a new pending check
                mPendingCheckDebounce = new CheckDebounce(value);
                mDebounceHandler.postDelayed(mPendingCheckDebounce, mDebounceDelay);
            }
            // Continue listening for more interrupts
            return true;
        }

        @Override
        public void onGpioError(Gpio gpio, int error) {
            Logger.w(TAG, gpio + ": Error event " + error + " on:" + SensorGpio.this.toString());
        }
    };

    /**
     * Invoke button event callback
     */
    @VisibleForTesting
    /*package*/ void performSensorEvent(Object event) {
        mValue = event;
        Logger.d(TAG, "performSensorEvent event: " + event + " on:" + this.toString());
        if (mListener != null) {
            mListener.onUnitChanged(SensorGpio.this, event);
        } else {
            Logger.w(TAG, "listener not registered on:" + SensorGpio.this.toString());
        }
    }

    /**
     * Clear pending debounce check
     */
    private void removeDebounceCallback() {
        if (mPendingCheckDebounce != null) {
            mDebounceHandler.removeCallbacks(mPendingCheckDebounce);
            mPendingCheckDebounce = null;
        }
    }

    /**
     * Pending check to delay input events from the initial
     * trigger edge.
     */
    private final class CheckDebounce implements Runnable {
        private final Object mTriggerState;

        public CheckDebounce(Object triggerState) {
            mTriggerState = triggerState;
        }

        @Override
        public void run() {
            if (mGpio != null) {
                // Final check that state hasn't changed
                if (SensorGpio.this.readValue() == mTriggerState) {
                    performSensorEvent(mTriggerState);
                }
                removeDebounceCallback();
            }
        }
    }
}
