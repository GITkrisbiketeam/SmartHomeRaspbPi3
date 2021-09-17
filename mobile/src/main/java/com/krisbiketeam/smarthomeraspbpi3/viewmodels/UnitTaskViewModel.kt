package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HOME_ACTION_STORAGE_UNITS
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.TRIGGER_TYPE_LIST
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.UnitTask
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.UnitTaskHomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_HUMIDITY
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_PRESSURES
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_TEMPERATURES
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomDetailFragment
import com.krisbiketeam.smarthomeraspbpi3.ui.UnitTaskFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import timber.log.Timber


/**
 * The ViewModel used in [RoomDetailFragment].
 */
@ExperimentalCoroutinesApi
class UnitTaskViewModel(
        private val homeRepository: FirebaseHomeInformationRepository,
        taskName: String,
        private val unitName: String,
        private val unitType: String
) : ViewModel() {
    private val addingNewUnit = taskName.isEmpty()

    val isBooleanApplySensor = MutableStateFlow(!(unitType == HOME_TEMPERATURES || unitType == HOME_PRESSURES || unitType == HOME_HUMIDITY))

    // Helper LiveData for UnitTaskList
    private val unitTaskList: StateFlow<Map<String, UnitTask>> = homeRepository.unitTaskListFlow(unitType, unitName).flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())

    private val unitTask: StateFlow<UnitTask?> =
            (if (addingNewUnit) flowOf(null) else unitTaskList.map { taskList -> taskList[taskName] }.onEach { unitTask ->
                Timber.e("unitTask changed:$unitTask")
                showProgress.value = false
                name.value = unitTask?.name ?: ""
                homeUnitsTypeName.value = unitTask?.homeUnitsList?.flatMapToString()
                trigger.value = unitTask?.trigger
                resetOnInverseTrigger.value = unitTask?.resetOnInverseTrigger
                inverse.value = unitTask?.inverse
                startTime.value = unitTask?.startTime
                endTime.value = unitTask?.endTime
                delay.value = unitTask?.delay
                duration.value = unitTask?.duration
                threshold.value = unitTask?.threshold.toString()
                hysteresis.value = unitTask?.hysteresis.toString()
                periodically.value = unitTask?.periodically
                periodicallyOnlyHw.value = unitTask?.periodicallyOnlyHw
                disabled.value = unitTask?.disabled
            }.flowOn(Dispatchers.IO)).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private var homeRepositoryTask: Task<Void>? = null

    val showProgress: MutableStateFlow<Boolean> = MutableStateFlow(!addingNewUnit)

    private val _isEditMode: MutableStateFlow<Boolean> = MutableStateFlow(addingNewUnit)

    val isEditMode: StateFlow<Boolean> = unitTask.flatMapLatest { _isEditMode }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), _isEditMode.value)

    val name: MutableStateFlow<String> = MutableStateFlow("")

    /*val homeUnitTypeList = HOME_ACTION_STORAGE_UNITS
    val homeUnitType: MutableLiveData<String?> = if (addingNewUnit) MutableLiveData() else Transformations.map(unitTask) { unit -> unit?.homeUnitType } as MutableLiveData<String?>

    // List with all available HomeUnits to be used for this Task
    @ExperimentalCoroutinesApi
    val homeUnitNameList = Transformations.switchMap(isEditMode) { edit ->      // HomeUnitListLiveData
        Timber.d("init homeUnitList isEditMode edit: $edit")
        if (edit) {
            Transformations.switchMap(homeUnitType) { type ->
                homeRepository.homeUnitListFlow(type).map { homeUnitList ->
                    homeUnitList.map(HomeUnit<Any>::name)
                }.asLiveData(Dispatchers.Default)
            }
        } else MutableLiveData(emptyList())
    }
    val homeUnitName: MutableLiveData<String?> = if (addingNewUnit) MutableLiveData() else Transformations.map(unitTask) { unit -> unit?.homeUnitName } as MutableLiveData<String?>*/

    // Decide how to handle this list
    val homeUnitsTypeNameList: StateFlow<List<String>> = _isEditMode.flatMapLatest { edit ->      // HomeUnitListLiveData
        Timber.d("init homeUnitList isEditMode edit: $edit")
        if (edit) {
            combine(HOME_ACTION_STORAGE_UNITS.map { type ->
                homeRepository.homeUnitListFlow(type)
            }) { array ->
                array.flatMap {
                    it.map { homeUnit ->
                        "${homeUnit.type}.${homeUnit.name}"
                    }
                }
            }
        } else {
            flowOf(emptyList())
        }
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val homeUnitsTypeName: MutableStateFlow<String?> = MutableStateFlow(null)

    val triggerTypeList = TRIGGER_TYPE_LIST
    val trigger: MutableStateFlow<String?> = MutableStateFlow(null)
    val resetOnInverseTrigger: MutableStateFlow<Boolean?> = MutableStateFlow(null)

    val inverse: MutableStateFlow<Boolean?> = MutableStateFlow(null)

    val startTime: MutableStateFlow<Long?> = MutableStateFlow(null)
    val endTime: MutableStateFlow<Long?> = MutableStateFlow(null)

    val delay: MutableStateFlow<Long?> = MutableStateFlow(null)
    val duration: MutableStateFlow<Long?> = MutableStateFlow(null)

    val threshold: MutableStateFlow<String?> = MutableStateFlow(null)
    val hysteresis: MutableStateFlow<String?> = MutableStateFlow(null)

    val periodically: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    val periodicallyOnlyHw: MutableStateFlow<Boolean?> = MutableStateFlow(null)

    val disabled: MutableStateFlow<Boolean?> = MutableStateFlow(null)

    val startTimeVisible: StateFlow<Boolean> = _isEditMode.flatMapLatest { edit ->
        if (edit) {
            delay.map {
                it == null || it <= 0
            }
        } else startTime.map {
            it != null && it > 0
        }
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    val endTimeVisible: StateFlow<Boolean> = _isEditMode.flatMapLatest { edit ->
        if (edit) {
            duration.map {
                it == null || it <= 0
            }
        } else endTime.map {
            it != null && it > 0
        }
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    val delayVisible: StateFlow<Boolean> = _isEditMode.flatMapLatest { edit ->
        if (edit) {
            startTime.map {
                it == null || it <= 0
            }
        } else delay.map {
            it != null && it > 0
        }
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    val durationVisible: StateFlow<Boolean> = _isEditMode.flatMapLatest { edit ->
        if (edit) {
            endTime.map {
                it == null || it <= 0
            }
        } else duration.map {
            it != null && it > 0
        }
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)


    init {
        Timber.d("init taskName: $taskName")

        if (!addingNewUnit) {
            Timber.d("init Editing existing HomeUnit")
            showProgress.value = true
        }
    }

    fun noChangesMade(): Boolean {
        Timber.d("noChangesMade unitTask?.value: ${unitTask.value}")
        Timber.d("noChangesMade name.value: ${name.value}")
        Timber.d("noChangesMade homeUnitsTypeName: ${homeUnitsTypeName.value}")

        return unitTask.value?.let { unit ->
            unit.name == name.value &&
                    unit.homeUnitsList.flatMapToString() == homeUnitsTypeName.value &&
                    unit.trigger == trigger.value &&
                    unit.resetOnInverseTrigger == resetOnInverseTrigger.value &&
                    unit.inverse == inverse.value &&
                    unit.delay == delay.value &&
                    unit.duration == duration.value &&
                    unit.periodically == periodically.value &&
                    unit.periodicallyOnlyHw == periodicallyOnlyHw.value &&
                    unit.startTime == startTime.value &&
                    unit.endTime == endTime.value &&
                    unit.threshold == threshold.value?.toFloatOrNull() &&
                    unit.hysteresis == hysteresis.value?.toFloatOrNull() &&
                    unit.disabled == disabled.value
        } ?: name.value.isEmpty() || homeUnitsTypeName.value.isNullOrEmpty() /*|| homeUnitType.value.isNullOrEmpty() || homeUnitName.value.isNullOrEmpty() || hwUnitName.value.isNullOrEmpty())*/
    }

    fun actionEdit() {
        _isEditMode.value = true
    }

    /**
     * return true if we want to exit [UnitTaskFragment]
     */
    fun actionDiscard(): Boolean {
        return if (addingNewUnit) {
            true
        } else {
            _isEditMode.value = false
            unitTask.value?.let { unit ->
                name.value = unit.name
                homeUnitsTypeName.value = unit.homeUnitsList.flatMapToString()
                trigger.value = unit.trigger
                resetOnInverseTrigger.value = unit.resetOnInverseTrigger
                inverse.value = unit.inverse
                delay.value = unit.delay
                duration.value = unit.duration
                periodically.value = unit.periodically
                periodicallyOnlyHw.value = unit.periodicallyOnlyHw
                startTime.value = unit.startTime
                endTime.value = unit.endTime
                threshold.value = unit.threshold.toString()
                hysteresis.value = unit.hysteresis.toString()
                disabled.value = unit.disabled
            }
            false
        }
    }

    /**
     * first return param is message Res Id, second return param if present will show dialog with this resource Id as a confirm button text, if not present Snackbar will be show.
     */
    fun actionSave(): Pair<Int, Int?> {
        Timber.d("actionSave addingNewUnit: $addingNewUnit name.value: ${name.value}")
        return if (addingNewUnit) {
            // Adding new HomeUnit
            when {
                name.value.trim().isEmpty() -> Pair(R.string.unit_task_empty_name, null)
                homeUnitsTypeName.value.mapToList().isNullOrEmpty() -> Pair(R.string.unit_task_empty_home_unit_name, null)
                unitTaskList.value.keys.contains(name.value.trim()) -> {
                    //This name is already used
                    Timber.d("This name is already used")
                    Pair(R.string.unit_task_name_already_used, null)
                }
                else -> Pair(R.string.unit_task_save_changes, R.string.menu_save)
            }
        } else {
            // Editing existing HomeUnit
            unitTask.value?.let {
                return when {
                    name.value.trim().isEmpty() || homeUnitsTypeName.value.mapToList().isNullOrEmpty() /* || homeUnitType.value.isNullOrEmpty() || homeUnitName.value.isNullOrEmpty() && hwUnitName.value.isNullOrEmpty()*/ -> {
                        Pair(R.string.unit_task_empty_name_unit_or_hw, null)
                    }
                    noChangesMade() -> {
                        Pair(R.string.add_edit_home_unit_no_changes, null)
                    }
                    else -> {
                        Pair(R.string.add_edit_home_unit_overwrite_changes, R.string.overwrite)
                    }
                }
            } ?: Pair(R.string.unit_task_error_empty_editing_unit_task, null)
        }
    }

    fun deleteUnitTask(): Task<Void>? {
        Timber.d("deleteHomeUnit homeUnit: ${unitTask.value} homeRepositoryTask.isComplete: ${homeRepositoryTask?.isComplete}")
        homeRepositoryTask = unitTask.value?.let { unit ->
            showProgress.value = true
            homeRepository.deleteUnitTask(unitType, unitName, unit)
        }?.addOnCompleteListener {
            Timber.d("Task completed")
            showProgress.value = false
        }
        return homeRepositoryTask
    }

    fun saveChanges(): Task<Void>? {
        Timber.d("saveChanges homeUnit: ${unitTask.value} homeRepositoryTask.isComplete: ${homeRepositoryTask?.isComplete}")
        showProgress.value = true
        Timber.e("Save all changes")
        homeRepositoryTask = doSaveChanges()?.let { task ->
            unitTask.value?.let { unitTask ->
                if (name.value != unitTask.name) {
                    Timber.d("Name changed, will need to delete old value name=${name.value}")
                    // delete old HomeUnit
                    task.continueWithTask {
                        homeRepository.deleteUnitTask(unitType, unitName, unitTask) ?: task
                    }
                } else {
                    task
                }
            } ?: task
        }?.addOnCompleteListener {
            Timber.d("Task completed")
            showProgress.value = false
        } ?: run {
            showProgress.value = false
            null
        }
        return homeRepositoryTask
    }

    private fun doSaveChanges(): Task<Void>? {
        return homeUnitsTypeName.value?.mapToList()?.let { homeUnitsTypeNameList ->
            homeRepository.saveUnitTask(unitType, unitName,
                    UnitTask(
                            name = name.value,
                            homeUnitsList = homeUnitsTypeNameList,
                            inverse = inverse.value,
                            trigger = trigger.value,
                            resetOnInverseTrigger = resetOnInverseTrigger.value,
                            delay = delay.value,
                            duration = duration.value,
                            periodically = periodically.value,
                            periodicallyOnlyHw = periodicallyOnlyHw.value,
                            startTime = startTime.value,
                            endTime = endTime.value,
                            threshold = threshold.value?.toFloatOrNull(),
                            hysteresis = hysteresis.value?.toFloatOrNull(),
                            disabled = disabled.value
                    ))
        }
    }
}

private fun List<UnitTaskHomeUnit>?.flatMapToString(): String? {
    return this?.joinToString {
        "${it.type}.${it.name}"
    }
}

private fun String?.mapToList(): List<UnitTaskHomeUnit>? {
    return this?.split(", ", ignoreCase = true)?.mapNotNull { item ->
        val taskItem = item.split(".", ignoreCase = true)
        if (taskItem.size == 2) {
            UnitTaskHomeUnit(taskItem[0], taskItem[1])
        } else {
            null
        }
    }
}