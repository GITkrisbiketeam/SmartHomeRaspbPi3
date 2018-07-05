package com.krisbiketeam.smarthomeraspbpi3.driver;

@SuppressWarnings("WeakerAccess")
public class MCP23017Pin {

    public enum PinMode {
        DIGITAL_INPUT, DIGITAL_OUTPUT
    }
    public enum PinState {
        LOW, HIGH
    }

    public enum PinPullResistance {
        OFF, PULL_UP
    }

    public static final MCP23017Pin GPIO_A0 = new MCP23017Pin(1, "GPIO A0");
    public static final MCP23017Pin GPIO_A1 = new MCP23017Pin (2, "GPIO A1");
    public static final MCP23017Pin GPIO_A2 = new MCP23017Pin (4, "GPIO A2");
    public static final MCP23017Pin GPIO_A3 = new MCP23017Pin (8, "GPIO A3");
    public static final MCP23017Pin GPIO_A4 = new MCP23017Pin (16, "GPIO A4");
    public static final MCP23017Pin GPIO_A5 = new MCP23017Pin (32, "GPIO A5");
    public static final MCP23017Pin GPIO_A6 = new MCP23017Pin (64, "GPIO A6");
    public static final MCP23017Pin GPIO_A7 = new MCP23017Pin (128, "GPIO A7");
    public static final MCP23017Pin GPIO_B0 = new MCP23017Pin (1001, "GPIO B0");
    public static final MCP23017Pin GPIO_B1 = new MCP23017Pin (1002, "GPIO B1");
    public static final MCP23017Pin GPIO_B2 = new MCP23017Pin (1004, "GPIO B2");
    public static final MCP23017Pin GPIO_B3 = new MCP23017Pin (1008, "GPIO B3");
    public static final MCP23017Pin GPIO_B4 = new MCP23017Pin (1016, "GPIO B4");
    public static final MCP23017Pin GPIO_B5 = new MCP23017Pin (1032, "GPIO B5");
    public static final MCP23017Pin GPIO_B6 = new MCP23017Pin (1064, "GPIO B6");
    public static final MCP23017Pin GPIO_B7 = new MCP23017Pin (1128, "GPIO B7");

    public static final MCP23017Pin[] ALL_A_PINS = { MCP23017Pin.GPIO_A0, MCP23017Pin.GPIO_A1,
            MCP23017Pin.GPIO_A2, MCP23017Pin.GPIO_A3,
            MCP23017Pin.GPIO_A4, MCP23017Pin.GPIO_A5, MCP23017Pin.GPIO_A6, MCP23017Pin.GPIO_A7 };

    public static final MCP23017Pin[] ALL_B_PINS = { MCP23017Pin.GPIO_B0, MCP23017Pin.GPIO_B1,
            MCP23017Pin.GPIO_B2, MCP23017Pin.GPIO_B3,
            MCP23017Pin.GPIO_B4, MCP23017Pin.GPIO_B5, MCP23017Pin.GPIO_B6, MCP23017Pin.GPIO_B7 };

    public static final MCP23017Pin[] ALL = { MCP23017Pin.GPIO_A0, MCP23017Pin.GPIO_A1,
            MCP23017Pin.GPIO_A2, MCP23017Pin.GPIO_A3,
            MCP23017Pin.GPIO_A4, MCP23017Pin.GPIO_A5, MCP23017Pin.GPIO_A6, MCP23017Pin.GPIO_A7,
            MCP23017Pin.GPIO_B0, MCP23017Pin.GPIO_B1, MCP23017Pin.GPIO_B2, MCP23017Pin.GPIO_B3,
            MCP23017Pin.GPIO_B4, MCP23017Pin.GPIO_B5, MCP23017Pin.GPIO_B6, MCP23017Pin.GPIO_B7 };

    private final int address;
    private final String name;
    
    public MCP23017Pin(int address, String name) {
        this.address = address;
        this.name = name;
    }

    public String getName() {
        return name;
    }
    public int getAddress() {
        return address;
    }



}