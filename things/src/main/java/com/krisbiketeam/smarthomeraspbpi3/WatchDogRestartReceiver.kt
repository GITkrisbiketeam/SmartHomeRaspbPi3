package com.krisbiketeam.smarthomeraspbpi3

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.things.device.DeviceManager
import com.jakewharton.processphoenix.ProcessPhoenix
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.RemoteLog
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

const val WATCH_DOG_RESTART_ACTION = "watch_dog_restart_action"

class WatchDogRestartReceiver  : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.i("onReceive ${intent.action}")

        // Is triggered when alarm goes off, i.e. receiving a system broadcast
        if (intent.action == WATCH_DOG_RESTART_ACTION) {
            Timber.e("Restart Things Application")
            val homeInformationRepository: FirebaseHomeInformationRepository by inject()
            val threadedTag = "[${Thread.currentThread().name}] ThingsActivity"
            val timeStamp = System.currentTimeMillis()
            val time = SimpleDateFormat("dd MMM HH:mm:ss.SSS", Locale.getDefault()).format(Date(timeStamp))
            val remoteLog = RemoteLog("ERROR", threadedTag, "restartApp", null, time)
            homeInformationRepository.logThingsLog(remoteLog, timeStamp)
            ProcessPhoenix.triggerRebirth(context)
            //Timber.e("Reboot Things")
            //DeviceManager.getInstance().reboot();
        }
    }
}