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
class HomeUnitDetailViewModel(
        val homeRepository: HomeInformationRepository,
        val roomName: String,
        val unitName: String,
        val unitType: String
) : ViewModel() {

    val unitTaskListAdapter = UnitTaskListAdapter()

    val showProgress: MutableLiveData<Boolean>

    val isEditMode: MutableLiveData<Boolean> = MutableLiveData()

    val homeUnit: LiveData<HomeUnit<Any?>>?

    val name: MutableLiveData<String>
    val type: MutableLiveData<String>
    val room: MutableLiveData<String>
    val hwUnitName: MutableLiveData<String>
    val value: MutableLiveData<String>
    val firebaseNotify: MutableLiveData<Boolean>
    val unitTaskList: LiveData<List<UnitTask>>


    val typeList = HOME_STORAGE_UNITS
    val roomNameList: LiveData<List<String>>                                // RoomListLiveData
    val hwUnitNameList: LiveData<List<String>>                              // HwUnitListLiveData

    var homeRepositoryTask: Task<Void>? = null

    // used for checking if given homeUnit name is not already used
    var homeUnitNameList: MediatorLiveData<MutableList<String>>             // HomeUnitListLiveData

    init {
        Timber.d("init unitName: $unitName unitType: $unitType roomName: $roomName")

        if (unitType.isNotEmpty() && unitName.isNotEmpty()) {
            Timber.d("init Editing existing HomeUnit")
            homeUnit = homeRepository.homeUnitLiveData(unitType, unitName)
            name = Transformations.map(homeUnit) { unit -> unit.name } as MutableLiveData<String>
            type = Transformations.map(homeUnit) { unit -> unit.firebaseTableName } as MutableLiveData<String>
            room = Transformations.map(homeUnit) { unit -> unit.room } as MutableLiveData<String>
            hwUnitName = Transformations.map(homeUnit) { unit -> unit.hardwareUnitName } as MutableLiveData<String>
            value = Transformations.map(homeUnit) { unit -> unit.value.toString() } as MutableLiveData<String>
            firebaseNotify = Transformations.map(homeUnit) { unit -> unit.firebaseNotify ?: false } as MutableLiveData<Boolean>

            showProgress = Transformations.map(homeUnit) { false } as MutableLiveData<Boolean>

            showProgress.value = true
            isEditMode.value = false
        } else {
            Timber.d("init Adding new HomeUnit")
            homeUnit = null
            name = MutableLiveData()
            type = MutableLiveData()
            room = MutableLiveData()
            hwUnitName = MutableLiveData()
            value = MutableLiveData()
            firebaseNotify = MutableLiveData()

            showProgress = MutableLiveData()

            room.value = roomName
            firebaseNotify.value = false

            showProgress.value = false
            isEditMode.value = true
        }

        // Decide how to handle this list
        unitTaskList =
                if (unitType.isNotEmpty() && unitName.isNotEmpty())
                    homeRepository.unitTaskListLiveData(unitType, unitName)
                else MutableLiveData()

        // LiveDatas for editing mode
        roomNameList =
                Transformations.switchMap(isEditMode) { edit ->
                    Timber.d("init roomNameList isEditMode edit: $edit")
                    if (edit)
                        Transformations.map(homeRepository.roomsLiveData()) { list -> list.map(Room::name) }
                    else MutableLiveData()
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
                    typeList.forEach { type ->
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
        return homeUnit?.value?.let { unit ->
            unit.name == name.value &&
            unit.firebaseTableName == type.value &&
            unit.room == room.value &&
            unit.hardwareUnitName == hwUnitName.value &&
            unit.firebaseNotify == firebaseNotify.value &&
            unit.unitsTasks == unitTaskList.value
        } ?: name.value?.isEmpty()
        /*?: type.value?.isEmpty()
        ?: room.value?.isEmpty()
        ?: hwUnitName.value?.isEmpty()*/
        ?: true
    }

    fun actionEdit() {
        isEditMode.value = true
    }

    /**
     * return true if we want to exit [HomeUnitDetailFragment]
     */
    fun actionDiscard(): Boolean {
        return if (unitName.isNotEmpty()) {
            isEditMode.value = false
            homeUnit?.value?.let { unit ->
                name.value = unit.name
                type.value = unit.firebaseTableName
                room.value = unit.room
                hwUnitName.value = unit.hardwareUnitName
                firebaseNotify.value = unit.firebaseNotify
            }
            false
        } else {
            true
        }
    }

    /**
     * first return param is message Res Id, second return param if present will show dialog with this resource Id as a confirm button text, if not present Snackbar will be show.
     */
    fun actionSave(): Pair<Int, Int?> {
        Timber.d("tyrSaveChanges")
        if (unitName.isEmpty() && unitType.isEmpty()) {
            // Adding new HomeUnit
            if (name.value?.trim()?.isEmpty() == true) {
                return Pair(R.string.add_edit_home_unit_empty_name, null)
            } else if (homeUnitNameList.value?.contains(name.value?.trim()) == true) {
                //This name is already used
                Timber.d("This name is already used")
                return Pair(R.string.add_edit_home_unit_name_already_used, null)
            }
        } else {
            // Editing existing HomeUnit
            homeUnit?.value?.let { unit ->
                if (name.value?.trim()?.isEmpty() == true) {
                    return Pair(R.string.add_edit_home_unit_empty_name, null)
                } else if (unitName != unit.name || unitType != unit.firebaseTableName) {
                    return Pair(R.string.add_edit_home_unit_save_with_delete, R.string.overwrite)
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
        Timber.d("deleteHomeUnit homeUnit: ${homeUnit?.value} homeRepositoryTask.isComplete: ${homeRepositoryTask?.isComplete}")
        homeRepositoryTask = homeUnit?.value?.let { unit ->
            showProgress.value = true
            homeRepository.deleteHomeUnit(unit)
        }?.addOnCompleteListener { task ->
            sleep(1000)
            Timber.d("Task completed")
            showProgress.value = false
        }
        return homeRepositoryTask
    }

    fun saveChanges(): Task<Void>? {
        Timber.d("saveChanges homeUnit: ${homeUnit?.value} homeRepositoryTask.isComplete: ${homeRepositoryTask?.isComplete}")
        homeRepositoryTask = homeUnit?.value?.let { unit ->
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
                                    doSaveChanges()?: task
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
        }
        return homeRepositoryTask
    }

    fun doSaveChanges(): Task<Void>?{
        return name.value?.let { name ->
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
        }
    }
}
