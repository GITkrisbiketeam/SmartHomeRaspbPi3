package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.HomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomDetailFragment
import timber.log.Timber


/**
 * The ViewModel used in [RoomDetailFragment].
 */
class RoomDetailViewModel(
        private val homeRepository: HomeInformationRepository,
        roomName: String
) : ViewModel() {

    val isEditMode: MutableLiveData<Boolean> = MutableLiveData(false)
    val room= homeRepository.roomLiveData(roomName)

    val roomName = MutableLiveData<String>("")
    val homeUnitsMap = Transformations.switchMap(room) { room ->
        MediatorLiveData<MutableMap<String, HomeUnit<Any?>>>().apply {
            room.homeUnits.keys.forEach { type ->
                room.homeUnits[type]?.forEach { homeUnitName ->
                    addSource(homeRepository.homeUnitLiveData(type, homeUnitName)) {
                        value = value ?: mutableMapOf()
                        value?.put(it.name, it)
                        postValue(value)
                    }
                }
            }
        }

    }

    init {
        Timber.d("init roomName: $roomName")
    }

    fun saveNewRoomName(){
        roomName.value?.let {newRoomName ->
            room.value?.let {room ->
                homeUnitsMap.value?.let {homeUnitsMap ->
                    //TODO
                    /*val oldRoomName = room.name
                    room.name = newRoomName
                    homeRepository.saveRoom(room)
                    homeUnits.value?*/
                }

            }
        }
    }
}
