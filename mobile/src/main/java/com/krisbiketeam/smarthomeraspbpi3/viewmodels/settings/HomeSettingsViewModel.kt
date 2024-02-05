package com.krisbiketeam.smarthomeraspbpi3.viewmodels.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krisbiketeam.smarthomeraspbpi3.common.RemoteConnectionState
import com.krisbiketeam.smarthomeraspbpi3.common.ble.BleClient
import com.krisbiketeam.smarthomeraspbpi3.common.ble.BleScanner
import com.krisbiketeam.smarthomeraspbpi3.common.ble.BluetoothEnablerManager
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.FirebaseStateNotification
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.HomeNameNotification
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.HomeState
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.HomeStateNotification
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.NetworkIpNotification
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.NetworkStateNotification
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadFirebaseLoginRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadFirebaseStateRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadHomeNameRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadHomeStateRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadNetworkIpRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadNetworkStateRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.WriteHomeNameData
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber


/**
 * The ViewModel used in HomeSettingsFragment.
 */
class HomeSettingsViewModel(
    private val secureStorage: SecureStorage,
    private val homeInformationRepository: FirebaseHomeInformationRepository,
    private val bluetoothEnablerManager: BluetoothEnablerManager,
    private val bleScanner: BleScanner,
    private val bleClient: BleClient
) : ViewModel() {
    val homeName: MutableStateFlow<String> = MutableStateFlow(secureStorage.homeName)
    val remoteHomeSetup: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val bleConnectionState: MutableStateFlow<RemoteConnectionState> =
        MutableStateFlow(RemoteConnectionState.INIT)

    val homeNameList: StateFlow<List<String>> =
        homeInformationRepository.getHomesFLow().flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun setupHomeName(homeName: String) {
        Timber.d("setupHomeName")
        if (!homeNameList.value.contains(homeName)) {
            homeInformationRepository.addHomeToList(homeName)
        }
        secureStorage.homeName = homeName
        homeInformationRepository.setHomeReference(homeName)
        if (remoteHomeSetup.value) {
            viewModelScope.launch {
                launch {
                    bleClient.isConnectedFlow().collect { connected ->
                        Timber.e("BleClient connected? $connected")

                    }
                }
                bleConnectionState.value = RemoteConnectionState.CONNECTING
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
                                    is FirebaseStateNotification -> {}
                                    is HomeNameNotification -> {}
                                    is HomeStateNotification -> {
                                        Timber.e("All set (Home) ${it.state}")
                                        if (it.state == HomeState.SET) {
                                            bleConnectionState.value = RemoteConnectionState.DONE
                                        }
                                    }

                                    is NetworkIpNotification -> {}
                                    is NetworkStateNotification -> {}
                                }
                            }

                            Timber.i("BLE device connected")
                            val networkIp =
                                bleClient.readCharacteristic(ReadNetworkIpRequest())
                            Timber.e("BleClient read networkIp: $networkIp")
                            val networkState =
                                bleClient.readCharacteristic(ReadNetworkStateRequest())
                            Timber.e("BleClient read networkState: $networkState")
                            val firebaseLogin =
                                bleClient.readCharacteristic(ReadFirebaseLoginRequest())
                            Timber.e("BleClient read firebaseLogin: $firebaseLogin")
                            val firebaseState =
                                bleClient.readCharacteristic(ReadFirebaseStateRequest())
                            Timber.e("BleClient read firebaseState: $firebaseState")
                            val homeNameRead = bleClient.readCharacteristic(ReadHomeNameRequest())
                            Timber.e("BleClient read homeName: $homeNameRead")
                            val homeState = bleClient.readCharacteristic(ReadHomeStateRequest())
                            Timber.e("BleClient read homeState: $homeState")

                            bleClient.writeCharacteristic(WriteHomeNameData(homeName))

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
        } else {
            bleConnectionState.value = RemoteConnectionState.DONE
        }
    }
}
