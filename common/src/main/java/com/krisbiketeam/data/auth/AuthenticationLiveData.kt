package com.krisbiketeam.data.auth

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import timber.log.Timber

enum class AuthenticationState {
    INIT,
    CONNECTING,
    ERROR,
    DONE
}

class AuthenticationLiveData(private val authentication: Authentication) : LiveData<Pair<AuthenticationState, FirebaseCredentials?>>() {
    private var state: AuthenticationState = AuthenticationState.INIT
    private var data: FirebaseCredentials? = null

    init {
        Timber.d("init")
    }

    private val loginResultListener = object : Authentication.LoginResultListener {
        override fun success() {
            value = Pair(AuthenticationState.DONE, data)
        }

        override fun failed(exception: Exception) {
            Timber.e(exception, "Request failed")
            value = Pair(AuthenticationState.ERROR, null)
        }
    }

    override fun observe(owner: LifecycleOwner, observer: Observer<Pair<AuthenticationState, FirebaseCredentials?>>) {
        authentication.addLoginResultListener(loginResultListener)
        super.observe(owner, observer)
    }

    override fun observeForever(observer: Observer<Pair<AuthenticationState, FirebaseCredentials?>>) {
        authentication.addLoginResultListener(loginResultListener)
        super.observeForever(observer)
    }

    override fun getValue(): Pair<AuthenticationState, FirebaseCredentials?> {
        return Pair(state, data)
    }

    override fun postValue(pair: Pair<AuthenticationState, FirebaseCredentials?>?) {
        Timber.d("postValue")
        setValueInternal(pair)

        super.postValue(pair)
    }

    public override fun setValue(pair: Pair<AuthenticationState, FirebaseCredentials?>?) {
        Timber.d("setValue")
        setValueInternal(pair)
        super.setValue(pair)
    }

    private fun setValueInternal(pair: Pair<AuthenticationState, FirebaseCredentials?>?) {
        Timber.d("setValueInternal pair: $pair")
        pair?.let {
            val (newState, newData) = it
            state = newState
            data = newData
            Timber.d("setValueInternal state: $state, data: $data")
            newData?.let {
                when (newState) {
                    AuthenticationState.CONNECTING -> authentication.login(newData)
                }
            }
        }
    }
}