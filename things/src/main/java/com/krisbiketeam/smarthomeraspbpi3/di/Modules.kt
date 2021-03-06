package com.krisbiketeam.smarthomeraspbpi3.di

import com.krisbiketeam.smarthomeraspbpi3.common.auth.Authentication
import com.krisbiketeam.smarthomeraspbpi3.common.auth.FirebaseAuthentication
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyService
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyServiceProvider
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorageImpl
import com.squareup.moshi.Moshi
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.dsl.module


val myModule: Module = module {

    single { FirebaseHomeInformationRepository() }
    single<SecureStorage> { SecureStorageImpl(androidApplication(), get()) }
    single<Authentication> { FirebaseAuthentication() }
    single { Moshi.Builder().build() }

    factory<NearbyService> { NearbyServiceProvider(androidApplication(), get()) }
}