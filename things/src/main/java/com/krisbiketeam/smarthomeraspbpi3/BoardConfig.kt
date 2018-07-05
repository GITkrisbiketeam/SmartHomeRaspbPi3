package com.krisbiketeam.smarthomeraspbpi3

import com.google.android.things.contrib.driver.bmx280.Bmx280
import com.krisbiketeam.smarthomeraspbpi3.driver.TMP102

object BoardConfig {
    const val I2C = "I2C1"

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

    const val MOTION_1 = "Motion 1"
    const val MOTION_1_PIN = "BCM14"

    const val REED_SWITCH_1 = "Reed Switch 1"
    const val REED_SWITCH_1_PIN = "BCM15"

    const val FOUR_CHAR_DISP = "Hat Four Char Display"
    const val FOUR_CHAR_DISP_PIN = I2C

    const val TEMP_SENSOR_TMP102 = "Temperature Sensor"
    const val TEMP_SENSOR_TMP102_PIN = I2C
    const val TEMP_SENSOR_TMP102_ADDR = TMP102.DEFAULT_I2C_GND_ADDRESS

    const val TEMP_PRESS_SENSOR_BMP280 = "Temperature and Pressure Sensor"
    const val TEMP_PRESS_SENSOR_BMP280_PIN = I2C
    const val TEMP_PRESS_SENSOR_BMP280_ADDR = Bmx280.DEFAULT_I2C_ADDRESS

}
