package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentTaskListItemCardBinding
import com.krisbiketeam.smarthomeraspbpi3.model.TaskListAdapterModel
import com.krisbiketeam.smarthomeraspbpi3.ui.TaskListFragment
import com.krisbiketeam.smarthomeraspbpi3.ui.TaskListFragmentDirections
import timber.log.Timber

/**
 * Adapter for the [RecyclerView] in [TaskListFragment].
 */
class TaskListAdapter : ListAdapter<TaskListAdapterModel, TaskListAdapter.ViewHolder>(TaskListAdapterDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.apply {
            bind(createOnClickListener(item), item)
            itemView.tag = item
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentTaskListItemCardBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }


    private fun createOnClickListener(item: TaskListAdapterModel): View.OnClickListener {
        return View.OnClickListener { view ->
            Timber.d("onClick")
            val direction = when {
                item.homeUnit != null -> TaskListFragmentDirections.actionTaskListFragmentToHomeUnitDetailFragment(
                        "", item.homeUnit?.name ?: "", item.homeUnit?.type ?: "")
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

        fun bind(listener: View.OnClickListener, item: TaskListAdapterModel) {
            if (binding is FragmentTaskListItemCardBinding) {
                binding.apply {
                    clickListener = listener
                    taskModel = item
                    executePendingBindings()
                }
            }
        }
    }
}