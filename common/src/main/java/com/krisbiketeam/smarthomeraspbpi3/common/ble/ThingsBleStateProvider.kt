package com.krisbiketeam.smarthomeraspbpi3.common.ble

import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadCharacteristicData
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadCharacteristicRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadFirebaseLoginData
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadFirebaseLoginRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadFirebaseStateData
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadFirebaseStateRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadHomeNameData
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadHomeNameRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadHomeStateData
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadHomeStateRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadNetworkIpData
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadNetworkIpRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadNetworkStateData
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadNetworkStateRequest
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage

class ThingsBleStateProvider(private val secureStorage: SecureStorage) {

    fun readData(request: ReadCharacteristicRequest): ReadCharacteristicData {
        return when (request) {
            is ReadFirebaseLoginRequest -> ReadFirebaseLoginData(secureStorage.firebaseCredentials.email)
            is ReadFirebaseStateRequest -> ReadFirebaseStateData(secureStorage.firebaseState)
            is ReadHomeNameRequest -> ReadHomeNameData(secureStorage.homeName)
            is ReadHomeStateRequest -> ReadHomeStateData(secureStorage.homeState)
            is ReadNetworkIpRequest -> ReadNetworkIpData(secureStorage.networkIpAddress)
            is ReadNetworkStateRequest -> ReadNetworkStateData(secureStorage.networkState)
        }
    }
}