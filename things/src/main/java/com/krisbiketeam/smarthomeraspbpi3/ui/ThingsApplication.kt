package com.krisbiketeam.smarthomeraspbpi3.ui

import android.app.Application
import com.google.firebase.FirebaseApp
import com.krisbiketeam.smarthomeraspbpi3.di.myModule
import com.krisbiketeam.smarthomeraspbpi3.utils.ConsoleLoggerTree
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber


class ThingsApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        Timber.plant(ConsoleLoggerTree)

        startKoin {
            androidContext(this@ThingsApplication)
            androidLogger()
            modules(myModule)
        }
    }
}
