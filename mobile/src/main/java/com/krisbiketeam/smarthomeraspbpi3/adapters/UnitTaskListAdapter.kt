package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.krisbiketeam.data.storage.dto.UnitTask
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentHomeUnitDetailUnitListItemBinding
import com.krisbiketeam.smarthomeraspbpi3.ui.HomeUnitDetailFragment
import com.krisbiketeam.smarthomeraspbpi3.ui.HomeUnitDetailFragmentDirections
import timber.log.Timber

/**
 * Adapter for the [RecyclerView] in [HomeUnitDetailFragment].
 */
class UnitTaskListAdapter : ListAdapter<UnitTask, UnitTaskListAdapter.ViewHolder>(UnitTaskListAdapterDiffCallback()) {
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val taskName = getItem(position)
        holder.apply {
            bind(createOnClickListener(), taskName)
            itemView.tag = taskName
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentHomeUnitDetailUnitListItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }

    private fun createOnClickListener(): View.OnClickListener {
        return View.OnClickListener {view ->
            Timber.d("onClick")
            val direction = HomeUnitDetailFragmentDirections.ActionHomeUnitDetailFragmentToUnitTaskFragment()
            view.findNavController().navigate(direction)
        }
    }

    class ViewHolder(
            private val binding: FragmentHomeUnitDetailUnitListItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: View.OnClickListener, item: UnitTask) {
            binding.apply {
                clickListener = listener
                unitTask = item
                executePendingBindings()
            }
        }
    }
}