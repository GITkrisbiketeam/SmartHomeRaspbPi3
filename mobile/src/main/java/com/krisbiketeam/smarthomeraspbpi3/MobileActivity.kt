package com.krisbiketeam.smarthomeraspbpi3

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.krisbiketeam.data.auth.Authentication
import com.krisbiketeam.data.storage.ChildEventType
import com.krisbiketeam.data.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.data.storage.SecureStorage
import com.krisbiketeam.data.storage.StorageUnitsLiveData
import com.krisbiketeam.data.storage.dto.StorageUnit
import com.krisbiketeam.data.storage.obsolete.HomeInformation
import kotlinx.android.synthetic.main.activity_mobile.*
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.*


class MobileActivity : AppCompatActivity() {

    private val authentication: Authentication by inject()
    private val secureStorage: SecureStorage by inject()

    private lateinit var lightsLiveData: LiveData<HomeInformation>
    private lateinit var storageUnitsLiveData: StorageUnitsLiveData

    // Mobile Activity
    // Obsolete code START
    private val lightDataObserver = Observer<HomeInformation> { homeInformation ->
        setToggle(homeInformation?.light ?: false)
        setTemperature(homeInformation?.temperature ?: 0f)
        setPressure(homeInformation?.pressure ?: 0f)
        Timber.d("HomeInformation changed: $homeInformation")
    }
    // Obsolete code END

    private val unitsDataObserver = Observer<Pair<ChildEventType, StorageUnit<Any>>> { temperature ->
        Timber.d("Unit changed: $temperature")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mobile)

        lightsLiveData = FirebaseHomeInformationRepository.lightLiveData()
        storageUnitsLiveData = FirebaseHomeInformationRepository.storageUnitsLiveData()

        lightToggle.setOnCheckedChangeListener { _, state: Boolean ->
            FirebaseHomeInformationRepository.saveLightState(state)
        }

        sendText.setOnClickListener {
            FirebaseHomeInformationRepository.saveMessage(messageEdit.text.toString())
            messageEdit.text.clear()
        }

        buttonClearLog.setOnClickListener {
            FirebaseHomeInformationRepository.clearLog()
        }

        buttonWifi.setOnClickListener {
            callActivity(WifiActivity::class.java)
        }
        buttonLogin.setOnClickListener {
            callActivity(LoginActivity::class.java)
        }
        buttonAddStorageHomeUnit.setOnClickListener{

        }

        //val home = Home()
        //home.saveToRepository(homeInformationRepository)

    }

    private fun observeLightsData() {
        Timber.d("Observing lights data")
        // Obsolete code
        lightsLiveData.observe(this, lightDataObserver)

        storageUnitsLiveData.observe(this, unitsDataObserver)
    }

    private fun stopObserveLightsData() {
        Timber.d("Stop Observing lights data")
        // Obsolete code
        lightsLiveData.removeObserver { lightDataObserver }

        storageUnitsLiveData.removeObserver(unitsDataObserver)
    }

    override fun onResume() {
        super.onResume()
        when {
            secureStorage.isAuthenticated() -> {
                authentication.login(secureStorage.firebaseCredentials)
            }
            else -> Timber.d("You should have credentials!")
        }
        lightToggle.isActivated = true
        observeLightsData()
    }

    override fun onPause() {
        super.onPause()
        stopObserveLightsData()
        lightToggle.isActivated = false
    }

    // Obsolete code START
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
    // Obsolete code END

    private fun callActivity(clazz: Class<*>) {
        val intent = Intent(this, clazz)
        startActivity(intent)
        finish()
    }
}
