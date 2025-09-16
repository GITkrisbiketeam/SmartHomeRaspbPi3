package com.krisbiketeam.smarthomeraspbpi3

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.FirebaseApp
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.di.composeModule
import com.krisbiketeam.smarthomeraspbpi3.di.myModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class MobileApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        Timber.plant(object : Timber.DebugTree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                super.log(priority, "[${Thread.currentThread().name}] SHRP3_$tag", message, t)
            }
        })
        startKoin {
            androidContext(this@MobileApplication)
            modules(myModule, composeModule)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channels = HomeUnitType.entries.filterNot { it == HomeUnitType.UNKNOWN }.map { type ->
                // Create the NotificationChannel.
                val name = type.firebaseTableName
                //val descriptionText = getString(R.string.channel_description)
                val importance =
                    when (type) {
                        HomeUnitType.HOME_REED_SWITCHES,
                        HomeUnitType.HOME_MOTIONS -> NotificationManager.IMPORTANCE_MAX

                        else -> NotificationManager.IMPORTANCE_DEFAULT
                    }
                notificationManager.deleteNotificationChannel(type.name)

                NotificationChannel(type.name, name, importance).apply {
                    //description = descriptionText

                    val groupName = type.firebaseTableName
                    val groupId = type.name
                    val group = NotificationChannelGroup(groupId, groupName)
                    notificationManager.createNotificationChannelGroup(group)

                    setGroup(type.name)
                }
            }
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            notificationManager.createNotificationChannels(channels)
        }
    }

}
