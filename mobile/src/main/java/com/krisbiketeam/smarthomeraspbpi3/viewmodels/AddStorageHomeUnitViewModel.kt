package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import com.krisbiketeam.data.storage.HomeInformationRepository
import com.krisbiketeam.data.storage.StorageUnitListLiveData
import com.krisbiketeam.data.storage.dto.HOME_STORAGE_UNITS
import com.krisbiketeam.data.storage.dto.UnitTask
import com.krisbiketeam.smarthomeraspbpi3.ui.AddStorageHomeUnitFragment
import timber.log.Timber

/**
 * The ViewModel used in [AddStorageHomeUnitFragment].
 */
class AddStorageHomeUnitViewModel(
        homeRepository: HomeInformationRepository
) : ViewModel() {

    var name: MutableLiveData<String> = MutableLiveData()

    var firebaseTableName: MutableLiveData<String> = MutableLiveData()
    val typeList = HOME_STORAGE_UNITS

    var room: MutableLiveData<String> = MutableLiveData()
    val roomNameList: LiveData<List<String>>

    var hardwareUnitName: MutableLiveData<String> = MutableLiveData()
    val hwUnitNameList: LiveData<List<String>>

    val unitsTasks: MutableLiveData<List<UnitTask>> = MutableLiveData()

    var storageUnitListLiveData: StorageUnitListLiveData? = null

    init {
        Timber.d("init")
        roomNameList = Transformations.map(homeRepository.roomsLiveData()){ list ->
            list.map {it.name}
        }
        hwUnitNameList = Transformations.map(homeRepository.hwUnitListLiveData()){ list ->
            list.map {it.name}
        }
    }

}
