package com.krisbiketeam.smarthomeraspbpi3.ui

import android.content.Context
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.KeyEvent.*
import com.google.android.things.contrib.driver.button.Button
import com.google.android.things.contrib.driver.button.ButtonInputDriver
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat
import com.google.android.things.pio.Gpio
import com.krisbiketeam.smarthomeraspbpi3.common.auth.Authentication
import com.krisbiketeam.smarthomeraspbpi3.common.auth.FirebaseAuthentication
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.NotSecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnitLog
import com.krisbiketeam.smarthomeraspbpi3.Home
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.ui.setup.FirebaseCredentialsReceiverManager
import com.krisbiketeam.smarthomeraspbpi3.ui.setup.WiFiCredentialsReceiverManager
import com.krisbiketeam.smarthomeraspbpi3.units.hardware.HwUnitGpioActuator
import com.krisbiketeam.smarthomeraspbpi3.units.hardware.HwUnitI2CFourCharDisplay
import com.krisbiketeam.smarthomeraspbpi3.utils.ConsoleLoggerTree
import timber.log.Timber
import java.io.IOException

private const val WIFI_RAINBOW_LED = 0
private const val FIREBASE_RAINBOW_LED = 1

class ThingsActivity : AppCompatActivity() {
    private lateinit var authentication: Authentication
    private lateinit var secureStorage: SecureStorage
    private lateinit var networkConnectionMonitor: NetworkConnectionMonitor

    private lateinit var home: Home
    private val ledA: HwUnitGpioActuator
    private val ledB: HwUnitGpioActuator
    private val ledC: HwUnitGpioActuator

    private var buttonAInputDriver: ButtonInputDriver? = null
    private var buttonBInputDriver: ButtonInputDriver? = null
    private var buttonCInputDriver: ButtonInputDriver? = null

    private var wiFiCredentialsReceiverManager: WiFiCredentialsReceiverManager? = null
    private var firebaseCredentialsReceiverManager: FirebaseCredentialsReceiverManager? = null

    private val mRainbowLeds = IntArray(RainbowHat.LEDSTRIP_LENGTH)

    private val fourCharDisplay = HwUnitI2CFourCharDisplay(BoardConfig.FOUR_CHAR_DISP, "Raspberry Pi", BoardConfig.FOUR_CHAR_DISP_PIN)

    init {
        Timber.d("init")

        ledA = HwUnitGpioActuator(BoardConfig.LED_A, "Raspberry Pi",
                BoardConfig.LED_A_PIN,
                Gpio.ACTIVE_HIGH)
        ledB = HwUnitGpioActuator(BoardConfig.LED_B, "Raspberry Pi",
                BoardConfig.LED_B_PIN,
                Gpio.ACTIVE_HIGH)
        ledC = HwUnitGpioActuator(BoardConfig.LED_C, "Raspberry Pi",
                BoardConfig.LED_C_PIN,
                Gpio.ACTIVE_HIGH)

        lightTheRainbow(true)
    }

    private val networkConnectionListener = object : NetworkConnectionListener {
        override fun onNetworkAvailable(available: Boolean) {
            Timber.d("Received onNetworkAvailable $available")
            lightOnOffOneRainbowLed(WIFI_RAINBOW_LED, available)
        }
    }

