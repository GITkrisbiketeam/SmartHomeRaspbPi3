package com.krisbiketeam.smarthomeraspbpi3.adapters

import androidx.recyclerview.widget.DiffUtil
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit

class RoomDetailHomeUnitListAdapterDiffCallback : DiffUtil.ItemCallback<HomeUnit<Any?>>() {

    override fun areItemsTheSame(oldItem: HomeUnit<Any?>, newItem: HomeUnit<Any?>): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: HomeUnit<Any?>, newItem: HomeUnit<Any?>): Boolean {
        return oldItem == newItem
    }
}