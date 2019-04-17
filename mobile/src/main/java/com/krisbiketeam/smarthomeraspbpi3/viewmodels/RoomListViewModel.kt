package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.krisbiketeam.smarthomeraspbpi3.common.storage.HomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.livedata.RoomListLiveData
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragment
import timber.log.Timber


/**
 * The ViewModel for [RoomListFragment].
 */
class RoomListViewModel(homeRepository: HomeInformationRepository) : ViewModel() {

    val roomList: RoomListLiveData
    val isEditMode: MutableLiveData<Boolean> = MutableLiveData(false)

    init {
        Timber.d("init")

        roomList = homeRepository.roomListLiveData()
    }

}
