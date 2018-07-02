package com.krisbiketeam.smarthomeraspbpi3

import com.krisbiketeam.smarthomeraspbpi3.driver.TMP102

object BoardConfig {
    val I2C = "I2C1"

    val LED_A = "Led A"
    val LED_A_PIN = "BCM6"
    val LED_B = "Led B"
    val LED_B_PIN = "BCM19"
    val LED_C = "Led C"
    val LED_C_PIN = "BCM26"

    val BUTTON_A = "Button A"
    val BUTTON_A_PIN = "BCM21"
    val BUTTON_B = "Button B"
    val BUTTON_B_PIN = "BCM20"
    val BUTTON_C = "Button C"
    val BUTTON_C_PIN = "BCM16"

    val MOTION_1 = "Motion 1"
    val MOTION_1_PIN = "BCM14"

    val CONTACT_1 = "Contactron 1"
    val CONTACT_1_PIN = "BCM15"

    val FOUR_CHAR_DISP = "Hat Four Char Display"
    val FOUR_CHAR_DISP_PIN = I2C

    val TEMP_SENSOR_TMP102 = "Temperature Sensor"
    val TEMP_SENSOR_TMP102_PIN = I2C
    val TEMP_SENSOR_TMP102_ADDR = TMP102.DEFAULT_I2C_GND_ADDRESS

}
