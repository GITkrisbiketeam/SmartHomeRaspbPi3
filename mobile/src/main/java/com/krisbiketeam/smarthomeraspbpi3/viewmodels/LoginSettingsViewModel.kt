package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.krisbiketeam.data.auth.Authentication
import com.krisbiketeam.data.auth.AuthenticationLiveData
import com.krisbiketeam.data.auth.AuthenticationState
import com.krisbiketeam.data.auth.FirebaseCredentials
import com.krisbiketeam.data.nearby.NearbyServiceLiveData
import com.krisbiketeam.data.nearby.NearbySettingsState
import com.krisbiketeam.smarthomeraspbpi3.ui.WifiSettingsFragment
import org.koin.android.ext.android.inject
import timber.log.Timber


/**
 * The ViewModel used in [WifiSettingsFragment].
 */
class LoginSettingsViewModel(val authentication: AuthenticationLiveData, val nearByState: NearbyServiceLiveData) : ViewModel() {
    var email: MutableLiveData<String> = MutableLiveData()
    var password: MutableLiveData<String> = MutableLiveData()
    var remoteLogin: MutableLiveData<Boolean> = MutableLiveData()

    init{
        Timber.d("init")
        remoteLogin.value = false
    }

    fun sendData(data: Any) {
        Timber.d("sendData")
        nearByState.value = Pair(NearbySettingsState.CONNECTING, data)
    }

    fun login(data: FirebaseCredentials) {
        Timber.d("login")
        authentication.value = Pair(AuthenticationState.CONNECTING, data)
    }

    override fun onCleared() {
        nearByState.onCleared()
        super.onCleared()
    }
}
