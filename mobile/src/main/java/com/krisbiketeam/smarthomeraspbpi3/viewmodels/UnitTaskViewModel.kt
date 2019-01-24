package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.*
import com.google.android.gms.tasks.Task
import com.krisbiketeam.smarthomeraspbpi3.common.storage.HomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HOME_STORAGE_UNITS
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.UnitTask
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomDetailFragment
import com.krisbiketeam.smarthomeraspbpi3.ui.UnitTaskFragment
import timber.log.Timber
import java.lang.Thread.sleep


/**
 * The ViewModel used in [RoomDetailFragment].
 */
class UnitTaskViewModel(
        private val homeRepository: HomeInformationRepository,
        taskName: String,
        private val unitName: String,
        private val unitType: String
) : ViewModel() {

    val showProgress: MutableLiveData<Boolean>

    val isEditMode: MutableLiveData<Boolean> = MutableLiveData()

    // Helper LiveData for UnitTaskList
    private val unitTaskList = homeRepository.unitTaskListLiveData(unitType, unitName)

    val unitTask: LiveData<UnitTask>?

    var name: MutableLiveData<String>
    var homeUnitName: MutableLiveData<String>
//    var hwUnitName: MutableLiveData<String>
    var delay: MutableLiveData<Long>
    var duration: MutableLiveData<Long>
    var period: MutableLiveData<Long>
    var startTime: MutableLiveData<Long>
    var endTime: MutableLiveData<Long>
    var inverse: MutableLiveData<Boolean>


    private var homeRepositoryTask: Task<Void>? = null

    // used for checking if given homeUnit name is not already used
    private var unitTaskNameList: LiveData<List<String>>                              // UnitTaskListLiveData

    private val addingNewUnit = taskName.isEmpty()


    // used for checking if given homeUnit name is not already used
    var homeUnitNameList: MediatorLiveData<MutableList<String>>             // HomeUnitListLiveData
//    val hwUnitNameList: LiveData<List<String>>                              // HwUnitListLiveData

    init {
        Timber.d("init taskName: $taskName")

        if (!addingNewUnit) {
            Timber.d("init Editing existing HomeUnit")
            unitTask = Transformations.map(unitTaskList) { taskList -> taskList[taskName] }

            name = Transformations.map(unitTask) { unit -> unit?.name } as MutableLiveData<String>
            homeUnitName = Transformations.map(unitTask) { unit -> unit?.homeUnitName } as MutableLiveData<String>
//            hwUnitName = Transformations.map(unitTask) { unit -> unit?.hwUnitName } as MutableLiveData<String>
            delay = Transformations.map(unitTask) { unit -> unit?.delay } as MutableLiveData<Long>
            duration = Transformations.map(unitTask) { unit -> unit?.duration } as MutableLiveData<Long>
            period = Transformations.map(unitTask) { unit -> unit?.period } as MutableLiveData<Long>
            startTime = Transformations.map(unitTask) { unit -> unit?.startTime } as MutableLiveData<Long>
            endTime = Transformations.map(unitTask) { unit -> unit?.endTime } as MutableLiveData<Long>
            inverse = Transformations.map(unitTask) { unit -> unit?.inverse } as MutableLiveData<Boolean>
            showProgress = Transformations.map(unitTask) { false } as MutableLiveData<Boolean>

            showProgress.value = true
            isEditMode.value = false
        } else {
            Timber.d("init Adding new HomeUnit")
            unitTask = null

            name = MutableLiveData()
            homeUnitName = MutableLiveData()
//            hwUnitName = MutableLiveData()
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

        // List with all available HwUnits to be used for this Task
/*        hwUnitNameList =
                Transformations.switchMap(isEditMode) { edit ->
                    Timber.d("init hwUnitNameList isEditMode edit: $edit")
                    if (edit)
                        Transformations.map(homeRepository.hwUnitListLiveData()) { list -> list.map(HwUnit::name) }
                    else MutableLiveData()
                }*/

        // List with all available HomeUnits to be used for this Task
        homeUnitNameList = Transformations.switchMap(isEditMode) { edit ->
            Timber.d("init homeUnitList isEditMode edit: $edit")
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

        // used for checking if given homeUnit name is not already used
        unitTaskNameList = Transformations.switchMap(isEditMode) { edit ->
            Timber.d("init unitTaskNameList isEditMode edit: $edit")
            if (edit) {
                Transformations.map(unitTaskList) { list -> list.values.map(UnitTask::name) }
            } else MutableLiveData()
        }
    }

    fun noChangesMade(): Boolean {
        Timber.d("noChangesMade unitTask?.value: ${unitTask?.value}")
        Timber.d("noChangesMade name.value: ${name.value}")
        Timber.d("noChangesMade homeUnitName.value: ${homeUnitName.value}")
//        Timber.d("noChangesMade hwUnitName.value: ${hwUnitName.value}")

        return unitTask?.value?.let { unit ->
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
            unitTask?.value?.let { unit ->
                name.value = unit.name
                homeUnitName.value = unit.homeUnitName
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
            unitTask?.value?.let {
                return if (name.value?.trim().isNullOrEmpty() || (homeUnitName.value.isNullOrEmpty()/* && hwUnitName.value.isNullOrEmpty()*/)) {
                    Pair(R.string.unit_task_empty_name_unit_or_hw, null)
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
        Timber.d("deleteHomeUnit homeUnit: ${unitTask?.value} homeRepositoryTask.isComplete: ${homeRepositoryTask?.isComplete}")
        homeRepositoryTask = unitTask?.value?.let { unit ->
            showProgress.value = true
            homeRepository.deleteUnitTask(unitType, unitName, unit)
        }?.addOnCompleteListener {
            sleep(1000)
            Timber.d("Task completed")
            showProgress.value = false
        }
        return homeRepositoryTask
    }

    fun saveChanges(): Task<Void>? {
        Timber.d("saveChanges homeUnit: ${unitTask?.value} homeRepositoryTask.isComplete: ${homeRepositoryTask?.isComplete}")
        homeRepositoryTask = unitTask?.value?.let { unitTask ->
            showProgress.value = true
            Timber.e("Save all changes")
            doSaveChanges().apply {
                if (name.value != unitTask.name) {
                    Timber.d("Name changed, will need to delete old value name=${name.value}")
                    // delete old HomeUnit
                    this?.continueWithTask { task -> homeRepository.deleteUnitTask(unitType, unitName, unitTask) }
                }
            }
        } ?: doSaveChanges()?.addOnCompleteListener {
            sleep(1000)
            Timber.d("Task completed")
            showProgress.value = false
        }
        return homeRepositoryTask
    }

    private fun doSaveChanges(): Task<Void>? {
        return name.value?.let { name ->
                    homeRepository.saveUnitTask(unitType, unitName, UnitTask(
                            name = name,
                            homeUnitName = homeUnitName.value,
                            //hwUnitName = hwUnitName.value,
                            delay = delay.value,
                            duration = duration.value,
                            period = period.value,
                            startTime = startTime.value
                    ))
                }
    }
}
