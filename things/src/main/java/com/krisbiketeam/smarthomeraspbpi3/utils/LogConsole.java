package com.krisbiketeam.smarthomeraspbpi3.utils;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

import com.krisbiketeam.smarthomeraspbpi3.BR;

public class LogConsole extends BaseObservable {

    private String consoleMessage;
    
    @Bindable
    public String getConsoleMessage() {
        return consoleMessage;
    }

    public void setConsoleMessage(String consoleMessage) {
        this.consoleMessage = consoleMessage;
        notifyPropertyChanged(BR.consoleMessage);
    }
}
