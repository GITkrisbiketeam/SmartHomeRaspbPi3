package com.krisbiketeam.data.nearby

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import timber.log.Timber

enum class WifiSettingsState {
    INIT,
    CONNECTING,
    ERROR,
    DONE
}

class NearbyServiceLiveData(private val nearbyService: NearbyService) : LiveData<Pair<WifiSettingsState, Any>>() {
    private var state: WifiSettingsState = WifiSettingsState.INIT

    private val dataSendResultListener = object : NearbyService.DataSendResultListener {
        override fun onSuccess() {
            value = Pair(WifiSettingsState.DONE, "")
        }

        override fun onFailure(exception: Exception) {
            value = Pair(WifiSettingsState.ERROR, exception)
        }
    }

    override fun observe(owner: LifecycleOwner, observer: Observer<Pair<WifiSettingsState, Any>>) {
        nearbyService.dataSendResultListener(dataSendResultListener)
        super.observe(owner, observer)
    }

    override fun observeForever(observer: Observer<Pair<WifiSettingsState, Any>>) {
        nearbyService.dataSendResultListener(dataSendResultListener)
        super.observeForever(observer)
    }

    override fun getValue(): Pair<WifiSettingsState, Any>? {
        return Pair(state, "")
    }

    public override fun setValue(pair: Pair<WifiSettingsState, Any>?) {
        Timber.d("setValue pair: $pair")
        pair?.let {
            val (newState, data) = it
            state = newState
            Timber.d("setValue state: $state")
            when (state) {
                WifiSettingsState.CONNECTING -> nearbyService.sendData(data)
            }
        }
        super.setValue(value)
    }

    override fun onActive() {
        nearbyService.pause()
    }

    override fun onInactive() {
        nearbyService.resume()
    }

    fun onCleared() {
        nearbyService.stop()
    }
}