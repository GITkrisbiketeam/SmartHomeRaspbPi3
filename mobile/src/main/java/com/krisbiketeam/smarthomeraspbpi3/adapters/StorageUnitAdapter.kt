package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.krisbiketeam.data.storage.dto.StorageUnit
import timber.log.Timber
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentRoomDetailListItemBinding
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragmentDirections

/**
 * Adapter for the [RecyclerView] in [RoomListFragment].
 */
class StorageUnitAdapter : ListAdapter<StorageUnit<out Any>, StorageUnitAdapter.ViewHolder>(StorageUnitDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val unitName = getItem(position)
        holder.apply {
            bind(createOnClickListener(unitName.name), unitName)
            itemView.tag = unitName
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentRoomDetailListItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }

    private fun createOnClickListener(name: String): View.OnClickListener {
        return View.OnClickListener {
            Timber.d("onClick")
            val direction = RoomListFragmentDirections.ActionRoomListFragmentToRoomDetailFragment(name)
            it.findNavController().navigate(direction)
        }
    }

    class ViewHolder(
        private val binding: FragmentRoomDetailListItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: View.OnClickListener, item: StorageUnit<out Any>) {
            binding.apply {
                clickListener = listener
                this.storageUnit = item
                executePendingBindings()
            }
        }
    }
}