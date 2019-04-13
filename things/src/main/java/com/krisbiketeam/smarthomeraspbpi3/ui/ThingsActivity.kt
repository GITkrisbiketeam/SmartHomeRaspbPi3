package com.krisbiketeam.smarthomeraspbpi3.ui

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyEvent.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.things.userdriver.UserDriverManager
import com.google.android.things.userdriver.input.InputDriver
import com.google.android.things.userdriver.input.InputDriverEvent
import com.krisbiketeam.smarthomeraspbpi3.Home
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.auth.Authentication
import com.krisbiketeam.smarthomeraspbpi3.common.auth.FirebaseAuthentication
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.NotSecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnitLog
import com.krisbiketeam.smarthomeraspbpi3.ui.setup.FirebaseCredentialsReceiverManager
import com.krisbiketeam.smarthomeraspbpi3.ui.setup.WiFiCredentialsReceiverManager
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import com.krisbiketeam.smarthomeraspbpi3.units.hardware.HwUnitI2CPCF8574ATActuator
import com.krisbiketeam.smarthomeraspbpi3.units.hardware.HwUnitI2CPCF8574ATSensor
import com.krisbiketeam.smarthomeraspbpi3.utils.ConsoleLoggerTree
import timber.log.Timber

// Driver parameters
private const val DRIVER_NAME = "PCF8574AT Button Driver"

class ThingsActivity : AppCompatActivity(), Sensor.HwUnitListener<Boolean> {
    private lateinit var authentication: Authentication
    private lateinit var secureStorage: SecureStorage
    private lateinit var networkConnectionMonitor: NetworkConnectionMonitor

    private lateinit var home: Home
    private val ledA: HwUnitI2CPCF8574ATActuator
    private val ledB: HwUnitI2CPCF8574ATActuator
    private val ledC: HwUnitI2CPCF8574ATActuator

    private val led1: HwUnitI2CPCF8574ATActuator
    private val led2: HwUnitI2CPCF8574ATActuator

    private var buttonAInputDriver: HwUnitI2CPCF8574ATSensor
    private var buttonBInputDriver: HwUnitI2CPCF8574ATSensor
    private var buttonCInputDriver: HwUnitI2CPCF8574ATSensor

    private var wiFiCredentialsReceiverManager: WiFiCredentialsReceiverManager? = null
    private var firebaseCredentialsReceiverManager: FirebaseCredentialsReceiverManager? = null


    private lateinit var mDriver: InputDriver

    init {
        Timber.d("init")

        ledA = HwUnitI2CPCF8574ATActuator(BoardConfig.IO_EXTENDER_PCF8574AT_LED_1,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_PCF8574AT_PIN,
                BoardConfig.IO_EXTENDER_PCF8574AT_ADDR,
                BoardConfig.IO_EXTENDER_PCF8574AT_INT_PIN,
                BoardConfig.IO_EXTENDER_PCF8574AT_LED_1_PIN)

        ledB = HwUnitI2CPCF8574ATActuator(BoardConfig.IO_EXTENDER_PCF8574AT_LED_2,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_PCF8574AT_PIN,
                BoardConfig.IO_EXTENDER_PCF8574AT_ADDR,
                BoardConfig.IO_EXTENDER_PCF8574AT_INT_PIN,
                BoardConfig.IO_EXTENDER_PCF8574AT_LED_2_PIN)
        ledC = HwUnitI2CPCF8574ATActuator(BoardConfig.IO_EXTENDER_PCF8574AT_LED_3,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_PCF8574AT_PIN,
                BoardConfig.IO_EXTENDER_PCF8574AT_ADDR,
                BoardConfig.IO_EXTENDER_PCF8574AT_INT_PIN,
                BoardConfig.IO_EXTENDER_PCF8574AT_LED_3_PIN)
        led1 = HwUnitI2CPCF8574ATActuator(BoardConfig.IO_EXTENDER_PCF8574AT_LED_4,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_PCF8574AT_PIN,
                BoardConfig.IO_EXTENDER_PCF8574AT_ADDR,
                BoardConfig.IO_EXTENDER_PCF8574AT_INT_PIN,
                BoardConfig.IO_EXTENDER_PCF8574AT_LED_4_PIN)
        led2 = HwUnitI2CPCF8574ATActuator(BoardConfig.IO_EXTENDER_PCF8574AT_LED_5,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_PCF8574AT_PIN,
                BoardConfig.IO_EXTENDER_PCF8574AT_ADDR,
                BoardConfig.IO_EXTENDER_PCF8574AT_INT_PIN,
                BoardConfig.IO_EXTENDER_PCF8574AT_LED_5_PIN)

        buttonAInputDriver = HwUnitI2CPCF8574ATSensor(BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_1,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_PCF8574AT_PIN,
                BoardConfig.IO_EXTENDER_PCF8574AT_ADDR,
                BoardConfig.IO_EXTENDER_PCF8574AT_INT_PIN,
                BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_1_PIN)

        buttonBInputDriver = HwUnitI2CPCF8574ATSensor(BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_2,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_PCF8574AT_PIN,
                BoardConfig.IO_EXTENDER_PCF8574AT_ADDR,
                BoardConfig.IO_EXTENDER_PCF8574AT_INT_PIN,
                BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_2_PIN)
        buttonCInputDriver = HwUnitI2CPCF8574ATSensor(BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_3,
                "Raspberry Pi",
                BoardConfig.IO_EXTENDER_PCF8574AT_PIN,
                BoardConfig.IO_EXTENDER_PCF8574AT_ADDR,
                BoardConfig.IO_EXTENDER_PCF8574AT_INT_PIN,
                BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_3_PIN)

    }

