package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.annotation.SuppressLint
import androidx.recyclerview.widget.DiffUtil
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Room

class RoomHomeUnitListAdapterDiffCallback : DiffUtil.ItemCallback<Any>() {

    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return (oldItem is Room && newItem is Room && oldItem.name == newItem.name) ||
                (oldItem is String && newItem is String && oldItem == newItem) ||
                (oldItem is HomeUnit<*> && newItem is HomeUnit<*> && oldItem.name == newItem.name)
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return (oldItem is Room && newItem is Room && oldItem == newItem) ||
                (oldItem is HomeUnit<*> && newItem is HomeUnit<*> && oldItem == newItem)
    }
}