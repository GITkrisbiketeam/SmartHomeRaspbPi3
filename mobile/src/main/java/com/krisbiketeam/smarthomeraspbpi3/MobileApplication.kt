package com.krisbiketeam.smarthomeraspbpi3

import android.app.Application
import com.google.firebase.FirebaseApp
import com.krisbiketeam.smarthomeraspbpi3.di.myModule
import org.koin.android.ext.android.startKoin
import timber.log.Timber

class MobileApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        Timber.plant(object: Timber.DebugTree(){
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                super.log(priority, "SHRP3_$tag", message, t)
            }
        })
        startKoin(this, listOf(myModule))
    }

}
