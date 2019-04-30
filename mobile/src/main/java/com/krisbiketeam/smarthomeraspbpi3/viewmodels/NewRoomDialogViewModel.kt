package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.HomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Room
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragment


/**
 * The ViewModel for [RoomListFragment].
 */
class NewRoomDialogViewModel(private val appl: Application, private val homeRepository: HomeInformationRepository) : AndroidViewModel(appl) {

    private val roomList = homeRepository.roomListLiveData()
    val roomName = MutableLiveData<String?>("")

    var saveMessage: LiveData<String?> = Transformations.switchMap(Transformations.distinctUntilChanged(roomList)) { roomList ->
        Transformations.map(roomName) { newRoomName ->
            when {
                newRoomName?.trim()?.isEmpty() != false -> appl.getString(R.string.new_room_empty_name)
                roomList.find { room ->
                    room.name == newRoomName
                } != null -> appl.getString(R.string.new_room_name_already_used)
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
