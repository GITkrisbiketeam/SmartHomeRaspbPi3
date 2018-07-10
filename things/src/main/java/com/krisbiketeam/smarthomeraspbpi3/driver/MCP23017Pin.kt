package com.krisbiketeam.smarthomeraspbpi3.driver

class MCP23017Pin private constructor(val address: Int, val name: String) {

    interface MCP23017PinStateChangeListener {
        fun onPinStateChanged(pin: MCP23017Pin, state: MCP23017Pin.PinState)
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

    companion object {

        val GPIO_A0 = MCP23017Pin(1, "GPIO A0")
        val GPIO_A1 = MCP23017Pin(2, "GPIO A1")
        val GPIO_A2 = MCP23017Pin(4, "GPIO A2")
        val GPIO_A3 = MCP23017Pin(8, "GPIO A3")
        val GPIO_A4 = MCP23017Pin(16, "GPIO A4")
        val GPIO_A5 = MCP23017Pin(32, "GPIO A5")
        val GPIO_A6 = MCP23017Pin(64, "GPIO A6")
        val GPIO_A7 = MCP23017Pin(128, "GPIO A7")
        val GPIO_B0 = MCP23017Pin(1001, "GPIO B0")
        val GPIO_B1 = MCP23017Pin(1002, "GPIO B1")
        val GPIO_B2 = MCP23017Pin(1004, "GPIO B2")
        val GPIO_B3 = MCP23017Pin(1008, "GPIO B3")
        val GPIO_B4 = MCP23017Pin(1016, "GPIO B4")
        val GPIO_B5 = MCP23017Pin(1032, "GPIO B5")
        val GPIO_B6 = MCP23017Pin(1064, "GPIO B6")
        val GPIO_B7 = MCP23017Pin(1128, "GPIO B7")

        val ALL_A_PINS = arrayOf(MCP23017Pin.GPIO_A0, MCP23017Pin.GPIO_A1, MCP23017Pin.GPIO_A2, MCP23017Pin.GPIO_A3, MCP23017Pin.GPIO_A4, MCP23017Pin.GPIO_A5, MCP23017Pin.GPIO_A6, MCP23017Pin.GPIO_A7)

        val ALL_B_PINS = arrayOf(MCP23017Pin.GPIO_B0, MCP23017Pin.GPIO_B1, MCP23017Pin.GPIO_B2, MCP23017Pin.GPIO_B3, MCP23017Pin.GPIO_B4, MCP23017Pin.GPIO_B5, MCP23017Pin.GPIO_B6, MCP23017Pin.GPIO_B7)

        val ALL = arrayOf(MCP23017Pin.GPIO_A0, MCP23017Pin.GPIO_A1, MCP23017Pin.GPIO_A2, MCP23017Pin.GPIO_A3, MCP23017Pin.GPIO_A4, MCP23017Pin.GPIO_A5, MCP23017Pin.GPIO_A6, MCP23017Pin.GPIO_A7, MCP23017Pin.GPIO_B0, MCP23017Pin.GPIO_B1, MCP23017Pin.GPIO_B2, MCP23017Pin.GPIO_B3, MCP23017Pin.GPIO_B4, MCP23017Pin.GPIO_B5, MCP23017Pin.GPIO_B6, MCP23017Pin.GPIO_B7)
    }


}