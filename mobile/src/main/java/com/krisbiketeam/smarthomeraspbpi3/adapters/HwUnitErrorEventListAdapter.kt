package com.krisbiketeam.smarthomeraspbpi3.adapters

import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnitLog
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentHwUnitErrorEventListItemBinding
import com.krisbiketeam.smarthomeraspbpi3.ui.HwUnitErrorEventListFragmentDirections
import com.krisbiketeam.smarthomeraspbpi3.ui.HwUnitErrorEventListFragment
import timber.log.Timber

/**
 * Adapter for the [RecyclerView] in [HwUnitErrorEventListFragment].
 */
class HwUnitErrorEventListAdapter : ListAdapter<HwUnitLog<Any>, HwUnitErrorEventListAdapter.ViewHolder>(HwUnitErrorEventListAdapterDiffCallback()) {
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val hwUnit = getItem(position)
        holder.apply {
            bind(createOnClickListener(hwUnit.name), hwUnit)
            itemView.tag = hwUnit
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentHwUnitErrorEventListItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }

    private fun createOnClickListener(hwUnitName: String): View.OnClickListener {
        return View.OnClickListener {view ->
            Timber.d("onClick")
            view.findNavController().navigate(HwUnitErrorEventListFragmentDirections.actionHwUnitErrorEventListFragmentToAddEditHwUnitFragment(hwUnitName))
        }
    }

    class ViewHolder(
            private val binding: FragmentHwUnitErrorEventListItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: View.OnClickListener, item: HwUnitLog<Any>) {
            binding.apply {
                clickListener = listener
                hwUnit = item
                executePendingBindings()
            }
        }
    }
}