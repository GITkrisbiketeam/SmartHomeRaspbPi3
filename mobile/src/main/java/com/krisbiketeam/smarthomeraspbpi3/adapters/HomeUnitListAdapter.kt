package com.krisbiketeam.smarthomeraspbpi3.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.krisbiketeam.data.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.data.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentRoomDetailListItemBinding
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomDetailFragmentDirections
import com.krisbiketeam.smarthomeraspbpi3.ui.RoomListFragment
import timber.log.Timber
import java.util.*

/**
 * Adapter for the [RecyclerView] in [RoomListFragment].
 */
class HomeUnitListAdapter : RecyclerView.Adapter<HomeUnitListAdapter.ViewHolder>() {
    val homeUnits: MutableList<HomeUnit<Any>> = mutableListOf()
    /*val homeUnits: SortedSet<HomeUnit<Any>> = sortedSetOf(Comparator<HomeUnit<Any>> { o1, o2 ->
        o1.name.compareTo(o2.name)
    })*/

    override fun getItemCount(): Int {
        return homeUnits.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val unit = homeUnits[position]
        holder.apply {
            bind(createOnClickListener(unit), unit)
            itemView.tag = unit.name
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FragmentRoomDetailListItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }

    private fun createOnClickListener(item: HomeUnit<Any>): View.OnClickListener {
        return View.OnClickListener { view ->
            Timber.d("onClick item: $item")
            val direction = RoomDetailFragmentDirections.ActionRoomDetailFragmentToHomeUnitDetailFragment(item.room, item.name, item.type)
            view.findNavController().navigate(direction)
        }
    }

    fun getItemIdx(unit: HomeUnit<Any>): Int {
        var idx = -1
        homeUnits.forEachIndexed { index, homeUnit ->
            if (homeUnit.name == unit.name) {
                idx = index
                return@forEachIndexed
            }
        }
        return idx
    }

    class ViewHolder(
            private val binding: FragmentRoomDetailListItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: View.OnClickListener, item: HomeUnit<Any>) {
            binding.apply {
                clickListener = listener
                homeUnit = item
                homeUnitItemSwitch.setOnCheckedChangeListener { _, isChecked ->
                    Timber.d("OnCheckedChangeListener isChecked: $isChecked item: $item")
                    homeUnit?.apply {
                        value = isChecked
                        FirebaseHomeInformationRepository.saveHomeUnit(this)
                    }
                }

                executePendingBindings()
            }
        }
    }
}