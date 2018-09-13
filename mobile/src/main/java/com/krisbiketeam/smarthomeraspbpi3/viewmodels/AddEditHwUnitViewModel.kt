package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import com.krisbiketeam.data.storage.HomeInformationRepository
import com.krisbiketeam.data.storage.dto.HOME_STORAGE_UNITS
import com.krisbiketeam.smarthomeraspbpi3.ui.AddEditHwUnitFragment
import timber.log.Timber

/**
 * The ViewModel used in [AddEditHwUnitFragment].
 */
class AddEditHwUnitViewModel(
        homeRepository: HomeInformationRepository
) : ViewModel() {

    var name: MutableLiveData<String> = MutableLiveData()

    var homeUnitType: MutableLiveData<String> = MutableLiveData()
    val homeUnitTypeList = HOME_STORAGE_UNITS

    var room: MutableLiveData<String> = MutableLiveData()
    val roomNameList: LiveData<List<String>>

    var hardwareUnitName: MutableLiveData<String> = MutableLiveData()
    val hardwareUnitNameList: LiveData<List<String>>

    var homeUnitListLiveData: LiveData<List<String>>//HomeUnitListLiveData

    init {
        Timber.d("init")
        roomNameList = Transformations.map(homeRepository.roomsLiveData()){ list ->
            list.map {it.name}
        }
        hardwareUnitNameList = Transformations.map(homeRepository.hwUnitListLiveData()){ list ->
            list.map {it.name}
        }
        homeUnitListLiveData = Transformations.switchMap(homeUnitType){ tableName ->
            Transformations.map(homeRepository.homeUnitListLiveData(tableName)){ homeUnitList ->
                homeUnitList.map {it.name}
            }
        }
    }

}
