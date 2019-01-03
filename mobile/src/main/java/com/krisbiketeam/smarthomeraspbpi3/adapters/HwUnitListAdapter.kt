package com.krisbiketeam.smarthomeraspbpi3.adapters

import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnit
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentHwUnitListItemBinding
import com.krisbiketeam.smarthomeraspbpi3.ui.HwUnitListFragmentDirections
import com.krisbiketeam.smarthomeraspbpi3.ui.HwUnitListFragment
import timber.log.Timber

/**
 * Adapter for the [RecyclerView] in [HwUnitListFragment].
 */
class HwUnitListAdapter : ListAdapter<HwUnit, HwUnitListAdapter.ViewHolder>(HwUnitListAdapterDiffCallback()) {
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val hwUnit = getItem(position)
        holder.apply {
            bind(createOnClickListener(hwUnit.name), hwUnit)
            itemView.tag = hwUnit
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentHwUnitListItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }

    private fun createOnClickListener(hwUnitName: String): View.OnClickListener {
        return View.OnClickListener {view ->
            Timber.d("onClick")
            view.findNavController().navigate(HwUnitListFragmentDirections.ActionHwUnitListFragmentToAddEditHwUnitFragment(hwUnitName))
        }
    }

    class ViewHolder(
            private val binding: FragmentHwUnitListItemBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: View.OnClickListener, item: HwUnit) {
            binding.apply {
                clickListener = listener
                hwUnit = item
                executePendingBindings()
            }
        }
    }
}