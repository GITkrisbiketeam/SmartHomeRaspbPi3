package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider

import com.krisbiketeam.data.storage.HomeInformationRepository

/**
 * Factory for creating a [RoomListViewModel] with a constructor that takes a [HomeInformationRepository].
 */
class RoomListViewModelFactory(
    private val repository: HomeInformationRepository
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>) = RoomListViewModel(repository) as T
}
