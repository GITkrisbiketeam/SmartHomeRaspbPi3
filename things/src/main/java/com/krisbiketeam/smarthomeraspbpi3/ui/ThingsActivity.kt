package com.krisbiketeam.smarthomeraspbpi3.ui

import android.app.Activity
import android.content.Context
import android.net.wifi.WifiConfiguration
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
import com.krisbiketeam.smarthomeraspbpi3.common.auth.FirebaseCredentials
import com.krisbiketeam.smarthomeraspbpi3.common.auth.WifiCredentials
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyService
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyServiceProvider
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.NotSecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnitLog
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import com.krisbiketeam.smarthomeraspbpi3.units.hardware.HwUnitI2CPCF8574ATActuator
import com.krisbiketeam.smarthomeraspbpi3.units.hardware.HwUnitI2CPCF8574ATSensor
import com.krisbiketeam.smarthomeraspbpi3.utils.ConsoleLoggerTree
import com.squareup.moshi.Moshi
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.IOException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Driver parameters
private const val DRIVER_NAME = "PCF8574AT Button Driver"
private const val NEARBY_TIMEOUT = 60000L        // 60 sec
private const val NEARBY_BLINK_DELAY = 1000L        // 1 sec

class ThingsActivity : AppCompatActivity(), Sensor.HwUnitListener<Boolean>, CoroutineScope {
    private lateinit var authentication: Authentication
    private lateinit var secureStorage: SecureStorage
    private lateinit var networkConnectionMonitor: NetworkConnectionMonitor
    private lateinit var wifiManager: WifiManager

    private lateinit var home: Home
    private val ledA: HwUnitI2CPCF8574ATActuator
    private val ledB: HwUnitI2CPCF8574ATActuator
    private val ledC: HwUnitI2CPCF8574ATActuator

    private val led1: HwUnitI2CPCF8574ATActuator
    private val led2: HwUnitI2CPCF8574ATActuator

    private var buttonAInputDriver: HwUnitI2CPCF8574ATSensor
    private var buttonBInputDriver: HwUnitI2CPCF8574ATSensor
    private var buttonCInputDriver: HwUnitI2CPCF8574ATSensor


    private lateinit var mDriver: InputDriver

    private var connectAndSetupJob: Job? = null

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

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

        authentication = FirebaseAuthentication()

        networkConnectionMonitor = NetworkConnectionMonitor(this)

        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager

        Timber.e("onCreate isNetworkConnected: ${networkConnectionMonitor.isNetworkConnected}")

        ledA.setValue(false)
        ledB.setValue(false)
        ledC.setValue(false)
        led1.setValue(false)
        led2.setValue(false)

        home = Home(secureStorage)
        //home.saveToRepository()

        //FirebaseHomeInformationRepository.setHomeReference("test home")

