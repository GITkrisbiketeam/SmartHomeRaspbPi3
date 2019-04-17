package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.krisbiketeam.smarthomeraspbpi3.common.storage.HomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Room
import com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata.RoomListLiveData
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragment
import timber.log.Timber


/**
 * The ViewModel for [RoomListFragment].
 */
class NewRoomDialogViewModel(private val homeRepository: HomeInformationRepository) : ViewModel() {

    private val roomList: RoomListLiveData
    val roomName = MutableLiveData<String>("")

    init {
        Timber.d("init")
        roomList = homeRepository.roomListLiveData()
    }

    fun nameAlreadyUsed(): LiveData<Boolean> {
        return Transformations.switchMap(Transformations.distinctUntilChanged(roomList)) { roomList ->
            Transformations.map(roomName) { newRoomName ->
                roomList.find { room ->
                    room.name == newRoomName
                } != null
            }
        }
    }

    fun saveNewRoom() {
        roomName.value?.let {
            homeRepository.saveRoom(Room(it))
        }
    }
}
