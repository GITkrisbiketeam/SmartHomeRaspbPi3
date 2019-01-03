package com.krisbiketeam.smarthomeraspbpi3.common.auth

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.krisbiketeam.smarthomeraspbpi3.common.MyLiveDataState
import timber.log.Timber

class AuthenticationLiveData(private val authentication: Authentication) : LiveData<Pair<MyLiveDataState, Any>>() {
    private var state: MyLiveDataState = MyLiveDataState.INIT
    private var data: Any = Unit

    init {
        Timber.d("init")
    }

    private val loginResultListener = object : Authentication.LoginResultListener {
        override fun success() {
            value = Pair(MyLiveDataState.DONE, data)
        }

        override fun failed(exception: Exception) {
            value = Pair(MyLiveDataState.ERROR, exception)
        }
    }

    override fun observe(owner: LifecycleOwner, observer: Observer<in Pair<MyLiveDataState, Any>>) {
        authentication.addLoginResultListener(loginResultListener)
        super.observe(owner, observer)
    }

    override fun observeForever(observer: Observer<in Pair<MyLiveDataState, Any>>) {
        authentication.addLoginResultListener(loginResultListener)
        super.observeForever(observer)
    }

    override fun getValue(): Pair<MyLiveDataState, Any> {
        return Pair(state, data)
    }

    override fun postValue(pair: Pair<MyLiveDataState, Any>?) {
        Timber.d("postValue")
        setValueInternal(pair)

        super.postValue(pair)
    }

    public override fun setValue(pair: Pair<MyLiveDataState, Any>?) {
        Timber.d("setValue")
        setValueInternal(pair)
        super.setValue(pair)
    }

    private fun setValueInternal(pair: Pair<MyLiveDataState, Any>?) {
        Timber.d("setValueInternal pair: $pair")
        pair?.let {
            val (newState, newData) = it
            state = newState
            data = newData
            if (newData is FirebaseCredentials) {
                when (newState) {
                    MyLiveDataState.CONNECTING -> authentication.login(newData)
                    else -> Unit // Do noting
                }
            }
        }
    }
}