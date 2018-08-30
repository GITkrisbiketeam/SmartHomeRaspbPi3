package com.krisbiketeam.smarthomeraspbpi3.utils

import android.databinding.BaseObservable
import android.databinding.Bindable

import com.krisbiketeam.smarthomeraspbpi3.BR

class LogConsole : BaseObservable() {

    @get:Bindable
    var consoleMessage: String? = null
        set(consoleMessage) {
            field = consoleMessage
            notifyPropertyChanged(BR.consoleMessage)
        }
}
