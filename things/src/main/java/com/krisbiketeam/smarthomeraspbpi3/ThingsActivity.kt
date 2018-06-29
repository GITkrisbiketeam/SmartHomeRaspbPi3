package com.krisbiketeam.smarthomeraspbpi3

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.krisbiketeam.data.auth.Authentication
import com.krisbiketeam.data.auth.FirebaseAuthentication
import com.krisbiketeam.data.storage.*
import com.krisbiketeam.smarthomeraspbpi3.hardware.*
import timber.log.Timber


class ThingsActivity : AppCompatActivity() {

    private lateinit var authentication: Authentication
    private lateinit var lightsLiveData: LiveData<HomeInformation>
    private lateinit var homeInformationRepository: HomeInformationRepository
    private lateinit var secureStorage: SecureStorage
    private lateinit var networkConnectionMonitor: NetworkConnectionMonitor

    private val led = Led()
    private val ledHat = LedHat(LedHat.LED.GREEN)
    private val tempAndPressureSensor = HatTemperatureAndPressureSensor()
    private val fourCharDisplay = FourCharDisplay()

    private val lightsDataObserver = Observer<HomeInformation> { homeInformation ->
        setLightState(homeInformation?.light ?: false)
        setMessage(homeInformation?.message)
        Timber.d("homeInformation changed: $homeInformation")
    }

    private val temperatureAndPressureChangedListener = object : Sensor.OnStateChangeListener<TemperatureAndPressure> {
        override fun onStateChanged(state: TemperatureAndPressure) {
            Timber.d("Received TemperatureAndPressure $state")
            homeInformationRepository.saveTemperature(state.temperature)
            homeInformationRepository.savePressure(state.pressure)
        }
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
        super.onCreate(savedInstanceState)

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
        lightsLiveData = homeInformationRepository.lightsLiveData()
    }

    override fun onStart() {
        super.onStart()
        authentication.addLoginResultListener(loginResultListener)
        authentication.login(secureStorage.retrieveFirebaseCredentials()!!)

        // lightsLiveData is registered after successful login

        networkConnectionMonitor.startListen(networkConnectionListener)

        tempAndPressureSensor.start(temperatureAndPressureChangedListener)
        led.start()
        ledHat.start()
        fourCharDisplay.start()
    }

    override fun onStop() {
        Timber.d("Shutting down lights observer")
        lightsLiveData.removeObserver { lightsDataObserver }

        networkConnectionMonitor.stopListen()

        tempAndPressureSensor.stop()
        led.stop()
        ledHat.stop()
        fourCharDisplay.stop()
        super.onStop()
    }

    private fun observeLightsData() {
        Timber.d("Observing lights data")
        lightsLiveData.observe(this, lightsDataObserver)
    }

    private fun setLightState(b: Boolean) {
        Timber.d("Setting light to $b")
        led.setState(b)
        ledHat.setState(b)
    }

    private fun setMessage(message: String?) {
        Timber.d("Setting message to $message")
        fourCharDisplay.setState(message ?: "")
    }
}
