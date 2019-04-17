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
    val homeUnits = homeRepository.homeUnitsLiveData(roomName)
    val roomName = MutableLiveData<String>("")
    private val homeUnits2: LiveData<MutableList<HomeUnit<Any?>>> = Transformations.switchMap(room) { room ->
        val mediator = MediatorLiveData<MutableList<HomeUnit<Any?>>>()
        // TODO: add other types or change Room types to list and make similar to UnitTaskViewModel
        room.blinds.forEach {blindName ->
            mediator.addSource(homeRepository.homeUnitLiveData("blinds", blindName)) {
                mediator.value?.add(it)
            }
        }
        mediator
    }

    init {
        Timber.d("init roomName: $roomName")
    }

    fun saveNewRoomName(){
        roomName.value?.let {newRoomName ->
            room.value?.let {room ->
                homeUnits2.value?.let {homeUnits ->
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
