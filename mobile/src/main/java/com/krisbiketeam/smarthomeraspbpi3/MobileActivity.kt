package com.krisbiketeam.smarthomeraspbpi3

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.krisbiketeam.data.auth.Authentication
import com.krisbiketeam.data.auth.FirebaseAuthentication
import com.krisbiketeam.data.storage.*
import com.krisbiketeam.data.storage.obsolete.HomeInformation
import com.krisbiketeam.data.storage.UnitsLiveData
import kotlinx.android.synthetic.main.activity_mobile.*
import timber.log.Timber
import java.util.*


class MobileActivity : AppCompatActivity() {

    private lateinit var authentication: Authentication
    private lateinit var lightsLiveData: LiveData<HomeInformation>
    private lateinit var homeInformationRepository: HomeInformationRepository
    private lateinit var secureStorage: SecureStorage
    private lateinit var unitsLiveData: UnitsLiveData

    // Mobile Activity
    private val lightDataObserver = Observer<HomeInformation> { homeInformation ->
        setToggle(homeInformation?.light ?: false)
        setTemperature(homeInformation?.temperature ?: 0f)
        setPressure(homeInformation?.pressure ?: 0f)
        Timber.d("HomeInformation changed: $homeInformation")
    }
    private val unitsDataObserver = Observer<Any> { temperature ->
        Timber.d("Unit changed: $temperature")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mobile)

        secureStorage = NotSecureStorage(this)
        homeInformationRepository = FirebaseHomeInformationRepository()
        authentication = FirebaseAuthentication()
        lightsLiveData = homeInformationRepository.lightLiveData()
        unitsLiveData = homeInformationRepository.unitsLiveData()

        lightToggle.setOnCheckedChangeListener { _, state: Boolean ->
            homeInformationRepository.saveLightState(state)
        }

        sendText.setOnClickListener {
            homeInformationRepository.saveMessage(messageEdit.text.toString())
            messageEdit.text.clear()
        }

        buttonClearLog.setOnClickListener {
            homeInformationRepository.clearLog()
        }

        buttonWifi.setOnClickListener {
            callActivity(WifiActivity::class.java)
        }
        buttonLogin.setOnClickListener {
            callActivity(LoginActivity::class.java)
        }

        //val home = Home()
        //home.saveToRepository(homeInformationRepository)

    }

    private fun observeLightsData() {
        Timber.d("Observing lights data")
        lightsLiveData.observe(this, lightDataObserver)
        unitsLiveData.observe(this, unitsDataObserver)
    }

    private fun stopObserveLightsData() {
        Timber.d("Stop Observing lights data")
        lightsLiveData.removeObserver { lightDataObserver }
        unitsLiveData.removeObserver(unitsDataObserver)
    }

    override fun onResume() {
        super.onResume()
        when {
            secureStorage.retrieveFirebaseCredentials() != null -> {
                authentication.login(secureStorage.retrieveFirebaseCredentials()!!)
            }
            else -> throw Exception("You should have credentials!")
        }
        lightToggle.isActivated = true
        observeLightsData()
    }

    override fun onPause() {
        super.onPause()
        stopObserveLightsData()
        lightToggle.isActivated = false
    }

    private fun setToggle(state: Boolean) {
        Timber.d("Changing the toggle to $state")
        lightToggle.isChecked = state
    }

    private fun setPressure(pressure: Float) {
        pressureText.text = String.format(Locale.UK, "Current pressure: %.2f", pressure)
    }

    private fun setTemperature(temperature: Float) {
        temperatureText.text = String.format(Locale.UK, "Current pressure: %.2f", temperature)
    }

    private fun callActivity(clazz: Class<*>) {
        val intent = Intent(this, clazz)
        startActivity(intent)
        finish()
    }
}