    private val networkConnectionListener = object : NetworkConnectionListener {
        override fun onNetworkAvailable(available: Boolean) {
            Timber.d("Received onNetworkAvailable $available")
            led1.setValue(available)
        }
    }

    private val loginResultListener = object : Authentication.LoginResultListener {
        override fun success() {
            Timber.d("LoginResultListener success")
            led2.setValue(true)
        }

        override fun failed(exception: Exception) {
            Timber.d("LoginResultListener failed e: $exception")
            led2.setValue(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        ConsoleLoggerTree.setLogConsole(this)

        ledA.connect()
        ledB.connect()
        ledC.connect()
        led1.connect()
        led2.connect()
        buttonAInputDriver.connect()
        buttonBInputDriver.connect()
        buttonCInputDriver.connect()

        ledA.setValue(true)
        ledB.setValue(true)
        ledC.setValue(true)
        led1.setValue(true)
        led2.setValue(true)

        // Create a new driver instance
        mDriver = InputDriver.Builder()
                .setName(DRIVER_NAME)
                .setSupportedKeys(intArrayOf(KEYCODE_A, KEYCODE_B, KEYCODE_C))
                .build()

        // Register with the framework
        UserDriverManager.getInstance().registerInputDriver(mDriver)

        secureStorage = NotSecureStorage(this)

        networkConnectionMonitor = NetworkConnectionMonitor(this)

        val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager

        Timber.e("onCreate isNetworkConnectedVal: ${networkConnectionMonitor.isNetworkConnectedVal}")
        Timber.e("onCreate isNetworkConnected(): ${networkConnectionMonitor.isNetworkConnected()}")

        ledA.setValue(false)
        ledB.setValue(false)
        ledC.setValue(false)
        led1.setValue(false)
        led2.setValue(false)

        if (!networkConnectionMonitor.isNetworkConnected()) {
            if (!wifiManager.isWifiEnabled) {
                Timber.d("Wifi not enabled try enable it")
                val enabled = wifiManager.setWifiEnabled(true)
                Timber.d("Wifi enabled? $enabled")
            }
            Timber.d("Not connected to WiFi, starting WiFiCredentialsReceiver")

            startWiFiCredentialsReceiver()
        } else {
            led1.setValue(true)
        }

        if (!secureStorage.isAuthenticated()) {
            Timber.d("Not authenticated, starting FirebaseCredentialsReceiver")
            startFirebaseCredentialsReceiver()
        }

        authentication = FirebaseAuthentication()

        home = Home()
        //home.saveToRepository()
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

        buttonAInputDriver.registerListener(this)
        buttonBInputDriver.registerListener(this)
        buttonCInputDriver.registerListener(this)

        home.start()
    }

    override fun onStop() {
        Timber.d("onStop Shutting down lights observer")

        networkConnectionMonitor.stopListen()

        buttonAInputDriver.unregisterListener()
        buttonBInputDriver.unregisterListener()
        buttonCInputDriver.unregisterListener()

        home.stop()
        super.onStop()
    }

    override fun onDestroy() {
        Timber.d("onDestroy")

        ledA.close()
        ledB.close()
        ledC.close()
        led1.close()
        led2.close()
        buttonAInputDriver.close()
        buttonBInputDriver.close()
        buttonCInputDriver.close()
        UserDriverManager.getInstance().unregisterInputDriver(mDriver)
        super.onDestroy()
    }

    override fun onUnitChanged(hwUnit: HwUnit, unitValue: Boolean?, updateTime: String) {
        Timber.d("onUnitChanged hwUnit: $hwUnit ; unitValue: $unitValue ; updateTime: $updateTime")
        unitValue?.let {

            val keyCode = when(hwUnit.ioPin){
                BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_1_PIN.name -> KEYCODE_A
                BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_2_PIN.name -> KEYCODE_B
                BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_3_PIN.name -> KEYCODE_C
                else -> null
            }
            keyCode?.let {
                mDriver.emit(
                        InputDriverEvent().apply {
                            setKeyPressed(keyCode, !unitValue)
                        })
            }
        }
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        Timber.d("onKeyLongPress keyCode: $keyCode ; keEvent: $event")
        when (keyCode) {
            KEYCODE_A -> {
                ledA.setValue(true)
            }
            KEYCODE_B -> {
                ledB.setValue(true)
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
                                BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_1,
                                "Raspberry Pi",
                                BoardConfig.IO_EXTENDER_PCF8474AT_INPUT,
                                BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_1_PIN.name,
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
                                BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_2,
                                "Raspberry Pi",
                                BoardConfig.IO_EXTENDER_PCF8474AT_INPUT,
                                BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_2_PIN.name,
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
                                BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_3,
                                "Raspberry Pi",
                                BoardConfig.IO_EXTENDER_PCF8474AT_INPUT,
                                BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_3_PIN.name,
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
                        BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_1,
                        "Raspberry Pi",
                        BoardConfig.IO_EXTENDER_PCF8474AT_INPUT,
                        BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_1_PIN.name,
                        ConnectionType.GPIO,
                        value = false))
                ledA.setValue(false)
            }
            KEYCODE_B -> {
                FirebaseHomeInformationRepository.logUnitEvent(HwUnitLog(
                        BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_2,
                        "Raspberry Pi",
                        BoardConfig.IO_EXTENDER_PCF8474AT_INPUT,
                        BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_2_PIN.name,
                        ConnectionType.GPIO,
                        value = false))
                ledB.setValue(false)
            }
            KEYCODE_C -> {
                FirebaseHomeInformationRepository.logUnitEvent(HwUnitLog(
                        BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_3,
                        "Raspberry Pi",
                        BoardConfig.IO_EXTENDER_PCF8474AT_INPUT,
                        BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_3_PIN.name,
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
}
