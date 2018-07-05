package com.krisbiketeam.smarthomeraspbpi3.driver;

import android.util.Log;

import com.google.android.things.pio.I2cDevice;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class})
public class MCP23017Test {
    @Mock
    private I2cDevice mI2c;
    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Test
    public void getMode_gpio_A() throws IOException {
        PowerMockito.mockStatic(Log.class);
        PowerMockito.when(mI2c.readRegByte(anyInt())).thenReturn((byte)0);
        MCP23017 mcp23017 = new MCP23017(mI2c);
        mcp23017.setMode(MCP23017Pin.GPIO_A1, MCP23017Pin.PinMode.DIGITAL_INPUT);
        MCP23017Pin.PinMode pinMode = mcp23017.getMode(MCP23017Pin.GPIO_A1);
        assertEquals(MCP23017Pin.PinMode.DIGITAL_INPUT, pinMode);
    }

    @Test
    public void getMode_gpio_B() throws IOException {
        PowerMockito.mockStatic(Log.class);
        PowerMockito.when(mI2c.readRegByte(anyInt())).thenReturn((byte)0);
        MCP23017 mcp23017 = new MCP23017(mI2c);
        mcp23017.setMode(MCP23017Pin.GPIO_B2, MCP23017Pin.PinMode.DIGITAL_INPUT);
        MCP23017Pin.PinMode pinMode = mcp23017.getMode(MCP23017Pin.GPIO_B2);
        assertEquals(MCP23017Pin.PinMode.DIGITAL_INPUT, pinMode);
    }

    @Test
    public void getMode_notSet() throws IOException {
        PowerMockito.mockStatic(Log.class);
        PowerMockito.when(mI2c.readRegByte(anyInt())).thenReturn((byte)0);
        MCP23017 mcp23017 = new MCP23017(mI2c);
        mcp23017.setMode(MCP23017Pin.GPIO_A4, MCP23017Pin.PinMode.DIGITAL_OUTPUT);
        MCP23017Pin.PinMode pinMode = mcp23017.getMode(MCP23017Pin.GPIO_A4);
        assertEquals(MCP23017Pin.PinMode.DIGITAL_OUTPUT, pinMode);
    }
    @Test
    public void getPullResistance_gpio_A() throws IOException {
        PowerMockito.mockStatic(Log.class);
        PowerMockito.when(mI2c.readRegByte(anyInt())).thenReturn((byte)0);
        MCP23017 mcp23017 = new MCP23017(mI2c);
        mcp23017.setPullResistance(MCP23017Pin.GPIO_A1, MCP23017Pin.PinPullResistance.PULL_UP);
        MCP23017Pin.PinPullResistance pullRes = mcp23017.getPullResistance(MCP23017Pin.GPIO_A1);
        assertEquals(MCP23017Pin.PinPullResistance.PULL_UP, pullRes);
    }

    @Test
    public void getPullResistance_gpio_B() throws IOException {
        PowerMockito.mockStatic(Log.class);
        PowerMockito.when(mI2c.readRegByte(anyInt())).thenReturn((byte)0);
        MCP23017 mcp23017 = new MCP23017(mI2c);
        mcp23017.setPullResistance(MCP23017Pin.GPIO_B2, MCP23017Pin.PinPullResistance.PULL_UP);
        MCP23017Pin.PinPullResistance pullRes = mcp23017.getPullResistance(MCP23017Pin.GPIO_B2);
        assertEquals(MCP23017Pin.PinPullResistance.PULL_UP, pullRes);
    }

    @Test
    public void getPullResistance_notSet() throws IOException {
        PowerMockito.mockStatic(Log.class);
        PowerMockito.when(mI2c.readRegByte(anyInt())).thenReturn((byte)0);
        MCP23017 mcp23017 = new MCP23017(mI2c);
        mcp23017.setPullResistance(MCP23017Pin.GPIO_A1, MCP23017Pin.PinPullResistance.OFF);
        MCP23017Pin.PinPullResistance pullRes = mcp23017.getPullResistance(MCP23017Pin.GPIO_A1);
        assertEquals(MCP23017Pin.PinPullResistance.OFF, pullRes);
    }
}
