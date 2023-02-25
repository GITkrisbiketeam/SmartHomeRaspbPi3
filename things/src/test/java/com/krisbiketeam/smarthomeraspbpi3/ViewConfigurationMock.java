package com.krisbiketeam.smarthomeraspbpi3;

import android.view.ViewConfiguration;

import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

public class ViewConfigurationMock {

    public static void mockStatic() {
        PowerMockito.mockStatic(ViewConfiguration.class);
        Mockito.when(ViewConfiguration.getTapTimeout()).thenAnswer((Answer<Integer>) invocation -> 100);
    }
}