package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.lifecycle.*
import com.krisbiketeam.data.storage.HomeInformationRepository
import com.krisbiketeam.data.storage.livedata.HomeUnitsLiveData
import com.krisbiketeam.data.storage.dto.Room
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
