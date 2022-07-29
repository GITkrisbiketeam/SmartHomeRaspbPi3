package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import androidx.lifecycle.ViewModel
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HOME_STORAGE_UNITS
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragment
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The ViewModel for [RoomListFragment].
 */
class HomeUnitTypeChooserDialogViewModel : ViewModel() {

    val typeList = HOME_STORAGE_UNITS
    val type: MutableStateFlow<HomeUnitType> = MutableStateFlow(HomeUnitType.UNKNOWN)

}
