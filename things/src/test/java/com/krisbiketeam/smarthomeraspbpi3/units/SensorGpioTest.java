package com.krisbiketeam.smarthomeraspbpi3.units;

import android.util.Log;
import android.view.ViewConfiguration;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.krisbiketeam.smarthomeraspbpi3.ViewConfigurationMock;
import com.krisbiketeam.smarthomeraspbpi3.units.hardware.HwUnitGpioNoiseSensor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Matchers.any;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ViewConfiguration.class, HwUnitGpioNoiseSensor.class, Gpio.class, Log.class})
public class SensorGpioTest {

    @Mock
    private Gpio mGpio;

    @Rule
    public final ExpectedException mExpectedException = ExpectedException.none();

    @Before
    public void setup() throws Exception {
        ViewConfigurationMock.mockStatic();

        // Note: we need PowerMockito, so instantiate mocks here instead of using a MockitoRule
        mGpio = PowerMockito.mock(Gpio.class);
        PowerMockito.doNothing().when(mGpio).registerGpioCallback(any(GpioCallback.class));
    }
/*

    @Test
    public void close() {
        HwUnitGpioNoiseSensor gpio = new HwUnitGpioNoiseSensor(mGpio, Gpio.ACTIVE_LOW);
        gpio.close();
        Mockito.verify(mGpio).unregisterGpioCallback(any(GpioCallback.class));
        //Mockito.verify(mGpio).close();
    }

    @Test
    public void close_safeToCallTwice() throws IOException {
        HwUnitGpioNoiseSensor gpio = new HwUnitGpioNoiseSensor(mGpio, Gpio.ACTIVE_LOW);
        gpio.close();
        gpio.close(); // should not throw
        Mockito.verify(mGpio, times(1)).close();
    }

    @Test
    public void setDebounceDelay() {
        HwUnitGpioNoiseSensor gpio = new HwUnitGpioNoiseSensor(mGpio, Gpio.ACTIVE_LOW);
        final long DELAY = 1000L;
        gpio.setDebounceDelay(DELAY);
        assertEquals(DELAY, gpio.getDebounceDelay());
    }

    @Test
    public void setDebounceDelay_throwsIfTooSmall() {
        HwUnitGpioNoiseSensor gpio = new HwUnitGpioNoiseSensor(mGpio, Gpio.ACTIVE_LOW);
        mExpectedException.expect(IllegalArgumentException.class);
        gpio.setDebounceDelay(-1);
    }

    @Test
    public void getValue() throws IOException {
        PowerMockito.when(mGpio.getValue()).thenReturn(Boolean.TRUE);
        HwUnitGpioNoiseSensor gpio = new HwUnitGpioNoiseSensor(mGpio, Gpio.ACTIVE_LOW);
        assertEquals(gpio.readValue(), Boolean.TRUE);
    }

    @Test
    public void setButtonEventListener() {
        PowerMockito.mockStatic(Log.class);
        HwUnitGpioNoiseSensor gpio = new HwUnitGpioNoiseSensor(mGpio, Gpio.ACTIVE_LOW);
        // Add listener
        HwUnitLog.HwUnitListener mockListener = Mockito.mock(HwUnitLog.HwUnitListener.class);
        gpio.registerListener(mockListener);

        // Perform button events and check the listener is called
        gpio.performSensorEvent(Boolean.TRUE);
        Mockito.verify(mockListener, times(1)).onUnitChanged(gpio, Boolean.TRUE);
        gpio.performSensorEvent(Boolean.FALSE);
        Mockito.verify(mockListener, times(1)).onUnitChanged(gpio, Boolean.FALSE);

        // Remove listener
        gpio.unregisterListener();
        // Perform button events and check the listener is NOT called
        gpio.performSensorEvent(Boolean.TRUE);
        Mockito.verifyNoMoreInteractions(mockListener);
    }
*/

}