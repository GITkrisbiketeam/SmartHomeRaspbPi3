package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.adapters.UnitTaskListAdapter
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HOME_STORAGE_UNITS
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Room
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.UnitTask
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_LIGHT_SWITCHES
import com.krisbiketeam.smarthomeraspbpi3.ui.HomeUnitDetailFragment
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomDetailFragment
import com.krisbiketeam.smarthomeraspbpi3.utils.getLastUpdateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import timber.log.Timber


/**
 * The ViewModel used in [RoomDetailFragment].
 */
class HomeUnitDetailViewModel(application: Application,
                              private val homeRepository: FirebaseHomeInformationRepository,
                              roomName: String, unitName: String, unitType: String) :
        AndroidViewModel(application) {

    val unitTaskListAdapter = UnitTaskListAdapter(unitName, unitType)

    val homeUnit =
            if (unitName.isEmpty() && unitType.isEmpty()) null else homeRepository.homeUnitFlow(
                    unitType, unitName).asLiveData(Dispatchers.Default)

    val showProgress: MutableLiveData<Boolean> =
            if (homeUnit == null) {
                MutableLiveData(false)
            } else {
                Transformations.switchMap(homeUnit) {
                    Transformations.map(homeRepository.isUserOnline()) { online ->
                        online != true
                    }
                } as MutableLiveData<Boolean>
            }

    val isEditMode: MutableLiveData<Boolean> = MutableLiveData(homeUnit == null)

    val name: MutableLiveData<String> =
            if (homeUnit == null) {
                MutableLiveData()
            } else {
                Transformations.map(homeUnit) { unit -> unit.name } as MutableLiveData<String>
            }

    val typeList = HOME_STORAGE_UNITS
    val type = if (homeUnit == null) {
        MutableLiveData()
    } else {
        Transformations.map(Transformations.distinctUntilChanged(homeUnit)) { unit ->
            unit.type
        } as MutableLiveData<String?>
    }

    val roomNameList: LiveData<List<String>> =
            Transformations.switchMap(Transformations.distinctUntilChanged(isEditMode)) { isEdit ->
                if (isEdit) {
                    Transformations.map(
                            Transformations.distinctUntilChanged(homeRepository.roomListLiveData())) { list ->
                        list.map(Room::name)
                    }
                } else {
                    MutableLiveData(emptyList())
                }
            }
    val roomName = if (homeUnit == null) {
        MutableLiveData(roomName)
    } else {
        Transformations.map(homeUnit) { unit -> unit.room }
    } as MutableLiveData<String?>

    @ExperimentalCoroutinesApi
    val hwUnitNameList: LiveData<List<Pair<String, Boolean>>> =
            Transformations.switchMap(Transformations.distinctUntilChanged(isEditMode)) { isEdit ->
                Timber.d("init hwUnitNameList isEditMode: $isEdit")
                if (isEdit) {
                    Transformations.switchMap(type) { type ->
                        combine(homeRepository.homeUnitListFlow(), homeRepository.hwUnitListFlow()) { homeUnitList, hwUnitList ->
                            homeUnitOfSelectedTypeList = homeUnitList.filter {
                                it.type == type
                            }
                            hwUnitList.map {
                                Pair(it.name,
                                        homeUnitList.find { unit -> unit.hwUnitName == it.name || unit.secondHwUnitName == it.name } != null)
                            }
                        }.asLiveData(Dispatchers.Default)
                    }
                } else {
                    MutableLiveData(emptyList())
                }
            }
    val hwUnitName = if (homeUnit == null) {
        MutableLiveData()
    } else {
        Transformations.map(homeUnit) { unit -> unit.hwUnitName }
    } as MutableLiveData<String?>

    val secondHwUnitName = if (homeUnit == null) {
        MutableLiveData()
    } else {
        Transformations.map(homeUnit) { unit -> unit.secondHwUnitName }
    } as MutableLiveData<String?>

    val value: MutableLiveData<String> =
            if (homeUnit == null) MutableLiveData() else Transformations.map(homeUnit) { unit -> unit.value.toString() } as MutableLiveData<String>
    val lastUpdateTime: MutableLiveData<String> =
            if (homeUnit == null) MutableLiveData() else Transformations.map(homeUnit) { unit ->
                getLastUpdateTime(application, unit.lastUpdateTime)
            } as MutableLiveData<String>

    val secondValue: MutableLiveData<String> =
            if (homeUnit == null) MutableLiveData() else Transformations.map(homeUnit) { unit -> unit.secondValue.toString() } as MutableLiveData<String>
    val secondLastUpdateTime: MutableLiveData<String> =
            if (homeUnit == null) MutableLiveData() else Transformations.map(homeUnit) { unit ->
                getLastUpdateTime(application, unit.secondLastUpdateTime)
            } as MutableLiveData<String>

    val minValue: MutableLiveData<String> =
            if (homeUnit == null) MutableLiveData() else Transformations.map(homeUnit) { unit -> unit.min.toString() } as MutableLiveData<String>
    val minLastUpdateTime: MutableLiveData<String> =
            if (homeUnit == null) MutableLiveData() else Transformations.map(homeUnit) { unit ->
                getLastUpdateTime(application, unit.minLastUpdateTime)
            } as MutableLiveData<String>

    val maxValue: MutableLiveData<String> =
            if (homeUnit == null) MutableLiveData() else Transformations.map(homeUnit) { unit -> unit.max.toString() } as MutableLiveData<String>
    val maxLastUpdateTime: MutableLiveData<String> =
            if (homeUnit == null) MutableLiveData() else Transformations.map(homeUnit) { unit ->
                getLastUpdateTime(application, unit.maxLastUpdateTime)
            } as MutableLiveData<String>

    val firebaseNotify: MutableLiveData<Boolean> =
            if (homeUnit == null) MutableLiveData(false) else Transformations.map(homeUnit) { unit -> unit.firebaseNotify } as MutableLiveData<Boolean>

    // Decide how to handle this list
    val unitTaskList: LiveData<Map<String, UnitTask>> = if (homeUnit != null) {
        Transformations.switchMap(isEditMode) { edit ->
            Timber.d("init unitTaskList isEditMode edit: $edit")
            Transformations.map(homeRepository.unitTaskListLiveData(unitType, unitName)) {
                if (edit) {
                    Timber.d("init unitTaskList edit it: $it")
                    // Add empty UnitTask which will be used for adding new UnitTask
                    val newList = it.toMutableMap()
                    newList[""] = UnitTask()
                    Timber.d("init unitTaskList edit newList: $newList")

                    newList
                } else {
                    Timber.d("init unitTaskList it: $it")
                    it
                }
            }
        }
    } else {
        Transformations.switchMap(isEditMode) { edit ->
            Timber.d("init adding add new item to unitTaskList isEditMode edit: $edit")
            // We cannot add option to Add new Task before Home Unit is saved as there will not be where to save ne UnitTask
            /*if (edit) {
                MutableLiveData<Map<String, UnitTask>>().apply {
                    value = mutableMapOf(Pair("", UnitTask()))
                }//newList
            } else {*/
            MutableLiveData()
            //}
        }
    }

    private var homeRepositoryTask: Task<Void>? = null

    // used for checking if given homeUnit name is not already used this is populated in hwUnitNameList
    private var homeUnitOfSelectedTypeList: List<HomeUnit<Any>> = emptyList()

    init {
        Timber.d("init unitName: $unitName unitType: $unitType roomName: $roomName")

        if (homeUnit != null) {
            Timber.d("init Editing existing HomeUnit")

            showProgress.value = true
        } else {
            Timber.d("init Adding new HomeUnit")
        }
    }

    fun clearMinValue(): Task<Void>? {
        Timber.d("clearMinValue homeUnit: ${homeUnit?.value}")
        return homeUnit?.value?.let (homeRepository::clearMinHomeUnitValue)
    }

    fun clearMaxValue(): Task<Void>? {
        Timber.d("clearMaxValue homeUnit: ${homeUnit?.value}")
        return homeUnit?.value?.let (homeRepository::clearMaxHomeUnitValue)
    }

    fun noChangesMade(): Boolean {
        return homeUnit?.value?.let { unit ->
            unit.name == name.value
                    && unit.type == type.value
                    && unit.room == roomName.value
                    && unit.hwUnitName == hwUnitName.value
                    && unit.secondHwUnitName == secondHwUnitName.value
                    && unit.firebaseNotify == firebaseNotify.value/* &&
            unit.unitsTasks == unitTaskList.value*/
        } ?: true
        /*name.value.isNullOrEmpty()
        ?: type.value.isNullOrEmpty()
        ?: roomName.value.isNullOrEmpty()
        ?: hwUnitName.value.isNullOrEmpty() ?: true*/
    }

    fun actionEdit() {
        isEditMode.value = true
    }

    /**
     * return true if we want to exit [HomeUnitDetailFragment]
     */
    fun actionDiscard(): Boolean {
        return if (homeUnit == null) {
            true
        } else {
            isEditMode.value = false
            homeUnit.value?.let { unit ->
                name.value = unit.name
                type.value = unit.type
                roomName.value = unit.room
                hwUnitName.value = unit.hwUnitName
                secondHwUnitName.value = unit.secondHwUnitName
                firebaseNotify.value = unit.firebaseNotify
            }
            false
        }
    }

    /**
     * first return param is message Res Id, second return param if present will show dialog with this resource Id as a confirm button text, if not present Snackbar will be show.
     */
    fun actionSave(): Pair<Int, Int?> {
        Timber.d("actionSave addingNewUnit: ${homeUnit == null} name.value: ${name.value}")
        if (homeUnit == null) {
            // Adding new HomeUnit
            when {
                name.value?.trim().isNullOrEmpty() -> return Pair(
                        R.string.add_edit_home_unit_empty_name, null)
                type.value?.trim()
                        .isNullOrEmpty() -> return Pair(
                        R.string.add_edit_home_unit_empty_unit_type, null)
                /*hwUnitName.value?.trim()
                        .isNullOrEmpty() -> return Pair(
                        R.string.add_edit_home_unit_empty_unit_hw_unit, null)*/
                type.value?.trim() == HOME_LIGHT_SWITCHES && secondHwUnitName.value?.trim()
                        .isNullOrEmpty() -> return Pair(
                        R.string.add_edit_home_unit_empty_unit_second_hw_unit, null)
                homeUnitOfSelectedTypeList.find { unit -> unit.name == name.value?.trim() } != null -> {
                    //This name is already used
                    Timber.d("This name is already used")
                    return Pair(R.string.add_edit_home_unit_name_already_used, null)
                }
            }
        } else {
            // Editing existing HomeUnit
            homeUnit.value?.let { unit ->
                return if (name.value?.trim().isNullOrEmpty()) {
                    Pair(R.string.add_edit_home_unit_empty_name, null)
                } else if (name.value?.trim() != unit.name && homeUnitOfSelectedTypeList.find { it.name == name.value?.trim() } != null) {
                    return Pair(R.string.add_edit_home_unit_name_already_used, null)
                } else if (name.value?.trim() != unit.name || type.value?.trim() != unit.type) {
                    Pair(R.string.add_edit_home_unit_save_with_delete, R.string.overwrite)
                } else if (noChangesMade()) {
                    Pair(R.string.add_edit_home_unit_no_changes, null)
                } else {
                    Pair(R.string.add_edit_home_unit_overwrite_changes, R.string.overwrite)
                }
            }
        }
        // new Home Unit adding just show Save Dialog
        return Pair(R.string.add_edit_home_unit_save_changes, R.string.menu_save)
    }

    fun deleteHomeUnit(): Task<Void>? {
        Timber.d(
                "deleteHomeUnit homeUnit: ${homeUnit?.value} homeRepositoryTask.isComplete: ${homeRepositoryTask?.isComplete}")
        homeRepositoryTask = homeUnit?.value?.let { unit ->
            showProgress.value = true
            homeRepository.deleteHomeUnit(unit)
        }?.addOnCompleteListener {
            Timber.d("Task completed")
            showProgress.value = false
        }
        return homeRepositoryTask
    }

    fun saveChanges(): Task<Void>? {
        Timber.d(
                "saveChanges homeUnit: ${homeUnit?.value} homeRepositoryTask.isComplete: ${homeRepositoryTask?.isComplete}")
        homeRepositoryTask = homeUnit?.value?.let { unit ->
            showProgress.value = true
            Timber.e("Save all changes")
            doSaveChanges().apply {
                if (name.value != unit.name || type.value != unit.type) {
                    Timber.d(
                            "Name or type changed will need to delete old value name=${name.value}, type = ${type.value}")
                    // delete old HomeUnit
                    this?.continueWithTask { homeRepository.deleteHomeUnit(unit) }
                }
            }
        } ?: doSaveChanges()?.addOnCompleteListener {
            Timber.d("Task completed")
            showProgress.value = false
        }
        return homeRepositoryTask
    }

    private fun doSaveChanges(): Task<Void>? {
        return name.value?.let { name ->
            type.value?.let { type ->
                roomName.value?.let { room ->
                    //hwUnitName.value?.let { hwUnitName ->
                        firebaseNotify.value?.let { firebaseNotify ->
                            //unitTaskList.value?.let { unitTaskList ->
                            showProgress.value = true
                            homeRepository.saveHomeUnit(
                                    HomeUnit(name = name, type = type, room = room,
                                            hwUnitName = hwUnitName.value,
                                            secondHwUnitName = secondHwUnitName.value,
                                            firebaseNotify = firebaseNotify,
                                            value = homeUnit?.value?.value,
                                            lastUpdateTime = homeUnit?.value?.lastUpdateTime,
                                            unitsTasks = unitTaskList.value?.toMutableMap()?.also {
                                                it.remove("")
                                            } ?: HashMap()))?.continueWithTask {
                                if (type == HOME_LIGHT_SWITCHES) {
                                    homeRepository.saveUnitTask(type, name,
                                            UnitTask(
                                                    name = name,
                                                    homeUnitName = name,
                                                    homeUnitType = type
                                            ))
                                } else {
                                    Tasks.forResult(null)
                                }
                            }
                            //}
                        }
                    //}
                }
            }
        }
    }
}
