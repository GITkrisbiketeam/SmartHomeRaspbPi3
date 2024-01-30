package com.krisbiketeam.smarthomeraspbpi3.common.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BleScanner(private val bluetoothManager: BluetoothManager) {

    suspend fun scanLeDevice(): BluetoothDevice = suspendCancellableCoroutine { continuation ->

        val bluetoothLeScanner = bluetoothManager.adapter.bluetoothLeScanner

        @SuppressLint("MissingPermission")
        val leScanCallback: ScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                // Resume coroutine with a value provided by the callback
                if (continuation.isActive) {
                    continuation.resume(result.device)
                }
                bluetoothLeScanner.stopScan(this)
            }

            override fun onScanFailed(errorCode: Int) {
                // Resume coroutine with an exception provided by the callback
                continuation.resumeWithException(Throwable("onScanFailed $errorCode"))
                bluetoothLeScanner.stopScan(this)
            }
        }
        val leScanSettings = ScanSettings.Builder()
            .setReportDelay(0) // Set to 0 to be notified of scan results immediately.
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        val scanFiltersBuilder =
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(SERVICE_UUID))
                .build()

        // Register callback with an API
        bluetoothLeScanner.startScan(listOf(scanFiltersBuilder), leScanSettings, leScanCallback)
        // Remove callback on cancellation
        continuation.invokeOnCancellation { bluetoothLeScanner.stopScan(leScanCallback) }
        // At this point the coroutine is suspended by suspendCancellableCoroutine until callback fires
    }
}