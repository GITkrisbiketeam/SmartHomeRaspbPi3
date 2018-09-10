package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.support.v7.util.DiffUtil
import com.krisbiketeam.data.storage.dto.UnitTask

class UnitTaskListAdapterDiffCallback : DiffUtil.ItemCallback<UnitTask>() {

    override fun areItemsTheSame(oldItem: UnitTask, newItem: UnitTask): Boolean {
        return oldItem.name == newItem.name //oldItem.storageUnitName == newItem.storageUnitName && oldItem.hardwareUnitName == newItem.hardwareUnitName
    }

    override fun areContentsTheSame(oldItem: UnitTask, newItem: UnitTask): Boolean {
        return oldItem == newItem
    }
}