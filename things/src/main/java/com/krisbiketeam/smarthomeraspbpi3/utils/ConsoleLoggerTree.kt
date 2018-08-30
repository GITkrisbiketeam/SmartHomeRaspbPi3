package com.krisbiketeam.smarthomeraspbpi3.utils

import android.app.Activity
import android.databinding.DataBindingUtil
import android.util.Log

import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.databinding.ActivityHomeBinding
import timber.log.Timber

object ConsoleLoggerTree : Timber.DebugTree() {
    private var logger: LogConsole? = null

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, message, t)
        if (priority > Log.VERBOSE && logger != null) {
            var lastConsoleMsg: String? = logger!!.consoleMessage
            if (lastConsoleMsg == null) {
                lastConsoleMsg = ""
            }
            logger?.consoleMessage = "$tag: $message\n$lastConsoleMsg"
        }
    }

    fun setLogConsole(activity: Activity) {
        logger = LogConsole()
        val binding = DataBindingUtil.setContentView<ActivityHomeBinding>(activity, R.layout
                .activity_home)
        binding.logConsole = logger //This is where we bind the layout with the object*/
    }
}
