package com.krisbiketeam.smarthomeraspbpi3

import android.app.Application
import com.google.android.things.device.TimeManager
import com.google.firebase.FirebaseApp
import com.krisbiketeam.smarthomeraspbpi3.di.myModule
import com.krisbiketeam.smarthomeraspbpi3.utils.ConsoleLoggerTree
import kotlinx.coroutines.IO_PARALLELISM_PROPERTY_NAME
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber


class ThingsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("native-bsec")


        // Set to ulimited number of corutines to run on new threads not limiter do default 64
        System.setProperty(IO_PARALLELISM_PROPERTY_NAME, Int.MAX_VALUE.toString())

        // Set current RP3 Tings timezone
        TimeManager.getInstance().setTimeZone("Europe/Warsaw")

        FirebaseApp.initializeApp(this)

        Timber.plant(ConsoleLoggerTree)

        startKoin {
            androidContext(this@ThingsApplication)
            androidLogger(Level.ERROR)
            modules(myModule)
        }
    }
}
