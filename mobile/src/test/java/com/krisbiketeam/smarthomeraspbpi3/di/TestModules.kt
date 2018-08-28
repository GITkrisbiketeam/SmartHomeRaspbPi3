package com.krisbiketeam.smarthomeraspbpi3.di

import com.krisbiketeam.data.nearby.NearbyService
import com.krisbiketeam.data.nearby.NearbyServiceLiveData
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.WifiSettingsViewModel
import org.koin.android.architecture.ext.viewModel
import org.koin.dsl.module.applicationContext
import org.mockito.Mockito.mock

val testModule = applicationContext {
    viewModel { WifiSettingsViewModel(get()) }
    bean { NearbyServiceLiveData(mock(NearbyService::class.java)) }
}