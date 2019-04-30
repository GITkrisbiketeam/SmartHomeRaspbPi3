package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.Room
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentRoomDetailListItemBinding
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentRoomListHomeUnitSectionBinding
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentRoomListItemBinding
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragment
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragmentDirections
import timber.log.Timber

private const val ROOM_TYPE = 1
private const val HOME_UNIT_TYPE = 2
private const val HOME_UNIT_TYPE_HEADER = 3

/**
 * Adapter for the [RecyclerView] in [RoomListFragment].
 */
class RoomHomeUnitListAdapter : ListAdapter<Any, RoomHomeUnitListAdapter.ViewHolder>(RoomHomeUnitListAdapterDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.apply {
            bind(createOnClickListener(item), item)
            itemView.tag = item
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            ROOM_TYPE -> ViewHolder(FragmentRoomListItemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false))
            HOME_UNIT_TYPE -> ViewHolder(FragmentRoomDetailListItemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false))
            HOME_UNIT_TYPE_HEADER -> ViewHolder(FragmentRoomListHomeUnitSectionBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false))
            else -> super.createViewHolder(parent, viewType)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Room -> ROOM_TYPE
            is HomeUnit<*> -> HOME_UNIT_TYPE
            is String -> HOME_UNIT_TYPE_HEADER
            else -> super.getItemViewType(position)
        }
    }

    private fun createOnClickListener(item: Any): View.OnClickListener {
        return View.OnClickListener { view ->
            Timber.d("onClick")
            val direction = when (item) {
                is Room -> RoomListFragmentDirections.actionRoomListFragmentToRoomDetailFragment(item.name)
                is HomeUnit<*> -> RoomListFragmentDirections.actionRoomListFragmentToHomeUnitDetailFragment(item.room, item.name, item.type)
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

        fun bind(listener: View.OnClickListener, item: Any) {
            when (binding) {
                is FragmentRoomListItemBinding -> {
                    binding.apply {
                        clickListener = listener
                        room = item as Room
                        executePendingBindings()
                    }
                }
                is FragmentRoomDetailListItemBinding -> {
                    binding.apply {
                        clickListener = listener
                        homeUnit = item as HomeUnit<*>
                        executePendingBindings()
                    }
                }
                is FragmentRoomListHomeUnitSectionBinding -> {
                    binding.apply {
                        title = item as String
                        executePendingBindings()
                    }
                }
            }
        }
    }
}