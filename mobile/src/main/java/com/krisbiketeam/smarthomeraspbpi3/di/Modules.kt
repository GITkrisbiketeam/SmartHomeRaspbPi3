package com.krisbiketeam.smarthomeraspbpi3.di

import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import com.krisbiketeam.smarthomeraspbpi3.adapters.RoomDetailHomeUnitListAdapter
import com.krisbiketeam.smarthomeraspbpi3.adapters.TaskListAdapter
import com.krisbiketeam.smarthomeraspbpi3.common.Analytics
import com.krisbiketeam.smarthomeraspbpi3.common.auth.Authentication
import com.krisbiketeam.smarthomeraspbpi3.common.auth.AuthenticationLiveData
import com.krisbiketeam.smarthomeraspbpi3.common.auth.FirebaseAuthentication
import com.krisbiketeam.smarthomeraspbpi3.common.ble.BleClient
import com.krisbiketeam.smarthomeraspbpi3.common.ble.BleScanner
import com.krisbiketeam.smarthomeraspbpi3.common.ble.BluetoothEnablerManager
import com.krisbiketeam.smarthomeraspbpi3.common.ble.ThingsBleStateProvider
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyService
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyServiceLiveData
import com.krisbiketeam.smarthomeraspbpi3.common.nearby.NearbyServiceProvider
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorageImpl
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.AddEditHwUnitViewModel
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.HomeUnitGenericDetailViewModel
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.HomeUnitLightSwitchDetailViewModel
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.HomeUnitMCP23017WatchDogDetailViewModel
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.HomeUnitTypeChooserDialogViewModel
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.HomeUnitWaterCirculationDetailViewModel
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.HwUnitErrorEventListViewModel
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.HwUnitErrorLogsViewModel
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.HwUnitListViewModel
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.LogsViewModel
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.NavigationViewModel
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.NewRoomDialogViewModel
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.RoomDetailViewModel
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.RoomListViewModel
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.TaskListViewModel
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.ThingsAppLogsViewModel
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.UnitTaskViewModel
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
    viewModel { HomeUnitTypeChooserDialogViewModel() }
    viewModel { (roomName: String) -> RoomDetailViewModel(get(), roomName) }
    viewModel { (roomName: String?, homeUnitName: String?, homeUnitType: HomeUnitType) ->
        HomeUnitGenericDetailViewModel(androidApplication(), get(), roomName, homeUnitName, homeUnitType)
    }
    viewModel { (roomName: String?, homeUnitName: String?) ->
        HomeUnitLightSwitchDetailViewModel(androidApplication(), get(), roomName, homeUnitName)
    }
    viewModel { (roomName: String?, homeUnitName: String?) ->
        HomeUnitWaterCirculationDetailViewModel(androidApplication(), get(), roomName, homeUnitName)
    }
    viewModel { (roomName: String?, homeUnitName: String?) ->
        HomeUnitMCP23017WatchDogDetailViewModel(androidApplication(), get(), roomName, homeUnitName)
    }
    viewModel { (taskName: String?, homeUnitName: String, homeUnitType: HomeUnitType) ->
        UnitTaskViewModel(get(), taskName, homeUnitName, homeUnitType)
    }
    viewModel { (wifiManager: WifiManager, connectivityManager: ConnectivityManager) ->
        WifiSettingsViewModel(get(), wifiManager, connectivityManager)
    }
    viewModel { LoginSettingsViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { HomeSettingsViewModel(get(), get(), get(), get(), get()) }
    viewModel { NavigationViewModel(get(), get()) }
    viewModel { (hwUnitName: String) -> AddEditHwUnitViewModel(get(), hwUnitName) }
    viewModel { HwUnitListViewModel(get()) }
    viewModel { HwUnitErrorEventListViewModel(get()) }
    viewModel { LogsViewModel(get()) }
    viewModel { ThingsAppLogsViewModel(get(), get()) }
    viewModel { HwUnitErrorLogsViewModel(get()) }

    single { FirebaseHomeInformationRepository() }
    single<SecureStorage> { SecureStorageImpl(androidApplication(), get()) }
    single<Authentication> { FirebaseAuthentication() }
    single { Moshi.Builder().build() }
    single { Analytics() }

    factory { AuthenticationLiveData(get()) }
    factory { RoomDetailHomeUnitListAdapter(get()) }
    factory { TaskListAdapter(get()) }

    factory { NearbyServiceLiveData(get()) }
    factory<NearbyService> { NearbyServiceProvider(androidApplication()) }

    //scope<BluetoothScope> {
    factory { androidApplication().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }

    factory { BluetoothEnablerManager(androidApplication(), get()) }

    factory { ThingsBleStateProvider(get()) }

    factory { BleScanner(get()) }

    factory { BleClient(androidApplication()) }
    //}
}