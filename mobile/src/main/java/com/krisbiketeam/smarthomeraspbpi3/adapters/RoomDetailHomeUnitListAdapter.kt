package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.LightSwitchHomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.MCP23017WatchDogHomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.WaterCirculationHomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.LAST_TRIGGER_SOURCE_ROOM_HOME_UNITS_LIST
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentRoomDetailListItemBinding
import com.krisbiketeam.smarthomeraspbpi3.model.RoomDetailListAdapterModel
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomDetailFragmentDirections
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragment
import com.krisbiketeam.smarthomeraspbpi3.utils.getLastUpdateTime
import timber.log.Timber

/**
 * Adapter for the [RecyclerView] in [RoomListFragment].
 */

class RoomDetailHomeUnitListAdapter(private val homeInformationRepository: FirebaseHomeInformationRepository) :
    ListAdapter<RoomDetailListAdapterModel, RoomDetailHomeUnitListAdapter.ViewHolder>(
        RoomDetailHomeUnitListAdapterDiffCallback()
    ) {
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

    private fun createOnClickListener(item: RoomDetailListAdapterModel): View.OnClickListener {
        return View.OnClickListener { view ->
            Timber.d("onClick item: $item")
            val direction = when (item.homeUnit.type) {
                HomeUnitType.HOME_LIGHT_SWITCHES -> RoomDetailFragmentDirections.actionRoomDetailFragmentToHomeUnitLightSwitchDetailFragment(
                    item.homeUnit.room,
                    item.homeUnit.name
                )
                HomeUnitType.HOME_WATER_CIRCULATION -> RoomDetailFragmentDirections.actionRoomDetailFragmentToHomeUnitWaterCirculationDetailFragment(
                    item.homeUnit.room,
                    item.homeUnit.name
                )
                HomeUnitType.HOME_MCP23017_WATCH_DOG -> RoomDetailFragmentDirections.actionRoomDetailFragmentToHomeUnitMcp23017WatchDogDetailFragment(
                    item.homeUnit.room,
                    item.homeUnit.name
                )
                else -> RoomDetailFragmentDirections.actionRoomDetailFragmentToHomeUnitGenericDetailFragment(
                    item.homeUnit.room,
                    item.homeUnit.name,
                    item.homeUnit.type
                )
            }
            view.findNavController().navigate(direction)
        }
    }

    class ViewHolder(
            private val binding: FragmentRoomDetailListItemBinding,
            private val homeInformationRepository: FirebaseHomeInformationRepository
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: View.OnClickListener, item: RoomDetailListAdapterModel) {
            binding.apply {
                clickListener = listener
                homeUnit = item.homeUnit
                error = item.error
                val homeUnitItem = item.homeUnit
                lastUpdateTime = getLastUpdateTime(root.context, homeUnitItem.lastUpdateTime)
                // TODO Add handling of other type of HomeUnits (LightSwitchhomeUnit etc...
                //  add some other types of ViewHolder for them)
                additionalLastUpdateTime =
                    if (homeUnitItem.type == HomeUnitType.HOME_LIGHT_SWITCHES && homeUnitItem is LightSwitchHomeUnit) {
                        getLastUpdateTime(root.context, homeUnitItem.switchLastUpdateTime)
                    } else if (homeUnitItem.type == HomeUnitType.HOME_WATER_CIRCULATION && homeUnitItem is WaterCirculationHomeUnit) {
                        getLastUpdateTime(root.context, homeUnitItem.motionLastUpdateTime)
                    } else if (homeUnitItem.type == HomeUnitType.HOME_MCP23017_WATCH_DOG && homeUnitItem is MCP23017WatchDogHomeUnit) {
                        getLastUpdateTime(root.context, homeUnitItem.inputLastUpdateTime)
                    } else {
                        null
                    }
                value = if(homeUnitItem.value is Double || homeUnitItem.value is Float) {
                    String.format("%.2f", homeUnitItem.value)
                } else if(homeUnitItem.type == HomeUnitType.HOME_LIGHT_SWITCHES && homeUnitItem is LightSwitchHomeUnit) {
                    homeUnitItem.switchValue.toString()
                } else if(homeUnitItem.type == HomeUnitType.HOME_WATER_CIRCULATION && homeUnitItem is WaterCirculationHomeUnit) {
                    homeUnitItem.motionValue.toString()
                } else if(homeUnitItem.type == HomeUnitType.HOME_MCP23017_WATCH_DOG && homeUnitItem is MCP23017WatchDogHomeUnit) {
                    homeUnitItem.inputValue.toString()
                } else{
                    homeUnitItem.value.toString()
                }
                secondValue =
                    if (homeUnitItem.type == HomeUnitType.HOME_WATER_CIRCULATION && homeUnitItem is WaterCirculationHomeUnit && homeUnitItem.temperatureValue is Float) {
                        String.format("%.2f", homeUnitItem.temperatureValue)
                    } else {
                        null
                    }
                secondValueLastUpdateTime =
                if (homeUnitItem.type == HomeUnitType.HOME_WATER_CIRCULATION && homeUnitItem is WaterCirculationHomeUnit) {
                    getLastUpdateTime(root.context, homeUnitItem.temperatureLastUpdateTime)
                } else {
                    null
                }
                secondValueVisible = secondValue != null

                homeUnitItemSwitch.setOnCheckedChangeListener { _, isChecked ->
                    Timber.d("OnCheckedChangeListener isChecked: $isChecked homeUnitItem: $homeUnitItem")
                    if (homeUnitItem.value != isChecked) {
                        homeInformationRepository.updateHomeUnitValue(
                            homeUnitItem.type, homeUnitItem.name,
                            isChecked,
                            System.currentTimeMillis(),
                            LAST_TRIGGER_SOURCE_ROOM_HOME_UNITS_LIST
                        )
                    }
                }

                executePendingBindings()
            }
        }
    }
}