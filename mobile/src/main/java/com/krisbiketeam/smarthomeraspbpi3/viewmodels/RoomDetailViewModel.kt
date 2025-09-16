package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Room
import com.krisbiketeam.smarthomeraspbpi3.model.RoomDetailListAdapterModel
import com.krisbiketeam.smarthomeraspbpi3.ui.HomeUnitGenericDetailFragment
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomDetailFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import timber.log.Timber
import kotlin.collections.LinkedHashMap
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.associateWithTo
import kotlin.collections.emptyList
import kotlin.collections.filterNotNull
import kotlin.collections.find
import kotlin.collections.firstOrNull
import kotlin.collections.forEach
import kotlin.collections.map
import kotlin.collections.set
import kotlin.collections.toMutableList


/**
 * The ViewModel used in [RoomDetailFragment].
 */
@ExperimentalCoroutinesApi
@FlowPreview
class RoomDetailViewModel(
        private val homeRepository: FirebaseHomeInformationRepository,
        private val inputRoomName: String
) : ViewModel() {

    private val roomList = homeRepository.roomListFlow().flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private val homeUnitsOrderStateFlow = MutableStateFlow<List<String>>(emptyList())

    val isEditMode: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val room: StateFlow<Room?> = homeRepository.roomUnitFlow(inputRoomName).flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    val roomName:MutableStateFlow<String> = MutableStateFlow(inputRoomName)
    val showProgress:MutableStateFlow<Boolean> = MutableStateFlow(false)

    val homeUnitsList: StateFlow<List<RoomDetailListAdapterModel>> by lazy {
        combine(
            homeRepository.homeUnitListFlow().debounce(100),
            homeRepository.hwUnitErrorEventListFlow(),
            homeUnitsOrderStateFlow,
            room.map {
                it?.unitsOrder ?: emptyList()
            }) { homeUnitList, hwUnitErrorEventList, newOrderList, existingOrderList ->
            Timber.e("homeUnitsMap Flow")
            val orderList = newOrderList.ifEmpty { existingOrderList }
            val map: MutableMap<String, RoomDetailListAdapterModel?> =
                orderList.associateWithTo(LinkedHashMap(orderList.size)) { null }
            homeUnitList.forEach {
                if (it.room == room.value?.name) {
                    //Timber.e("homeUnitsMap Flow filter")
                    map[it.type.toString() + '.' + it.name] = RoomDetailListAdapterModel(
                        it,
                        hwUnitErrorEventList.firstOrNull { hwUnitLog -> hwUnitLog.name == it.hwUnitName } != null)
                }
            }
            map.values.filterNotNull().also { unitsList ->
                val newOrder =
                    unitsList.map { it.homeUnit.let { homeUnit -> homeUnit.type.toString() + '.' + homeUnit.name } }
                if (newOrder != orderList || newOrderList.isEmpty()) {
                    homeUnitsOrderStateFlow.value = newOrder
                }
            }
        }.flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    }

    fun noChangesMade(): Boolean {
        return room.value?.name == roomName.value.trim() && room.value?.unitsOrder == homeUnitsOrderStateFlow.value
    }

    /**
     * first return param is message Res Id, second return param if present will show dialog with this resource Id as a confirm button text, if not present Snackbar will be show.
     */
    fun actionSave(): Pair<Int, Int?> {
        Timber.d("actionSave room.name: ${room.value?.name} roomName.value: ${roomName.value}")

        return when {
            roomName.value.trim().isEmpty() -> Pair(R.string.new_room_empty_name, null)
            noChangesMade() -> Pair(R.string.add_edit_home_unit_no_changes, null)
            roomList.value.find { room -> room.name == roomName.value.trim() } != null -> Pair(R.string.new_room_name_already_used, null)
            else -> Pair(R.string.add_edit_home_unit_overwrite_changes, R.string.overwrite)
        }
    }

    /**
     * return true if we want to exit [HomeUnitGenericDetailFragment]
     */
    fun actionDiscard() {
        isEditMode.value = false
        roomName.value = room.value?.name?: inputRoomName
        homeUnitsOrderStateFlow.value = room.value?.unitsOrder ?: emptyList()
    }

    fun actionDeleteRoom(): Task<Void> {
        Timber.d("deleteHomeUnit room.name: ${room.value?.name} ")
        showProgress.value = true
        return homeUnitsList.value.let { homeUnitList ->
            Tasks.whenAll(homeUnitList.map { homeUnitModel ->
                homeRepository.updateHomeUnitRoomName(
                    homeUnitModel.homeUnit.type,
                    homeUnitModel.homeUnit.name,
                    ""
                )
            }).continueWithTask {
                room.value?.let { room ->
                    homeRepository.deleteRoom(room.name)
                } ?: it
            }.addOnCompleteListener {
                Timber.d("Task completed")
                showProgress.value = false
            }
        }
    }

    fun saveChanges(): Task<Void> {
        showProgress.value = true
        return roomName.value.let { newRoomName ->
            homeUnitsList.value.let { homeUnitList ->
                Tasks.whenAll(if (newRoomName != room.value?.name) {
                    homeUnitList.map { homeUnitModel ->
                        homeRepository.updateHomeUnitRoomName(
                            homeUnitModel.homeUnit.type,
                            homeUnitModel.homeUnit.name,
                            newRoomName
                        )
                    }
                } else null).continueWithTask {
                    room.value?.let { room ->
                        val oldRoomName = room.name
                        homeRepository.saveRoom(room.apply {
                            name = newRoomName
                            unitsOrder = homeUnitsOrderStateFlow.value
                        })?.continueWithTask { saveRoomTask ->
                            if (newRoomName != oldRoomName) {
                                homeRepository.deleteRoom(oldRoomName)?: saveRoomTask
                            } else {
                                Tasks.forResult(null)
                            }
                        }?: it
                    } ?: it
                }.addOnCompleteListener {
                    Timber.d("Task completed")
                    showProgress.value = false
                }
            }
        }
    }

    fun moveItem(from: Int, to: Int) {
        val itemsOrderCopy = homeUnitsOrderStateFlow.value.toMutableList()
        val itemFrom = itemsOrderCopy.removeAt(from)
        itemsOrderCopy.add(to, itemFrom)

        homeUnitsOrderStateFlow.value = itemsOrderCopy
    }
}
