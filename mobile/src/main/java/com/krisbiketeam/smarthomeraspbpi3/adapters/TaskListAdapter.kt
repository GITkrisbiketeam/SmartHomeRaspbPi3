package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.GenericHomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.LightSwitchHomeUnit
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.LAST_TRIGGER_SOURCE_TASK_LIST
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentTaskListItemCardBinding
import com.krisbiketeam.smarthomeraspbpi3.model.TaskListAdapterModel
import com.krisbiketeam.smarthomeraspbpi3.ui.TaskListFragment
import com.krisbiketeam.smarthomeraspbpi3.ui.TaskListFragmentDirections
import timber.log.Timber

/**
 * Adapter for the [RecyclerView] in [TaskListFragment].
 */
class TaskListAdapter(private val homeInformationRepository: FirebaseHomeInformationRepository) :
    ListAdapter<TaskListAdapterModel, TaskListAdapter.ViewHolder>(TaskListAdapterDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.apply {
            bind(createOnClickListener(item), item)
            itemView.tag = item
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            FragmentTaskListItemCardBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ),
            homeInformationRepository
        )
    }


    private fun createOnClickListener(item: TaskListAdapterModel): View.OnClickListener {
        return View.OnClickListener { view ->
            Timber.d("onClick")
            val homeUnit = item.homeUnit
            val direction = when {
                homeUnit != null && homeUnit.type == HomeUnitType.HOME_LIGHT_SWITCHES_V2 -> TaskListFragmentDirections.actionTaskListFragmentToHomeUnitLightSwitchDetailFragment(
                    "", homeUnit.name
                )
                homeUnit != null -> TaskListFragmentDirections.actionTaskListFragmentToHomeUnitGenericDetailFragment(
                    "", homeUnit.name, homeUnit.type
                )
                else -> null
            }
            direction?.let {
                view.findNavController().navigate(it)
            }
        }
    }

    class ViewHolder(
        private val binding: ViewDataBinding,
        private val homeInformationRepository: FirebaseHomeInformationRepository
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: View.OnClickListener, item: TaskListAdapterModel) {
            if (binding is FragmentTaskListItemCardBinding) {
                binding.apply {
                    clickListener = listener
                    taskModel = item
                    // TODO Add handling of other type of HomeUnits (LightSwitchhomeUnit etc...
                    //  add some other types of ViewHolder for them)
                    value = item.homeUnit?.let { homeUnit ->
                        if (homeUnit.value is Double || homeUnit.value is Float) {
                            String.format("%.2f", homeUnit.value)
                        } else if (homeUnit.type == HomeUnitType.HOME_LIGHT_SWITCHES && homeUnit is GenericHomeUnit) {
                            homeUnit.secondValue.toString()
                        } else if (homeUnit.type == HomeUnitType.HOME_LIGHT_SWITCHES_V2 && homeUnit is LightSwitchHomeUnit) {
                            homeUnit.switchValue.toString()
                        } else {
                            homeUnit.value.toString()
                        }
                    } ?: "N/A"

                    taskItemValueSwitch.setOnCheckedChangeListener { _, isChecked ->
                        Timber.d("OnCheckedChangeListener isChecked: $isChecked item: $item")
                        item.homeUnit?.let { homeUnit ->
                            if (homeUnit.value != isChecked && homeUnit is GenericHomeUnit) {
                                homeUnit.copy().also { unit ->
                                    unit.value = isChecked
                                    unit.lastUpdateTime = System.currentTimeMillis()
                                    unit.lastTriggerSource = LAST_TRIGGER_SOURCE_TASK_LIST
                                    homeInformationRepository.updateHomeUnitValue(unit)
                                }
                            }
                        }

                    }

                    executePendingBindings()
                }
            }
        }
    }
}