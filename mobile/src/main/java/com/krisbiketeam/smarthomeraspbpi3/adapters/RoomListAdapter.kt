package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.krisbiketeam.data.storage.dto.Room
import timber.log.Timber
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentRoomListItemBinding
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragmentDirections
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragment

/**
 * Adapter for the [RecyclerView] in [RoomListFragment].
 */
class RoomListAdapter : ListAdapter<Room, RoomListAdapter.ViewHolder>(RoomListAdapterDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val roomName = getItem(position)
        holder.apply {
            bind(createOnClickListener(roomName.name), roomName)
            itemView.tag = roomName
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentRoomListItemBinding.inflate(
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
        private val binding: FragmentRoomListItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: View.OnClickListener, item: Room) {
            binding.apply {
                clickListener = listener
                room = item
                executePendingBindings()
            }
        }
    }
}