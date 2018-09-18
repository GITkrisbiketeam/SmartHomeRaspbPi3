package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.lifecycle.*
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.krisbiketeam.data.storage.HomeInformationRepository
import com.krisbiketeam.data.storage.dto.*
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.adapters.UnitTaskListAdapter
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomDetailFragment
import timber.log.Timber
import java.lang.Thread.sleep


/**
 * The ViewModel used in [RoomDetailFragment].
 */
class UnitTaskViewModel(
        private val homeRepository: HomeInformationRepository,
        taskName: String,
        unitName: String,
        unitType: String
) : ViewModel() {

    val showProgress: MutableLiveData<Boolean>

    val isEditMode: MutableLiveData<Boolean> = MutableLiveData()

    // Helper LiveData for UnitTaskList
    val unitTaskList = homeRepository.unitTaskListLiveData(unitType, unitName)

    val unitTask: LiveData<UnitTask>?

    var name: MutableLiveData<String>
    var homeUnitName: MutableLiveData<String>
    var hwUnitName: MutableLiveData<String>
    var delay: MutableLiveData<Long>
    var duration: MutableLiveData<Long>
    var period: MutableLiveData<Long>
    var startTime: MutableLiveData<Long>
    var endTime: MutableLiveData<Long>
    var inverse: MutableLiveData<Boolean>


    var homeRepositoryTask: Task<Void>? = null

    // used for checking if given homeUnit name is not already used
    var unitTaskNameList: LiveData<List<String>>                              // UnitTaskListLiveData

    val addingNewUnit = unitName.isEmpty() && unitType.isEmpty()


    // used for checking if given homeUnit name is not already used
    var homeUnitNameList: MediatorLiveData<MutableList<String>>             // HomeUnitListLiveData
    val hwUnitNameList: LiveData<List<String>>                              // HwUnitListLiveData

    init {
        Timber.d("init unitName: $unitName unitType: $unitType taskName: $taskName")

        if (!addingNewUnit) {
            Timber.d("init Editing existing HomeUnit")
            unitTask = Transformations.map(unitTaskList) { taskList -> taskList.find { it.name == taskName }            }

            name = Transformations.map(unitTask) { unit -> unit.name } as MutableLiveData<String>
            homeUnitName = Transformations.map(unitTask) { unit -> unit.homeUnitName } as MutableLiveData<String>
            hwUnitName = Transformations.map(unitTask) { unit -> unit.hwUnitName } as MutableLiveData<String>
            delay = Transformations.map(unitTask) { unit -> unit.delay } as MutableLiveData<Long>
            duration = Transformations.map(unitTask) { unit -> unit.duration } as MutableLiveData<Long>
            period = Transformations.map(unitTask) { unit -> unit.period } as MutableLiveData<Long>
            startTime = Transformations.map(unitTask) { unit -> unit.startTime } as MutableLiveData<Long>
            endTime = Transformations.map(unitTask) { unit -> unit.endTime } as MutableLiveData<Long>
            inverse = Transformations.map(unitTask) { unit -> unit.inverse } as MutableLiveData<Boolean>
            showProgress = Transformations.map(unitTask) { false } as MutableLiveData<Boolean>

            showProgress.value = true
            isEditMode.value = false
        } else {
            Timber.d("init Adding new HomeUnit")
            unitTask = null

            name = MutableLiveData()
            homeUnitName = MutableLiveData()
            hwUnitName = MutableLiveData()
            delay = MutableLiveData()
            duration = MutableLiveData()
            period = MutableLiveData()
            startTime = MutableLiveData()
            endTime = MutableLiveData()
            inverse = MutableLiveData()

            showProgress = MutableLiveData()

            showProgress.value = false
            isEditMode.value = true
        }

        // used for checking if given homeUnit name is not already used
        unitTaskNameList = Transformations.switchMap(isEditMode) { edit ->
            Timber.d("init unitTaskNameList isEditMode edit: $edit")
            if (edit) {
                Transformations.map(unitTaskList) { list -> list.map(UnitTask::name) }
            } else MutableLiveData()
        }

        hwUnitNameList =
                Transformations.switchMap(isEditMode) { edit ->
                    Timber.d("init hwUnitNameList isEditMode edit: $edit")
                    if (edit)
                        Transformations.map(homeRepository.hwUnitListLiveData()) { list -> list.map(HwUnit::name) }
                    else MutableLiveData()
                }

        // used for checking if given homeUnit name is not already used
        homeUnitNameList = Transformations.switchMap(isEditMode) { edit ->
            Timber.d("init homeUnitNameList isEditMode edit: $edit")
            if (edit) {
                MediatorLiveData<MutableList<String>>().apply {
                    HOME_STORAGE_UNITS.forEach { type ->
                        addSource(homeRepository.homeUnitListLiveData(type)) { homeUnitList ->
                            value = value ?: ArrayList()
                            value?.addAll(homeUnitList?.map { it.name } ?: emptyList())
                        }
                    }
                }
            } else MediatorLiveData()
        } as MediatorLiveData<MutableList<String>>
    }

    fun noChangesMade(): Boolean {
        return unitTask?.value?.let { unit ->
            unit.name == name.value &&
            unit.homeUnitName == homeUnitName.value &&
            unit.hwUnitName == hwUnitName.value &&
            unit.delay == delay.value &&
            unit.period == period.value &&
            unit.startTime == startTime.value &&
            unit.endTime == endTime.value &&
            unit.inverse == inverse.value
        } ?: name.value.isNullOrEmpty()
        ?: (homeUnitName.value.isNullOrEmpty() && hwUnitName.value.isNullOrEmpty())
        ?: true
    }

    fun actionEdit() {
        isEditMode.value = true
    }

    /**
     * return true if we want to exit [HomeUnitDetailFragment]
     */
    fun actionDiscard(): Boolean {
        return if (addingNewUnit) {
            true
        } else {
            isEditMode.value = false
            unitTask?.value?.let { unit ->
                name.value = unit.name
                homeUnitName.value = unit.homeUnitName
                hwUnitName.value = unit.hwUnitName
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
        Timber.d("tyrSaveChanges addingNewUnit: $addingNewUnit name.value: ${name.value}")
        if (addingNewUnit) {
            // Adding new HomeUnit
            if (name.value?.trim().isNullOrEmpty()) {
                return Pair(R.string.add_edit_home_unit_empty_name, null)
            } else if (unitTaskNameList.value?.contains(name.value?.trim()) == true) {
                //This name is already used
                Timber.d("This name is already used")
                return Pair(R.string.add_edit_home_unit_name_already_used, null)
            }
        } else {
            // Editing existing HomeUnit
            unitTask?.value?.let { unit ->
                if (name.value?.trim().isNullOrEmpty() && homeUnitName.value.isNullOrEmpty() && hwUnitName.value.isNullOrEmpty()) {
                    return Pair(R.string.add_edit_home_unit_empty_name, null)
                } else if (noChangesMade()) {
                    return Pair(R.string.add_edit_home_unit_no_changes, null)
                } else {
                    return Pair(R.string.add_edit_home_unit_overwrite_changes, R.string.overwrite)
                }
            }
        }
        // new Home Unit adding just show Save Dialog
        return Pair(R.string.add_edit_home_unit_save_changes, R.string.menu_save)
    }

    fun deleteHomeUnit(): Task<Void>? {
        /*Timber.d("deleteHomeUnit homeUnit: ${unitTask?.value} homeRepositoryTask.isComplete: ${homeRepositoryTask?.isComplete}")
        homeRepositoryTask = unitTask?.value?.let { unit ->
            showProgress.value = true
            homeRepository.deleteHomeUnit(unit)
        }?.addOnCompleteListener { task ->
            sleep(1000)
            Timber.d("Task completed")
            showProgress.value = false
        }*/
        return homeRepositoryTask
    }

    fun saveChanges(): Task<Void>? {
        /*Timber.d("saveChanges homeUnit: ${unitTask?.value} homeRepositoryTask.isComplete: ${homeRepositoryTask?.isComplete}")
        homeRepositoryTask = unitTask?.value?.let { unit ->
            showProgress.value = true
            if (name.value != unit.name || type.value != unit.firebaseTableName) {
                Timber.d("Name or type changed will need to delete old value name=${name.value}, firebaseTableName = ${type.value}")
                // delete old HomeUnit
                homeRepository.deleteHomeUnit(unit)
                        .continueWithTask(object : Continuation<Void, Task<Void>> {
                            override fun then(task: Task<Void>): Task<Void> {
                                return if (task.isCanceled) {
                                    task
                                } else {
                                    doSaveChanges() ?: task
                                }
                            }
                        })
            } else {
                Timber.e("Save all changes")
                doSaveChanges()
            }
        } ?: doSaveChanges()?.addOnCompleteListener { task ->
            sleep(1000)
            Timber.d("Task completed")
            showProgress.value = false
        }*/
        return homeRepositoryTask
    }

    fun doSaveChanges(): Task<Void>? {
        return null /*name.value?.let { name ->
            type.value?.let { type ->
                room.value?.let { room ->
                    hwUnitName.value?.let { hwUnitName ->
                        firebaseNotify.value?.let { firebaseNotify ->
                            unitTaskList.value?.let { unitTaskList ->
                                showProgress.value = true
                                homeRepository.saveHomeUnit(HomeUnit<Boolean>(
                                        name = name,
                                        firebaseTableName = type,
                                        room = room,
                                        hardwareUnitName = hwUnitName,
                                        firebaseNotify = firebaseNotify,
                                        value = value.value?.toBoolean(),
                                        unitsTasks = unitTaskList
                                ))
                            }
                        }
                    }
                }
            }
        }*/
    }
}
