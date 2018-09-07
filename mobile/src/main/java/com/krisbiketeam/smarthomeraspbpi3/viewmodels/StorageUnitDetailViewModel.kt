package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.lifecycle.*
import com.krisbiketeam.data.storage.HomeInformationRepository
import com.krisbiketeam.data.storage.dto.HOME_STORAGE_UNITS
import com.krisbiketeam.data.storage.dto.UnitTask
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomDetailFragment
import timber.log.Timber


/**
 * The ViewModel used in [RoomDetailFragment].
 */
class StorageUnitDetailViewModel(
        homeRepository: HomeInformationRepository,
        roomName: String?,
        unitName: String?,
        unitType: String?
) : ViewModel() {

    var isEditMode: MutableLiveData<Boolean> = MutableLiveData()

    val value: MutableLiveData<Any> = MutableLiveData()

    var name: MutableLiveData<String> = MutableLiveData()

    var type: MutableLiveData<String> = MutableLiveData()
    val typeList = HOME_STORAGE_UNITS

    var room: MutableLiveData<String> = MutableLiveData()
    val roomNameList: LiveData<List<String>>

    var hardwareUnitName: MutableLiveData<String> = MutableLiveData()
    val hardwareUnitNameList: LiveData<List<String>>

    val unitsTasks: MutableLiveData<List<UnitTask>> = MutableLiveData()



    init {
        Timber.d("init unitName: $unitName unitType: $unitType roomName: $roomName")

        isEditMode.value = false

        name.value = unitName
        type.value = unitType
        room.value = roomName

        roomNameList = Transformations.map(homeRepository.roomsLiveData()){ list ->
            list.map {it.name}
        }
        hardwareUnitNameList = Transformations.map(homeRepository.hwUnitListLiveData()){ list ->
            list.map {it.name}
        }

    }
}
