package com.krisbiketeam.smarthomeraspbpi3.common.hardware

import com.google.android.things.contrib.driver.bmx280.Bmx280
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver.*

object BoardConfig {
    private const val I2C = "I2C1"

    const val GPIO_INPUT = "Gpio Input"
    const val GPIO_OUTPUT = "Gpio Output"

    private const val GPIO5 = "BCM5"            // External direct LED
    private const val GPIO19 = "BCM19"          // PCF8474AT Button and LED driver

    private const val GPIO4 = "BCM4"
    private const val GPIO17 = "BCM17"
    private const val GPIO27 = "BCM27"
    private const val GPIO22 = "BCM22"
    private const val GPIO26 = "BCM26"
    private const val GPIO21 = "BCM21"
    private const val GPIO20 = "BCM20"
    private const val GPIO16 = "BCM16"
    private const val GPIO12 = "BCM12"
    private const val GPIO7 = "BCM7"
    private const val GPIO25 = "BCM25"
    private const val GPIO24 = "BCM24"
    private const val GPIO23 = "BCM23"
    private const val GPIO18 = "BCM18"
    private const val GPIO15 = "BCM15"
    private const val GPIO14 = "BCM14"
    private const val GPIO6 = "BCM6"            // 3.3V MCP23017
    private const val GPIO13 = "BCM13"          // 3.3V MCP23017

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

    const val TEMP_RH_SENSOR_SI7021 = "Temperature and Humidity Sensor Si7021"
    const val TEMP_RH_SENSOR_SI7021_PIN = I2C
    const val TEMP_RH_SENSOR_SI7021_ADDR = Si7021.I2C_ADDRESS
    val TEMP_RH_SENSOR_SI7021_ADDR_LIST = listOf(Si7021.I2C_ADDRESS)

    const val TEMP_RH_SENSOR_AM2320 = "Temperature and Humidity Sensor AM2320"
    const val TEMP_RH_SENSOR_AM2320_PIN = I2C
    const val TEMP_RH_SENSOR_AM2320_ADDR = AM2320.I2C_ADDRESS
    val TEMP_RH_SENSOR_AM2320_ADDR_LIST = listOf(AM2320.I2C_ADDRESS)

    const val AIR_QUALITY_SENSOR_BME680 = "Air Quality Sensor BME680"
    const val AIR_QUALITY_SENSOR_BME680_PIN = I2C
    const val AIR_QUALITY_SENSOR_BME680_ADDR = 0x77
    val AIR_QUALITY_SENSOR_BME680_ADDR_LIST = listOf(0x76, 0x77)

    const val IO_EXTENDER_PCF8474AT_INPUT = "8-bit IO Extender Input"
    const val IO_EXTENDER_PCF8474AT_OUTPUT = "8-bit IO Extender Output"
    const val IO_EXTENDER_PCF8574AT_PIN = I2C
    const val IO_EXTENDER_PCF8574AT_ADDR = PCF8574AT.DEFAULT_I2C_000_ADDRESS
    val IO_EXTENDER_PCF8474AT_ADDR_LIST = listOf(
            PCF8574AT.DEFAULT_I2C_000_ADDRESS,
            PCF8574AT.DEFAULT_I2C_001_ADDRESS,
            PCF8574AT.DEFAULT_I2C_010_ADDRESS,
            PCF8574AT.DEFAULT_I2C_011_ADDRESS,
            PCF8574AT.DEFAULT_I2C_100_ADDRESS,
            PCF8574AT.DEFAULT_I2C_101_ADDRESS,
            PCF8574AT.DEFAULT_I2C_110_ADDRESS,
            PCF8574AT.DEFAULT_I2C_111_ADDRESS)
    const val IO_EXTENDER_PCF8574AT_INT_PIN = GPIO19

    const val IO_EXTENDER_MCP23017_INPUT = "16-bit IO Extender Input"
    const val IO_EXTENDER_MCP23017_OUTPUT = "16-bit IO Extender Output"
    const val IO_EXTENDER_MCP23017_PIN = I2C
    val IO_EXTENDER_MCP23017_ADDR_LIST = listOf(
            MCP23017.DEFAULT_I2C_000_ADDRESS,
            MCP23017.DEFAULT_I2C_001_ADDRESS,
            MCP23017.DEFAULT_I2C_010_ADDRESS,
            MCP23017.DEFAULT_I2C_011_ADDRESS,
            MCP23017.DEFAULT_I2C_100_ADDRESS,
            MCP23017.DEFAULT_I2C_101_ADDRESS,
            MCP23017.DEFAULT_I2C_110_ADDRESS,
            MCP23017.DEFAULT_I2C_111_ADDRESS)

