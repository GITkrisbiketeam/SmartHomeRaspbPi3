package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.krisbiketeam.data.MyLiveDataState
import com.krisbiketeam.data.auth.AuthenticationLiveData
import com.krisbiketeam.data.auth.FirebaseCredentials
import com.krisbiketeam.data.nearby.NearbyServiceLiveData
import com.krisbiketeam.data.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.firebase.getFirebaseAppToken
import com.krisbiketeam.smarthomeraspbpi3.firebase.sendRegistrationToServer
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
        loginState.addSource(authentication) { pair ->
            Timber.d("authenticationLivedata changed: $pair")
            pair?.let { (state, data) ->
                Timber.d("authenticationLivedata remoteLogin.value: ${remoteLogin.value}")
                var updateValue = true
                if (state == MyLiveDataState.DONE && data is FirebaseCredentials) {
                    FirebaseHomeInformationRepository.writeNewUser(data.email.substringBefore("@"), data.email)
                    getFirebaseAppToken { token ->
                        Timber.d("getFirebaseAppToken token: $token")
                        token?.let {
                            sendRegistrationToServer(data.email, token)
                            if (remoteLogin.value == true) {
                                updateValue = false
                                // initialize Nearby FirebaseCredentials transfer
                                nearByState.value = Pair(MyLiveDataState.CONNECTING, data)
                            }
                        }
                    }
                }
                if (updateValue) loginState.value = pair
            }
        }
        loginState.addSource(nearByState) {
            Timber.d("nearByState changed: $it")
            loginState.value = it
        }

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
