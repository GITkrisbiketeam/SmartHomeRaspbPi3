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
import com.krisbiketeam.data.storage.*
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.BaseUnit
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import com.krisbiketeam.smarthomeraspbpi3.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.ui.setup.FirebaseCredentialsReceiverActivity
import com.krisbiketeam.smarthomeraspbpi3.ui.setup.WiFiCredentialsReceiverActivity
import com.krisbiketeam.smarthomeraspbpi3.units.*
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor.HomeUnitListener
import timber.log.Timber
import java.util.*


class ThingsActivity : AppCompatActivity(), HomeUnitListener<Any> {
    private lateinit var authentication: Authentication
    private lateinit var lightsLiveData: LiveData<HomeInformation>
    private lateinit var homeInformationRepository: HomeInformationRepository
    private lateinit var secureStorage: SecureStorage
    private lateinit var networkConnectionMonitor: NetworkConnectionMonitor

    private val unitList: MutableMap<String, BaseUnit<Any>> = HashMap()

    //private val tempAndPressureSensor = HatTemperatureAndPressureSensor()
    private val fourCharDisplay = HomeUnitI2CFourCharDisplay(BoardConfig.FOUR_CHAR_DISP, "Raspberry Pi", BoardConfig.FOUR_CHAR_DISP_PIN)

    init{
        val ledA = HomeUnitGpioActuator(BoardConfig.LED_A, "Raspberry Pi", BoardConfig
                .LED_A_PIN, Gpio.ACTIVE_HIGH) as Actuator<Any>
        unitList.put(BoardConfig.LED_A, ledA)
        val ledB = HomeUnitGpioActuator(BoardConfig.LED_B, "Raspberry Pi", BoardConfig
                .LED_B_PIN, Gpio.ACTIVE_HIGH) as Actuator<Any>
        unitList.put(BoardConfig.LED_B, ledB)
        val ledC = HomeUnitGpioActuator(BoardConfig.LED_C, "Raspberry Pi", BoardConfig
                .LED_C_PIN, Gpio.ACTIVE_HIGH) as BaseUnit<Any>
        unitList.put(BoardConfig.LED_C, ledC)

        val buttonA = HomeUnitGpioSensor(BoardConfig.BUTTON_A, "Raspberry Pi", BoardConfig
                .BUTTON_A_PIN, Gpio.ACTIVE_LOW) as Sensor<Any>
        unitList.put(BoardConfig.BUTTON_A, buttonA)
        buttonA.registerListener(this)

        val buttonB = HomeUnitGpioSensor(BoardConfig.BUTTON_B, "Raspberry Pi", BoardConfig
                .BUTTON_B_PIN, Gpio.ACTIVE_LOW) as Sensor<Any>
        unitList.put(BoardConfig.BUTTON_B, buttonB)
        buttonB.registerListener(this)

        val buttonC = HomeUnitGpioSensor(BoardConfig.BUTTON_C, "Raspberry Pi", BoardConfig
                .BUTTON_C_PIN, Gpio.ACTIVE_LOW) as Sensor<Any>
        unitList.put(BoardConfig.BUTTON_C, buttonC)
        buttonC.registerListener(this)

        val motion = HomeUnitGpioSensor(BoardConfig.MOTION_1, "Raspberry Pi", BoardConfig
                .MOTION_1_PIN, Gpio.ACTIVE_HIGH) as Sensor<Any>
        unitList.put(BoardConfig.MOTION_1, motion)
        motion.registerListener(this)

        val contactron = HomeUnitGpioNoiseSensor(BoardConfig.CONTACT_1, "Raspberry Pi", BoardConfig
                .CONTACT_1_PIN, Gpio.ACTIVE_LOW) as Sensor<Any>
        unitList.put(BoardConfig.CONTACT_1, contactron)
        contactron.registerListener(this)

        val temperatureSensor = HomeUnitI2CTemperatureSensor(BoardConfig.TEMP_SENSOR_TMP102, "Raspberry Pi", BoardConfig
                .TEMP_SENSOR_TMP102_PIN, BoardConfig.TEMP_SENSOR_TMP102_ADDR) as Sensor<Any>
        unitList.put(BoardConfig.TEMP_SENSOR_TMP102, temperatureSensor)
        temperatureSensor.registerListener(this)

        //private val tempAndPressureSensor = HatTemperatureAndPressureSensor()
        //unitList.put(BoardConfig.TEMP_SENSOR_TMP102, tempAndPressureSensor)
        //tempAndPressureSensor.registerListener(this)

    }

    /*private val temperatureAndPressureChangedListener = object : Sensor.OnStateChangeListener<TemperatureAndPressure> {
        override fun onStateChanged(state: TemperatureAndPressure) {
            Timber.d("Received TemperatureAndPressure $state")
            homeInformationRepository.saveTemperature(state.temperature)
            homeInformationRepository.savePressure(state.pressure)
        }
    }*/

    private val lightsDataObserver = Observer<HomeInformation> { homeInformation ->
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

        unitList.values.forEach { unit ->
            unit.connect()
            if (unit is Sensor) {
                unit.registerListener(this)
            }
        }
    }

    override fun onStop() {
        Timber.d("Shutting down lights observer")
        lightsLiveData.removeObserver { lightsDataObserver }

        networkConnectionMonitor.stopListen()

        for (unit in unitList.values) {
            try {
                unit.close()
            } catch (e: Exception) {
                Timber.e( "Error on PeripheralIO API", e)
            }
        }

        super.onStop()
    }

    private fun observeLightsData() {
        Timber.d("Observing lights data")
        lightsLiveData.observe(this, lightsDataObserver)
    }

    private fun setLightState(b: Boolean) {
        Timber.d("Setting light to $b")
        val unit = unitList.get(BoardConfig.LED_A)
        if (unit is Actuator) {
            unit.setValue(b)
        }
    }

    private fun setMessage(message: String?) {
        Timber.d("Setting message to $message")
        fourCharDisplay.setValue(message)
    }

    override fun onUnitChanged(homeUnit: HomeUnit<out Any>, value: Any?) {
        Timber.d("onUnitChanged unit: $homeUnit value: $value")
        homeInformationRepository.logUnitEvent(
                HomeUnitDB(homeUnit.name, homeUnit.connectionType, homeUnit.location, homeUnit.pinName, homeUnit.softAddress, homeUnit.value))
        val unit: BaseUnit<Any>?
        when (homeUnit.name) {
            BoardConfig.BUTTON_A -> {
                unit = unitList.get(BoardConfig.LED_A)
                if (unit is Actuator && value != null) {
                    unit.setValue(value)
                }
            }
            BoardConfig.BUTTON_B -> {
                unit = unitList.get(BoardConfig.LED_B)
                if (unit is Actuator && value != null) {
                    unit.setValue(value)
                }
                /*try {
                    mBuzzer.play(440.0)
                } catch (e: IOException) {
                    e.printStackTrace()
                }*/

            }
            BoardConfig.BUTTON_C -> {
                unit = unitList.get(BoardConfig.LED_C)
                if (unit is Actuator && value != null) {
                    unit.setValue(value)
                }
                /*try {
                    // Stop the buzzer.
                    mBuzzer.stop()
                } catch (e: IOException) {
                    e.printStackTrace()
                }*/

            }
            /*BoardConfig.MOTION_1 -> if (value != null) {
                lightOnOffOneRainbowLed(0, (value as Boolean?)!!)
            }
            BoardConfig.CONTACT_1 -> if (value != null) {
                lightOnOffOneRainbowLed(1, (value as Boolean?)!!)
            }*/
        }
    }


}
