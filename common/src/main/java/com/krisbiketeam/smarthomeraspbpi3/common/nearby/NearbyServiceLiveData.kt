package com.krisbiketeam.smarthomeraspbpi3.common.nearby

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.krisbiketeam.smarthomeraspbpi3.common.MyLiveDataState
import timber.log.Timber

class NearbyServiceLiveData(private val nearbyService: NearbyService) : LiveData<Pair<MyLiveDataState, Any>>() {
    private var state: MyLiveDataState = MyLiveDataState.INIT
    private var data: Any = Unit

    init {
        Timber.d("init")
    }

    private val dataSendResultListener = object : NearbyService.DataSendResultListener {
        override fun onSuccess() {
            value = Pair(MyLiveDataState.DONE, Unit)
        }

        override fun onFailure(exception: Exception) {
            value = Pair(MyLiveDataState.ERROR, exception)
        }
    }

    override fun observe(owner: LifecycleOwner, observer: Observer<in Pair<MyLiveDataState, Any>>) {
        nearbyService.dataSendResultListener(dataSendResultListener)
        super.observe(owner, observer)
    }

    override fun observeForever(observer: Observer<in Pair<MyLiveDataState, Any>>) {
        nearbyService.dataSendResultListener(dataSendResultListener)
        super.observeForever(observer)
    }

    override fun getValue(): Pair<MyLiveDataState, Any> {
        return Pair(state, data)
    }

    public override fun setValue(pair: Pair<MyLiveDataState, Any>?) {
        Timber.d("setValue pair: $pair")
        pair?.let {
            val (newState, newData) = it
            state = newState
            data = newData
            Timber.d("setValue state: $state")
            when (newState) {
                MyLiveDataState.CONNECTING -> nearbyService.sendData(newData)
                else -> Unit // Do noting
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