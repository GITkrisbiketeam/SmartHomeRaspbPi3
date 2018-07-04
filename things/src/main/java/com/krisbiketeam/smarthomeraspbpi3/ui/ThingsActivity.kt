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
import com.krisbiketeam.data.storage.dto.HomeInformation
import com.krisbiketeam.data.storage.UnitsLiveData
import com.krisbiketeam.data.storage.dto.HomeUnitLog
import com.krisbiketeam.smarthomeraspbpi3.units.Actuator
import com.krisbiketeam.smarthomeraspbpi3.units.BaseUnit
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor
import com.krisbiketeam.smarthomeraspbpi3.BoardConfig
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.ui.setup.FirebaseCredentialsReceiverActivity
import com.krisbiketeam.smarthomeraspbpi3.ui.setup.WiFiCredentialsReceiverActivity
import com.krisbiketeam.smarthomeraspbpi3.units.Sensor.HomeUnitListener
import com.krisbiketeam.smarthomeraspbpi3.units.hardware.*
import com.krisbiketeam.smarthomeraspbpi3.utils.Logger
import timber.log.Timber
import java.util.*


class ThingsActivity : AppCompatActivity(), HomeUnitListener<Any> {
    private lateinit var authentication: Authentication
    private lateinit var lightsLiveData: LiveData<HomeInformation>
    private lateinit var homeInformationRepository: HomeInformationRepository
    private lateinit var secureStorage: SecureStorage
    private lateinit var networkConnectionMonitor: NetworkConnectionMonitor

    private lateinit var unitsLiveData: UnitsLiveData


    private val home = Home()

    private val unitList: MutableMap<String, BaseUnit<Any>> = HashMap()

    private val fourCharDisplay = HomeUnitI2CFourCharDisplay(BoardConfig.FOUR_CHAR_DISP, "Raspberry Pi", BoardConfig.FOUR_CHAR_DISP_PIN)

    init {
        Timber.d("init")

        val ledA = HomeUnitGpioActuator(BoardConfig.LED_A, "Raspberry Pi", BoardConfig
                .LED_A_PIN, Gpio.ACTIVE_HIGH) as Actuator<Any>
        unitList[BoardConfig.LED_A] = ledA
        val ledB = HomeUnitGpioActuator(BoardConfig.LED_B, "Raspberry Pi", BoardConfig
                .LED_B_PIN, Gpio.ACTIVE_HIGH) as Actuator<Any>
        unitList[BoardConfig.LED_B] = ledB
        val ledC = HomeUnitGpioActuator(BoardConfig.LED_C, "Raspberry Pi", BoardConfig
                .LED_C_PIN, Gpio.ACTIVE_HIGH) as BaseUnit<Any>
        unitList[BoardConfig.LED_C] = ledC

        val buttonA = HomeUnitGpioSensor(BoardConfig.BUTTON_A, "Raspberry Pi", BoardConfig
                .BUTTON_A_PIN, Gpio.ACTIVE_LOW) as Sensor<Any>
        unitList[BoardConfig.BUTTON_A] = buttonA

        val buttonB = HomeUnitGpioSensor(BoardConfig.BUTTON_B, "Raspberry Pi", BoardConfig
                .BUTTON_B_PIN, Gpio.ACTIVE_LOW) as Sensor<Any>
        unitList[BoardConfig.BUTTON_B] = buttonB

        val buttonC = HomeUnitGpioSensor(BoardConfig.BUTTON_C, "Raspberry Pi", BoardConfig
                .BUTTON_C_PIN, Gpio.ACTIVE_LOW) as Sensor<Any>
        unitList[BoardConfig.BUTTON_C] = buttonC

        val motion = HomeUnitGpioSensor(BoardConfig.MOTION_1, "Raspberry Pi", BoardConfig
                .MOTION_1_PIN, Gpio.ACTIVE_HIGH) as Sensor<Any>
        unitList[BoardConfig.MOTION_1] = motion

        val contactron = HomeUnitGpioNoiseSensor(BoardConfig.REED_SWITCH_1, "Raspberry Pi", BoardConfig
                .REED_SWITCH_1_PIN, Gpio.ACTIVE_LOW) as Sensor<Any>
        unitList[BoardConfig.REED_SWITCH_1] = contactron

        val temperatureSensor = HomeUnitI2CTempTMP102Sensor(BoardConfig.TEMP_SENSOR_TMP102, "Raspberry Pi", BoardConfig
                .TEMP_SENSOR_TMP102_PIN, BoardConfig.TEMP_SENSOR_TMP102_ADDR) as Sensor<Any>
        unitList[BoardConfig.TEMP_SENSOR_TMP102] = temperatureSensor

        val tempePressSensor = HomeUnitI2CTempPressBMP280Sensor(BoardConfig.TEMP_PRESS_SENSOR_BMP280, "Raspberry Pi", BoardConfig
                .TEMP_PRESS_SENSOR_BMP280_PIN, BoardConfig.TEMP_PRESS_SENSOR_BMP280_ADDR) as Sensor<Any>
        unitList[BoardConfig.TEMP_PRESS_SENSOR_BMP280] = tempePressSensor

    }

