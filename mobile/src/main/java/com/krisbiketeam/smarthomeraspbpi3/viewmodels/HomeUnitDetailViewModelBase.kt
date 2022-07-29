package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.adapters.UnitTaskListAdapter
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.ui.HomeUnitDetailFragment
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomDetailFragment
import com.krisbiketeam.smarthomeraspbpi3.utils.getLastUpdateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import timber.log.Timber
import kotlin.collections.set

/**
 * The ViewModel used in [RoomDetailFragment].
 */
@ExperimentalCoroutinesApi
abstract class HomeUnitDetailViewModelBase<T : HomeUnit<Any>>(
    application: Application,
    protected val homeRepository: FirebaseHomeInformationRepository,
    roomName: String?, unitName: String?, unitType: HomeUnitType
) : AndroidViewModel(application) {

    val unitTaskListAdapter = UnitTaskListAdapter(homeRepository, unitName, unitType)

    val homeUnit: StateFlow<T?>? =
        if (unitName.isNullOrEmpty() || unitType == HomeUnitType.UNKNOWN) null else getHomeUnitFlow(
            unitType, unitName
        ).onEach { homeUnit ->
            Timber.e("homeUnit changed:$homeUnit")
            showProgress.value = false
            name.value = homeUnit.name
            type.value = homeUnit.type
            room.value = homeUnit.room
            hwUnitName.value = homeUnit.hwUnitName
            value.value = homeUnit.value.toString()
            lastUpdateTime.value = getLastUpdateTime(application, homeUnit.lastUpdateTime)
            firebaseNotify.value = homeUnit.firebaseNotify
            firebaseNotifyTrigger.value = homeUnit.firebaseNotifyTrigger ?: RISING_EDGE
            showInTaskList.value = homeUnit.showInTaskList

            initializeAdditionalHomeUnitStates(homeUnit)
        }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val showProgress: MutableStateFlow<Boolean> = MutableStateFlow(homeUnit != null)

    val isEditMode: MutableStateFlow<Boolean> = MutableStateFlow(homeUnit == null)

    // TODO check if possible nullable
    val name: MutableStateFlow<String> = MutableStateFlow(unitName ?: "")

    open val typeList = HOME_STORAGE_UNITS.filterNot { it == HomeUnitType.HOME_LIGHT_SWITCHES_V2 }
    open val type: MutableStateFlow<HomeUnitType> = MutableStateFlow(unitType)

    val roomList: StateFlow<List<String>> =
        isEditMode.flatMapLatest { isEdit ->
            if (isEdit) {
                homeRepository.roomListFlow().map { list ->
                    list.map(Room::name)
                }
            } else {
                flowOf(emptyList())
            }
        }.flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    // TODO check if possible nullable
    val room: MutableStateFlow<String> = MutableStateFlow(roomName ?: "")

    val hwUnitNameList: StateFlow<List<Pair<String, Boolean>>> =
        isEditMode.flatMapLatest { isEdit ->
            Timber.d("init hwUnitNameList isEditMode: $isEdit")
            if (isEdit) {
                combine(
                    homeRepository.homeUnitListFlow(),
                    homeRepository.hwUnitListFlow(),
                    type
                ) { homeUnitList, hwUnitList, type ->
                    homeUnitOfSelectedTypeList = homeUnitList.filter {
                        it.type == type
                    }
                    hwUnitList.map {
                        Pair(it.name,
                            homeUnitList.find { unit -> unit.hwUnitName == it.name || (unit is GenericHomeUnit<*> && unit.secondHwUnitName == it.name) } != null)
                    }
                }
            } else {
                flowOf(emptyList())
            }
        }.flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val hwUnitName: MutableStateFlow<String?> = MutableStateFlow(null)

    val value: MutableStateFlow<String> = MutableStateFlow("")
    val lastUpdateTime: MutableStateFlow<String> = MutableStateFlow("")

    val firebaseNotify: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val firebaseNotifyTriggerTypeList = TRIGGER_TYPE_LIST
    val firebaseNotifyTrigger: MutableStateFlow<String?> = MutableStateFlow(RISING_EDGE)
    val showFirebaseNotifyTrigger: StateFlow<Boolean> =
        combine(firebaseNotify, type) { notify, type ->
            notify && HOME_FIREBASE_NOTIFY_STORAGE_UNITS.contains(type)
        }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val showInTaskList: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showInTaskListVisibility: StateFlow<Boolean> =
        combine(hwUnitName, type) { hwUnitName, type ->
            !hwUnitName.isNullOrEmpty() && HOME_ACTION_STORAGE_UNITS.contains(type)
        }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    // Decide how to handle this list
    val unitTaskList: StateFlow<Map<String, UnitTask>> =
        if (homeUnit != null && unitType != HomeUnitType.UNKNOWN && !unitName.isNullOrEmpty()) {
            combine(
                isEditMode,
                homeRepository.unitTaskListFlow(unitType, unitName)
            ) { edit, taskList ->
                Timber.d("init unitTaskList isEditMode edit: $edit")
                if (edit) {
                    Timber.d("init unitTaskList edit: $taskList")
                    // Add empty UnitTask which will be used for adding new UnitTask
                    val newList = taskList.toMutableMap()
                    newList[""] = UnitTask()
                    Timber.d("init unitTaskList edit newList: $newList")

                    newList
                } else {
                    Timber.d("init unitTaskList: $taskList")
                    taskList
                }
            }.flowOn(Dispatchers.IO)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())
        } else {
            MutableStateFlow(emptyMap())
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

    abstract fun getHomeUnitFlow(unitType: HomeUnitType, unitName: String): Flow<T>

    abstract fun initializeAdditionalHomeUnitStates(homeUnit: T)

    fun noChangesMade(): Boolean {
        return homeUnit?.value?.let { unit ->
            unit.name == name.value
                    && unit.type == type.value
                    && unit.room == room.value
                    && unit.hwUnitName == hwUnitName.value
                    && unit.firebaseNotify == firebaseNotify.value
                    && unit.firebaseNotifyTrigger == firebaseNotifyTrigger.value
                    && unit.showInTaskList == showInTaskList.value/* &&
            unit.unitsTasks == unitTaskList.value*/
                    && additionalNoChangesMade(unit)
        } ?: true
        /*name.value.isNullOrEmpty()
        ?: type.value.isNullOrEmpty()
        ?: roomName.value.isNullOrEmpty()
        ?: hwUnitName.value.isNullOrEmpty() ?: true*/
    }

    abstract fun additionalNoChangesMade(homeUnit:T): Boolean

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
                room.value = unit.room
                hwUnitName.value = unit.hwUnitName
                firebaseNotify.value = unit.firebaseNotify
                firebaseNotifyTrigger.value = unit.firebaseNotifyTrigger
                showInTaskList.value = unit.showInTaskList

                restoreAdditionalHomeUnitInitialStates(unit)
            }
            false
        }
    }

    abstract fun restoreAdditionalHomeUnitInitialStates(homeUnit:T)

    /**
     * first return param is message Res Id, second return param if present will show dialog with this resource Id as a confirm button text, if not present Snackbar will be show.
     */
    fun actionSave(): Pair<Int, Int?> {
        Timber.d("actionSave addingNewUnit: ${homeUnit == null} name.value: ${name.value}")
        if (homeUnit == null) {
            // Adding new HomeUnit
            when {
                name.value.trim().isEmpty() -> return Pair(
                    R.string.add_edit_home_unit_empty_name, null
                )
                type.value == HomeUnitType.UNKNOWN -> return Pair(
                    R.string.add_edit_home_unit_empty_unit_type, null
                )
                homeUnitOfSelectedTypeList.find { unit -> unit.name == name.value.trim() } != null -> {
                    //This name is already used
                    Timber.d("This name is already used")
                    return Pair(R.string.add_edit_home_unit_name_already_used, null)
                }
            }
            actionSaveGetCustomSavePair()?.let { return it }
        } else {
            // Editing existing HomeUnit
            homeUnit.value?.let { unit ->
                return if (name.value.trim().isEmpty()) {
                    Pair(R.string.add_edit_home_unit_empty_name, null)
                } else if (name.value.trim() != unit.name && homeUnitOfSelectedTypeList.find { it.name == name.value.trim() } != null) {
                    return Pair(R.string.add_edit_home_unit_name_already_used, null)
                } else if (name.value.trim() != unit.name || type.value != unit.type) {
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

    abstract fun actionSaveGetCustomSavePair(): Pair<Int, Int?>?

    fun deleteHomeUnit(): Task<Void>? {
        Timber.d(
            "deleteHomeUnit homeUnit: ${homeUnit?.value} homeRepositoryTask.isComplete: ${homeRepositoryTask?.isComplete}"
        )
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
            "saveChanges homeUnit: ${homeUnit?.value} homeRepositoryTask.isComplete: ${homeRepositoryTask?.isComplete}"
        )
        homeRepositoryTask = (homeUnit?.value?.let { unit ->
            showProgress.value = true
            Timber.e("Save all changes")
            doSaveChanges().apply {
                if (name.value != unit.name || type.value != unit.type) {
                    Timber.d(
                        "Name or type changed will need to delete old value name=${name.value}, type = ${type.value}"
                    )
                    // delete old HomeUnit
                    this?.continueWithTask { homeRepository.deleteHomeUnit(unit) ?: it }
                }
            }
        } ?: doSaveChanges())?.addOnCompleteListener {
            Timber.d("Task completed")
            showProgress.value = false
        }
        return homeRepositoryTask
    }

    private fun doSaveChanges(): Task<Void>? {
        showProgress.value = true
        return homeRepository.saveHomeUnit(
            getHomeUnitToSave()
        )?.let { task ->
            if (type.value == HomeUnitType.HOME_LIGHT_SWITCHES) {
                task.continueWithTask {
                    homeRepository.saveUnitTask(
                        type.value, name.value,
                        UnitTask(
                            name = name.value,
                            homeUnitsList = listOf(
                                UnitTaskHomeUnit(
                                    type.value.toString(),
                                    name.value
                                )
                            )
                        )
                    ) ?: it
                }
            } else {
                task
            }
        }
    }

    abstract fun getHomeUnitToSave(): HomeUnit<Any>
}
