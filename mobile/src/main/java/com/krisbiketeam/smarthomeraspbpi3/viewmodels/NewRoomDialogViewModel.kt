package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Room
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/**
 * The ViewModel for [RoomListFragment].
 */
class NewRoomDialogViewModel(application: Application, private val homeRepository: FirebaseHomeInformationRepository) : ViewModel() {

    @ExperimentalCoroutinesApi
    private val roomList = homeRepository.roomListFlow()

    val roomName: MutableStateFlow<String?> = MutableStateFlow(null)

    @ExperimentalCoroutinesApi
    var saveMessage: StateFlow<String?> = combine(roomList, roomName) { roomList, newRoomName ->
        when {
            newRoomName?.trim()?.isEmpty() != false -> application.getString(R.string.new_room_empty_name)
            roomList.find { room ->
                room.name == newRoomName
            } != null -> application.getString(R.string.new_room_name_already_used)
            else -> null
        }
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)


    fun saveNewRoom() {
        roomName.value?.let {
            homeRepository.saveRoom(Room(it))
        }
    }
}
