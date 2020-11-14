package com.krisbiketeam.smarthomeraspbpi3.viewmodels.settings

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.krisbiketeam.smarthomeraspbpi3.common.MyLiveDataState
import com.krisbiketeam.smarthomeraspbpi3.common.auth.AuthenticationLiveData
import com.krisbiketeam.smarthomeraspbpi3.common.auth.FirebaseCredentials
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyServiceLiveData
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.firebase.getFirebaseAppToken
import com.krisbiketeam.smarthomeraspbpi3.firebase.sendRegistrationToServer
import com.krisbiketeam.smarthomeraspbpi3.ui.settings.WifiSettingsFragment
import timber.log.Timber


/**
 * The ViewModel used in [WifiSettingsFragment].
 */
class LoginSettingsViewModel(private val authentication: AuthenticationLiveData,
                             private val nearByState: NearbyServiceLiveData,
                             private val homeInformationRepository: FirebaseHomeInformationRepository) : ViewModel() {
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
                Timber.d(
                        "authenticationLivedata remoteLogin.value: ${remoteLogin.value} data: $data state: $state")
                var updateValue = true
                if (state == MyLiveDataState.DONE && data is FirebaseCredentials) {
                    homeInformationRepository.writeNewUser(data.email.substringBefore("@"),
                                                                   data.email)
                    homeInformationRepository.setUserReference(data.email)
                    updateValue = false
                    getFirebaseAppToken { token ->
                        Timber.d("getFirebaseAppToken token: $token")
                        if (token?.let {
                                    sendRegistrationToServer(homeInformationRepository, data.email, token)
                                    if (remoteLogin.value == true) {
                                        // initialize Nearby FirebaseCredentials transfer
                                        nearByState.value = Pair(MyLiveDataState.CONNECTING, data)
                                        false
                                    } else true
                                } != false) {
                            loginState.value = pair
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
