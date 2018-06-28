package com.krisbiketeam.smarthomeraspbpi3.driver;

import android.util.Log;

import com.google.android.things.pio.I2cDevice;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class})
public class TMP102Test {
    @Mock
    private I2cDevice mI2c;
    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Test
    public void readSample16() throws IOException {
        PowerMockito.mockStatic(Log.class);
        TMP102 tmp102 = new TMP102(mI2c);
        tmp102.readTemperature();
        Mockito.verify(mI2c).readRegBuffer(eq(0x00), any(byte[].class), eq(2));
    }

    @Test
    public void calculateTemperature_NormalMode() throws IOException {
        PowerMockito.mockStatic(Log.class);
        TMP102 tmp102 = new TMP102(mI2c);
        float actual = tmp102.calculateTemperature(0x1900);
        float expectedValue = 25.0f;
        assertEquals(expectedValue, actual, 0);
    }

    @Test
    public void calculateTemperature_NegativeNormalMode() throws IOException {
        PowerMockito.mockStatic(Log.class);
        TMP102 tmp102 = new TMP102(mI2c);
        float actual = tmp102.calculateTemperature(0xFFC0);
        float expectedValue = -0.25f;
        assertEquals(expectedValue, actual, 0);
    }

    @Test
    public void calculateTemperature_ExtendedMode() throws IOException {
        PowerMockito.mockStatic(Log.class);
        TMP102 tmp102 = new TMP102(mI2c);
        float actual = tmp102.calculateTemperature(0xC81);
        float expectedValue = 25.0f;
        assertEquals(expectedValue, actual, 0);
    }

    @Test
    public void calculateTemperature_NegativeExtendedMode() throws IOException {
        PowerMockito.mockStatic(Log.class);
        TMP102 tmp102 = new TMP102(mI2c);
        float actual = tmp102.calculateTemperature(0xFFC0);
        float expectedValue = -0.25f;
        assertEquals(expectedValue, actual, 0);
    }

    @Test
    public void isExtendedMode_Extended() throws IOException {
        PowerMockito.mockStatic(Log.class);
        TMP102 tmp102 = new TMP102(mI2c);
        assertTrue(tmp102.isExtendedMode(0x60B0));
    }

    @Test
    public void isExtendedMode_Normal() throws IOException {
        PowerMockito.mockStatic(Log.class);
        TMP102 tmp102 = new TMP102(mI2c);
        assertFalse(tmp102.isExtendedMode(0x60A0));
    }
    @Test
    public void setExtendedMode_set() throws IOException {
        PowerMockito.mockStatic(Log.class);
        TMP102 tmp102 = new TMP102(mI2c);
        int actual = tmp102.setExtendedMode(0x60A0, true);
        int expectedValue = 0x60B0;
        assertEquals(expectedValue, actual);
    }

    @Test
    public void setExtendedMode_clear() throws IOException {
        PowerMockito.mockStatic(Log.class);
        TMP102 tmp102 = new TMP102(mI2c);
        int actual = tmp102.setExtendedMode(0x60B0, false);
        int expectedValue = 0x60A0;
        assertEquals(expectedValue, actual);
    }
}
