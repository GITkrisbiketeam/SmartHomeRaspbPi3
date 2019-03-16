package com.krisbiketeam.smarthomeraspbpi3.common.hardware.driver

class PCF8574ATPin {

    interface PCF8574ATPinStateChangeListener {
        fun onPinStateChanged(pin: Pin, state: PinState)
    }

    enum class PinMode {
        DIGITAL_INPUT, DIGITAL_OUTPUT
    }

    enum class PinState {
        LOW, HIGH
    }

    enum class Pin(var address: Int) {
        GPIO_0(1),
        GPIO_1(2),
        GPIO_2(4),
        GPIO_3(8),
        GPIO_4(16),
        GPIO_5(32),
        GPIO_6(64),
        GPIO_7(128)
    }
}