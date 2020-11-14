package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.ui.HomeUnitDetailFragment
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomDetailFragment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber


/**
 * The ViewModel used in [RoomDetailFragment].
 */
class RoomDetailViewModel(
        private val homeRepository: FirebaseHomeInformationRepository,
        roomName: String
) : ViewModel() {

    val isEditMode: MutableLiveData<Boolean> = MutableLiveData(false)
    val room = homeRepository.roomLiveData(roomName)
    val roomName = MutableLiveData(roomName)
    val showProgress = MutableLiveData(false)

    private val roomList = homeRepository.roomListLiveData()

    private val homeUnitsListFlow: Flow<List<HomeUnit<Any?>>> = homeRepository.homeUnitListFlow().map {homeUnitList ->
        Timber.e("homeUnitsMap Flow")
        homeUnitList.filter {
            Timber.e("homeUnitsMap Flow filter")
            it.room == room.value?.name
        }
    }
    val homeUnitsList: LiveData<List<HomeUnit<Any?>>> = homeUnitsListFlow.asLiveData()

    fun noChangesMade(): Boolean {
        return room.value?.name?.trim() == roomName.value
    }

    /**
     * first return param is message Res Id, second return param if present will show dialog with this resource Id as a confirm button text, if not present Snackbar will be show.
     */
    fun actionSave(): Pair<Int, Int?> {
        Timber.d("actionSave room.name: ${room.value?.name} roomName.value: ${roomName.value}")

        return when {
            roomName.value?.trim().isNullOrEmpty() -> Pair(R.string.new_room_empty_name, null)
            roomName.value?.trim() == room.value?.name -> Pair(R.string.add_edit_home_unit_no_changes, null)
            roomList.value?.find { room ->  room.name == roomName.value?.trim()} != null -> Pair(R.string.new_room_name_already_used, null)
            else -> Pair(R.string.add_edit_home_unit_overwrite_changes, R.string.overwrite)
        }
    }

    /**
     * return true if we want to exit [HomeUnitDetailFragment]
     */
    fun actionDiscard() {
        isEditMode.value = false
        roomName.value = room.value?.name
    }
    fun actionDeleteHomeUnit(): Task<Void> {
        Timber.d("deleteHomeUnit room.name: ${room.value?.name} ")
        showProgress.value = true
        return homeUnitsList.value?.let { homeUnitList ->
            Tasks.whenAll(homeUnitList.map { homeUnit ->
                homeUnit.run {
                    room = ""
                    homeRepository.saveHomeUnit(this)
                }
            }).continueWithTask {
                room.value?.let { room ->
                    homeRepository.deleteRoom(room.name)
                } ?: it
            }.addOnCompleteListener {
                Timber.d("Task completed")
                showProgress.value = false
            }
        } ?: Tasks.call { showProgress.value = false } as Task<Void>
    }

    fun saveChanges(): Task<Void> {
        showProgress.value = true
        return roomName.value?.let { newRoomName ->
            homeUnitsList.value?.let { homeUnitList ->
                Tasks.whenAll(homeUnitList.map { homeUnit ->
                    homeUnit.run {
                        room = newRoomName
                        homeRepository.saveHomeUnit(this)
                    }
                }).continueWithTask {
                    room.value?.let { room ->
                        val oldRoomName = room.name
                        homeRepository.saveRoom(room.apply { name = newRoomName })
                                ?.continueWithTask { homeRepository.deleteRoom(oldRoomName) }
                    } ?: it
                }.addOnCompleteListener {
                    Timber.d("Task completed")
                    showProgress.value = false
                }
            }
        }?: Tasks.call { showProgress.value = false } as Task<Void>
    }
}
