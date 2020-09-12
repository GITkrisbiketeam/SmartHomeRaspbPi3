package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.krisbiketeam.smarthomeraspbpi3.adapters.HwUnitErrorEventListAdapter
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnitLog
import com.krisbiketeam.smarthomeraspbpi3.ui.HwUnitErrorEventListFragment

/**
 * The ViewModel for [HwUnitErrorEventListFragment].
 */
class HwUnitErrorEventListViewModel(private val homeRepository: FirebaseHomeInformationRepository) : ViewModel() {

    val hwUnitErrorEventList: LiveData<List<HwUnitLog<Any>>> = homeRepository.hwUnitErrorEventListFlow().asLiveData()

    val hwUnitErrorEventListAdapter: HwUnitErrorEventListAdapter = HwUnitErrorEventListAdapter()

    fun clearHwErrors(){
        homeRepository.clearHwErrors()
    }
}
