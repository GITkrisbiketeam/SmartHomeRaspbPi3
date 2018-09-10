package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.lifecycle.*
import com.krisbiketeam.data.storage.HomeInformationRepository
import com.krisbiketeam.data.storage.dto.HOME_STORAGE_UNITS
import com.krisbiketeam.data.storage.dto.UnitTask
import com.krisbiketeam.smarthomeraspbpi3.adapters.UnitTaskListAdapter
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

    val value: LiveData<Any?>

    var name: MutableLiveData<String> = MutableLiveData()

    var type: MutableLiveData<String> = MutableLiveData()
    val typeList = HOME_STORAGE_UNITS

    var room: MutableLiveData<String> = MutableLiveData()
    val roomNameList: LiveData<List<String>>    // RoomListLiveData

    var hardwareUnitName: MutableLiveData<String> = MutableLiveData()
    val hardwareUnitNameList: LiveData<List<String>>    //HwUnitListLiveData

    val unitTaskList: LiveData<List<UnitTask>>

    val unitTaskListAdapter: UnitTaskListAdapter

    var storageUnitNameList: LiveData<List<String>>     // StorageUnitListLiveData

    init {
        Timber.d("init unitName: $unitName unitType: $unitType roomName: $roomName")

        isEditMode.value = unitName == null || unitType == null

        name.value = unitName
        type.value = unitType
        room.value = roomName

        unitTaskListAdapter = UnitTaskListAdapter()

        value = Transformations.switchMap(isEditMode) { edit ->
            Timber.d("init isEditMode value edit: $edit")
            if (edit) {
                null
            } else {
                if (unitType != null && unitName != null) {
                    Transformations.map(homeRepository.storageUnitLiveData(unitType, unitName)) { storageUnit ->
                        storageUnit.value
                    }
                } else {
                    null
                }
            }
        }

        // Decide how to handle this list
        unitTaskList = if (unitType != null && unitName != null) {
            homeRepository.unitTaskListLiveData(unitType, unitName)
        } else {
            MutableLiveData<List<UnitTask>>().also {
                it.value = emptyList()
            }
        }

        // LiveDatas for editing mode
        roomNameList = Transformations.switchMap(isEditMode) { edit ->
            Timber.d("init isEditMode roomNameList edit: $edit")
            if (edit) {
                Transformations.map(homeRepository.roomsLiveData()) { list ->
                    list.map { it.name }
                }
            } else {
                MutableLiveData<List<String>>().also {
                    it.value = emptyList()
                }
            }
        }
        hardwareUnitNameList = Transformations.switchMap(isEditMode) { edit ->
            Timber.d("init isEditMode hardwareUnitNameList edit: $edit")
            if (edit) {
                Transformations.map(homeRepository.hwUnitListLiveData()) { list ->
                    list.map { it.name }
                }
            } else {
                MutableLiveData<List<String>>().also {
                    it.value = emptyList()
                }
            }
        }
        storageUnitNameList = Transformations.switchMap(isEditMode) { edit ->
            Timber.d("init isEditMode storageUnitNameList edit: $edit")
            if (edit) {
                Transformations.switchMap(type) { tableName ->
                    Transformations.map(homeRepository.storageUnitListLiveData(tableName)) { storageUnitList ->
                        storageUnitList.map { it.name }
                    }
                }
            } else {
                MutableLiveData<List<String>>().also {
                    it.value = emptyList()
                }
            }
        }
    }
}
