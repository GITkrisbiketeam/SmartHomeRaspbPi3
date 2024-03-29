package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
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
                item.room != null     -> RoomListFragmentDirections.actionRoomListFragmentToRoomDetailFragment(
                        item.room.name)
                item.homeUnit != null -> item.homeUnit?.let { homeUnit ->
                    when (homeUnit.type) {
                        HomeUnitType.HOME_LIGHT_SWITCHES -> RoomListFragmentDirections.actionRoomListFragmentToHomeUnitLightSwitchDetailFragment(
                            "", homeUnit.name
                        )
                        HomeUnitType.HOME_WATER_CIRCULATION -> RoomListFragmentDirections.actionRoomListFragmentToHomeUnitWaterCirculationDetailFragment(
                            "", homeUnit.name
                        )
                        else -> RoomListFragmentDirections.actionRoomListFragmentToHomeUnitGenericDetailFragment(
                            "", homeUnit.name, homeUnit.type
                        )
                    }
                }
                else                  -> null
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