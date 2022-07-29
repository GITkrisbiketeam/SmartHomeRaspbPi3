package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.UnitTask
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentHomeUnitDetailUnitTaskListItemAddBinding
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentHomeUnitDetailUnitTaskListItemBinding
import com.krisbiketeam.smarthomeraspbpi3.ui.HomeUnitGenericDetailFragment
import com.krisbiketeam.smarthomeraspbpi3.ui.HomeUnitGenericDetailFragmentDirections
import timber.log.Timber

private const val VIEW_TYPE_NORMAL = 0
private const val VIEW_TYPE_ADD_NEW = 1

/**
 * Adapter for the [RecyclerView] in [HomeUnitGenericDetailFragment].
 */
class UnitTaskListAdapter(
    private val homeRepository: FirebaseHomeInformationRepository,
    private val unitName: String?,
    private val unitType: HomeUnitType
) : ListAdapter<UnitTask, UnitTaskListAdapter.ViewHolder>(HomeUnitUnitTaskListAdapterDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val unitTask = getItem(position)
        holder.apply {
            bind(
                createOnClickListener(unitTask.name),
                unitTask,
                this@UnitTaskListAdapter::saveSwitchValue
            )
            itemView.tag = unitTask
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == VIEW_TYPE_NORMAL) {
            ViewHolder(
                FragmentHomeUnitDetailUnitTaskListItemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        } else {
            ViewHolder(
                FragmentHomeUnitDetailUnitTaskListItemAddBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }
    }

    private fun createOnClickListener(taskName: String?): View.OnClickListener {
        return View.OnClickListener { view ->
            Timber.d("onClick taskName: $taskName")
            if (unitType != HomeUnitType.UNKNOWN && !unitName.isNullOrEmpty()) {
                view.findNavController().navigate(
                    HomeUnitGenericDetailFragmentDirections.actionHomeUnitGenericDetailFragmentToUnitTaskFragment(
                        taskName,
                        unitName,
                        unitType
                    )
                )
            }
        }
    }

    private fun saveSwitchValue(unitTask: UnitTask, isChecked: Boolean) {
        Timber.d("saveSwitchValue unitTask: $unitTask isChecked: $isChecked")
        if (unitType != HomeUnitType.UNKNOWN && !unitName.isNullOrEmpty()) {
            homeRepository.saveUnitTask(
                unitType,
                unitName,
                unitTask.apply { disabled = !isChecked })
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position)?.name.isNullOrEmpty()) VIEW_TYPE_ADD_NEW else VIEW_TYPE_NORMAL
    }

    class ViewHolder(
        private val binding: ViewDataBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            listener: View.OnClickListener,
            item: UnitTask,
            enabledSwitch: (UnitTask, Boolean) -> Unit
        ) {
            when (binding) {
                is FragmentHomeUnitDetailUnitTaskListItemBinding -> {
                    binding.apply {
                        clickListener = listener
                        unitTask = item
                        unitTaskItemSwitch.setOnCheckedChangeListener { _, isChecked ->
                            enabledSwitch(
                                item,
                                isChecked
                            )
                        }
                    }
                }
                is FragmentHomeUnitDetailUnitTaskListItemAddBinding -> {
                    binding.apply {
                        clickListener = listener
                    }
                }
            }
            binding.executePendingBindings()
        }
    }

}