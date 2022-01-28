package com.krisbiketeam.smarthomeraspbpi3.di

import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import com.krisbiketeam.smarthomeraspbpi3.adapters.RoomDetailHomeUnitListAdapter
import com.krisbiketeam.smarthomeraspbpi3.adapters.TaskListAdapter
import com.krisbiketeam.smarthomeraspbpi3.common.Analytics
import com.krisbiketeam.smarthomeraspbpi3.common.auth.Authentication
import com.krisbiketeam.smarthomeraspbpi3.common.auth.AuthenticationLiveData
import com.krisbiketeam.smarthomeraspbpi3.common.auth.FirebaseAuthentication
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyService
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyServiceLiveData
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyServiceProvider
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorageImpl
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.*
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.settings.HomeSettingsViewModel
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.settings.LoginSettingsViewModel
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.settings.WifiSettingsViewModel
import com.squareup.moshi.Moshi
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module


val myModule: Module = module {
    viewModel { RoomListViewModel(get(), get()) }
    viewModel { TaskListViewModel(get(), get()) }
    viewModel { NewRoomDialogViewModel(androidApplication(), get()) }
    viewModel { (roomName: String) -> RoomDetailViewModel(get(), roomName) }
    viewModel { (roomName: String, homeUnitName: String, homeUnitType: String) ->
        HomeUnitDetailViewModel(androidApplication(), get(), roomName, homeUnitName, homeUnitType)
    }
    viewModel { (taskName: String, homeUnitName: String, homeUnitType: String) ->
        UnitTaskViewModel(get(), taskName, homeUnitName, homeUnitType)
    }
    viewModel { (wifiManager: WifiManager, connectivityManager: ConnectivityManager) ->
        WifiSettingsViewModel(get(), wifiManager, connectivityManager)
    }
    viewModel { LoginSettingsViewModel(get(), get(), get()) }
    viewModel { HomeSettingsViewModel(get(), get(), get()) }
    viewModel { NavigationViewModel(get(), get()) }
    viewModel { (hwUnitName: String) -> AddEditHwUnitViewModel(get(), hwUnitName) }
    viewModel { HwUnitListViewModel(get()) }
    viewModel { HwUnitErrorEventListViewModel(get()) }
    viewModel { LogsViewModel(get(), get()) }

    single { FirebaseHomeInformationRepository() }
    single<SecureStorage> { SecureStorageImpl(androidApplication(), get()) }
    single<Authentication> { FirebaseAuthentication() }
    single { Moshi.Builder().build() }
    single { Analytics() }

    factory { AuthenticationLiveData(get()) }
    factory { RoomDetailHomeUnitListAdapter(get()) }
    factory { TaskListAdapter(get()) }

    factory { NearbyServiceLiveData(get()) }
    factory<NearbyService> { NearbyServiceProvider(androidApplication(), get()) }
}