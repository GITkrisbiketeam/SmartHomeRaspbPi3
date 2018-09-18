package com.krisbiketeam.smarthomeraspbpi3.di

import com.krisbiketeam.data.auth.Authentication
import com.krisbiketeam.data.auth.AuthenticationLiveData
import com.krisbiketeam.data.auth.FirebaseAuthentication
import com.krisbiketeam.data.nearby.NearbyService
import com.krisbiketeam.data.nearby.NearbyServiceLiveData
import com.krisbiketeam.data.nearby.NearbyServiceProvider
import com.krisbiketeam.data.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.data.storage.NotSecureStorage
import com.krisbiketeam.data.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.di.Params.ROOM_NAME
import com.krisbiketeam.smarthomeraspbpi3.di.Params.HOME_UNIT_NAME
import com.krisbiketeam.smarthomeraspbpi3.di.Params.HOME_UNIT_TYPE
import com.krisbiketeam.smarthomeraspbpi3.di.Params.UNIT_TASK_NAME
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.*
import com.squareup.moshi.Moshi
import org.koin.android.architecture.ext.viewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module.applicationContext

val myModule = applicationContext {
    viewModel { RoomListViewModel(FirebaseHomeInformationRepository) }
    viewModel { RoomDetailViewModel(FirebaseHomeInformationRepository, getProperty(ROOM_NAME)) }
    viewModel { HomeUnitDetailViewModel(FirebaseHomeInformationRepository, getProperty(ROOM_NAME), getProperty(HOME_UNIT_NAME), getProperty(HOME_UNIT_TYPE)) }
    viewModel { UnitTaskViewModel(FirebaseHomeInformationRepository, getProperty(UNIT_TASK_NAME), getProperty(HOME_UNIT_NAME), getProperty(HOME_UNIT_TYPE)) }
    viewModel { WifiSettingsViewModel(get()) }
    viewModel { LoginSettingsViewModel(get(), get()) }
    viewModel { NavigationViewModel(get()) }
    viewModel { AddEditHwUnitViewModel(FirebaseHomeInformationRepository) }
    viewModel { HwUnitListViewModel(FirebaseHomeInformationRepository) }

    bean { NotSecureStorage(androidApplication()) as SecureStorage }
    bean { FirebaseAuthentication() as Authentication }
    bean { Moshi.Builder().build() as Moshi}

    factory { AuthenticationLiveData(get()) }

    factory { NearbyServiceLiveData(get()) }
    factory { NearbyServiceProvider(androidApplication(), get()) as NearbyService }
}

object Params {
    const val ROOM_NAME = "room_name"
    const val HOME_UNIT_NAME = "home_unit_name"
    const val HOME_UNIT_TYPE = "home_unit_type"
    const val UNIT_TASK_NAME = "unit_task_name"
}
