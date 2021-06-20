package com.krisbiketeam.smarthomeraspbpi3.di

import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import com.krisbiketeam.smarthomeraspbpi3.common.auth.Authentication
import com.krisbiketeam.smarthomeraspbpi3.common.auth.AuthenticationLiveData
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyService
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyServiceLiveData
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.settings.LoginSettingsViewModel
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.settings.WifiSettingsViewModel
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module
import org.mockito.Mockito.mock

val testModule = module {
    viewModel {
        WifiSettingsViewModel(get(), mock(WifiManager::class.java),
                              mock(ConnectivityManager::class.java))
    }
    single { NearbyServiceLiveData(mock(NearbyService::class.java)) }

    viewModel {
        LoginSettingsViewModel(get(), get())
    }
    single { AuthenticationLiveData(mock(Authentication::class.java)) }
}