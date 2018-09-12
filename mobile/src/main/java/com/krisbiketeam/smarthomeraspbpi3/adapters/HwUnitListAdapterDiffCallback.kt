package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.support.v7.util.DiffUtil
import com.krisbiketeam.data.storage.dto.HwUnit

class HwUnitListAdapterDiffCallback : DiffUtil.ItemCallback<HwUnit>() {

    override fun areItemsTheSame(oldItem: HwUnit, newItem: HwUnit): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: HwUnit, newItem: HwUnit): Boolean {
        return oldItem == newItem
    }
}