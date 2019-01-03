package com.krisbiketeam.smarthomeraspbpi3.adapters

import androidx.recyclerview.widget.DiffUtil
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Room

class RoomListAdapterDiffCallback : DiffUtil.ItemCallback<Room>() {

    override fun areItemsTheSame(oldItem: Room, newItem: Room): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: Room, newItem: Room): Boolean {
        return oldItem == newItem
    }
}