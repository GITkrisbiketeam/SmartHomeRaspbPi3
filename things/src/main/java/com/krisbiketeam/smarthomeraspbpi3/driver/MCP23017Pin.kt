package com.krisbiketeam.smarthomeraspbpi3.driver

class MCP23017Pin {

    interface MCP23017PinStateChangeListener {
        fun onPinStateChanged(pin: Pin, state: PinState)
    }

    enum class PinMode {
        DIGITAL_INPUT, DIGITAL_OUTPUT
    }

    enum class PinState {
        LOW, HIGH
    }

    enum class PinPullResistance {
        OFF, PULL_UP
    }

    enum class Pin(var address: Int, var gpio_name: String) {
        GPIO_A0(1, "GPIO A0"),
        GPIO_A1(2, "GPIO A1"),
        GPIO_A2(4, "GPIO A2"),
        GPIO_A3(8, "GPIO A3"),
        GPIO_A4(16, "GPIO A4"),
        GPIO_A5(32, "GPIO A5"),
        GPIO_A6(64, "GPIO A6"),
        GPIO_A7(128, "GPIO A7"),
        GPIO_B0(1001, "GPIO B0"),
        GPIO_B1(1002, "GPIO B1"),
        GPIO_B2(1004, "GPIO B2"),
        GPIO_B3(1008, "GPIO B3"),
        GPIO_B4(1016, "GPIO B4"),
        GPIO_B5(1032, "GPIO B5"),
        GPIO_B6(1064, "GPIO B6"),
        GPIO_B7(1128, "GPIO B7")
    }

    companion object {

        val ALL_A_PINS = arrayOf(Pin.GPIO_A0, Pin.GPIO_A1, Pin.GPIO_A2, Pin.GPIO_A3, Pin.GPIO_A4,
                Pin.GPIO_A5, Pin.GPIO_A6, Pin.GPIO_A7)

        val ALL_B_PINS = arrayOf(Pin.GPIO_B0, Pin.GPIO_B1, Pin.GPIO_B2, Pin.GPIO_B3, Pin.GPIO_B4,
                Pin.GPIO_B5, Pin.GPIO_B6, Pin.GPIO_B7)

        val ALL = arrayOf(Pin.GPIO_A0, Pin.GPIO_A1, Pin.GPIO_A2, Pin.GPIO_A3, Pin.GPIO_A4,
                Pin.GPIO_A5, Pin.GPIO_A6, Pin.GPIO_A7, Pin.GPIO_B0, Pin.GPIO_B1, Pin.GPIO_B2,
                Pin.GPIO_B3, Pin.GPIO_B4, Pin.GPIO_B5, Pin.GPIO_B6, Pin.GPIO_B7)
    }
}