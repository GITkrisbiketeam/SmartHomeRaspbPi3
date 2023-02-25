package com.krisbiketeam.smarthomeraspbpi3.adapters

import androidx.recyclerview.widget.DiffUtil
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnitLog

class HwUnitErrorEventListAdapterDiffCallback : DiffUtil.ItemCallback<HwUnitLog<Any>>() {

    override fun areItemsTheSame(oldItem: HwUnitLog<Any>, newItem: HwUnitLog<Any>): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: HwUnitLog<Any>, newItem: HwUnitLog<Any>): Boolean {
        return oldItem == newItem
    }
}