package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HOME_LIGHT_SWITCHES
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentTaskListItemCardBinding
import com.krisbiketeam.smarthomeraspbpi3.model.TaskListAdapterModel
import com.krisbiketeam.smarthomeraspbpi3.ui.TaskListFragment
import com.krisbiketeam.smarthomeraspbpi3.ui.TaskListFragmentDirections
import timber.log.Timber

/**
 * Adapter for the [RecyclerView] in [TaskListFragment].
 */
class TaskListAdapter(private val homeInformationRepository: FirebaseHomeInformationRepository) : ListAdapter<TaskListAdapterModel, TaskListAdapter.ViewHolder>(TaskListAdapterDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.apply {
            bind(createOnClickListener(item), item)
            itemView.tag = item
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentTaskListItemCardBinding.inflate(
                LayoutInflater.from(parent.context), parent, false),
                homeInformationRepository)
    }


    private fun createOnClickListener(item: TaskListAdapterModel): View.OnClickListener {
        return View.OnClickListener { view ->
            Timber.d("onClick")
            val direction = when {
                item.homeUnit != null -> TaskListFragmentDirections.actionTaskListFragmentToHomeUnitDetailFragment(
                        "", item.homeUnit?.name ?: "", item.homeUnit?.type ?: "")
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

                    value = if(item.homeUnit?.value is Double || item.homeUnit?.value is Float) {
                        String.format("%.2f", item.homeUnit?.value)
                    } else if(item.homeUnit?.type == HOME_LIGHT_SWITCHES) {
                        item.homeUnit?.secondValue.toString()
                    } else{
                        item.homeUnit?.value.toString()
                    }

                    taskItemValueSwitch.setOnCheckedChangeListener { _, isChecked ->
                        Timber.d("OnCheckedChangeListener isChecked: $isChecked item: $item")
                        if (item.homeUnit?.value != isChecked) {
                            item.homeUnit?.copy()?.also { unit ->
                                unit.value = isChecked
                                unit.lastUpdateTime = System.currentTimeMillis()
                                homeInformationRepository.updateHomeUnitValue(unit)
                            }
                        }
                    }

                    executePendingBindings()
                }
            }
        }
    }
}