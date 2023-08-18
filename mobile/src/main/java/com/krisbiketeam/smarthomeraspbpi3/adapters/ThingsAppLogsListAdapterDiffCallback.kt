package com.krisbiketeam.smarthomeraspbpi3.adapters

import androidx.recyclerview.widget.DiffUtil
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.RemoteLog

class ThingsAppLogsListAdapterDiffCallback : DiffUtil.ItemCallback<RemoteLog>() {

    override fun areItemsTheSame(oldItem: RemoteLog, newItem: RemoteLog): Boolean {
        return oldItem.time == newItem.time
    }

    override fun areContentsTheSame(oldItem: RemoteLog, newItem: RemoteLog): Boolean {
        return oldItem == newItem
    }
}