package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HwUnitLog
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentHwUnitErrorLogsListItemBinding
import com.krisbiketeam.smarthomeraspbpi3.ui.HwUnitErrorLogsFragment
import com.krisbiketeam.smarthomeraspbpi3.ui.HwUnitErrorLogsFragmentDirections
import timber.log.Timber

/**
 * Adapter for the [RecyclerView] in [HwUnitErrorLogsFragment].
 */
class HwUnitErrorLogsListAdapter : ListAdapter<HwUnitLog<Any>, HwUnitErrorLogsListAdapter.ViewHolder>(HwUnitErrorEventListAdapterDiffCallback()) {
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val hwUnit = getItem(position)
        holder.apply {
            bind(createOnClickListener(hwUnit.name), hwUnit)
            itemView.tag = hwUnit
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            FragmentHwUnitErrorLogsListItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false))
    }

    private fun createOnClickListener(hwUnitName: String): View.OnClickListener {
        return View.OnClickListener { view ->
            Timber.d("onClick")
            view.findNavController().navigate(HwUnitErrorLogsFragmentDirections.actionHwUnitErrorLogsFragmentToAddEditHwUnitFragment(hwUnitName))
        }
    }

    class ViewHolder(
        private val binding: FragmentHwUnitErrorLogsListItemBinding
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