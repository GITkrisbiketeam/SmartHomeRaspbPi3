package com.krisbiketeam.smarthomeraspbpi3.viewmodels.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krisbiketeam.smarthomeraspbpi3.common.RemoteConnectionState
import com.krisbiketeam.smarthomeraspbpi3.common.auth.Authentication
import com.krisbiketeam.smarthomeraspbpi3.common.auth.FirebaseCredentials
import com.krisbiketeam.smarthomeraspbpi3.common.ble.BleClient
import com.krisbiketeam.smarthomeraspbpi3.common.ble.BleScanner
import com.krisbiketeam.smarthomeraspbpi3.common.ble.BluetoothEnablerManager
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.FirebaseState
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.FirebaseStateNotification
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.HomeNameNotification
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.HomeStateNotification
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.NetworkIpNotification
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.NetworkStateNotification
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadFirebaseLoginRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadFirebaseStateRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadHomeNameRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadHomeStateRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadNetworkIpRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadNetworkStateRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.WriteFirebaseLoginData
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.WriteFirebasePasswordData
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.firebase.getFirebaseAppToken
import com.krisbiketeam.smarthomeraspbpi3.firebase.sendRegistrationToServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


/**
 * The ViewModel used in LoginSettingsFragment.
 */
class LoginSettingsViewModel(
    private val authentication: Authentication,
    private val secureStorage: SecureStorage,
    private val homeInformationRepository: FirebaseHomeInformationRepository,
    private val bluetoothEnablerManager: BluetoothEnablerManager,
    private val bleScanner: BleScanner,
    private val bleClient: BleClient
) : ViewModel() {
    var email: MutableStateFlow<String> = MutableStateFlow("")
    var password: MutableStateFlow<String> = MutableStateFlow("")
    var remoteLogin: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val bleConnectionState: MutableStateFlow<RemoteConnectionState> =
        MutableStateFlow(RemoteConnectionState.INIT)

    fun login(loginData: FirebaseCredentials) {
        Timber.d("login")
        viewModelScope.launch {
            bleConnectionState.value = RemoteConnectionState.CONNECTING
            runCatching {
                suspendCancellableCoroutine { continuation ->

                    val loginResultListener = object : Authentication.LoginResultListener {
                        override fun success(uid: String?) {
                            continuation.resume(
                                FirebaseCredentials(
                                    loginData.email,
                                    loginData.password,
                                    uid
                                )
                            )

                            if (!remoteLogin.value) {
                                bleConnectionState.value = RemoteConnectionState.DONE
                            }
                            authentication.removeLoginResultListener()
                        }

                        override fun failed(exception: Exception) {
                            bleConnectionState.value = RemoteConnectionState.ERROR

                            continuation.resumeWithException(exception)
                            authentication.removeLoginResultListener()
                        }
                    }

                    authentication.addLoginResultListener(loginResultListener)

                    authentication.login(loginData)

                    continuation.invokeOnCancellation {
                        authentication.removeLoginResultListener()
                    }
                }
            }.getOrNull()?.let { newLoginStateResult: FirebaseCredentials ->
                Timber.d(
                    "authentication data remoteLogin.value: ${remoteLogin.value} newLoginStateResult: $newLoginStateResult"
                )
                secureStorage.firebaseCredentials = newLoginStateResult

                val uid = newLoginStateResult.uid
                if (uid != null) {
                    homeInformationRepository.writeNewUser(
                        uid,
                        newLoginStateResult.email.substringBefore("@"),
                        newLoginStateResult.email
                    )
                    homeInformationRepository.setUserReference(uid)

                    getFirebaseAppToken { token ->
                        Timber.d("getFirebaseAppToken token: $token")
                        if (token != null) {
                            sendRegistrationToServer(homeInformationRepository, uid, token)
                            if (remoteLogin.value) {
                                // initialize Nearby FirebaseCredentials transfer
                                bleConnectionState.value = RemoteConnectionState.CONNECTING

                                sendToRemote(newLoginStateResult)

                            }
                        }
                    }
                }
            }
        }
    }

    private fun sendToRemote(loginData: FirebaseCredentials) {
        viewModelScope.launch {
            launch {
                bleClient.isConnectedFlow().collect { connected ->
                    Timber.e("BleClient connected? $connected")
                }
            }

            if (bluetoothEnablerManager.enableBluetooth()) {
                Timber.d("search for BLE device")
                val bleDevice = withTimeoutOrNull(60000) {
                    bleScanner.scanLeDevice()
                }
                if (bleDevice != null) {
                    Timber.d("found BLE device: $bleDevice")
                    Timber.i("BleClient tryConnect")
                    withTimeoutOrNull(60000) {
                        bleClient.connect(bleDevice) {
                            Timber.e("notifyData $it")
                            when (it) {
                                is FirebaseStateNotification -> {
                                    if (it.state == FirebaseState.LOGGED_IN) {
                                        bleConnectionState.value = RemoteConnectionState.DONE
                                    } else {
                                        Timber.e("Cannot remote login to Firebase")
                                        bleConnectionState.value = RemoteConnectionState.ERROR
                                    }
                                }

                                is HomeNameNotification -> {}
                                is HomeStateNotification -> {}
                                is NetworkIpNotification -> {}
                                is NetworkStateNotification -> {}
                            }
                        }

                        Timber.i("BLE device connected")
                        val networkIp = bleClient.readCharacteristic(ReadNetworkIpRequest())
                        Timber.e("BleClient read networkIp: $networkIp")
                        val networkState = bleClient.readCharacteristic(ReadNetworkStateRequest())
                        Timber.e("BleClient read networkState: $networkState")
                        val firebaseLogin = bleClient.readCharacteristic(ReadFirebaseLoginRequest())
                        Timber.e("BleClient read firebaseLogin: $firebaseLogin")
                        val firebaseState = bleClient.readCharacteristic(ReadFirebaseStateRequest())
                        Timber.e("BleClient read firebaseState: $firebaseState")
                        val homeName = bleClient.readCharacteristic(ReadHomeNameRequest())
                        Timber.e("BleClient read homeName: $homeName")
                        val homeState = bleClient.readCharacteristic(ReadHomeStateRequest())
                        Timber.e("BleClient read homeState: $homeState")

                        bleClient.writeCharacteristic(WriteFirebaseLoginData(loginData.email))
                        bleClient.writeCharacteristic(WriteFirebasePasswordData(loginData.password))
                    }
                } else {
                    Timber.e("BLE device not found")
                    bleConnectionState.value = RemoteConnectionState.ERROR
                }
            } else {
                Timber.e("bluetooth could not be enabled")
                bleConnectionState.value = RemoteConnectionState.ERROR
            }

        }
    }
}

