package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Room
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragment
import kotlinx.coroutines.Dispatchers

/**
 * The ViewModel for [RoomListFragment].
 */
class NewRoomDialogViewModel(application: Application, private val homeRepository: FirebaseHomeInformationRepository) : ViewModel() {

    private val roomList = homeRepository.roomListFlow().asLiveData(Dispatchers.IO)
    val roomName = MutableLiveData("")

    var saveMessage: LiveData<String?> = Transformations.switchMap(Transformations.distinctUntilChanged(roomList)) { roomList ->
        Transformations.map(roomName) { newRoomName ->
            when {
                newRoomName?.trim()?.isEmpty() != false -> application.getString(R.string.new_room_empty_name)
                roomList.find { room ->
                    room.name == newRoomName
                } != null -> application.getString(R.string.new_room_name_already_used)
                else -> null
            }
        }
    }

    fun saveNewRoom() {
        roomName.value?.let {
            homeRepository.saveRoom(Room(it))
        }
    }
}
