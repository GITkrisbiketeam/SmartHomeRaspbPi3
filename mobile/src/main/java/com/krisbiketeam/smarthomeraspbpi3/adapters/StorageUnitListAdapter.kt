package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.krisbiketeam.data.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.data.storage.dto.StorageUnit
import timber.log.Timber
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentRoomDetailListItemBinding
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragment

/**
 * Adapter for the [RecyclerView] in [RoomListFragment].
 */
class StorageUnitListAdapter : RecyclerView.Adapter<StorageUnitListAdapter.ViewHolder>() {
    val storageUnits: MutableList<StorageUnit<Any>> = mutableListOf()

    override fun getItemCount(): Int {
        return storageUnits.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val unit = storageUnits[position]
        holder.apply {
            bind(createOnClickListener(unit), unit)
            itemView.tag = unit.name
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentRoomDetailListItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }

    private fun createOnClickListener(item: StorageUnit<Any>): View.OnClickListener {
        return View.OnClickListener { view ->
            Timber.d("onClick item: $item")
            if (item.unitsTasks.find { it.hardwareUnitName != null } != null) {
                when (item.value) {
                    is Boolean -> {
                        item.value = (item.value as Boolean).not()
                        FirebaseHomeInformationRepository.saveStorageUnit(item)
                    }
                }
            }

            /*val direction = RoomListFragmentDirections.ActionRoomListFragmentToRoomDetailFragment(name)
            it.findNavController().navigate(direction)*/
        }
    }

    fun getItemIdx(unit: StorageUnit<Any>): Int {
        var idx = -1
        storageUnits.forEachIndexed { index, storageUnit ->
            if (storageUnit.name == unit.name) {
                idx = index
                return@forEachIndexed
            }
        }
        return idx
    }

    class ViewHolder(
            private val binding: FragmentRoomDetailListItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: View.OnClickListener, item: StorageUnit<Any>) {
            binding.apply {
                clickListener = listener
                this.storageUnit = item
                executePendingBindings()
            }
        }
    }
}