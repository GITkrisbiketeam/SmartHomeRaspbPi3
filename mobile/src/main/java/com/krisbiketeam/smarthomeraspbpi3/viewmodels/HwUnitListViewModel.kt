package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.lifecycle.*
import com.krisbiketeam.data.storage.HomeInformationRepository
import com.krisbiketeam.data.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.adapters.HwUnitListAdapter
import com.krisbiketeam.smarthomeraspbpi3.ui.HwUnitListFragment
import timber.log.Timber

/**
 * The ViewModel for [HwUnitListFragment].
 */
class HwUnitListViewModel(homeRepository: HomeInformationRepository) : ViewModel() {

    val hwUnitList: LiveData<List<HwUnit>>

    val hwUnitListAdapter: HwUnitListAdapter = HwUnitListAdapter()

    init {
        Timber.d("init")

        hwUnitList = homeRepository.hwUnitListLiveData()
    }
}
