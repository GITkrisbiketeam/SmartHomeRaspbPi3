package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.krisbiketeam.data.storage.dto.UnitTask
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentStorageUnitDetailUnitListItemBinding
import com.krisbiketeam.smarthomeraspbpi3.ui.StorageUnitDetailFragment
import com.krisbiketeam.smarthomeraspbpi3.ui.StorageUnitDetailFragmentDirections
import timber.log.Timber

/**
 * Adapter for the [RecyclerView] in [StorageUnitDetailFragment].
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
        return ViewHolder(FragmentStorageUnitDetailUnitListItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }

    private fun createOnClickListener(): View.OnClickListener {
        return View.OnClickListener {view ->
            Timber.d("onClick")
            val direction = StorageUnitDetailFragmentDirections.ActionStorageUnitDetailFragmentToUnitTaskFragment()
            view.findNavController().navigate(direction)
        }
    }

    class ViewHolder(
            private val binding: FragmentStorageUnitDetailUnitListItemBinding
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