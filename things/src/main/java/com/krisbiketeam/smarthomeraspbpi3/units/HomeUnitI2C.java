package com.krisbiketeam.smarthomeraspbpi3.units;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class HomeUnitI2C extends HomeUnit {

    @Nullable
    @Override
    public Object readValue() {
        return null;
    }

    @Override
    public void registerListener(@NonNull HomeUnitListener listener) {

    }

    @Override
    public void unregisterListener() {

    }

    @Override
    public void close() {

    }
}
