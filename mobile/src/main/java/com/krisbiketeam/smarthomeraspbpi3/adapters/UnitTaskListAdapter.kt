package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.databinding.ViewDataBinding
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.krisbiketeam.data.storage.dto.UnitTask
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentHomeUnitDetailUnitListItemBinding
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentHomeUnitDetailUnitListItemAddBinding
import com.krisbiketeam.smarthomeraspbpi3.ui.HomeUnitDetailFragment
import com.krisbiketeam.smarthomeraspbpi3.ui.HomeUnitDetailFragmentDirections
import timber.log.Timber

private val VIEW_TYPE_NORMAL = 0
private val VIEW_TYPE_ADD_NEW = 1

/**
 * Adapter for the [RecyclerView] in [HomeUnitDetailFragment].
 */
class UnitTaskListAdapter : ListAdapter<UnitTask, UnitTaskListAdapter.ViewHolder>(UnitTaskListAdapterDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val unitTask = getItem(position)
        holder.apply {
            bind(createOnClickListener(), unitTask, getItemViewType(position))
            itemView.tag = unitTask
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == VIEW_TYPE_NORMAL) ViewHolder(FragmentHomeUnitDetailUnitListItemAddBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
        else ViewHolder(FragmentHomeUnitDetailUnitListItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }

    private fun createOnClickListener(): View.OnClickListener {
        return View.OnClickListener { view ->
            Timber.d("onClick")
            val direction = HomeUnitDetailFragmentDirections.ActionHomeUnitDetailFragmentToUnitTaskFragment()
            view.findNavController().navigate(direction)
        }
    }

    override fun getItemViewType(position: Int): Int {
        // last Item is for adding new task
        return if (position == itemCount - 1) VIEW_TYPE_ADD_NEW else VIEW_TYPE_NORMAL
    }

    class ViewHolder(
            private val binding: ViewDataBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: View.OnClickListener, item: UnitTask, itemViewType: Int) {
            when (binding){
                is FragmentHomeUnitDetailUnitListItemBinding -> {
                    binding.apply {
                        clickListener = listener
                        unitTask = item
                    }
                }
                is FragmentHomeUnitDetailUnitListItemAddBinding -> {
                    binding.apply {
                        clickListener = listener
                        unitTask = item
                    }
                }
            }
            binding.executePendingBindings()
        }
    }

}