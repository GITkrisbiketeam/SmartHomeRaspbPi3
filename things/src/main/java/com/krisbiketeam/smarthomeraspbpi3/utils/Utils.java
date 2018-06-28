package com.krisbiketeam.smarthomeraspbpi3.utils;

import android.support.annotation.NonNull;

public class Utils {
    private static final String TAG_PREFIX = "SmartHome_";

    private static final String TAG = getLogTag(Utils.class);

    /**
     * Generate uniform Log TAG
     */
    public static String getLogTag(@NonNull Class cls) {
        return TAG_PREFIX + cls.getSimpleName();
    }

}
