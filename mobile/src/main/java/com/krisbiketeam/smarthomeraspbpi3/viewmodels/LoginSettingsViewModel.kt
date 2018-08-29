package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.krisbiketeam.data.MyLiveDataState
import com.krisbiketeam.data.auth.AuthenticationLiveData
import com.krisbiketeam.data.auth.FirebaseCredentials
import com.krisbiketeam.data.nearby.NearbyServiceLiveData
import com.krisbiketeam.smarthomeraspbpi3.ui.WifiSettingsFragment
import timber.log.Timber


/**
 * The ViewModel used in [WifiSettingsFragment].
 */
class LoginSettingsViewModel(private val authentication: AuthenticationLiveData, private val nearByState: NearbyServiceLiveData) : ViewModel() {
    var email: MutableLiveData<String> = MutableLiveData()
    var password: MutableLiveData<String> = MutableLiveData()
    var remoteLogin: MutableLiveData<Boolean> = MutableLiveData()
    val loginState = MediatorLiveData<Pair<MyLiveDataState, Any>>()

    init {
        Timber.d("init")
        remoteLogin.value = false
        loginState.value = Pair(MyLiveDataState.INIT, Unit)
        loginState.addSource(authentication, {
            Timber.d("authenticationLivedata changed: $it")
            it?.let { (state, data) ->
                Timber.d("authenticationLivedata remoteLogin.value: ${remoteLogin.value}")
                if (state == MyLiveDataState.DONE && remoteLogin.value == true) {
                    // initialize Nearby FirebaseCredentials transfer
                    nearByState.value = Pair(MyLiveDataState.CONNECTING, data)
                } else {
                    loginState.value = it
                }
            }
        })
        loginState.addSource(nearByState, {
            Timber.d("nearByState changed: $it")
            loginState.value = it
        })

    }

    fun login(data: FirebaseCredentials) {
        Timber.d("login")
        authentication.value = Pair(MyLiveDataState.CONNECTING, data)
    }

    override fun onCleared() {
        nearByState.onCleared()
        super.onCleared()
    }
}
