package com.krisbiketeam.smarthomeraspbpi3.common.hardware

import com.google.android.things.contrib.driver.bmx280.Bmx280
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP23017Pin
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.TMP102
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.MCP9808

object BoardConfigHat {
    private const val I2C = "I2C1"

    const val GPIO_INPUT = "Gpio Input"
    const val GPIO_OUTPUT = "Gpio Output"

    const val LED_A = "Led A"
    const val LED_A_PIN = "BCM6"
    const val LED_B = "Led B"
    const val LED_B_PIN = "BCM19"
    const val LED_C = "Led C"
    const val LED_C_PIN = "BCM26"

    const val BUTTON_A = "Button A"
    const val BUTTON_A_PIN = "BCM21"
    const val BUTTON_B = "Button B"
    const val BUTTON_B_PIN = "BCM20"
    const val BUTTON_C = "Button C"
    const val BUTTON_C_PIN = "BCM16"

    const val FOUR_CHAR_DISP = "Hat Four Char Display"
    const val FOUR_CHAR_DISP_PIN = I2C

    const val TEMP_SENSOR_TMP102 = "Temperature Sensor TMP102"
    const val TEMP_SENSOR_TMP102_PIN = I2C
    const val TEMP_SENSOR_TMP102_ADDR = TMP102.DEFAULT_I2C_GND_ADDRESS
    val TEMP_SENSOR_TMP102_ADDR_LIST = listOf(
            TMP102.DEFAULT_I2C_GND_ADDRESS,
            TMP102.DEFAULT_I2C_VCC_ADDRESS,
            TMP102.DEFAULT_I2C_SDA_ADDRESS,
            TMP102.DEFAULT_I2C_SCL_ADDRESS)

    const val TEMP_SENSOR_MCP9808 = "Temperature Sensor MCP9808"
    const val TEMP_SENSOR_MCP9808_PIN = I2C
    const val TEMP_SENSOR_MCP9808_ADDR = MCP9808.DEFAULT_I2C_111_ADDRESS
    val TEMP_SENSOR_MCP9808_ADDR_LIST = listOf(
            MCP9808.DEFAULT_I2C_000_ADDRESS,
            MCP9808.DEFAULT_I2C_001_ADDRESS,
            MCP9808.DEFAULT_I2C_010_ADDRESS,
            MCP9808.DEFAULT_I2C_011_ADDRESS,
            MCP9808.DEFAULT_I2C_100_ADDRESS,
            MCP9808.DEFAULT_I2C_101_ADDRESS,
            MCP9808.DEFAULT_I2C_110_ADDRESS,
            MCP9808.DEFAULT_I2C_111_ADDRESS)

    const val TEMP_PRESS_SENSOR_BMP280 = "Temperature and Pressure Sensor"
    const val TEMP_PRESS_SENSOR_BMP280_PIN = I2C
    const val TEMP_PRESS_SENSOR_BMP280_ADDR = Bmx280.DEFAULT_I2C_ADDRESS
    val TEMP_PRESS_SENSOR_BMP280_ADDR_LIST = listOf(Bmx280.DEFAULT_I2C_ADDRESS)

    const val IO_EXTENDER_MCP23017_INPUT = "16-bit IO Extender Input"
    const val IO_EXTENDER_MCP23017_OUTPUT = "16-bit IO Extender Output"
    val IO_EXTENDER_MCP23017_ADDR_LIST = listOf(
            MCP23017.DEFAULT_I2C_000_ADDRESS,
            MCP23017.DEFAULT_I2C_001_ADDRESS,
            MCP23017.DEFAULT_I2C_010_ADDRESS,
            MCP23017.DEFAULT_I2C_011_ADDRESS,
            MCP23017.DEFAULT_I2C_100_ADDRESS,
            MCP23017.DEFAULT_I2C_101_ADDRESS,
            MCP23017.DEFAULT_I2C_110_ADDRESS,
            MCP23017.DEFAULT_I2C_111_ADDRESS)

    const val IO_EXTENDER_MCP23017_1_PIN = I2C
    const val IO_EXTENDER_MCP23017_1_ADDR = MCP23017.DEFAULT_I2C_111_ADDRESS
    const val IO_EXTENDER_MCP23017_1_INTA_PIN = "BCM15"

