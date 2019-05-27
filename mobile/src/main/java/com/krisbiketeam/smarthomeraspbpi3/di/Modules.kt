package com.krisbiketeam.smarthomeraspbpi3.di

import com.krisbiketeam.smarthomeraspbpi3.common.auth.Authentication
import com.krisbiketeam.smarthomeraspbpi3.common.auth.AuthenticationLiveData
import com.krisbiketeam.smarthomeraspbpi3.common.auth.FirebaseAuthentication
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyService
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyServiceLiveData
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyServiceProvider
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.NotSecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.*
import com.squareup.moshi.Moshi
import org.koin.android.ext.koin.androidApplication
import org.koin.android.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.Module
import org.koin.dsl.module.module

val myModule: Module = module {
    viewModel { RoomListViewModel(FirebaseHomeInformationRepository) }
    viewModel { NewRoomDialogViewModel(androidApplication(), FirebaseHomeInformationRepository) }
    viewModel { (roomName: String) -> RoomDetailViewModel(FirebaseHomeInformationRepository, roomName) }
    viewModel { (roomName: String, homeUnitName: String, homeUnitType:String) -> HomeUnitDetailViewModel(FirebaseHomeInformationRepository, roomName, homeUnitName, homeUnitType) }
    viewModel { (taskName: String, homeUnitName: String, homeUnitType:String) -> UnitTaskViewModel(FirebaseHomeInformationRepository, taskName, homeUnitName, homeUnitType) }
    viewModel { WifiSettingsViewModel(get()) }
    viewModel { LoginSettingsViewModel(get(), get()) }
    viewModel { HomeSettingsViewModel(get()) }
    viewModel { NavigationViewModel(get()) }
    viewModel { (hwUnitName: String) -> AddEditHwUnitViewModel(FirebaseHomeInformationRepository, hwUnitName) }
    viewModel { HwUnitListViewModel(FirebaseHomeInformationRepository) }
    viewModel { HwUnitErrorEventListViewModel(FirebaseHomeInformationRepository) }

    single { NotSecureStorage(androidApplication()) as SecureStorage }
    single { FirebaseAuthentication() as Authentication }
    single { Moshi.Builder().build() as Moshi}

    factory { AuthenticationLiveData(get()) }

    factory { NearbyServiceLiveData(get()) }
    factory { NearbyServiceProvider(androidApplication(), get()) as NearbyService }
}