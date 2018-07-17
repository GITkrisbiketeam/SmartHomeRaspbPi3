package com.krisbiketeam.smarthomeraspbpi3.ui

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.android.things.pio.Gpio
import com.krisbiketeam.data.auth.Authentication
import com.krisbiketeam.data.auth.FirebaseAuthentication
import com.krisbiketeam.data.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.data.storage.HomeInformationRepository
import com.krisbiketeam.data.storage.NotSecureStorage
import com.krisbiketeam.data.storage.SecureStorage
import com.krisbiketeam.data.storage.dto.HomeUnitLog
import com.krisbiketeam.data.storage.obsolete.HomeInformation
import com.krisbiketeam.smarthomeraspbpi3.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.Home
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.ui.setup.FirebaseCredentialsReceiverActivity
import com.krisbiketeam.smarthomeraspbpi3.ui.setup.WiFiCredentialsReceiverActivity
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import com.krisbiketeam.smarthomeraspbpi3.units.hardware.HomeUnitGpioActuator
import com.krisbiketeam.smarthomeraspbpi3.units.hardware.HomeUnitGpioSensor
import com.krisbiketeam.smarthomeraspbpi3.units.hardware.HomeUnitI2CFourCharDisplay
import com.krisbiketeam.smarthomeraspbpi3.utils.Logger
import timber.log.Timber


class ThingsActivity : AppCompatActivity() {
    private lateinit var authentication: Authentication
    private lateinit var lightsLiveData: LiveData<HomeInformation>
    private lateinit var homeInformationRepository: HomeInformationRepository
    private lateinit var secureStorage: SecureStorage
    private lateinit var networkConnectionMonitor: NetworkConnectionMonitor

    private lateinit var home: Home
    private val ledA: HomeUnitGpioActuator
    private val ledB: HomeUnitGpioActuator
    private val ledC: HomeUnitGpioActuator
    private val buttonA: HomeUnitGpioSensor
    private val buttonB: HomeUnitGpioSensor
    private val buttonC: HomeUnitGpioSensor

    private val fourCharDisplay = HomeUnitI2CFourCharDisplay(BoardConfig.FOUR_CHAR_DISP, "Raspberry Pi", BoardConfig.FOUR_CHAR_DISP_PIN)

    init {
        Timber.d("init")

        ledA = HomeUnitGpioActuator(BoardConfig.LED_A, "Raspberry Pi",
                BoardConfig.LED_A_PIN,
                Gpio.ACTIVE_HIGH)
        ledB = HomeUnitGpioActuator(BoardConfig.LED_B, "Raspberry Pi",
                BoardConfig.LED_B_PIN,
                Gpio.ACTIVE_HIGH)
        ledC = HomeUnitGpioActuator(BoardConfig.LED_C, "Raspberry Pi",
                BoardConfig.LED_C_PIN,
                Gpio.ACTIVE_HIGH)

        buttonA = HomeUnitGpioSensor(BoardConfig.BUTTON_A, "Raspberry Pi",
                BoardConfig.BUTTON_A_PIN,
                Gpio.ACTIVE_LOW)
        buttonB = HomeUnitGpioSensor(BoardConfig.BUTTON_B, "Raspberry Pi",
                BoardConfig.BUTTON_B_PIN,
                Gpio.ACTIVE_LOW)
        buttonC = HomeUnitGpioSensor(BoardConfig.BUTTON_C, "Raspberry Pi",
                BoardConfig.BUTTON_C_PIN,
                Gpio.ACTIVE_LOW)
    }

    // Obsolete code
    private val lightDataObserver = Observer<HomeInformation> { homeInformation ->
        setLightState(homeInformation?.light ?: false)
        setMessage(homeInformation?.message)
        Timber.d("homeInformation changed: $homeInformation")
    }


    private val networkConnectionListener = object : NetworkConnectionListener {
        override fun onNetworkAvailable(available: Boolean) {
            Timber.d("Received onNetworkAvailable $available")

        }
    }

    private val loginResultListener = object : Authentication.LoginResultListener {
        override fun success() {
            Timber.d("LoginResultListener success")
            observeLightsData()
        }

        override fun failed(exception: Exception) {
            Timber.d("LoginResultListener failed e: $exception")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)
        Logger.getInstance().setLogConsole(this)

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
            Timber.d("Not connected to WiFi, starting WiFiCredentialsReceiverActivity")
            val intent = Intent(this, WiFiCredentialsReceiverActivity::class.java)
            startActivity(intent)
            finish()
        }

        if (!secureStorage.isAuthenticated()) {
            Timber.d("Not authenticated, starting FirebaseCredentialsReceiverActivity")
            val intent = Intent(this, FirebaseCredentialsReceiverActivity::class.java)
            startActivity(intent)
            finish()
        }

        homeInformationRepository = FirebaseHomeInformationRepository()
        authentication = FirebaseAuthentication()
        lightsLiveData = homeInformationRepository.lightLiveData()

        home = Home(homeInformationRepository)
        //home.saveToRepository(homeInformationRepository)

    }

    override fun onStart() {
        Timber.d("onStart")
        super.onStart()
        authentication.addLoginResultListener(loginResultListener)
        authentication.login(secureStorage.retrieveFirebaseCredentials()!!)

        // lightLiveData is registered after successful login

        networkConnectionMonitor.startListen(networkConnectionListener)

        buttonA.run {
            connect()
            registerListener(object : Sensor.HomeUnitListener<Boolean> {
                override fun onUnitChanged(homeUnit: HomeUnitLog<out Boolean>) {
                    Timber.d("onUnitChanged A unit: $homeUnit")
                    homeInformationRepository.logUnitEvent(homeUnit)
                    ledA.setValue(homeUnit.value)
                }
            })
        }
        buttonB.run {
            connect()
            registerListener(object : Sensor.HomeUnitListener<Boolean> {
                override fun onUnitChanged(homeUnit: HomeUnitLog<out Boolean>) {
                    Timber.d("onUnitChanged B unit: $homeUnit")
                    homeInformationRepository.logUnitEvent(homeUnit)
                    ledB.setValue(homeUnit.value)
                    /*try {
                        mBuzzer.play(440.0)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }*/
                }
            })
        }
        buttonC.run {
            connect()
            registerListener(object : Sensor.HomeUnitListener<Boolean> {
                override fun onUnitChanged(homeUnit: HomeUnitLog<out Boolean>) {
                    Timber.d("onUnitChanged C unit: $homeUnit")
                    homeInformationRepository.logUnitEvent(homeUnit)
                    ledC.setValue(homeUnit.value)
                    /*try {
                        // Stop the buzzer.
                        mBuzzer.stop()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }*/
                }
            })
        }
        ledA.connect()
        ledB.connect()
        ledC.connect()

        home.start()
    }

    override fun onStop() {
        Timber.d("onStop Shutting down lights observer")
        lightsLiveData.removeObserver(lightDataObserver)

        networkConnectionMonitor.stopListen()

        buttonA.close()
        buttonA.unregisterListener()
        buttonB.close()
        buttonB.unregisterListener()
        buttonC.close()
        buttonC.unregisterListener()
        ledA.close()
        ledB.close()
        ledC.close()

        home.stop()
        super.onStop()
    }

    private fun observeLightsData() {
        Timber.d("Observing lights data")
        lightsLiveData.observe(this, lightDataObserver)
    }

    private fun setLightState(b: Boolean) {
        Timber.d("Setting light to $b")
        ledA.setValue(b)
    }

    private fun setMessage(message: String?) {
        Timber.d("Setting message to $message")
        fourCharDisplay.setValue(message)
    }
}
