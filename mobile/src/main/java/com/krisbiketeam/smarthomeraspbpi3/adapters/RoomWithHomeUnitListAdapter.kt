package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentRoomListItemWithHomeUnitCardBinding
import com.krisbiketeam.smarthomeraspbpi3.model.RoomListAdapterModel
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragment
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragmentDirections
import timber.log.Timber

/**
 * Adapter for the [RecyclerView] in [RoomListFragment].
 */
class RoomWithHomeUnitListAdapter : ListAdapter<RoomListAdapterModel, RoomWithHomeUnitListAdapter.ViewHolder>(RoomWithHomeUnitListAdapterDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.apply {
            bind(createOnClickListener(item), item)
            itemView.tag = item
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentRoomListItemWithHomeUnitCardBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }


    private fun createOnClickListener(item: RoomListAdapterModel): View.OnClickListener {
        return View.OnClickListener { view ->
            Timber.d("onClick")
            val direction = when {
                item.room != null -> RoomListFragmentDirections.actionRoomListFragmentToRoomDetailFragment(item.room.name)
                // is HomeUnit<*> -> RoomListFragmentDirections.actionRoomListFragmentToHomeUnitDetailFragment(item.room, item.name, item.type)
                else -> null
            }
            direction?.let {
                view.findNavController().navigate(it)
            }
        }
    }

    class ViewHolder(
            private val binding: ViewDataBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: View.OnClickListener, item: RoomListAdapterModel) {
            if (binding is FragmentRoomListItemWithHomeUnitCardBinding) {
                binding.apply {
                    clickListener = listener
                    roomModel = item
                    executePendingBindings()
                }
            }
        }
    }
}