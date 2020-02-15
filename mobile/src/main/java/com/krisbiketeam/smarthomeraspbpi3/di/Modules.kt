package com.krisbiketeam.smarthomeraspbpi3.di

import android.net.ConnectivityManager
import android.net.wifi.WifiManager
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
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.settings.HomeSettingsViewModel
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.settings.LoginSettingsViewModel
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.settings.WifiSettingsViewModel
import com.squareup.moshi.Moshi
import org.koin.android.ext.koin.androidApplication
import org.koin.android.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.Module
import org.koin.dsl.module.module

val myModule: Module = module {
    viewModel { RoomListViewModel(FirebaseHomeInformationRepository) }
    viewModel { NewRoomDialogViewModel(androidApplication(), FirebaseHomeInformationRepository) }
    viewModel { (roomName: String) ->
        RoomDetailViewModel(FirebaseHomeInformationRepository, roomName)
    }
    viewModel { (roomName: String, homeUnitName: String, homeUnitType: String) ->
        HomeUnitDetailViewModel(FirebaseHomeInformationRepository, roomName, homeUnitName,
                                homeUnitType)
    }
    viewModel { (taskName: String, homeUnitName: String, homeUnitType: String) ->
        UnitTaskViewModel(FirebaseHomeInformationRepository, taskName, homeUnitName, homeUnitType)
    }
    viewModel { (wifiManager: WifiManager, connectivityManager: ConnectivityManager) ->
        WifiSettingsViewModel(get(), wifiManager, connectivityManager)
    }
    viewModel { LoginSettingsViewModel(get(), get()) }
    viewModel { HomeSettingsViewModel(get()) }
    viewModel { NavigationViewModel(get(), FirebaseHomeInformationRepository) }
    viewModel { (hwUnitName: String) ->
        AddEditHwUnitViewModel(FirebaseHomeInformationRepository, hwUnitName)
    }
    viewModel { HwUnitListViewModel(FirebaseHomeInformationRepository) }
    viewModel { HwUnitErrorEventListViewModel(FirebaseHomeInformationRepository) }

    single { NotSecureStorage(androidApplication()) as SecureStorage }
    single { FirebaseAuthentication() as Authentication }
    single { Moshi.Builder().build() as Moshi }

    factory { AuthenticationLiveData(get()) }

    factory { NearbyServiceLiveData(get()) }
    factory { NearbyServiceProvider(androidApplication(), get()) as NearbyService }
}