    private val lightDataObserver = Observer<HomeInformation> { homeInformation ->
        setLightState(homeInformation?.light ?: false)
        setMessage(homeInformation?.message)
        Timber.d("homeInformation changed: $homeInformation")
    }

    private val unitsDataObserver = Observer<Any> { value ->
        Timber.d("unitsDataObserver changed: $value")
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

        unitsLiveData = homeInformationRepository.unitsLiveData()

        //home.saveToRepository(homeInformationRepository)
    }

    override fun onStart() {
        Timber.d("onStart")
        super.onStart()
        authentication.addLoginResultListener(loginResultListener)
        authentication.login(secureStorage.retrieveFirebaseCredentials()!!)

        // lightLiveData is registered after successful login

        networkConnectionMonitor.startListen(networkConnectionListener)

        unitList.values.forEach { unit ->
            Timber.d("onStart connect unit: ${unit.homeUnit}")
            unit.connect()
            if (unit is Sensor) {
                unit.registerListener(this)
            }
        }
    }

    override fun onStop() {
        Timber.d("onStop Shutting down lights observer")
        lightsLiveData.removeObserver { lightDataObserver }
        unitsLiveData.removeObserver(unitsDataObserver)

        networkConnectionMonitor.stopListen()

        for (unit in unitList.values) {
            try {
                unit.close()
            } catch (e: Exception) {
                Timber.e("Error on PeripheralIO API", e)
            }
        }

        super.onStop()
    }

    private fun observeLightsData() {
        Timber.d("Observing lights data")
        lightsLiveData.observe(this, lightDataObserver)

        unitsLiveData.observe(this, unitsDataObserver)
    }

    private fun setLightState(b: Boolean) {
        Timber.d("Setting light to $b")
        val unit = unitList[BoardConfig.LED_C]
        if (unit is Actuator) {
            unit.setValue(b)
        }
    }

    private fun setMessage(message: String?) {
        Timber.d("Setting message to $message")
        fourCharDisplay.setValue(message)
    }

    override fun onUnitChanged(homeUnit: HomeUnitLog<out Any>, value: Any?) {
        Timber.d("onUnitChanged unit: $homeUnit value: $value")
        homeInformationRepository.logUnitEvent(homeUnit)
        val unit: BaseUnit<Any>?
        when (homeUnit.name) {
            BoardConfig.BUTTON_A -> {
                unit = unitList[BoardConfig.LED_A]
                if (unit is Actuator && value != null) {
                    unit.setValue(value)
                }
            }
            BoardConfig.BUTTON_B -> {
                unit = unitList[BoardConfig.LED_B]
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
                unit = unitList[BoardConfig.LED_C]
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
            BoardConfig.TEMP_PRESS_SENSOR_BMP280 -> if (value is TemperatureAndPressure) {
                Timber.d("Received TemperatureAndPressure $value")
                //homeInformationRepository.saveTemperature(value.temperature)
                //homeInformationRepository.savePressure(value.pressure)
                homeInformationRepository.saveTemperature(
                        home.temperatures.values.first().apply {
                            this.value = value.temperature
                        })
            }
            BoardConfig.TEMP_SENSOR_TMP102 -> if (value is Float) {
                Timber.d("Received Temperature $value")
                homeInformationRepository.saveTemperature(
                        home.temperatures.values.last().apply {
                            this.value = value
                        })
            }
            BoardConfig.MOTION_1 -> if (value is Boolean) {
                //lightOnOffOneRainbowLed(0, (value as Boolean?)!!)
                homeInformationRepository.saveMotion(
                        home.motions.values.first().apply {
                            this.active = value
                        })
            }
            BoardConfig.REED_SWITCH_1 -> if (value is Boolean) {
                //lightOnOffOneRainbowLed(1, (value as Boolean?)!!)
                homeInformationRepository.saveReedSwitch(
                        home.reedSwitches.values.first().apply {
                            this.active = value
                        })
            }
        }
    }


}