    val IO_HW_UNIT_TYPE_LIST = listOf(
            TEMP_SENSOR_TMP102,
            TEMP_SENSOR_MCP9808,
            TEMP_RH_SENSOR_SI7021,
            TEMP_RH_SENSOR_AM2320,
            AIR_QUALITY_SENSOR_BME680,
            IO_EXTENDER_MCP23017_INPUT,
            IO_EXTENDER_MCP23017_OUTPUT,
            GPIO_INPUT,
            GPIO_OUTPUT,
            TEMP_PRESS_SENSOR_BMP280)

    val I2C_HW_UNIT_LIST = listOf(
            TEMP_SENSOR_TMP102,
            TEMP_SENSOR_MCP9808,
            TEMP_RH_SENSOR_SI7021,
            TEMP_RH_SENSOR_AM2320,
            AIR_QUALITY_SENSOR_BME680,
            IO_EXTENDER_MCP23017_INPUT,
            IO_EXTENDER_MCP23017_OUTPUT,
            TEMP_PRESS_SENSOR_BMP280,
            FOUR_CHAR_DISP)
    val GPIO_HW_UNIT_LIST = listOf(
            GPIO_INPUT,
            GPIO_OUTPUT)

    val IO_GPIO_PIN_NAME_LIST = listOf(GPIO5)
    val IO_I2C_PIN_NAME_LIST = listOf(I2C)

    val IO_EXTENDER_INT_PIN_LIST = listOf(GPIO4, GPIO17, GPIO27, GPIO22, GPIO26, GPIO21, GPIO20, GPIO16, GPIO12, GPIO7, GPIO25, GPIO24, GPIO23, GPIO18, GPIO15, GPIO14, GPIO6, GPIO13)

    // region Button and LED driver PCF8574AT

    val IO_EXTENDER_PCF8574AT_LED_1_PIN = PCF8574ATPin.Pin.GPIO_0
    const val IO_EXTENDER_PCF8574AT_LED_1 = "Led 1"
    val IO_EXTENDER_PCF8574AT_LED_2_PIN = PCF8574ATPin.Pin.GPIO_2
    const val IO_EXTENDER_PCF8574AT_LED_2 = "Led 2"
    val IO_EXTENDER_PCF8574AT_LED_3_PIN = PCF8574ATPin.Pin.GPIO_4
    const val IO_EXTENDER_PCF8574AT_LED_3 = "Led 3"
    val IO_EXTENDER_PCF8574AT_LED_4_PIN = PCF8574ATPin.Pin.GPIO_6
    const val IO_EXTENDER_PCF8574AT_LED_4 = "Led 4"
    val IO_EXTENDER_PCF8574AT_LED_5_PIN = PCF8574ATPin.Pin.GPIO_7
    const val IO_EXTENDER_PCF8574AT_LED_5 = "Led 5"
    val IO_EXTENDER_PCF8574AT_BUTTON_1_PIN = PCF8574ATPin.Pin.GPIO_1
    const val IO_EXTENDER_PCF8574AT_BUTTON_1 = "Button 1"
    val IO_EXTENDER_PCF8574AT_BUTTON_2_PIN = PCF8574ATPin.Pin.GPIO_3
    const val IO_EXTENDER_PCF8574AT_BUTTON_2 = "Button 2"
    val IO_EXTENDER_PCF8574AT_BUTTON_3_PIN = PCF8574ATPin.Pin.GPIO_5
    const val IO_EXTENDER_PCF8574AT_BUTTON_3 = "Button 3"

    // endregion


    // region New MCP23017 config

    const val IO_EXTENDER_MCP23017_NEW_PIN = IO_EXTENDER_MCP23017_PIN
    const val IO_EXTENDER_MCP23017_NEW_ADDR = MCP23017.DEFAULT_I2C_000_ADDRESS
    const val IO_EXTENDER_MCP23017_NEW_INTA_PIN = GPIO21

    val IO_EXTENDER_MCP23017_NEW_IN_B0_PIN = MCP23017Pin.Pin.GPIO_B0
    val IO_EXTENDER_MCP23017_NEW_IN_B0 = IO_EXTENDER_MCP23017_INPUT.plus(IO_EXTENDER_MCP23017_NEW_ADDR).plus(IO_EXTENDER_MCP23017_NEW_IN_B0_PIN.name)
    val IO_EXTENDER_MCP23017_NEW_OUT_B7_PIN = MCP23017Pin.Pin.GPIO_B7
    val IO_EXTENDER_MCP23017_NEW_OUT_B7 = IO_EXTENDER_MCP23017_OUTPUT.plus(IO_EXTENDER_MCP23017_NEW_ADDR).plus(IO_EXTENDER_MCP23017_NEW_OUT_B7_PIN.name)

    // endregion


}
