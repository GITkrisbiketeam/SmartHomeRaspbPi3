package com.krisbiketeam.smarthomeraspbpi3.di

import com.krisbiketeam.smarthomeraspbpi3.compose.core.drawer.SmartDrawerViewModel
import com.krisbiketeam.smarthomeraspbpi3.compose.screens.roomlist.RoomListScreenViewModel
import com.krisbiketeam.smarthomeraspbpi3.compose.screens.tasklist.TaskListScreenViewModel
import com.krisbiketeam.smarthomeraspbpi3.usecases.ReloginLastUserWithHomeUseCase
import com.krisbiketeam.smarthomeraspbpi3.usecases.ReloginLastUserWithHomeUseCaseImpl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module


val composeModule: Module = module {
    viewModel { RoomListScreenViewModel(get()) }
    viewModel { TaskListScreenViewModel(get()) }
    viewModel { SmartDrawerViewModel(get(), get()) }

    factory<ReloginLastUserWithHomeUseCase> { ReloginLastUserWithHomeUseCaseImpl(get(), get()) }
}