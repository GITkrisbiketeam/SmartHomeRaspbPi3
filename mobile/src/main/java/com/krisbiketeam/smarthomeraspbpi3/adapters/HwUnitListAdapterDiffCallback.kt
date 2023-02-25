package com.krisbiketeam.smarthomeraspbpi3.adapters

import androidx.recyclerview.widget.DiffUtil
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit

class HwUnitListAdapterDiffCallback : DiffUtil.ItemCallback<HwUnit>() {

    override fun areItemsTheSame(oldItem: HwUnit, newItem: HwUnit): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: HwUnit, newItem: HwUnit): Boolean {
        return oldItem == newItem
    }
}