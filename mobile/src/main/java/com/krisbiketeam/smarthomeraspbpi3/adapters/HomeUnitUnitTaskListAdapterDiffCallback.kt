package com.krisbiketeam.smarthomeraspbpi3.adapters

import androidx.recyclerview.widget.DiffUtil
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.UnitTask

class HomeUnitUnitTaskListAdapterDiffCallback : DiffUtil.ItemCallback<UnitTask>() {

    override fun areItemsTheSame(oldItem: UnitTask, newItem: UnitTask): Boolean {
        return oldItem.name == newItem.name //oldItem.homeUnitName == newItem.homeUnitName && oldItem.hwUnitName == newItem.hwUnitName
    }

    override fun areContentsTheSame(oldItem: UnitTask, newItem: UnitTask): Boolean {
        return oldItem == newItem
    }
}