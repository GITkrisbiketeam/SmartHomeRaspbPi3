package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.lifecycle.*
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.krisbiketeam.data.storage.HomeInformationRepository
import com.krisbiketeam.data.storage.dto.HOME_STORAGE_UNITS
import com.krisbiketeam.data.storage.dto.HomeUnit
import com.krisbiketeam.data.storage.dto.UnitTask
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.adapters.UnitTaskListAdapter
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomDetailFragment
import timber.log.Timber


/**
 * The ViewModel used in [RoomDetailFragment].
 */
class HomeUnitDetailViewModel(
        private val homeRepository: HomeInformationRepository,
        private val unitName: String,
        private val unitType: String
) : ViewModel() {

    val unitTaskListAdapter = UnitTaskListAdapter()

    val showProgress: MutableLiveData<Boolean>

    val isEditMode: MutableLiveData<Boolean> = MutableLiveData()

    var homeUnit: LiveData<HomeUnit<Any?>>

    val name: MutableLiveData<String>
    val type: MutableLiveData<String>
    val room: MutableLiveData<String>
    val hwUnitName: MutableLiveData<String>


    val unitTaskList: LiveData<List<UnitTask>>


    val typeList = HOME_STORAGE_UNITS
    val roomNameList: LiveData<List<String>>    // RoomListLiveData
    val hwUnitNameList: LiveData<List<String>>


    // used for checking if given homeUnit name is not already used
    var homeUnitNameList = MediatorLiveData<MutableList<String>>()//LiveData<List<String>>// HomeUnitListLiveData

    init {
        Timber.d("init unitName: $unitName unitType: $unitType")

        isEditMode.value = unitName.isEmpty() || unitType.isEmpty()

        homeUnit = if (unitType.isNotEmpty() && unitName.isNotEmpty()) {
            Timber.d("init homeUnit is not empty")
            homeRepository.homeUnitLiveData(unitType, unitName)
        } else {
            MutableLiveData()
        }

        name = Transformations.map(homeUnit) { homeUnit -> homeUnit.name } as MutableLiveData<String>
        type = Transformations.map(homeUnit) { homeUnit -> homeUnit.firebaseTableName } as MutableLiveData<String>
        room = Transformations.map(homeUnit) { homeUnit -> homeUnit.room } as MutableLiveData<String>
        hwUnitName = Transformations.map(homeUnit) { homeUnit -> homeUnit.hardwareUnitName } as MutableLiveData<String>
        showProgress = Transformations.map(homeUnit) { false } as MutableLiveData<Boolean>

        showProgress.value = unitName.isNotEmpty() && unitType.isNotEmpty()


        // Decide how to handle this list
        unitTaskList = if (unitType.isNotEmpty() && unitName.isNotEmpty()) {
            Timber.d("init unitTaskList is not empty")
            homeRepository.unitTaskListLiveData(unitType, unitName)
        } else {
            MutableLiveData()
        }

        // LiveData's for editing mode
        roomNameList = Transformations.switchMap(isEditMode) { edit ->
            Timber.d("init roomNameList isEditMode edit: $edit")
            if (edit) {
                Transformations.map(homeRepository.roomsLiveData()) { list ->
                    Timber.d("init roomNameList isEditMode list: ${list.size}")
                    list.map {
                        it.name
                    }
                }
            } else {
                MutableLiveData()
            }
        }

        hwUnitNameList = Transformations.switchMap(isEditMode) { edit ->
            Timber.d("init hwUnitNameList isEditMode edit: $edit")
            if (edit) {
                Transformations.map(homeRepository.hwUnitListLiveData()) { list ->
                    Timber.d("init hwUnitNameList isEditMode list: ${list.size}")
                    list.map {
                        it.name
                    }
                }
            } else {
                MutableLiveData()
            }
        }

        // used for checking if given homeUnit name is not already used
        homeUnitNameList = Transformations.switchMap(isEditMode) { edit ->
            Timber.d("init homeUnitNameList isEditMode edit: $edit")
            if (edit) {
                MediatorLiveData<MutableList<String>>().apply {
                    typeList.forEach { type ->
                        addSource(homeRepository.homeUnitListLiveData(type)) { homeUnitList ->
                            Timber.d("init homeUnitNameList homeUnitList: ${homeUnitList?.size}")
                            if (homeUnitNameList.value == null) {
                                homeUnitNameList.value = ArrayList()
                            }
                            homeUnitNameList.value?.addAll(
                                    homeUnitList?.map {
                                        Timber.d("init homeUnitNameList it: $it.name")
                                        it.name
                                    } ?: emptyList())
                        }
                    }
                }
            } else {
                MediatorLiveData()
            }
        } as MediatorLiveData<MutableList<String>>//LiveData<List<String>>
    }

    fun trySaveChanges(): Pair<Int, Boolean> {
        Timber.d("tyrSaveChanges")
        if (unitName.isEmpty() && unitType.isEmpty()
                && homeUnitNameList.value?.contains(name.value?.trim()) == true) {
            //This name is already used
            Timber.d("This name is already used")
            val taskSource = TaskCompletionSource<Int>()

            return Pair(R.string.add_edit_home_unit_name_already_used, false)
        } else {
            homeUnit.value?.let { unit ->
                return if (unitName != unit.name || unitType != unit.firebaseTableName) {
                    Pair(R.string.add_edit_home_unit_save_with_delete, true)
                } else {
                    Pair(R.string.add_edit_home_unit_overwrite_changes, true)
                }
            }
            return Pair(R.string.add_edit_home_unit_save_changes, true)
        }
    }

    fun saveChanges(): Task<Void>? {
        Timber.d("tyrSaveChanges")
        homeUnit.value?.let { unit ->
            if (unitName != unit.name || unitType != unit.firebaseTableName) {
                // TODO: sow some UI to confirm rename or type change
                Timber.d("Name or type changed will need to delete old value name=$unitName, firebaseTableName = $unitType")
                showProgress.value = true
                // delete old HomeUnit
                homeRepository.deleteHomeUnit(unit.copy(name = unitName, firebaseTableName = unitType)).continueWithTask { task ->
                    if (task.isCanceled) {
                        task
                    } else {
                        homeRepository.saveHomeUnit(unit)
                    }
                }
            } else {
                homeRepository.saveHomeUnit(unit)
            }
        }
        return null
    }

    fun discardChanges() {
        Timber.d("tyrSaveChanges")
        homeUnit.value?.also {
            name.value = it.name
            type.value = it.firebaseTableName
            room.value = it.room
            hwUnitName.value = it.hardwareUnitName
        }
    }
}
