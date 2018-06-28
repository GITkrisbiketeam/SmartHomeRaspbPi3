package com.krisbiketeam.smarthomeraspbpi3.units;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krisbiketeam.smarthomeraspbpi3.ConnectionType;

public abstract class HomeUnit implements AutoCloseable{

    // HomeUnit type name ex. "BMP280" "Light"
    protected String mName;
    // HomeUnit Connection type see {@link ConnectionType} ex. ConnectionType.I2C
    protected ConnectionType mConnectionType;
    // Location of the sensor, ex. kitchen
    protected String mLocation;
    // Board Pin name this homeUnit is connected to
    protected String mPinName;

    // HomeUnit address for multiple units connected to one input ex I2c
    protected String mSoftAddress;

    // Current value this unit holds
    protected Object mValue;

    // Listener for HomeUnit events
    protected HomeUnitListener mListener;

    /**
     * Interface definition for a callback to be invoked when a Button event occurs.
     */
    public interface HomeUnitListener {
        /**
         * Called when a HomeUnit event occurs
         *
         * @param homeUnit the HomeUnit for which the event occurred
         * @param value Object with unit changed value
         */
        void onUnitChanged(@NonNull HomeUnit homeUnit, @Nullable Object value);
    }

    public HomeUnit() {

    }

    public HomeUnit(String name, ConnectionType connectionType, String location, String
            pinName, String softAddress) {
        this.mName = name;
        this.mConnectionType = connectionType;
        this.mLocation = location;
        this.mPinName = pinName;
        this.mSoftAddress = softAddress;
    }

    @Nullable
    public abstract Object readValue();

    public abstract void registerListener(@NonNull HomeUnitListener listener);

    public abstract void unregisterListener();

    @Override
    public String toString() {
        return "HomeUnit{" + "mName='" + mName + '\'' + ", mConnectionType=" + mConnectionType + "," +
                " mLocation='" + mLocation + '\'' + ", mPinName='" + mPinName + '\'' + ", " +
                "mSoftAddress='" + mSoftAddress + '\'' + ", mValue=" + mValue + '}';
    }
    public String getName() {
        return mName;
    }

    public ConnectionType getConnectionType() {
        return mConnectionType;
    }

    public String getLocation() {
        return mLocation;
    }

    public String getPinName() {
        return mPinName;
    }

    public String getSoftAddress() {
        return mSoftAddress;
    }

    public Object getValue() {
        return mValue;
    }
}
