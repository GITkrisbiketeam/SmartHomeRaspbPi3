package com.krisbiketeam.smarthomeraspbpi3.di

import com.krisbiketeam.data.auth.Authentication
import com.krisbiketeam.data.auth.AuthenticationLiveData
import com.krisbiketeam.data.nearby.NearbyService
import com.krisbiketeam.data.nearby.NearbyServiceLiveData
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.LoginSettingsViewModel
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.WifiSettingsViewModel
import org.koin.android.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module
import org.mockito.Mockito.mock

val testModule = module {
    viewModel { WifiSettingsViewModel(get()) }
    single { NearbyServiceLiveData(mock(NearbyService::class.java)) }

    viewModel { LoginSettingsViewModel(get(), get()) }
    single { AuthenticationLiveData(mock(Authentication::class.java)) }
}