    private val loginResultListener = object : Authentication.LoginResultListener {
        override fun success() {
            Timber.d("LoginResultListener success")
            lightOnOffOneRainbowLed(FIREBASE_RAINBOW_LED, true)
        }

        override fun failed(exception: Exception) {
            Timber.d("LoginResultListener failed e: $exception")
            lightOnOffOneRainbowLed(FIREBASE_RAINBOW_LED, false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        ConsoleLoggerTree.setLogConsole(this)

        lightTheRainbow(false)

        secureStorage = NotSecureStorage(this)

        networkConnectionMonitor = NetworkConnectionMonitor(this)

        val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager

        Timber.e("onCreate isNetworkConnectedVal: ${networkConnectionMonitor.isNetworkConnectedVal}")
        Timber.e("onCreate isNetworkConnected(): ${networkConnectionMonitor.isNetworkConnected()}")

        if (!networkConnectionMonitor.isNetworkConnected()) {
            if (!wifiManager.isWifiEnabled) {
                Timber.d("Wifi not enabled try enable it")
                val enabled = wifiManager.setWifiEnabled(true)
                Timber.d("Wifi enabled? $enabled")
            }
            Timber.d("Not connected to WiFi, starting WiFiCredentialsReceiver")

            startWiFiCredentialsReceiver()
        } else {
            lightOnOffOneRainbowLed(WIFI_RAINBOW_LED, true)
        }

        if (!secureStorage.isAuthenticated()) {
            Timber.d("Not authenticated, starting FirebaseCredentialsReceiver")
            startFirebaseCredentialsReceiver()
        }

        authentication = FirebaseAuthentication()

        home = Home()
        //home.saveToRepository()

        try {
            buttonAInputDriver = ButtonInputDriver(
                    BoardConfig.BUTTON_A_PIN,
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_A)
            buttonBInputDriver = ButtonInputDriver(
                    BoardConfig.BUTTON_B_PIN,
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_B)
            buttonCInputDriver = ButtonInputDriver(
                    BoardConfig.BUTTON_C_PIN,
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_C)
        } catch (e: IOException) {
            Timber.e(e, "Error configuring GPIO pin")
        }
    }

    private fun startWiFiCredentialsReceiver() {
        if (wiFiCredentialsReceiverManager == null) {
            wiFiCredentialsReceiverManager = WiFiCredentialsReceiverManager(this, networkConnectionMonitor)
        }
        wiFiCredentialsReceiverManager?.start()
    }

    private fun startFirebaseCredentialsReceiver() {
        if (firebaseCredentialsReceiverManager == null) {
            firebaseCredentialsReceiverManager = FirebaseCredentialsReceiverManager(this) {
                loginFirebase()
            }
        }
        firebaseCredentialsReceiverManager?.start()
    }

    override fun onStart() {
        Timber.d("onStart")
        super.onStart()
        loginFirebase()

        networkConnectionMonitor.startListen(networkConnectionListener)

        buttonAInputDriver?.register()
        buttonBInputDriver?.register()
        buttonCInputDriver?.register()

        ledA.connect()
        ledB.connect()
        ledC.connect()

        home.start()
    }

    override fun onStop() {
        Timber.d("onStop Shutting down lights observer")

        networkConnectionMonitor.stopListen()

        buttonAInputDriver?.unregister()
        buttonBInputDriver?.unregister()
        buttonCInputDriver?.unregister()

        ledA.close()
        ledB.close()
        ledC.close()

        home.stop()
        super.onStop()
    }

    override fun onDestroy() {
        Timber.d("onDestroy")

        try {
            buttonAInputDriver?.close()
            buttonBInputDriver?.close()
            buttonCInputDriver?.close()
        } catch (e: IOException) {
            Timber.e(e,"Error closing Button driver")
        }
        super.onDestroy()
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        Timber.d("onKeyLongPress keyCode: $keyCode ; keEvent: $event")
        when (keyCode) {
            KEYCODE_A -> {
                ledA.setValue(true)
                /*try {
                        mBuzzer.play(440.0)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }*/
            }
            KEYCODE_B -> {
                ledB.setValue(true)
                /*try {
                        // Stop the buzzer.
                        mBuzzer.stop()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }*/
                // will cause onKeyUp be called with flag cancelled
                return true
            }
            KEYCODE_C -> {
                ledC.setValue(true)
            }
        }
        return super.onKeyLongPress(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KEYCODE_A -> {
                when (event?.repeatCount) {
                    0 -> {
                        Timber.d("onKeyDown keyCode: $keyCode ; keEvent: $event")
                        FirebaseHomeInformationRepository.logUnitEvent(HwUnitLog(
                                BoardConfig.BUTTON_A,
                                "Raspberry Pi",
                                BoardConfig.BUTTON_A_PIN,
                                ConnectionType.GPIO,
                                value = true))
                        ledA.setValue(true)
                        // start listen for LongKeyPress event
                        event.startTracking()
                        return true
                    }
                    50 -> {
                        Timber.d("onKeyDown very long press")
                        startWiFiCredentialsReceiver()
                        ledA.setValue(false)
                        return true
                    }
                }
            }
            KEYCODE_B -> {
                when (event?.repeatCount) {
                    0 -> {
                        Timber.d("onKeyDown keyCode: $keyCode ; keEvent: $event")
                        FirebaseHomeInformationRepository.logUnitEvent(HwUnitLog(
                                BoardConfig.BUTTON_B,
                                "Raspberry Pi",
                                BoardConfig.BUTTON_B_PIN,
                                ConnectionType.GPIO,
                                value = true))
                        ledB.setValue(true)
                        // start listen for LongKeyPress event
                        event.startTracking()
                        return true
                    }
                    50 -> {
                        Timber.d("onKeyDown very long press")
                        startFirebaseCredentialsReceiver()
                        ledB.setValue(false)
                        return true
                    }
                }
            }
            KEYCODE_C -> {
                when (event?.repeatCount) {
                    0 -> {
                        Timber.d("onKeyDown keyCode: $keyCode ; keEvent: $event")
                        FirebaseHomeInformationRepository.logUnitEvent(HwUnitLog(
                                BoardConfig.BUTTON_C,
                                "Raspberry Pi",
                                BoardConfig.BUTTON_C_PIN,
                                ConnectionType.GPIO,
                                value = true))
                        ledC.setValue(true)
                        // start listen for LongKeyPress event
                        event.startTracking()
                        return true
                    }
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        Timber.d("onKeyUp keyCode: $keyCode ; keEvent: $event")
        when (keyCode) {
            KEYCODE_A -> {
                FirebaseHomeInformationRepository.logUnitEvent(HwUnitLog(
                        BoardConfig.BUTTON_A,
                        "Raspberry Pi",
                        BoardConfig.BUTTON_A_PIN,
                        ConnectionType.GPIO,
                        value = false))
                ledA.setValue(false)
            }
            KEYCODE_B -> {
                FirebaseHomeInformationRepository.logUnitEvent(HwUnitLog(
                        BoardConfig.BUTTON_B,
                        "Raspberry Pi",
                        BoardConfig.BUTTON_B_PIN,
                        ConnectionType.GPIO,
                        value = false))
                ledB.setValue(false)
            }
            KEYCODE_C -> {
                FirebaseHomeInformationRepository.logUnitEvent(HwUnitLog(
                        BoardConfig.BUTTON_C,
                        "Raspberry Pi",
                        BoardConfig.BUTTON_C_PIN,
                        ConnectionType.GPIO,
                        value = false))
                ledC.setValue(false)
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun loginFirebase() {
        if (secureStorage.isAuthenticated()) {
            authentication.addLoginResultListener(loginResultListener)
            authentication.login(secureStorage.firebaseCredentials)
        }
    }

    private fun lightTheRainbow(light: Boolean) {
        // Light up the rainbow
        try {
            val ledstrip = RainbowHat.openLedStrip()
            ledstrip.brightness = 1
            for (i in mRainbowLeds.indices) {
                mRainbowLeds[i] =
                        if (light)
                            Color.HSVToColor(255, floatArrayOf(i * 360f / mRainbowLeds
                                    .size, 1.0f, 1.0f))
                        else
                            0
            }
            ledstrip.write(mRainbowLeds)
            // Close the device when done.
            ledstrip.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun lightOnOffOneRainbowLed(led: Int, on: Boolean) {
        try {
            val ledStrip = RainbowHat.openLedStrip()
            ledStrip.brightness = 1
            mRainbowLeds[led] =
                    if (on)
                        Color.HSVToColor(255, floatArrayOf(led * 360f / mRainbowLeds
                                .size, 1.0f, 1.0f))
                    else
                        0
            ledStrip.write(mRainbowLeds)
            // Close the device when done.
            ledStrip.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
