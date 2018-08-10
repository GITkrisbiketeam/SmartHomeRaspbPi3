package com.krisbiketeam.smarthomeraspbpi3.viewmodels

import android.arch.lifecycle.ViewModel
import android.databinding.ObservableArrayList
import android.databinding.ObservableField
import com.krisbiketeam.data.storage.dto.UnitTask

/**
 * The ViewModel used in [AddStorageHomeUnitFragment].
 */
class AddStorageHomeUnitViewModel() : ViewModel() {
    var name: ObservableField<String> = ObservableField()
    var firebaseTableName: ObservableField<String> = ObservableField()
    var room: ObservableField<String> = ObservableField()
    var hardwareUnitName: ObservableField<String> = ObservableField()
    val unitsTasks: ObservableArrayList<UnitTask> = ObservableArrayList()

}
