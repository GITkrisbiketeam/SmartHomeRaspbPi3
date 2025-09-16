package com.krisbiketeam.smarthomeraspbpi3.common.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

class BluetoothEnablerManager(
    private val context: Context,
    private val bluetoothManager: BluetoothManager
) {

    @SuppressLint("MissingPermission")
    suspend fun enableBluetooth(): Boolean {
        return if (bluetoothManager.adapter.isEnabled) {
            Timber.i("enableBluetooth already enabled")
            true
        } else {
            suspendCancellableCoroutine { continuation ->
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent?) {
                        if (intent != null) {
                            when (intent.action) {
                                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                                    val state = intent.getIntExtra(
                                        BluetoothAdapter.EXTRA_STATE,
                                        BluetoothAdapter.STATE_OFF
                                    )
                                    Timber.i("onReceive ${intent.action} state:$state")
                                    if (state == BluetoothAdapter.STATE_ON) {
                                        continuation.resume(true)
                                        context.unregisterReceiver(this)
                                    }
                                }
                            }
                        }
                    }
                }

                Timber.i("enableBluetooth register receiver")
                context.registerReceiver(
                    receiver,
                    IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                )
                if (!bluetoothManager.adapter.enable()) {
                    continuation.resume(false)
                    context.unregisterReceiver(receiver)
                }
                continuation.invokeOnCancellation {
                    Timber.i("enableBluetooth canceled")
                    context.unregisterReceiver(receiver)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disableBluetooth(): Boolean {
        return if (!bluetoothManager.adapter.isEnabled) {
            Timber.i("disableBluetooth already disabled")
            true
        } else {
            bluetoothManager.adapter.disable()
        }
    }
}