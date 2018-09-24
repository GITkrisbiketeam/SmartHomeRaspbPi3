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
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.*
import com.squareup.moshi.Moshi
import org.koin.android.ext.koin.androidApplication
import org.koin.android.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.Module
import org.koin.dsl.module.module

val myModule: Module = module {
    viewModel { RoomListViewModel(FirebaseHomeInformationRepository) }
    viewModel { (roomName: String) -> RoomDetailViewModel(FirebaseHomeInformationRepository, roomName) }
    viewModel { (roomName: String, homeUnitame: String, homeUnitType:String) -> HomeUnitDetailViewModel(FirebaseHomeInformationRepository, roomName, homeUnitame, homeUnitType) }
    viewModel { (taskName: String, homeUnitame: String, homeUnitType:String) -> UnitTaskViewModel(FirebaseHomeInformationRepository, taskName, homeUnitame, homeUnitType) }
    viewModel { WifiSettingsViewModel(get()) }
    viewModel { LoginSettingsViewModel(get(), get()) }
    viewModel { NavigationViewModel(get()) }
    viewModel { AddEditHwUnitViewModel(FirebaseHomeInformationRepository) }
    viewModel { HwUnitListViewModel(FirebaseHomeInformationRepository) }

    single { NotSecureStorage(androidApplication()) as SecureStorage }
    single { FirebaseAuthentication() as Authentication }
    single { Moshi.Builder().build() as Moshi}

    factory { AuthenticationLiveData(get()) }

    factory { NearbyServiceLiveData(get()) }
    factory { NearbyServiceProvider(androidApplication(), get()) as NearbyService }
}