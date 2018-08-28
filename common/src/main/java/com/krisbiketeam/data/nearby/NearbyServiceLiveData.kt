package com.krisbiketeam.data.nearby

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import timber.log.Timber

enum class NearbySettingsState {
    INIT,
    CONNECTING,
    ERROR,
    DONE
}

class NearbyServiceLiveData(private val nearbyService: NearbyService) : LiveData<Pair<NearbySettingsState, Any>>() {
    private var state: NearbySettingsState = NearbySettingsState.INIT
    private var data: Any = Unit

    init {
        Timber.d("init")
    }

    private val dataSendResultListener = object : NearbyService.DataSendResultListener {
        override fun onSuccess() {
            value = Pair(NearbySettingsState.DONE, Unit)
        }

        override fun onFailure(exception: Exception) {
            value = Pair(NearbySettingsState.ERROR, exception)
        }
    }

    override fun observe(owner: LifecycleOwner, observer: Observer<Pair<NearbySettingsState, Any>>) {
        nearbyService.dataSendResultListener(dataSendResultListener)
        super.observe(owner, observer)
    }

    override fun observeForever(observer: Observer<Pair<NearbySettingsState, Any>>) {
        nearbyService.dataSendResultListener(dataSendResultListener)
        super.observeForever(observer)
    }

    override fun getValue(): Pair<NearbySettingsState, Any>? {
        return Pair(state, data)
    }

    public override fun setValue(pair: Pair<NearbySettingsState, Any>?) {
        Timber.d("setValue pair: $pair")
        pair?.let {
            val (newState, newData) = it
            state = newState
            data = newData
            Timber.d("setValue state: $state")
            when (newState) {
                NearbySettingsState.CONNECTING -> nearbyService.sendData(newData)
            }
        }
        super.setValue(pair)
    }

    override fun onActive() {
        nearbyService.resume()
    }

    override fun onInactive() {
        nearbyService.pause()
    }

    fun onCleared() {
        nearbyService.stop()
    }
}