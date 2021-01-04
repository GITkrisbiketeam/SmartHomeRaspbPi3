package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.annotation.SuppressLint
import androidx.recyclerview.widget.DiffUtil
import com.krisbiketeam.smarthomeraspbpi3.model.RoomListAdapterModel

class RoomWithHomeUnitListAdapterDiffCallback : DiffUtil.ItemCallback<RoomListAdapterModel>() {

    override fun areItemsTheSame(oldItem: RoomListAdapterModel, newItem: RoomListAdapterModel): Boolean {
        return (oldItem.room != null && newItem.room != null && oldItem.room.name == newItem.room.name) ||
                oldItem.homeUnit?.name == newItem.homeUnit?.name
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: RoomListAdapterModel, newItem: RoomListAdapterModel): Boolean {
        return oldItem == newItem
    }
}