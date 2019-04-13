package com.krisbiketeam.smarthomeraspbpi3.adapters

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.UnitTask
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentHomeUnitDetailUnitTaskListItemBinding
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentHomeUnitDetailUnitTaskListItemAddBinding
import com.krisbiketeam.smarthomeraspbpi3.ui.HomeUnitDetailFragment
import com.krisbiketeam.smarthomeraspbpi3.ui.HomeUnitDetailFragmentDirections
import timber.log.Timber

private const val VIEW_TYPE_NORMAL = 0
private const val VIEW_TYPE_ADD_NEW = 1

/**
 * Adapter for the [RecyclerView] in [HomeUnitDetailFragment].
 */
class UnitTaskListAdapter(private val unitName: String,
                          private val unitType: String) : ListAdapter<UnitTask, UnitTaskListAdapter.ViewHolder>(HomeUnitUnitTaskListAdapterDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val unitTask = getItem(position)
        holder.apply {
            bind(createOnClickListener(unitTask.name), unitTask)
            itemView.tag = unitTask
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == VIEW_TYPE_NORMAL) {
            ViewHolder(FragmentHomeUnitDetailUnitTaskListItemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false))
        } else {
            ViewHolder(FragmentHomeUnitDetailUnitTaskListItemAddBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false))
        }
    }

    private fun createOnClickListener(taskName: String): View.OnClickListener {
        return View.OnClickListener { view ->
            Timber.d("onClick taskName: $taskName")
            view.findNavController().navigate(HomeUnitDetailFragmentDirections.actionHomeUnitDetailFragmentToUnitTaskFragment(taskName, unitName, unitType))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position)?.name.isNullOrEmpty()) VIEW_TYPE_ADD_NEW else VIEW_TYPE_NORMAL
    }

    class ViewHolder(
            private val binding: ViewDataBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: View.OnClickListener, item: UnitTask) {
            when (binding){
                is FragmentHomeUnitDetailUnitTaskListItemBinding -> {
                    binding.apply {
                        clickListener = listener
                        unitTask = item
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