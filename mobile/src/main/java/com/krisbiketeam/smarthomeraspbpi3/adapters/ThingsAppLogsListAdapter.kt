package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.RemoteLog
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentThingsAppLogsListItemBinding
import com.krisbiketeam.smarthomeraspbpi3.ui.ThingsAppLogsFragment
import com.krisbiketeam.smarthomeraspbpi3.utils.getTextColor

/**
 * Adapter for the [RecyclerView] in [ThingsAppLogsFragment].
 */
class ThingsAppLogsListAdapter : ListAdapter<RemoteLog, ThingsAppLogsListAdapter.ViewHolder>(ThingsAppLogsListAdapterDiffCallback()) {
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val remoteLog = getItem(position)
        holder.apply {
            bind(remoteLog)
            itemView.tag = remoteLog
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentThingsAppLogsListItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }

    class ViewHolder(
            private val binding: FragmentThingsAppLogsListItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RemoteLog) {
            binding.apply {
                remoteLog = item
                textColor = item.getTextColor(binding.root.context)
                executePendingBindings()
            }
        }
    }
}

