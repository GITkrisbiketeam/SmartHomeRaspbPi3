package com.krisbiketeam.smarthomeraspbpi3.utils

import android.app.Activity
import android.util.Log
import androidx.databinding.DataBindingUtil
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.databinding.ActivityHomeBinding
import timber.log.Timber

object ConsoleLoggerTree : Timber.DebugTree() {
    private var logger: LogConsole? = null
    private var binding: ActivityHomeBinding? = null
    private var loggingEnabled: Boolean = false

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, "[${Thread.currentThread().name}] SHRP3_$tag", message, t)
        FirebaseCrashlytics.getInstance().log("[${Thread.currentThread().name}]$tag; $message; $t")
        if (loggingEnabled && priority > Log.VERBOSE && logger != null) {
            var lastConsoleMsg: String? = logger?.consoleMessage
            if (lastConsoleMsg == null) {
                lastConsoleMsg = ""
            }
            lastConsoleMsg.substring(0, 1000)
            logger?.consoleMessage = "$tag: $message\n${lastConsoleMsg.substring(0, 1000)}"
        }
    }

    fun setLogConsole(activity: Activity) {
        logger = LogConsole()
        binding = DataBindingUtil.setContentView(activity,
                                                 R.layout.activity_home)
        binding?.logConsole = logger //This is where we bind the layout with the object*/
    }

    fun setIpAddress(ipAddress: String) {
        logger?.ipAddress = ipAddress
    }

    fun enableLogging(enable: Boolean) {
        loggingEnabled = enable
    }
}
