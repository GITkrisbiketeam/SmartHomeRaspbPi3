package com.krisbiketeam.smarthomeraspbpi3.ui

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import android.view.KeyEvent
import android.view.KeyEvent.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.things.userdriver.UserDriverManager
import com.google.android.things.userdriver.input.InputDriver
import com.google.android.things.userdriver.input.InputDriverEvent
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.jakewharton.processphoenix.ProcessPhoenix
import com.krisbiketeam.smarthomeraspbpi3.Home
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.Analytics
import com.krisbiketeam.smarthomeraspbpi3.common.auth.Authentication
import com.krisbiketeam.smarthomeraspbpi3.common.auth.FirebaseCredentials
import com.krisbiketeam.smarthomeraspbpi3.common.auth.WifiCredentials
import com.krisbiketeam.smarthomeraspbpi3.common.hardware.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyService
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ConnectionType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnitLog
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.BaseHwUnit
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import com.krisbiketeam.smarthomeraspbpi3.units.hardware.HwUnitI2CPCF8574ATActuator
import com.krisbiketeam.smarthomeraspbpi3.units.hardware.HwUnitI2CPCF8574ATSensor
import com.krisbiketeam.smarthomeraspbpi3.utils.ConsoleAndCrashliticsLoggerTree
import com.krisbiketeam.smarthomeraspbpi3.utils.FirebaseDBLoggerTree
import com.squareup.moshi.Moshi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.IOException
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Driver parameters
private const val DRIVER_NAME = "PCF8574AT Button Driver"
private const val NEARBY_TIMEOUT = 60000L        // 60 sec
private const val NEARBY_BLINK_DELAY = 1000L        // 1 sec

class ThingsActivity : AppCompatActivity(), Sensor.HwUnitListener<Boolean> {
    private val authentication: Authentication by inject()
    private val secureStorage: SecureStorage by inject()
    private val homeInformationRepository: FirebaseHomeInformationRepository by inject()
    private lateinit var networkConnectionMonitor: NetworkConnectionMonitor
    private lateinit var wifiManager: WifiManager

    private val analytics: Analytics by inject()

    private val home: Home by inject()

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

    init {
        Timber.i("init secureStorage.remoteLoggingLevel ${secureStorage.remoteLoggingLevel}")
        FirebaseDBLoggerTree.setMinPriority(secureStorage.remoteLoggingLevel)
        FirebaseDBLoggerTree.setFirebaseRepository(homeInformationRepository)

        Timber.d("init")

        ledA = HwUnitI2CPCF8574ATActuator(BoardConfig.IO_EXTENDER_PCF8574AT_LED_1, "Raspberry Pi",
                                          BoardConfig.IO_EXTENDER_PCF8574AT_PIN,
                                          BoardConfig.IO_EXTENDER_PCF8574AT_ADDR,
                                          BoardConfig.IO_EXTENDER_PCF8574AT_LED_1_PIN)

        ledB = HwUnitI2CPCF8574ATActuator(BoardConfig.IO_EXTENDER_PCF8574AT_LED_2, "Raspberry Pi",
                                          BoardConfig.IO_EXTENDER_PCF8574AT_PIN,
                                          BoardConfig.IO_EXTENDER_PCF8574AT_ADDR,
                                          BoardConfig.IO_EXTENDER_PCF8574AT_LED_2_PIN)
        ledC = HwUnitI2CPCF8574ATActuator(BoardConfig.IO_EXTENDER_PCF8574AT_LED_3, "Raspberry Pi",
                                          BoardConfig.IO_EXTENDER_PCF8574AT_PIN,
                                          BoardConfig.IO_EXTENDER_PCF8574AT_ADDR,
                                          BoardConfig.IO_EXTENDER_PCF8574AT_LED_3_PIN)
        led1 = HwUnitI2CPCF8574ATActuator(BoardConfig.IO_EXTENDER_PCF8574AT_LED_4, "Raspberry Pi",
                                          BoardConfig.IO_EXTENDER_PCF8574AT_PIN,
                                          BoardConfig.IO_EXTENDER_PCF8574AT_ADDR,
                                          BoardConfig.IO_EXTENDER_PCF8574AT_LED_4_PIN)
        led2 = HwUnitI2CPCF8574ATActuator(BoardConfig.IO_EXTENDER_PCF8574AT_LED_5, "Raspberry Pi",
                                          BoardConfig.IO_EXTENDER_PCF8574AT_PIN,
                                          BoardConfig.IO_EXTENDER_PCF8574AT_ADDR,
                                          BoardConfig.IO_EXTENDER_PCF8574AT_LED_5_PIN)

        buttonAInputDriver =
                HwUnitI2CPCF8574ATSensor(BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_1, "Raspberry Pi",
                                         BoardConfig.IO_EXTENDER_PCF8574AT_PIN,
                                         BoardConfig.IO_EXTENDER_PCF8574AT_ADDR,
                                         BoardConfig.IO_EXTENDER_PCF8574AT_INT_PIN,
                                         BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_1_PIN)

        buttonBInputDriver =
                HwUnitI2CPCF8574ATSensor(BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_2, "Raspberry Pi",
                                         BoardConfig.IO_EXTENDER_PCF8574AT_PIN,
                                         BoardConfig.IO_EXTENDER_PCF8574AT_ADDR,
                                         BoardConfig.IO_EXTENDER_PCF8574AT_INT_PIN,
                                         BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_2_PIN)
        buttonCInputDriver =
                HwUnitI2CPCF8574ATSensor(BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_3, "Raspberry Pi",
                                         BoardConfig.IO_EXTENDER_PCF8574AT_PIN,
                                         BoardConfig.IO_EXTENDER_PCF8574AT_ADDR,
                                         BoardConfig.IO_EXTENDER_PCF8574AT_INT_PIN,
                                         BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_3_PIN)

    }

