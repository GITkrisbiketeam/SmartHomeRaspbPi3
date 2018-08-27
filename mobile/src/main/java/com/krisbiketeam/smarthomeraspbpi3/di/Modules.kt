package com.krisbiketeam.smarthomeraspbpi3.di

import com.krisbiketeam.data.auth.Authentication
import com.krisbiketeam.data.auth.FirebaseAuthentication
import com.krisbiketeam.data.nearby.NearbyService
import com.krisbiketeam.data.nearby.NearbyServiceProvider
import com.krisbiketeam.data.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.data.storage.NotSecureStorage
import com.krisbiketeam.data.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.di.Params.ROOM_NAME
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.RoomDetailViewModel
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.RoomListViewModel
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.WifiSettingsViewModel
import org.koin.android.architecture.ext.viewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module.applicationContext

val myModule = applicationContext {
    viewModel { RoomListViewModel(FirebaseHomeInformationRepository) }
    viewModel { RoomDetailViewModel(FirebaseHomeInformationRepository, getProperty(ROOM_NAME)) }
    viewModel { WifiSettingsViewModel(get()) }

    bean { NotSecureStorage(androidApplication()) as SecureStorage }
    bean { FirebaseAuthentication() as Authentication }

    factory { NearbyServiceProvider(androidApplication()) as NearbyService }
}

object Params {
    const val ROOM_NAME = "room_name"
}
