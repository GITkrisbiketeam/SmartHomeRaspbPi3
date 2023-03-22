package com.krisbiketeam.smarthomeraspbpi3.adapters

import androidx.recyclerview.widget.DiffUtil
import com.krisbiketeam.smarthomeraspbpi3.model.RoomDetailListAdapterModel

class RoomDetailHomeUnitListAdapterDiffCallback : DiffUtil.ItemCallback<RoomDetailListAdapterModel>() {

    override fun areItemsTheSame(oldItem: RoomDetailListAdapterModel, newItem: RoomDetailListAdapterModel): Boolean {
        return oldItem.homeUnit.name == newItem.homeUnit.name
    }

    override fun areContentsTheSame(oldItem: RoomDetailListAdapterModel, newItem: RoomDetailListAdapterModel): Boolean {
        return oldItem == newItem
    }
}