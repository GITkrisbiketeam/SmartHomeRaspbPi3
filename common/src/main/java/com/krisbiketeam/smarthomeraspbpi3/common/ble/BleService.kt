package com.krisbiketeam.smarthomeraspbpi3.common.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.NotificationData
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadFirebaseLoginRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadFirebaseStateRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadHomeNameRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadHomeStateRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadNetworkIpRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadNetworkStateRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.WriteCharacteristicData
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.WriteFirebaseLoginData
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.WriteFirebasePasswordData
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.WriteHomeNameData
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber
import java.util.Arrays
import java.util.UUID

class BleService(private val context: Context,
                 private val bluetoothManager: BluetoothManager,
                 private val thingsBleStateProvider: ThingsBleStateProvider) {

    private val bluetoothLeAdvertiser = bluetoothManager.adapter.bluetoothLeAdvertiser

    private var bluetoothGattServer: BluetoothGattServer? = null

    private val serviceAdded = Channel<BluetoothGattService>()

    private val writeData = Channel<WriteCharacteristicData>()

    private val connected: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val clientConfigurations: MutableMap<String, ByteArray> = mutableMapOf()
    private val devices: MutableMap<String, BluetoothDevice> = mutableMapOf()

    private val advertiseCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Timber.i("advertiseCallback onStartSuccess settingsInEffect:$settingsInEffect")
        }

        @SuppressLint("MissingPermission")
        override fun onStartFailure(errorCode: Int) {
            Timber.e("advertiseCallback onStartFailure errorCode:$errorCode")
            stopAdvertising()
            stopServer()
        }
    }

    private val gattServerCallback: BluetoothGattServerCallback =
        object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(
                device: BluetoothDevice,
                status: Int,
                newState: Int
            ) {
                Timber.d("onConnectionStateChange $device status:$status newState:$newState")
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    devices[device.address] = device
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    devices.remove(device.address)
                    if (devices.isEmpty()) {
                        connected.tryEmit(false)
                        startAdvertising()
                    }
                }
            }

            override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
                super.onMtuChanged(device, mtu)
                Timber.d("onMtuChanged $device mtu:$mtu")
                if (devices.isNotEmpty()) {
                    connected.tryEmit(true)
                    stopAdvertising()
                }
            }

            override fun onServiceAdded(status: Int, service: BluetoothGattService) {
                val sent = if (status == BluetoothGatt.GATT_SUCCESS) {
                    serviceAdded.trySend(service).isSuccess
                } else {
                    false
                }
                Timber.d("onServiceAdded status:$status service:${service.uuid} sent?$sent")
            }

            @SuppressLint("MissingPermission")
            override fun onCharacteristicReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic
            ) {
                Timber.d("onCharacteristicReadRequest $device requestId:$requestId offset:$offset characteristic:${characteristic.uuid}")

                val data: ByteArray? = when (characteristic.uuid) {
                    CHARACTERISTIC_HOME_NAME_UUID -> {
                        Timber.d("received CHARACTERISTIC_HOME_NAME_UUID")
                        thingsBleStateProvider.readData(ReadHomeNameRequest()).data
                    }

                    CHARACTERISTIC_HOME_STATE_UUID -> {
                        Timber.d("received CHARACTERISTIC_HOME_STATE_UUID")
                        thingsBleStateProvider.readData(ReadHomeStateRequest()).data
                    }

                    CHARACTERISTIC_FIREBASE_LOGIN_UUID -> {
                        Timber.d("received CHARACTERISTIC_FIREBASE_LOGIN_UUID")
                        thingsBleStateProvider.readData(ReadFirebaseLoginRequest()).data
                    }

                    CHARACTERISTIC_FIREBASE_STATE_UUID -> {
                        Timber.d("received CHARACTERISTIC_FIREBASE_STATE_UUID")
                        thingsBleStateProvider.readData(ReadFirebaseStateRequest()).data
                    }

                    CHARACTERISTIC_NETWORK_STATE_UUID -> {
                        Timber.d("received CHARACTERISTIC_NETWORK_STATE_UUID")
                        thingsBleStateProvider.readData(ReadNetworkStateRequest()).data
                    }

                    CHARACTERISTIC_NETWORK_IP_UUID -> {
                        Timber.d("received CHARACTERISTIC_NETWORK_IP_UUID")
                        thingsBleStateProvider.readData(ReadNetworkIpRequest()).data
                    }

                    else -> null
                }

                val responseSent = bluetoothGattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    data
                )
                Timber.d("received ReadRequest ${characteristic.uuid} responseSent: $responseSent")
            }

            @SuppressLint("MissingPermission")
            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray
            ) {
                super.onCharacteristicWriteRequest(
                    device,
                    requestId,
                    characteristic,
                    preparedWrite,
                    responseNeeded,
                    offset,
                    value
                )
                Timber.d(
                    "onCharacteristicWriteRequest $device requestId:$requestId characteristic:${characteristic.uuid} preparedWrite:$preparedWrite responseNeeded:$responseNeeded offset:$offset value:${
                        String(
                            value
                        )
                    }"
                )
                when (characteristic.uuid) {
                    CHARACTERISTIC_HOME_NAME_UUID -> {
                        val home = String(value)
                        Timber.d("received CHARACTERISTIC_HOME_NAME_UUID home: $home")
                        // optionally set back data, need to be set in BleClient writeCharacteristic
                        /*val reversed: ByteArray = value.reversed().toByteArray()
                        characteristic.value = reversed
                        bluetoothGattServer?.notifyCharacteristicChanged(device, characteristic, false)*/
                        writeData.trySend(WriteHomeNameData(home))
                    }

                    CHARACTERISTIC_FIREBASE_LOGIN_UUID -> {
                        val login = String(value)
                        Timber.d("received CHARACTERISTIC_FIREBASE_LOGIN_UUID login: $login")
                        writeData.trySend(WriteFirebaseLoginData(login))
                    }

                    (CHARACTERISTIC_FIREBASE_PASS_UUID) -> {
                        val pass = String(value)
                        Timber.d("received CHARACTERISTIC_FIREBASE_PASS_UUID pass: $pass")
                        writeData.trySend(WriteFirebasePasswordData(pass))
                    }
                }
                if (responseNeeded) {
                    val responseSent = bluetoothGattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        null
                    )
                    Timber.d("received ${characteristic.uuid} responseSent: $responseSent")
                }
            }

            @SuppressLint("MissingPermission")
            override fun onExecuteWrite(
                device: BluetoothDevice?,
                requestId: Int,
                execute: Boolean
            ) {
                super.onExecuteWrite(device, requestId, execute)
                Timber.d("onExecuteWrite $device requestId:$requestId execute:$execute ")

            }

            @SuppressLint("MissingPermission")
            override fun onDescriptorWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                descriptor: BluetoothGattDescriptor,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray
            ) {
                super.onDescriptorWriteRequest(
                    device,
                    requestId,
                    descriptor,
                    preparedWrite,
                    responseNeeded,
                    offset,
                    value
                )
                Timber.d(
                    "onDescriptorWriteRequest $device requestId:$requestId descriptor:${descriptor.uuid} preparedWrite:$preparedWrite responseNeeded:$responseNeeded offset:$offset value:${
                        String(value)
                    }"
                )
                if (CLIENT_CONFIGURATION_DESCRIPTOR_UUID == descriptor.uuid) {
                    clientConfigurations[device.address] = value
                    bluetoothGattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        null
                    )
                } else {
                    Timber.d("onDescriptorWriteRequest wrong descriptor")
                }
            }
        }

    fun isConnected(): Flow<Boolean> = connected
    fun writeDataRequestReceived(): Flow<WriteCharacteristicData> = writeData.receiveAsFlow()

    suspend fun start() {
        Timber.i("start")
        if (bluetoothGattServer == null) {
            startServer()
            startAdvertising()
        } else {
            Timber.i("already started")
        }
    }

    fun stop() {
        stopServer()
        stopAdvertising()
        Timber.i("stop")
    }

    fun sendNotification(data: NotificationData) {
        Timber.d("sendNotification $data")
        notifyCharacteristic(data.data, data.serviceUuid, data.characteristicUuid)
    }

    @SuppressLint("MissingPermission")
    private suspend fun startServer() {
        bluetoothGattServer = bluetoothManager.openGattServer(context, gattServerCallback).apply {
            Timber.i("setupServer")

            val serviceHome = BluetoothGattService(
                SERVICE_HOME_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )
            val characteristicHomeName = BluetoothGattCharacteristic(
                CHARACTERISTIC_HOME_NAME_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ
            )
            val homeClientConfigurationDescriptor = BluetoothGattDescriptor(
                CLIENT_CONFIGURATION_DESCRIPTOR_UUID,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
            ).apply {
                value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            }
            val characteristicHomeState = BluetoothGattCharacteristic(
                CHARACTERISTIC_HOME_STATE_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            characteristicHomeState.addDescriptor(homeClientConfigurationDescriptor)

            serviceHome.addCharacteristic(characteristicHomeName)
            serviceHome.addCharacteristic(characteristicHomeState)
            addService(serviceHome)

            var addedService = serviceAdded.receive()
            if (addedService.uuid != serviceHome.uuid) {
                Timber.w("different service added ${addedService.uuid}than requested ${serviceHome.uuid}")
            }

            val serviceFirebase = BluetoothGattService(
                SERVICE_FIREBASE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )
            val characteristicFirebaseLogin = BluetoothGattCharacteristic(
                CHARACTERISTIC_FIREBASE_LOGIN_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ
            )
            val characteristicFirebasePassword = BluetoothGattCharacteristic(
                CHARACTERISTIC_FIREBASE_PASS_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )

            val firebaseClientConfigurationDescriptor = BluetoothGattDescriptor(
                CLIENT_CONFIGURATION_DESCRIPTOR_UUID,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
            ).apply {
                value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            }
            val characteristicFirebaseState = BluetoothGattCharacteristic(
                CHARACTERISTIC_FIREBASE_STATE_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            characteristicFirebaseState.addDescriptor(firebaseClientConfigurationDescriptor)

            serviceFirebase.addCharacteristic(characteristicFirebaseLogin)
            serviceFirebase.addCharacteristic(characteristicFirebasePassword)
            serviceFirebase.addCharacteristic(characteristicFirebaseState)
            addService(serviceFirebase)

            addedService = serviceAdded.receive()
            if (addedService.uuid != serviceFirebase.uuid) {
                Timber.w("different service added ${addedService.uuid}than requested ${serviceHome.uuid}")
            }

            val serviceNetwork = BluetoothGattService(
                SERVICE_NETWORK_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )
            val characteristicNetworkIp = BluetoothGattCharacteristic(
                CHARACTERISTIC_NETWORK_IP_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ
            )
            val networkClientConfigurationDescriptor = BluetoothGattDescriptor(
                CLIENT_CONFIGURATION_DESCRIPTOR_UUID,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
            ).apply {
                value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            }
            val characteristicNetworkState = BluetoothGattCharacteristic(
                CHARACTERISTIC_NETWORK_STATE_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            characteristicNetworkState.addDescriptor(networkClientConfigurationDescriptor)

            serviceNetwork.addCharacteristic(characteristicNetworkIp)
            serviceNetwork.addCharacteristic(characteristicNetworkState)
            addService(serviceNetwork)

            addedService = serviceAdded.receive()
            if (addedService.uuid != serviceNetwork.uuid) {
                Timber.w("different service added ${addedService.uuid}than requested ${serviceNetwork.uuid}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopServer() {
        bluetoothGattServer?.run {
            clearServices()
            close()
            Timber.i("stopServer")
        }
        bluetoothGattServer = null
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertising() {
        bluetoothLeAdvertiser?.run {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                .build()

            val parcelUuid = ParcelUuid(SERVICE_UUID)

            val data = AdvertiseData.Builder()
                .addServiceUuid(parcelUuid)
                .build()

            startAdvertising(settings, data, advertiseCallback)
        } ?: Timber.e("startAdvertising bluetoothLeAdvertiser is null")
    }

    @SuppressLint("MissingPermission")
    private fun stopAdvertising() {
        bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
    }

    @SuppressLint("MissingPermission")
    private fun notifyCharacteristic(value: ByteArray, serviceUuid: UUID, charUuid: UUID) {
        bluetoothGattServer?.let { gattServer ->
            gattServer.getService(serviceUuid)?.getCharacteristic(charUuid)?.let {
                it.value = value
                val confirm =
                    it.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE == BluetoothGattCharacteristic.PROPERTY_INDICATE
                Timber.d("notifyCharacteristic ${value.joinToString()} uuid:${it.uuid} confirm?$confirm")
                devices.values.forEach { device ->
                    if (clientEnabledNotifications(device, it)) {
                        val result = gattServer.notifyCharacteristicChanged(device, it, confirm)
                        Timber.d("notifyCharacteristic uuid:${it.uuid} notifyCharacteristicChanged result:$result")
                    } else {
                        Timber.d("notifyCharacteristic uuid:${it.uuid} client notification not enabled")
                    }
                }
            }
        }
    }

    private fun clientEnabledNotifications(
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic
    ): Boolean {
        val descriptorList = characteristic.descriptors
        val descriptor = descriptorList.find { it.uuid == CLIENT_CONFIGURATION_DESCRIPTOR_UUID }
            ?: // There is no client configuration descriptor, treat as true
            return true
        val deviceAddress = device.address
        val clientConfiguration = clientConfigurations[deviceAddress]
            ?: // Descriptor has not been set
            return false
        return Arrays.equals(clientConfiguration, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
    }
}