    val IO_EXTENDER_MCP23017_1_IN_A7_PIN = MCP23017Pin.Pin.GPIO_A7
    val IO_EXTENDER_MCP23017_1_IN_A7 = IO_EXTENDER_MCP23017_INPUT.plus(IO_EXTENDER_MCP23017_1_ADDR).plus(IO_EXTENDER_MCP23017_1_IN_A7_PIN.name)
    val IO_EXTENDER_MCP23017_1_IN_A6_PIN = MCP23017Pin.Pin.GPIO_A6
    val IO_EXTENDER_MCP23017_1_IN_A6 = IO_EXTENDER_MCP23017_INPUT.plus(IO_EXTENDER_MCP23017_1_ADDR).plus(IO_EXTENDER_MCP23017_1_IN_A6_PIN.name)
    val IO_EXTENDER_MCP23017_1_IN_A5_PIN = MCP23017Pin.Pin.GPIO_A5
    val IO_EXTENDER_MCP23017_1_IN_A5 = IO_EXTENDER_MCP23017_INPUT.plus(IO_EXTENDER_MCP23017_1_ADDR).plus(IO_EXTENDER_MCP23017_1_IN_A5_PIN.name)
    val IO_EXTENDER_MCP23017_1_IN_A0_PIN = MCP23017Pin.Pin.GPIO_A0
    val IO_EXTENDER_MCP23017_1_IN_A0 = IO_EXTENDER_MCP23017_INPUT.plus(IO_EXTENDER_MCP23017_1_ADDR).plus(IO_EXTENDER_MCP23017_1_IN_A0_PIN.name)

    val IO_EXTENDER_MCP23017_1_OUT_B0_PIN = MCP23017Pin.Pin.GPIO_B0
    val IO_EXTENDER_MCP23017_1_OUT_B0 = IO_EXTENDER_MCP23017_OUTPUT.plus(IO_EXTENDER_MCP23017_1_ADDR).plus(IO_EXTENDER_MCP23017_1_OUT_B0_PIN.name)
    val IO_EXTENDER_MCP23017_1_OUT_B7_PIN = MCP23017Pin.Pin.GPIO_B7
    val IO_EXTENDER_MCP23017_1_OUT_B7 = IO_EXTENDER_MCP23017_OUTPUT.plus(IO_EXTENDER_MCP23017_1_ADDR).plus(IO_EXTENDER_MCP23017_1_OUT_B7_PIN.name)

    const val IO_EXTENDER_MCP23017_2_PIN = I2C
    const val IO_EXTENDER_MCP23017_2_ADDR = MCP23017.DEFAULT_I2C_000_ADDRESS
    const val IO_EXTENDER_MCP23017_2_INTA_PIN = "BCM14"

    val IO_EXTENDER_MCP23017_2_IN_A7_PIN = MCP23017Pin.Pin.GPIO_A7
    val IO_EXTENDER_MCP23017_2_IN_A7 = IO_EXTENDER_MCP23017_INPUT.plus(IO_EXTENDER_MCP23017_2_ADDR).plus(IO_EXTENDER_MCP23017_2_IN_A7_PIN.name)
    val IO_EXTENDER_MCP23017_2_IN_B0_PIN = MCP23017Pin.Pin.GPIO_B0
    val IO_EXTENDER_MCP23017_2_IN_B0 = IO_EXTENDER_MCP23017_INPUT.plus(IO_EXTENDER_MCP23017_2_ADDR).plus(IO_EXTENDER_MCP23017_2_IN_B0_PIN.name)

    val IO_HW_UNIT_TYPE_LIST = listOf(TEMP_SENSOR_TMP102, TEMP_PRESS_SENSOR_BMP280, IO_EXTENDER_MCP23017_INPUT, IO_EXTENDER_MCP23017_OUTPUT, GPIO_INPUT, GPIO_OUTPUT, FOUR_CHAR_DISP)

    val IO_GPIO_PIN_NAME_LIST = listOf(LED_A_PIN, LED_B_PIN, LED_C_PIN,
            BUTTON_A_PIN, BUTTON_B_PIN, BUTTON_C_PIN)
    val IO_I2C_PIN_NAME_LIST = listOf(I2C)

    val IO_EXTENDER_INT_PIN_LIST = listOf(IO_EXTENDER_MCP23017_1_INTA_PIN, IO_EXTENDER_MCP23017_2_INTA_PIN)

}
