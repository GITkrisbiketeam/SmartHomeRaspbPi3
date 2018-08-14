package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider

import com.krisbiketeam.data.storage.HomeInformationRepository
import com.krisbiketeam.data.storage.dto.Room

/**
 * Factory for creating a [RoomDetailViewModel] with a constructor that takes a [HomeInformationRepository]
 * and an ID for the current [Room].
 */
class RoomDetailViewModelFactory(
        private val homeRepository: HomeInformationRepository,
        private val roomName: String
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RoomDetailViewModel(homeRepository, roomName) as T
    }
}
