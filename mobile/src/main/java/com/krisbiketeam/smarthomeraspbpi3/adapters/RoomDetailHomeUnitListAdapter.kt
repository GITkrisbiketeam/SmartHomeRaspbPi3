package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.GenericHomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.LAST_TRIGGER_SOURCE_ROOM_HOME_UNITS_LIST
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentRoomDetailListItemBinding
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomDetailFragmentDirections
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragment
import com.krisbiketeam.smarthomeraspbpi3.utils.getLastUpdateTime
import timber.log.Timber

/**
 * Adapter for the [RecyclerView] in [RoomListFragment].
 */

class RoomDetailHomeUnitListAdapter(private val homeInformationRepository: FirebaseHomeInformationRepository) : ListAdapter<HomeUnit<Any>, RoomDetailHomeUnitListAdapter.ViewHolder>(RoomDetailHomeUnitListAdapterDiffCallback()) {
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val homeUnit = getItem(position)
        holder.apply {
            bind(createOnClickListener(homeUnit), homeUnit)
            itemView.tag = homeUnit
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentRoomDetailListItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false), homeInformationRepository)
    }

    private fun createOnClickListener(item: HomeUnit<Any>): View.OnClickListener {
        return View.OnClickListener { view ->
            Timber.d("onClick item: $item")
            val direction = RoomDetailFragmentDirections.actionRoomDetailFragmentToHomeUnitDetailFragment(item.room, item.name, item.type.firebaseTableName)
            view.findNavController().navigate(direction)
        }
    }

    class ViewHolder(
            private val binding: FragmentRoomDetailListItemBinding,
            private val homeInformationRepository: FirebaseHomeInformationRepository
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: View.OnClickListener, item: HomeUnit<Any>) {
            binding.apply {
                clickListener = listener
                homeUnit = item
                lastUpdateTime = getLastUpdateTime(root.context, item.lastUpdateTime)
                // TODO Add handling of other type of HomeUnits (LightSwitchhomeUnit etc...
                //  add some other types of ViewHolder for them)
                secondLastUpdateTime = if (item.type == HomeUnitType.HOME_LIGHT_SWITCHES) {
                    getLastUpdateTime(root.context, item.secondLastUpdateTime)
                } else {
                    null
                }
                value = if(item.value is Double || item.value is Float) {
                    String.format("%.2f", item.value)
                } else if(item.type == HomeUnitType.HOME_LIGHT_SWITCHES) {
                    item.secondValue.toString()
                } else{
                    item.value.toString()
                }

                homeUnitItemSwitch.setOnCheckedChangeListener { _, isChecked ->
                    Timber.d("OnCheckedChangeListener isChecked: $isChecked item: $item")
                    if (item.value != isChecked && item is GenericHomeUnit) {
                        item.copy().also { unit ->
                            unit.value = isChecked
                            unit.lastUpdateTime = System.currentTimeMillis()
                            unit.lastTriggerSource = LAST_TRIGGER_SOURCE_ROOM_HOME_UNITS_LIST
                            homeInformationRepository.updateHomeUnitValue(unit)
                        }
                    }
                }

                executePendingBindings()
            }
        }
    }
}