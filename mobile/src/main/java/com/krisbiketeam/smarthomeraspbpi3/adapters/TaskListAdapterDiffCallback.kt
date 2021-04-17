package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.annotation.SuppressLint
import androidx.recyclerview.widget.DiffUtil
import com.krisbiketeam.smarthomeraspbpi3.model.TaskListAdapterModel

class TaskListAdapterDiffCallback : DiffUtil.ItemCallback<TaskListAdapterModel>() {

    override fun areItemsTheSame(oldItem: TaskListAdapterModel, newItem: TaskListAdapterModel): Boolean {
        return (oldItem.room != null && newItem.room != null && oldItem.room.name == newItem.room.name) ||
                oldItem.homeUnit?.name == newItem.homeUnit?.name
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: TaskListAdapterModel, newItem: TaskListAdapterModel): Boolean {
        return oldItem == newItem
    }
}