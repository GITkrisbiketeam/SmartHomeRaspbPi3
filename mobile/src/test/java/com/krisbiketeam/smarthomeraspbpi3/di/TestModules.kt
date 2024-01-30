package com.krisbiketeam.smarthomeraspbpi3.di

import com.krisbiketeam.smarthomeraspbpi3.common.auth.Authentication
import com.krisbiketeam.smarthomeraspbpi3.common.auth.AuthenticationLiveData
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.settings.LoginSettingsViewModel
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module
import org.mockito.Mockito.mock

val testModule = module {
    viewModel {
        LoginSettingsViewModel(get(), get())
    }
    single { AuthenticationLiveData(mock(Authentication::class.java)) }
}