    private val networkConnectionListener = object : NetworkConnectionListener {
        override fun onNetworkAvailable(available: Boolean) {
            Timber.d("Received onNetworkAvailable $available")
            val ipAddress = wifiManager.connectionInfo.ipAddress
            val formattedIpAddress = Formatter.formatIpAddress(ipAddress)
            Timber.v("onAvailable ipAddress: $formattedIpAddress")
            ConsoleAndCrashliticsLoggerTree.setIpAddress(formattedIpAddress)
            lifecycleScope.launch {
                led1.setValueWithException(available)
            }
        }
    }

    private val loginResultListener = object : Authentication.LoginResultListener {
        override fun success(uid: String?) {
            Timber.d("LoginResultListener success $uid")
            lifecycleScope.launch {
                led2.setValueWithException(true)
            }
        }

        override fun failed(exception: Exception) {
            Timber.d("LoginResultListener failed e: $exception")
            lifecycleScope.launch {
                led2.setValueWithException(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        ConsoleAndCrashliticsLoggerTree.setLogConsole(this)

        homeInformationRepository.clearResetAppFlag()

        lifecycleScope.launch {
            ledA.connectValueWithException()
            ledB.connectValueWithException()
            ledC.connectValueWithException()
            led1.connectValueWithException()
            led2.connectValueWithException()
            buttonAInputDriver.connectValueWithException()
            buttonBInputDriver.connectValueWithException()
            buttonCInputDriver.connectValueWithException()

            ledA.setValueWithException(true)
            ledB.setValueWithException(true)
            ledC.setValueWithException(true)
            led1.setValueWithException(true)
            led2.setValueWithException(true)
        }
        // Create a new driver instance
        mDriver = InputDriver.Builder().setName(DRIVER_NAME)
                .setSupportedKeys(intArrayOf(KEYCODE_A, KEYCODE_B, KEYCODE_C)).build()

        // Register with the framework
        UserDriverManager.getInstance().registerInputDriver(mDriver)

        networkConnectionMonitor = NetworkConnectionMonitor(this)

        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager

        Timber.e("onCreate isNetworkConnected: ${networkConnectionMonitor.isNetworkConnected}")

        lifecycleScope.launch {
            ledA.setValueWithException(false)
            ledB.setValueWithException(false)
            ledC.setValueWithException(false)
            led1.setValueWithException(false)
            led2.setValueWithException(false)
        }

        //home.saveToRepository()

        //homeInformationRepository.setHomeReference("test home")

        connectAndSetupJob = lifecycleScope.launch(Dispatchers.IO) {
            if (networkConnectionMonitor.isNetworkConnected) {
                led1.setValueWithException(true)
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
                homeInformationRepository.setHomeReference(secureStorage.homeName)
                homeInformationRepository.startHomeToFirebaseConnectionActiveMonitor()
            } else {
                Timber.d("No Home Name defined, starting HomeNameReceiver")
                startHomeNameReceiver()
            }

            Timber.d("connectAndSetupJob finished")
        }
    }

    private suspend fun startWiFiCredentialsReceiver() {
        val blinkJob = blinkLed(ledA)
        try {
            withTimeout(NEARBY_TIMEOUT) {
                waitForWifiCredentials()?.let { wifiCredentials ->
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
        val blinkJob = blinkLed(ledB)
        try {
            withTimeout(NEARBY_TIMEOUT) {
                waitForFirebaseCredentials()?.let { credentials ->
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
        val blinkJob = blinkLed(ledC)
        try {
            withTimeout(NEARBY_TIMEOUT) {
                waitForHomeName()?.let { homeName ->
                    Timber.e("waitForHomeName returned:$homeName")
                    secureStorage.homeName = homeName
                    homeInformationRepository.setHomeReference(secureStorage.homeName)
                } ?: Timber.e("Could not get HomeName")
            }
        } finally {
            Timber.e("waitForHomeName timeout or finished")
            blinkJob.cancelAndJoin()
        }
    }

    private suspend fun waitForWifiCredentials(): WifiCredentials? = suspendCancellableCoroutine { cont ->
        val moshi: Moshi by inject()
        val nearbyService: NearbyService by inject()
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
                            Timber.d(
                                    "waitForWifiCredentials Data as wifiCredentials $wifiCredentials")
                            wifiCredentials?.run {
                                cont.resume(wifiCredentials)
                            }
                        } catch (e: IOException) {
                            cont.resumeWithException(e)
                            Timber.d(
                                    "waitForWifiCredentials Received Data could not be cast to WifiCredentials")
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

    private suspend fun waitForFirebaseCredentials(): FirebaseCredentials? = suspendCancellableCoroutine { cont ->
        val moshi: Moshi by inject()
        val nearbyService: NearbyService by inject()
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
                            Timber.d(
                                    "waitForFirebaseCredentials Received Data could not be cast to FirebaseCredentials")
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

    private suspend fun waitForHomeName(): String? = suspendCancellableCoroutine { cont ->
        val moshi: Moshi by inject()
        val nearbyService: NearbyService by inject()
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
                            Timber.d(
                                    "waitForHomeName Received Data could not be cast to FirebaseCredentials")
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

        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, "Home")
            param(FirebaseAnalytics.Param.SCREEN_CLASS, this@ThingsActivity::class.simpleName?: "ThingsActivity")
        }

        networkConnectionMonitor.startListen(networkConnectionListener)

        lifecycleScope.launch {
            buttonAInputDriver.registerListenerWithException(this@ThingsActivity)
            buttonBInputDriver.registerListenerWithException(this@ThingsActivity)
            buttonCInputDriver.registerListenerWithException(this@ThingsActivity)
        }
        if (connectAndSetupJob?.isCompleted == true) {
            home.start()
        } else {
            lifecycleScope.launch {
                connectAndSetupJob?.join()
                if (connectAndSetupJob?.isCancelled != true) {
                    Timber.d("onStart home.start()")
                    home.start()
                } else {
                    Timber.d("onStart was canceled do not call home.start()")
                }
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            connectAndSetupJob?.join()
            if (connectAndSetupJob?.isCancelled != true) {
                homeInformationRepository.restartAppFlow().collectLatest {
                    Timber.e("Remote restart app $it")
                    if (it) {
                        homeInformationRepository.clearResetAppFlag()
                        restartApp()
                    }
                }
            }
        }
    }

    override fun onStop() {
        Timber.d("onStop Shutting down lights observer")

        networkConnectionMonitor.stopListen()

        lifecycleScope.launch {
            connectAndSetupJob?.cancelAndJoin()
            buttonAInputDriver.unregisterListener()
            buttonBInputDriver.unregisterListener()
            buttonCInputDriver.unregisterListener()

            home.stop()
        }
        super.onStop()
    }

    override fun onDestroy() {
        Timber.d("onDestroy")

        UserDriverManager.getInstance().unregisterInputDriver(mDriver)
        runBlocking {
            connectAndSetupJob?.cancelAndJoin()
            ledA.closeValueWithException()
            ledB.closeValueWithException()
            ledC.closeValueWithException()
            led1.closeValueWithException()
            led2.closeValueWithException()
            buttonAInputDriver.closeValueWithException()
            buttonBInputDriver.closeValueWithException()
            buttonCInputDriver.closeValueWithException()
        }
        super.onDestroy()
    }

    override suspend fun onHwUnitChanged(hwUnit: HwUnit, unitValue: Boolean?, updateTime: Long) {
        Timber.d(
                "onHwUnitChanged hwUnit: $hwUnit ; unitValue: $unitValue ; updateTime: ${Date(updateTime)}")
        unitValue?.let {

            val keyCode = when (hwUnit.ioPin) {
                BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_1_PIN.name -> KEYCODE_A
                BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_2_PIN.name -> KEYCODE_B
                BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_3_PIN.name -> KEYCODE_C
                else                                                -> null
            }
            keyCode?.let {
                mDriver.emit(InputDriverEvent().apply {
                    setKeyPressed(keyCode, !unitValue)
                })
            }
        }
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        Timber.d("onKeyLongPress keyCode: $keyCode ; keEvent: $event")
        when (keyCode) {
            KEYCODE_A -> {
                lifecycleScope.launch {
                    ledA.setValueWithException(true)
                }
            }
            KEYCODE_B -> {
                lifecycleScope.launch {
                    ledB.setValueWithException(true)
                }
                // will cause onKeyUp be called with flag cancelled
                return true
            }
            KEYCODE_C -> {
                lifecycleScope.launch {
                    ledC.setValueWithException(true)
                }
            }
        }
        return super.onKeyLongPress(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KEYCODE_A -> {
                when (event?.repeatCount) {
                    0  -> {
                        Timber.d("onKeyDown keyCode: $keyCode ; keEvent: $event")
                        lifecycleScope.launch {
                            homeInformationRepository.logHwUnitEvent(
                                    HwUnitLog(BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_1,
                                            "Raspberry Pi", BoardConfig.IO_EXTENDER_PCF8474AT_INPUT,
                                            BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_1_PIN.name,
                                            ConnectionType.GPIO, value = true))
                            ledA.setValueWithException(true)
                        }
                        // start listen for LongKeyPress event
                        event.startTracking()

                        if (ledB.unitValue == true) {
                            lifecycleScope.launch(Dispatchers.Main) {
                                restartApp()
                            }
                        }
                        return true
                    }
                    50 -> {
                        Timber.d("onKeyDown very long press")
                        connectAndSetupJob?.cancel()
                        connectAndSetupJob = lifecycleScope.launch {
                            ledA.setValueWithException(false)
                            startWiFiCredentialsReceiver()
                            Timber.e("startWiFiCredentialsReceiver finished")
                        }
                        return true
                    }
                }
            }
            KEYCODE_B -> {
                when (event?.repeatCount) {
                    0  -> {
                        Timber.d("onKeyDown keyCode: $keyCode ; keEvent: $event")
                        lifecycleScope.launch {
                            homeInformationRepository.logHwUnitEvent(
                                    HwUnitLog(BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_2,
                                            "Raspberry Pi", BoardConfig.IO_EXTENDER_PCF8474AT_INPUT,
                                            BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_2_PIN.name,
                                            ConnectionType.GPIO, value = true))
                            ledB.setValueWithException(true)
                        }
                        // start listen for LongKeyPress event
                        event.startTracking()

                        if (ledA.unitValue == true) {
                            lifecycleScope.launch(Dispatchers.Main) {
                            restartApp()
                            }
                        }
                        return true
                    }
                    50 -> {
                        Timber.d("onKeyDown very long press")
                        connectAndSetupJob?.cancel()
                        connectAndSetupJob = lifecycleScope.launch {
                            ledB.setValueWithException(false)
                            startFirebaseCredentialsReceiver()
                            Timber.e("startFirebaseCredentialsReceiver finished")
                        }
                        return true
                    }
                }
            }
            KEYCODE_C -> {
                when (event?.repeatCount) {
                    0  -> {
                        Timber.d("onKeyDown keyCode: $keyCode ; keEvent: $event")
                        lifecycleScope.launch {
                            homeInformationRepository.logHwUnitEvent(
                                    HwUnitLog(BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_3,
                                            "Raspberry Pi", BoardConfig.IO_EXTENDER_PCF8474AT_INPUT,
                                            BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_3_PIN.name,
                                            ConnectionType.GPIO, value = true))
                            ledC.setValueWithException(true)
                        }
                        // start listen for LongKeyPress event
                        event.startTracking()
                        return true
                    }
                    50 -> {
                        Timber.d("onKeyDown very long press")
                        connectAndSetupJob?.cancel()
                        connectAndSetupJob = lifecycleScope.launch {
                            ledC.setValueWithException(false)
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
                lifecycleScope.launch {
                    homeInformationRepository.logHwUnitEvent(
                            HwUnitLog(BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_1, "Raspberry Pi",
                                    BoardConfig.IO_EXTENDER_PCF8474AT_INPUT,
                                    BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_1_PIN.name,
                                    ConnectionType.GPIO, value = false))
                    ledA.setValueWithException(false)
                }
            }
            KEYCODE_B -> {
                lifecycleScope.launch {
                    homeInformationRepository.logHwUnitEvent(
                            HwUnitLog(BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_2, "Raspberry Pi",
                                    BoardConfig.IO_EXTENDER_PCF8474AT_INPUT,
                                    BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_2_PIN.name,
                                    ConnectionType.GPIO, value = false))
                    ledB.setValueWithException(false)
                }
            }
            KEYCODE_C -> {
                lifecycleScope.launch {
                    homeInformationRepository.logHwUnitEvent(
                            HwUnitLog(BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_3, "Raspberry Pi",
                                    BoardConfig.IO_EXTENDER_PCF8474AT_INPUT,
                                    BoardConfig.IO_EXTENDER_PCF8574AT_BUTTON_3_PIN.name,
                                    ConnectionType.GPIO, value = false))
                    ledC.setValueWithException(false)
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun loginFirebase() {
        authentication.addLoginResultListener(loginResultListener)
        authentication.login(secureStorage.firebaseCredentials)
    }

    private suspend fun restartApp() {
        home.stop()
        Timber.e("restartApp after Home stop")
        ProcessPhoenix.triggerRebirth(application)
    }

    private fun addWiFi(wifiManager: WifiManager, wifiCredentials: WifiCredentials) {

        // only WPA is supported right now
        val wifiConfiguration = WifiConfiguration()
        wifiConfiguration.SSID = String.format("\"%s\"", wifiCredentials.ssid)
        wifiConfiguration.preSharedKey = String.format("\"%s\"", wifiCredentials.password)

        val existingConfig =
                wifiManager.configuredNetworks?.firstOrNull { wifiConfiguration.SSID == it.SSID }
        if (existingConfig != null) {
            Timber.d(
                    "This WiFi was already added update it. Existing: $existingConfig new one: $wifiConfiguration")
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

    private fun blinkLed(led: HwUnitI2CPCF8574ATActuator): Job {
        return lifecycleScope.launch(Dispatchers.IO) {
            try {
                repeat((NEARBY_TIMEOUT / NEARBY_BLINK_DELAY).toInt()) {
                    Timber.d("blinkLed ${led.unitValue}")
                    led.setValueWithException(led.unitValue?.not() ?: false)
                    delay(NEARBY_BLINK_DELAY)
                }
            } finally {
                Timber.d("blinkLed canceled or finished")
                led.setValueWithException(false)
            }
        }
    }

    private suspend fun <T : Any>Actuator<T>.setValueWithException(value: T){
        try {
            setValue(value)
        } catch (e: Exception) {
            addHwUnitErrorEvent(e, "Error updating hwUnit value on $hwUnit")
        }
    }
    private suspend fun <T : Any>Sensor<T>.readValueWithException(): T?{
        return try {
            readValue()
        } catch (e: Exception) {
            addHwUnitErrorEvent(e, "Error reading hwUnit value on $hwUnit")
            null
        }
    }
    private suspend fun <T : Any>Sensor<T>.registerListenerWithException(listener: Sensor.HwUnitListener<T>){
        supervisorScope {
            registerListener(this, listener, CoroutineExceptionHandler { _, error ->
                lifecycleScope.launch {
                    addHwUnitErrorEvent(error,
                            "Error registerListener CoroutineExceptionHandler hwUnit on $hwUnit")
                }
            })
        }
    }
    private suspend fun <T : Any> BaseHwUnit<T>.closeValueWithException(){
        try {
            close()
        } catch (e: Exception) {
            addHwUnitErrorEvent(e, "Error closing hwUnit on $hwUnit")
        }
    }
    private suspend fun <T : Any> BaseHwUnit<T>.connectValueWithException(){
        try {
            connect()
        } catch (e: Exception) {
            addHwUnitErrorEvent(e, "Error connecting hwUnit on $hwUnit")
        }
    }

    private fun <T : Any> BaseHwUnit<T>.addHwUnitErrorEvent(e: Throwable, logMessage: String) {
        homeInformationRepository.addHwUnitErrorEvent(
                HwUnitLog(hwUnit, unitValue, e.message))
        Firebase.crashlytics.recordException(e)
        Timber.e(e, logMessage)
    }
}
