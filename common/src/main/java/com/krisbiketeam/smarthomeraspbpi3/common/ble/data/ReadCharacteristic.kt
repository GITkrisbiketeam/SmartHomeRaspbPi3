package com.krisbiketeam.smarthomeraspbpi3.common.ble.data

import com.krisbiketeam.smarthomeraspbpi3.common.ble.CHARACTERISTIC_FIREBASE_LOGIN_UUID
import com.krisbiketeam.smarthomeraspbpi3.common.ble.CHARACTERISTIC_FIREBASE_STATE_UUID
import com.krisbiketeam.smarthomeraspbpi3.common.ble.CHARACTERISTIC_HOME_NAME_UUID
import com.krisbiketeam.smarthomeraspbpi3.common.ble.CHARACTERISTIC_HOME_STATE_UUID
import com.krisbiketeam.smarthomeraspbpi3.common.ble.CHARACTERISTIC_NETWORK_IP_UUID
import com.krisbiketeam.smarthomeraspbpi3.common.ble.CHARACTERISTIC_NETWORK_STATE_UUID
import com.krisbiketeam.smarthomeraspbpi3.common.ble.SERVICE_FIREBASE_UUID
import com.krisbiketeam.smarthomeraspbpi3.common.ble.SERVICE_HOME_UUID
import com.krisbiketeam.smarthomeraspbpi3.common.ble.SERVICE_NETWORK_UUID
import timber.log.Timber
import java.util.UUID

sealed interface ReadCharacteristic {
    val serviceUuid: UUID
    val characteristicUuid: UUID
}

sealed interface ReadCharacteristicRequest: ReadCharacteristic

sealed interface ReadCharacteristicData : ReadCharacteristic {
    val data: ByteArray
}

open class ReadHomeNameRequest : ReadCharacteristicRequest {
    override val serviceUuid: UUID = SERVICE_HOME_UUID
    override val characteristicUuid: UUID = CHARACTERISTIC_HOME_NAME_UUID
}

data class ReadHomeNameData(val name: String?) : ReadCharacteristicData, ReadHomeNameRequest() {
    override val data: ByteArray = name?.toByteArray() ?: byteArrayOf()
}

open class ReadHomeStateRequest : ReadCharacteristicRequest {
    override val serviceUuid: UUID = SERVICE_HOME_UUID
    override val characteristicUuid: UUID = CHARACTERISTIC_HOME_STATE_UUID
}

data class ReadHomeStateData(val state: HomeState) : ReadCharacteristicData, ReadHomeStateRequest() {
    override val data: ByteArray = byteArrayOf(state.value.toByte())
}

open class ReadFirebaseLoginRequest : ReadCharacteristicRequest {
    override val serviceUuid: UUID = SERVICE_FIREBASE_UUID
    override val characteristicUuid: UUID = CHARACTERISTIC_FIREBASE_LOGIN_UUID
}

data class ReadFirebaseLoginData(val login: String?) : ReadCharacteristicData,
    ReadFirebaseLoginRequest() {
    override val data: ByteArray = login?.toByteArray() ?: byteArrayOf()
}

open class ReadFirebaseStateRequest : ReadCharacteristicRequest {
    override val serviceUuid: UUID = SERVICE_FIREBASE_UUID
    override val characteristicUuid: UUID = CHARACTERISTIC_FIREBASE_STATE_UUID
}

data class ReadFirebaseStateData(val state: FirebaseState) : ReadCharacteristicData,
    ReadFirebaseStateRequest() {
    override val data: ByteArray = byteArrayOf(state.value.toByte())
}

open class ReadNetworkIpRequest : ReadCharacteristicRequest {
    override val serviceUuid: UUID = SERVICE_NETWORK_UUID
    override val characteristicUuid: UUID = CHARACTERISTIC_NETWORK_IP_UUID
}

data class ReadNetworkIpData(val name: String?) : ReadCharacteristicData, ReadNetworkIpRequest() {
    override val data: ByteArray = name?.toByteArray() ?: byteArrayOf()
}

open class ReadNetworkStateRequest : ReadCharacteristicRequest {
    override val serviceUuid: UUID = SERVICE_NETWORK_UUID
    override val characteristicUuid: UUID = CHARACTERISTIC_NETWORK_STATE_UUID
}

data class ReadNetworkStateData(val state: NetworkState) : ReadCharacteristicData,
    ReadNetworkStateRequest() {
    override val data: ByteArray = byteArrayOf(state.value.toByte())
}


fun UUID.mapToReadHomeNameData(byteArray: ByteArray?): ReadCharacteristicData? {
    return when (this) {
        CHARACTERISTIC_HOME_NAME_UUID -> {
            val name = runCatching { byteArray?.run(::String) }.getOrNull()
            Timber.d("received CHARACTERISTIC_HOME_NAME_UUID name: $name")
            ReadHomeNameData(name)
        }

        CHARACTERISTIC_HOME_STATE_UUID -> {
            val state = byteArray?.takeIf { byteArray.size == 1 }?.get(0)?.toInt() ?: -1
            Timber.d("received CHARACTERISTIC_HOME_STATE_UUID value: $state")
            ReadHomeStateData(HomeState.getState(state))
        }

        CHARACTERISTIC_FIREBASE_LOGIN_UUID -> {
            val login = runCatching { byteArray?.run(::String) }.getOrNull()
            Timber.d("received CHARACTERISTIC_FIREBASE_LOGIN_UUID login: $login")
            ReadFirebaseLoginData(login)
        }

        CHARACTERISTIC_FIREBASE_STATE_UUID -> {
            val state = byteArray?.takeIf { byteArray.size == 1 }?.get(0)?.toInt() ?: -1
            Timber.d("received CHARACTERISTIC_FIREBASE_STATE_UUID value: $state")
            ReadFirebaseStateData(FirebaseState.getState(state))
        }

        CHARACTERISTIC_NETWORK_IP_UUID -> {
            val ip = runCatching { byteArray?.run(::String) }.getOrNull()
            Timber.d("received CHARACTERISTIC_NETWORK_IP_UUID ip: $ip")
            ReadNetworkIpData(ip)
        }

        CHARACTERISTIC_NETWORK_STATE_UUID -> {
            val state = byteArray?.takeIf { byteArray.size == 1 }?.get(0)?.toInt() ?: -1
            Timber.d("received CHARACTERISTIC_NETWORK_STATE_UUID value: $state")
            ReadNetworkStateData(NetworkState.getState(state))
        }

        else -> null
    }
}