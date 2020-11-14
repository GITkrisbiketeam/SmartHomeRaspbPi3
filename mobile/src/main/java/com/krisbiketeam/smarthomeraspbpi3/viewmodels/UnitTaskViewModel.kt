package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.*
import com.google.android.gms.tasks.Task
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HOME_STORAGE_UNITS
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.UnitTask
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_ACTUATORS
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_BLINDS
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_LIGHTS
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomDetailFragment
import com.krisbiketeam.smarthomeraspbpi3.ui.UnitTaskFragment
import timber.log.Timber


/**
 * The ViewModel used in [RoomDetailFragment].
 */
class UnitTaskViewModel(
        private val homeRepository: FirebaseHomeInformationRepository,
        taskName: String,
        private val unitName: String,
        private val unitType: String
) : ViewModel() {
    private val addingNewUnit = taskName.isEmpty()

    // Helper LiveData for UnitTaskList
    private val unitTaskList: LiveData<Map<String, UnitTask>> = homeRepository.unitTaskListLiveData(unitType, unitName) //LiveData<Map<String, UnitTask>>

    private val unitTask = if (addingNewUnit) MutableLiveData() else Transformations.map(unitTaskList) { taskList -> taskList[taskName] }

    private var homeRepositoryTask: Task<Void>? = null

    val showProgress: MutableLiveData<Boolean> = if (addingNewUnit) MutableLiveData(false) else Transformations.map(unitTask) { false } as MutableLiveData<Boolean>

    val isEditMode: MutableLiveData<Boolean> = MutableLiveData(addingNewUnit)

    val name: MutableLiveData<String?> = if (addingNewUnit) MutableLiveData() else Transformations.map(unitTask) { unit -> unit?.name } as MutableLiveData<String?>
    val homeUnitName: MutableLiveData<String?> = if (addingNewUnit) MutableLiveData() else Transformations.map(unitTask) { unit -> unit?.homeUnitName } as MutableLiveData<String?>
    val delay: MutableLiveData<Long?> = if (addingNewUnit) MutableLiveData() else Transformations.map(unitTask) { unit -> unit?.delay } as MutableLiveData<Long?>
    val duration: MutableLiveData<Long?> = if (addingNewUnit) MutableLiveData() else Transformations.map(unitTask) { unit -> unit?.duration } as MutableLiveData<Long?>
    val period: MutableLiveData<Long?> = if (addingNewUnit) MutableLiveData() else Transformations.map(unitTask) { unit -> unit?.period } as MutableLiveData<Long?>
    val startTime: MutableLiveData<Long?> = if (addingNewUnit) MutableLiveData() else Transformations.map(unitTask) { unit -> unit?.startTime } as MutableLiveData<Long?>
    val endTime: MutableLiveData<Long?> = if (addingNewUnit) MutableLiveData() else Transformations.map(unitTask) { unit -> unit?.endTime } as MutableLiveData<Long?>
    val inverse: MutableLiveData<Boolean?> = if (addingNewUnit) MutableLiveData() else Transformations.map(unitTask) { unit -> unit?.inverse } as MutableLiveData<Boolean?>

    // List with all available HomeUnits to be used for this Task
    val homeUnitNameList = Transformations.switchMap(isEditMode) { edit ->      // HomeUnitListLiveData
        Timber.d("init homeUnitList isEditMode edit: $edit")
        if (edit) {
            MediatorLiveData<MutableList<String>>().apply {
                HOME_STORAGE_UNITS.filter {
                    when (it) {
                        HOME_LIGHTS, HOME_BLINDS, HOME_ACTUATORS -> true
                        else -> false
                    }
                }.forEach { type ->
                    addSource(homeRepository.homeUnitListLiveData(type)) { homeUnitList ->
                        value = value ?: ArrayList()
                        value?.addAll(homeUnitList?.map { it.name } ?: emptyList())
                        postValue(value)
                    }
                }
            }
        } else MediatorLiveData()
    }

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
        Timber.d("noChangesMade homeUnitName: $homeUnitName")
//        Timber.d("noChangesMade hwUnitName.value: ${hwUnitName.value}")

        return unitTask.value?.let { unit ->
            unit.name == name.value &&
                    unit.homeUnitName == homeUnitName.value &&
//            unit.hwUnitName == hwUnitName.value &&
                    unit.delay == delay.value &&
                    unit.period == period.value &&
                    unit.startTime == startTime.value &&
                    unit.endTime == endTime.value &&
                    unit.inverse == inverse.value
        } ?: name.value.isNullOrEmpty()
        ?: homeUnitName.value.isNullOrEmpty()/* && hwUnitName.value.isNullOrEmpty())*/
        ?: true
    }

    fun actionEdit() {
        isEditMode.value = true
    }

    /**
     * return true if we want to exit [UnitTaskFragment]
     */
    fun actionDiscard(): Boolean {
        return if (addingNewUnit) {
            true
        } else {
            isEditMode.value = false
            unitTask.value?.let { unit ->
                name.value = unit.name
//                hwUnitName.value = unit.hwUnitName
                delay.value = unit.delay
                period.value = unit.period
                startTime.value = unit.startTime
                endTime.value = unit.endTime
                inverse.value = unit.inverse
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
                name.value?.trim().isNullOrEmpty() -> Pair(R.string.unit_task_empty_name, null)
                homeUnitName.value.isNullOrEmpty() -> Pair(R.string.unit_task_empty_home_unit_name, null)
                unitTaskList.value?.keys?.contains(name.value?.trim()) == true -> {
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
                    name.value?.trim().isNullOrEmpty() || homeUnitName.value.isNullOrEmpty()/* && hwUnitName.value.isNullOrEmpty()*/ -> {
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
        homeRepositoryTask = unitTask.value?.let { unitTask ->
            showProgress.value = true
            Timber.e("Save all changes")
            doSaveChanges().apply {
                if (name.value != unitTask.name) {
                    Timber.d("Name changed, will need to delete old value name=${name.value}")
                    // delete old HomeUnit
                    this?.continueWithTask { homeRepository.deleteUnitTask(unitType, unitName, unitTask) }
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
            homeUnitName.value?.let { homeUnitName ->
                homeRepository.saveUnitTask(unitType, unitName,
                        UnitTask(
                                name = name,
                                homeUnitName = homeUnitName,
                                //hwUnitName = hwUnitName.value,
                                delay = delay.value,
                                duration = duration.value,
                                period = period.value,
                                startTime = startTime.value
                        ))
            }
        }
    }
}
