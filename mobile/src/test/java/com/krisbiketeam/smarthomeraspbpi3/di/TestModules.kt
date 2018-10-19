package com.krisbiketeam.smarthomeraspbpi3.di

import com.krisbiketeam.smarthomeraspbpi3.common.auth.Authentication
import com.krisbiketeam.smarthomeraspbpi3.common.auth.AuthenticationLiveData
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyService
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyServiceLiveData
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