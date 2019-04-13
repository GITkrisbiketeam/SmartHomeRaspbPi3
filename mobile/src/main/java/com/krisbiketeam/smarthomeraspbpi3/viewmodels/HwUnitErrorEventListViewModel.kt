package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.*
import com.krisbiketeam.smarthomeraspbpi3.adapters.HwUnitErrorEventListAdapter
import com.krisbiketeam.smarthomeraspbpi3.common.storage.HomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnitLog
import com.krisbiketeam.smarthomeraspbpi3.ui.HwUnitErrorEventListFragment
import timber.log.Timber

/**
 * The ViewModel for [HwUnitErrorEventListFragment].
 */
class HwUnitErrorEventListViewModel(homeRepository: HomeInformationRepository) : ViewModel() {

    val hwUnitErrorEventList: LiveData<List<HwUnitLog<Any>>>

    val hwUnitErrorEventListAdapter: HwUnitErrorEventListAdapter = HwUnitErrorEventListAdapter()

    init {
        Timber.d("init")

        hwUnitErrorEventList = homeRepository.hwUnitErrorEventListLiveData()
    }
}
