package com.krisbiketeam.smarthomeraspbpi3.common.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.FirebaseState
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.FirebaseStateNotification
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.HomeNameNotification
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.HomeState
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.HomeStateNotification
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.NotificationData
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadCharacteristicData
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.ReadCharacteristicRequest
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.WriteCharacteristicData
import com.krisbiketeam.smarthomeraspbpi3.common.ble.data.mapToReadHomeNameData
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber


class BleClient(private val context: Context) {

    private var bluetoothGatt: BluetoothGatt? = null
    private var notifyData: ((NotificationData) -> Unit)? = null

    private val connectedChannel = Channel<Boolean>()
    private val servicesDiscoveredChannel = Channel<Boolean>()
    private val mtuChangedChannel = Channel<Boolean>()

    private val characteristicReadChannel = Channel<ReadCharacteristicData>()
    private val characteristicWrittenChannel = Channel<BluetoothGattCharacteristic>()
    private val descriptorWrittenChannel = Channel<BluetoothGattDescriptor>()


    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Timber.d("onConnectionStateChange $gatt status:$status newState:$newState")
            if (status != BluetoothGatt.GATT_SUCCESS || newState == BluetoothProfile.STATE_DISCONNECTED) {
                disconnectGattServer()
                connectedChannel.trySend(false)
            } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Timber.d("onServicesDiscovered $gatt status:$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val emitted = servicesDiscoveredChannel.trySend(true)
                Timber.d("onServicesDiscovered emitted: $emitted")
            } else {
                disconnectGattServer()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServiceChanged(gatt: BluetoothGatt) {
            Timber.d("onServiceChanged $gatt")
            gatt.discoverServices()
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val sent = if (status == BluetoothGatt.GATT_SUCCESS) {
                mtuChangedChannel.trySend(true).isSuccess
            } else {
                false
            }
            Timber.d("onMtuChanged $gatt status:$status mtu:$mtu sent?$sent")
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            val sent = if (status == BluetoothGatt.GATT_SUCCESS) {
                descriptorWrittenChannel.trySend(descriptor).isSuccess
            } else {
                connectedChannel.trySend(false)
                false
            }
            Timber.d("onDescriptorWrite $gatt status:$status characteristic:${descriptor.uuid} sent?$sent")
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Timber.d("onCharacteristicRead $gatt status:$status characteristic:${characteristic.uuid} value:${value.toHexString()}")
                characteristicRead(characteristic, value)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Timber.d("onCharacteristicRead $gatt status:$status characteristic:${characteristic.uuid} value:${characteristic.value.toHexString()}")
                characteristicRead(characteristic, characteristic.value)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val sent = if (status == BluetoothGatt.GATT_SUCCESS) {
                characteristicWrittenChannel.trySend(characteristic).isSuccess
            } else {
                false
            }
            Timber.d("onCharacteristicWrite $gatt status:$status characteristic:${characteristic.uuid} sent?$sent")
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Timber.d("onCharacteristicChanged $gatt characteristic:${characteristic.uuid} value:${characteristic.value.toHexString()}")
                characteristicChanged(characteristic, characteristic.value)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Timber.d("onCharacteristicChanged $gatt characteristic:${characteristic.uuid} value:${value.toHexString()}")
                characteristicChanged(characteristic, value)
            }
        }
    }

    fun isConnectedFlow(): Flow<Boolean> = connectedChannel.receiveAsFlow()

    @SuppressLint("MissingPermission")
    suspend fun connect(bleDevice: BluetoothDevice, notifyData: (NotificationData) -> Unit) {
        this.notifyData = notifyData
        bluetoothGatt = bleDevice.connectGatt(context, false, gattCallback).also { gatt ->
            val servicesDiscovered = servicesDiscoveredChannel.receive()
            if (!servicesDiscovered) {
                Timber.e("Could not connet to BT device:$bleDevice")
                connectedChannel.trySend(false)
                return
            }
            val requested = gatt.requestMtu(512)
            Timber.d("connect requestMtu requested?$requested")
            val mtuChanged = mtuChangedChannel.receive()
            Timber.d("connect mtuChanged? $mtuChanged")
            enableNotifications(gatt)
            Timber.d("connect notifications enabled")
            connectedChannel.trySend(true)
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectGattServer() {
        Timber.d("disconnectGattServer")
        notifyData = null
        bluetoothGatt?.disconnect() ?: connectedChannel.trySend(false)
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    @SuppressLint("MissingPermission")
    suspend fun writeCharacteristic(data: WriteCharacteristicData) {
        Timber.d("writeCharacteristic data:$data")
        bluetoothGatt?.let { gatt ->
            gatt.getService(data.serviceUuid)?.getCharacteristic(data.characteristicUuid)?.apply {
                writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                // optionally set back data need to be set in BleService onCharacteristicWriteRequest
                //val homeNotificationSuccess = gatt.setCharacteristicNotification(this, true)
                //Timber.d("writeCharacteristic $uuid homeNotificationSuccess:$homeNotificationSuccess")
                value = data.data
                val writeHomeSuccess: Boolean = gatt.writeCharacteristic(this)
                Timber.d("writeCharacteristic $uuid writeHomeSuccess:$writeHomeSuccess")

                val writtenCharacteristic = characteristicWrittenChannel.receive()
                if (writtenCharacteristic.uuid != this.uuid) {
                    Timber.w("different characteristic written ${writtenCharacteristic.uuid} than requested ${this.uuid}")
                } else {
                    Timber.d("characteristic written ${writtenCharacteristic.uuid}")
                }
            } ?: Timber.e("WRITE CHARACTERISTIC ${data.characteristicUuid} not found")
        } ?: Timber.e("WRITE CHARACTERISTIC gatt not connected")
    }

    @SuppressLint("MissingPermission")
    suspend fun readCharacteristic(data: ReadCharacteristicRequest): ReadCharacteristicData? {
        Timber.d("readCharacteristic data:$data")
        return bluetoothGatt?.let { gatt ->
            gatt.getService(data.serviceUuid)?.getCharacteristic(data.characteristicUuid)?.run {
                val readHomeSuccess: Boolean = gatt.readCharacteristic(this)
                Timber.d("readCharacteristic $uuid readHomeSuccess:$readHomeSuccess")

                val readData = characteristicReadChannel.receive()
                if (readData.characteristicUuid != this.uuid) {
                    Timber.w("different characteristic read ${readData.characteristicUuid} than requested ${this.uuid}")
                    null
                } else {
                    Timber.d("characteristic read ${readData.characteristicUuid} readData:$readData")
                    readData
                }
            } ?: run {
                Timber.e("READ CHARACTERISTIC ${data.characteristicUuid} not found")
                null
            }
        } ?: run {
            Timber.e("READ CHARACTERISTIC gatt not connected")
            null
        }
    }


    @SuppressLint("MissingPermission")
    private suspend fun enableNotifications(gatt: BluetoothGatt) {
        Timber.d("enable Home notification")
        gatt.getService(SERVICE_HOME_UUID)
            ?.getCharacteristic(CHARACTERISTIC_HOME_STATE_UUID)
            ?.let {
                if (gatt.setCharacteristicNotification(it, true)) {
                    enableCharacteristicConfigurationDescriptor(gatt, it)
                }
            } ?: Timber.d("enable Firebase notification not found")
        gatt.getService(SERVICE_FIREBASE_UUID)
            ?.getCharacteristic(CHARACTERISTIC_FIREBASE_STATE_UUID)
            ?.let {
                if (gatt.setCharacteristicNotification(it, true)) {
                    enableCharacteristicConfigurationDescriptor(gatt, it)
                }
            } ?: Timber.d("enable Firebase notification not found")
        gatt.getService(SERVICE_NETWORK_UUID)
            ?.getCharacteristic(CHARACTERISTIC_NETWORK_STATE_UUID)
            ?.let {
                if (gatt.setCharacteristicNotification(it, true)) {
                    enableCharacteristicConfigurationDescriptor(gatt, it)
                }
            } ?: Timber.d("enable Network notification not found")
    }

    @SuppressLint("MissingPermission")
    private suspend fun enableCharacteristicConfigurationDescriptor(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        characteristic.getDescriptor(CLIENT_CONFIGURATION_DESCRIPTOR_UUID)?.let { descriptor ->
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            val result = gatt.writeDescriptor(descriptor)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            }
            Timber.i("enableCharacteristicConfigurationDescriptor result:$result")

            val writtenDescriptor = descriptorWrittenChannel.receive()
            if (writtenDescriptor.uuid != descriptor.uuid) {
                Timber.w("different descriptor written ${writtenDescriptor.uuid} than requested ${descriptor.uuid}")
            } else {
                Timber.d("Description Configuration finished")
            }
        } ?: Timber.e("Description Configuration could not be found ")
    }

    private fun characteristicChanged(
        characteristic: BluetoothGattCharacteristic,
        byteArray: ByteArray
    ) {
        when (characteristic.uuid) {
            CHARACTERISTIC_HOME_NAME_UUID -> {
                val value = kotlin.runCatching { String(byteArray) }.getOrNull()
                Timber.i("received CHARACTERISTIC_HOME_NAME_UUID value: $value")
                notifyData?.invoke(HomeNameNotification(value))
            }

            CHARACTERISTIC_HOME_STATE_UUID -> {
                val value = byteArray.takeIf { byteArray.size == 1 }?.get(0)?.toInt() ?: -1
                Timber.i("received CHARACTERISTIC_HOME_STATE_UUID value: $value")
                notifyData?.invoke(HomeStateNotification(HomeState.getState(value)))
            }

            CHARACTERISTIC_FIREBASE_STATE_UUID -> {
                val value = byteArray.takeIf { byteArray.size == 1 }?.get(0)?.toInt() ?: -1
                Timber.i("received CHARACTERISTIC_FIREBASE_STATE_UUID value: $value")
                notifyData?.invoke(FirebaseStateNotification(FirebaseState.getState(value)))
            }
        }
    }

    private fun characteristicRead(
        characteristic: BluetoothGattCharacteristic,
        byteArray: ByteArray?
    ) {
        val resultData = characteristic.uuid.mapToReadHomeNameData(byteArray)
        if (resultData != null) {
            characteristicReadChannel.trySend(resultData)
        } else {
            Timber.e("characteristicRead no matching Characteristic found")
        }
    }

    private fun ByteArray?.toHexString(): String? {
        return this?.takeIf { it.isNotEmpty() }
            ?.joinToString(separator = " ") { String.format("%02X", it) }
    }
}