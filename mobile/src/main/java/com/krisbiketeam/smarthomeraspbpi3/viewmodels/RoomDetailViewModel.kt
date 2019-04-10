package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.*
import com.krisbiketeam.smarthomeraspbpi3.common.storage.HomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata.HomeUnitsLiveData
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Room
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomDetailFragment
import timber.log.Timber


/**
 * The ViewModel used in [RoomDetailFragment].
 */
class RoomDetailViewModel(
        homeRepository: HomeInformationRepository,
        roomName: String
) : ViewModel() {

    var isEditMode: MutableLiveData<Boolean> = MutableLiveData()
    val room: LiveData<Room>
    val homeUnits: HomeUnitsLiveData


    init {
        Timber.d("init roomName: $roomName")

        room = homeRepository.roomLiveData(roomName)
        homeUnits = homeRepository.homeUnitsLiveData(roomName)
        isEditMode.value = false
    }
}