        connectAndSetupJob = launch{
            if (networkConnectionMonitor.isNetworkConnected) {
                led1.setValue(true)
            } else {
                if (!wifiManager.isWifiEnabled) {
                    Timber.d("Wifi not enabled try enable it")
                    val enabled = wifiManager.setWifiEnabled(true)
                    Timber.d("Wifi enabled? $enabled")
                }
                waitForNetworkAvailable().let { connected ->
                    Timber.e("WiFi is finally connected?: $connected")
                    if (!connected) {
                        Timber.d("Not connected to WiFi, starting WiFiCredentialsReceiver")
                        startWiFiCredentialsReceiver()
                    }
                }
            }

            if (secureStorage.isAuthenticated()) {
                Timber.d("Login Firebase:${secureStorage.firebaseCredentials.email}")
                loginFirebase()
            } else {
                Timber.d("Not authenticated, starting FirebaseCredentialsReceiver")
                startFirebaseCredentialsReceiver()
            }

            if (secureStorage.homeName.isNotEmpty()) {
                Timber.d("Set Home Name:${secureStorage.homeName}")
                FirebaseHomeInformationRepository.setHomeReference(secureStorage.homeName)
            } else {
                Timber.d("No Home Name defined, starting HomeNameReceiver")
                startHomeNameReceiver()
            }

            Timber.d("connectAndSetupJob finished")
        }
    }

    private suspend fun startWiFiCredentialsReceiver() {
        val blinkJob = blinkLed(ledA, this)
        try {
            withTimeout(NEARBY_TIMEOUT) {
                waitForWifiCredentials(this@ThingsActivity)?.let {wifiCredentials ->
                    Timber.e("waitForWifiCredentials returned:$wifiCredentials")
                    addWiFi(wifiManager, wifiCredentials)
                } ?: Timber.e("Could not get WifiCredentials")
            }
        } finally {
            Timber.e("waitForWifiCredentials timeout or finished")
            blinkJob.cancelAndJoin()
        }
    }

    private suspend fun startFirebaseCredentialsReceiver() {
        val blinkJob = blinkLed(ledB, this)
        try {
            withTimeout(NEARBY_TIMEOUT) {
                waitForFirebaseCredentials(this@ThingsActivity)?.let {credentials ->
                    Timber.e("waitForFirebaseCredentials returned:$credentials")
                    secureStorage.firebaseCredentials = credentials
                    waitForNetworkAvailable().let { connected ->
                        Timber.e("waitForFirebaseCredentials connected:$connected")
                        if (connected) {
                            loginFirebase()
                        }
                    }
                } ?: Timber.e("Could not get FirebaseCredentials")
            }
        } finally {
            Timber.e("waitForFirebaseCredentials timeout or finished")
            blinkJob.cancelAndJoin()
        }
    }

    private suspend fun startHomeNameReceiver() {
        val blinkJob = blinkLed(ledC, this)
        try {
            withTimeout(NEARBY_TIMEOUT) {
                waitForHomeName(this@ThingsActivity)?.let { homeName ->
                    Timber.e("waitForHomeName returned:$homeName")
                    secureStorage.homeName = homeName
                    FirebaseHomeInformationRepository.setHomeReference(secureStorage.homeName)
                } ?: Timber.e("Could not get HomeName")
            }
        } finally {
            Timber.e("waitForHomeName timeout or finished")
            blinkJob.cancelAndJoin()
        }
    }

    private suspend fun waitForWifiCredentials(activity: Activity): WifiCredentials? =
            suspendCancellableCoroutine { cont ->
                val moshi = Moshi.Builder().build()
                val nearbyService = NearbyServiceProvider(activity, moshi)
                if (!nearbyService.isActive()) {
                    nearbyService.dataReceivedListener(object : NearbyService.DataReceiverListener {
                        override fun onDataReceived(data: ByteArray?) {
                            Timber.d("waitForWifiCredentials Received data: $data")
                            data?.run {
                                val jsonString = String(data)
                                Timber.d("waitForWifiCredentials Data as String $jsonString")
                                val adapter = moshi.adapter(WifiCredentials::class.java)
                                val wifiCredentials: WifiCredentials?
                                try {
                                    wifiCredentials = adapter.fromJson(jsonString)
                                    Timber.d("waitForWifiCredentials Data as wifiCredentials $wifiCredentials")
                                    wifiCredentials?.run {
                                        cont.resume(wifiCredentials)
                                    }
                                } catch (e: IOException) {
                                    cont.resumeWithException(e)
                                    Timber.d("waitForWifiCredentials Received Data could not be cast to WifiCredentials")
                                } finally {
                                    nearbyService.stop()
                                }
                            }
                        }
                    })
                } else {
                    Timber.d("waitForWifiCredentials start we are already listening for credentials")
                    cont.resume(null)
                }
                cont.invokeOnCancellation {
                    Timber.d("waitForWifiCredentials canceled")
                    nearbyService.stop()
                }
            }

    private suspend fun waitForFirebaseCredentials(activity: Activity): FirebaseCredentials? =
            suspendCancellableCoroutine { cont ->
                val moshi = Moshi.Builder().build()
                val nearbyService = NearbyServiceProvider(activity, moshi)
                if (!nearbyService.isActive()) {
                    nearbyService.dataReceivedListener(object : NearbyService.DataReceiverListener {
                        override fun onDataReceived(data: ByteArray?) {
                            Timber.d("waitForFirebaseCredentials Received data: $data")
                            data?.run {
                                val jsonString = String(data)
                                Timber.d("waitForFirebaseCredentials Data as String $jsonString")
                                val adapter = moshi.adapter(FirebaseCredentials::class.java)
                                val credentials: FirebaseCredentials?
                                try {
                                    credentials = adapter.fromJson(jsonString)
                                    Timber.d("waitForFirebaseCredentials Data as credentials $credentials")
                                    credentials?.run {
                                        cont.resume(credentials)
                                    }
                                } catch (e: IOException) {
                                    cont.resumeWithException(e)
                                    Timber.d("waitForFirebaseCredentials Received Data could not be cast to FirebaseCredentials")
                                } finally {
                                    nearbyService.stop()
                                }
                            }
                        }
                    })
                } else {
                    Timber.d("waitForFirebaseCredentials start we are already listening for credentials")
                    cont.resume(null)
                }
                cont.invokeOnCancellation {
                    Timber.d("waitForFirebaseCredentials canceled")
                    nearbyService.stop()
                }
            }

    private suspend fun waitForHomeName(activity: Activity): String? =
            suspendCancellableCoroutine  { cont ->
                val moshi = Moshi.Builder().build()
                val nearbyService = NearbyServiceProvider(activity, moshi)
                if (!nearbyService.isActive()) {
                    nearbyService.dataReceivedListener(object : NearbyService.DataReceiverListener {
                        override fun onDataReceived(data: ByteArray?) {
                            Timber.d("waitForHomeName Received data: $data")
                            data?.run {
                                val jsonString = String(data)
                                Timber.d("waitForHomeName Data as String $jsonString")
                                val adapter = moshi.adapter(String::class.java)
                                val homeName: String?
                                try {
                                    homeName = adapter.fromJson(jsonString)
                                    Timber.d("waitForHomeName Data as homeName $homeName")
                                    homeName?.run {
                                        cont.resume(homeName)
                                    }
                                } catch (e: IOException) {
                                    cont.resumeWithException(e)
                                    Timber.d("waitForHomeName Received Data could not be cast to FirebaseCredentials")
                                } finally {
                                    nearbyService.stop()
                                }
                            }
                        }
                    })
                } else {
                    Timber.d("waitForHomeName start we are already listening for credentials")
                    cont.resume(null)
                }
                cont.invokeOnCancellation {
                    Timber.d("waitForHomeName canceled")
                    nearbyService.stop()
                }
            }

    private suspend fun waitForNetworkAvailable(): Boolean = suspendCancellableCoroutine { connected ->
        val netMonitor = NetworkConnectionMonitor(this)
        netMonitor.startListen(object : NetworkConnectionListener {
            override fun onNetworkAvailable(available: Boolean) {
                Timber.w("waitForNetworkAvailable onNetworkAvailable $available")
                netMonitor.stopListen()
                connected.resume(available)
            }
        })
        connected.invokeOnCancellation {
            Timber.w("waitForNetworkAvailable canceled")
            netMonitor.stopListen()
        }
    }

    override fun onStart() {
        Timber.d("onStart")
        super.onStart()

        networkConnectionMonitor.startListen(networkConnectionListener)

        buttonAInputDriver.registerListener(this)
        buttonBInputDriver.registerListener(this)
        buttonCInputDriver.registerListener(this)

        if (connectAndSetupJob?.isCompleted == true) {
            home.start()
        } else {
            launch {
                connectAndSetupJob?.join()
                Timber.d("onStart home.start()")
                home.start()
            }
        }
    }

    override fun onStop() {
        Timber.d("onStop Shutting down lights observer")

        networkConnectionMonitor.stopListen()

        buttonAInputDriver.unregisterListener()
        buttonBInputDriver.unregisterListener()
        buttonCInputDriver.unregisterListener()


        connectAndSetupJob?.cancel()
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
        connectAndSetupJob?.cancel()
        super.onDestroy()
    }

    override fun onHwUnitChanged(hwUnit: HwUnit, unitValue: Boolean?, updateTime: String) {
        Timber.d("onHwUnitChanged hwUnit: $hwUnit ; unitValue: $unitValue ; updateTime: $updateTime")
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
                        ledA.setValue(false)
                        connectAndSetupJob?.cancel()
                        connectAndSetupJob = launch {
                            startWiFiCredentialsReceiver()
                            Timber.e("startWiFiCredentialsReceiver finished")
                        }
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
                        ledB.setValue(false)
                        connectAndSetupJob?.cancel()
                        connectAndSetupJob = launch{
                            startFirebaseCredentialsReceiver()
                            Timber.e("startFirebaseCredentialsReceiver finished")
                        }
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
                    50 -> {
                        Timber.d("onKeyDown very long press")
                        ledC.setValue(false)
                        connectAndSetupJob?.cancel()
                        connectAndSetupJob = launch{
                            home.stop()
                            startHomeNameReceiver()
                            home.start()
                            Timber.e("startHomeNameReceiver finished")
                        }
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
        authentication.addLoginResultListener(loginResultListener)
        authentication.login(secureStorage.firebaseCredentials)
    }

    private fun addWiFi(wifiManager: WifiManager, wifiCredentials: WifiCredentials) {

        // only WPA is supported right now
        val wifiConfiguration = WifiConfiguration()
        wifiConfiguration.SSID = String.format("\"%s\"", wifiCredentials.ssid)
        wifiConfiguration.preSharedKey = String.format("\"%s\"", wifiCredentials.password)

        val existingConfig = wifiManager.configuredNetworks?.firstOrNull{ wifiConfiguration.SSID == it.SSID }
        if (existingConfig != null) {
            Timber.d("This WiFi was already added update it. Existing: $existingConfig new one: $wifiConfiguration")
            existingConfig.preSharedKey = wifiConfiguration.preSharedKey
            val networkId = wifiManager.updateNetwork(existingConfig)
            if (networkId != -1) {
                Timber.d("successful update wifiConfig")
                wifiManager.enableNetwork(networkId, true)
            } else {
                Timber.w("error updating wifiConfig")
            }
        } else {
            Timber.d("This adding new configuration $wifiConfiguration")
            val networkId = wifiManager.addNetwork(wifiConfiguration)
            if (networkId != -1) {
                Timber.d("successful added wifiConfig")
                wifiManager.enableNetwork(networkId, true)
            } else {
                Timber.w("error adding wifiConfig")
            }
        }
    }

    private fun blinkLed(led: HwUnitI2CPCF8574ATActuator, scope: CoroutineScope): Job {
        return scope.launch {
            try {
                repeat((NEARBY_TIMEOUT/NEARBY_BLINK_DELAY).toInt()) {
                    Timber.d("blinkLed ${led.unitValue}")
                    led.setValue(led.unitValue?.not() ?: false)
                    delay(NEARBY_BLINK_DELAY)
                }
            } finally {
                Timber.d("blinkLed canceled or finished")
                led.setValue(false)
            }

        }
    }
}
