package com.krisbiketeam.smarthomeraspbpi3.adapters

import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Room
import timber.log.Timber
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentRoomListItemBinding
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragmentDirections
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragment

/**
 * Adapter for the [RecyclerView] in [RoomListFragment].
 */
@Deprecated(message = "Please use other RoomList Adapter (RoomWithHomeUnitListAdapter)")
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
        return View.OnClickListener {view ->
            Timber.d("onClick")
            val direction = RoomListFragmentDirections.actionRoomListFragmentToRoomDetailFragment(name)
            view.findNavController().navigate(direction)
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