package com.krisbiketeam.smarthomeraspbpi3.units

import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

interface HwUnitGpio<T> : BaseHwUnit<T> {
    var gpio: Gpio?

    override suspend fun connect():Result<Unit> {
        Timber.e("connect on: $hwUnit")
        return if (gpio == null) {
            withContext(Dispatchers.Main) {
                kotlin.runCatching {
                    try {
                        gpio = PeripheralManager.getInstance()?.openGpio(hwUnit.pinName)
                    } catch (e: Exception) {
                        Timber.e(e, "Error connecting device")
                        close().onFailure {
                            Timber.e(e, "Error closing device")
                            throw (Exception("Error close HwUnitGpio", e))
                        }
                        Unit
                    }
                }
            }
        } else {
            Result.success(Unit)
        }
    }

    override suspend fun close():Result<Unit> {
        Timber.e("close on: $hwUnit")
        return withContext(Dispatchers.Main) {
            kotlin.runCatching {
                try {
                    gpio?.close()
                    gpio = null
                } catch (e: Exception) {
                    gpio = null
                    Timber.e(e, "Error closing PeripheralIO API on: $hwUnit")
                    throw (Exception("Error close HwUnitGpio", e))
                }
            }
        }
    }
}
