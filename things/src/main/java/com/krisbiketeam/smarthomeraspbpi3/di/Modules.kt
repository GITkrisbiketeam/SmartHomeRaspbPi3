package com.krisbiketeam.smarthomeraspbpi3.di

import android.bluetooth.BluetoothManager
import android.content.Context
import com.krisbiketeam.smarthomeraspbpi3.Home
import com.krisbiketeam.smarthomeraspbpi3.common.Analytics
import com.krisbiketeam.smarthomeraspbpi3.common.auth.Authentication
import com.krisbiketeam.smarthomeraspbpi3.common.auth.FirebaseAuthentication
import com.krisbiketeam.smarthomeraspbpi3.common.ble.BleService
import com.krisbiketeam.smarthomeraspbpi3.common.ble.BluetoothEnablerManager
import com.krisbiketeam.smarthomeraspbpi3.common.ble.BluetoothScope
import com.krisbiketeam.smarthomeraspbpi3.common.ble.ThingsBleStateProvider
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorageImpl
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.dsl.module


val myModule: Module = module {

    single { FirebaseHomeInformationRepository() }
    single<SecureStorage> { SecureStorageImpl(androidApplication(), get()) }
    single<Authentication> { FirebaseAuthentication() }
    single { Analytics() }
    single { Home(get(), get(), get()) }

    factory { BluetoothScope() }

    scope<BluetoothScope> {
        scoped { androidApplication().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }

        scoped { BluetoothEnablerManager(androidApplication(), get()) }

        scoped { ThingsBleStateProvider(get()) }

        scoped { BleService(androidApplication(), get(), get()) }
    }

}