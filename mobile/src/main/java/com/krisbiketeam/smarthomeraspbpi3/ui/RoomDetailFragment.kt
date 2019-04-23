package com.krisbiketeam.smarthomeraspbpi3.ui

import androidx.lifecycle.Observer
import androidx.databinding.DataBindingUtil
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.*
import androidx.navigation.fragment.findNavController
import com.krisbiketeam.smarthomeraspbpi3.common.storage.ChildEventType
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.adapters.RoomDetailHomeUnitListAdapter
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentRoomDetailBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.RoomDetailViewModel
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

/**
 * A fragment representing a single Room detail screen.
 */
class RoomDetailFragment : Fragment() {

    private val roomDetailViewModel: RoomDetailViewModel by viewModel {
        parametersOf(arguments?.let { RoomDetailFragmentArgs.fromBundle(it).roomName}?: "")
    }

    init {
        Timber.w("init $this")
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        Timber.w("onCreateView $this")
        val binding: FragmentRoomDetailBinding = DataBindingUtil.inflate<FragmentRoomDetailBinding>(
                inflater, R.layout.fragment_room_detail, container, false).apply {
            viewModel = roomDetailViewModel
            setLifecycleOwner(this@RoomDetailFragment)
            fab.setOnClickListener {
                val direction = RoomDetailFragmentDirections.actionRoomDetailFragmentToHomeUnitDetailFragment(
                        roomDetailViewModel.room.value?.name ?: "", "", "")
                findNavController().navigate(direction)
            }
            val adapter = RoomDetailHomeUnitListAdapter()
            homeUnitList.adapter = adapter
            subscribeUi(adapter)
        }

        roomDetailViewModel.isEditMode.observe(viewLifecycleOwner, Observer {
            activity?.invalidateOptionsMenu()
        })

        setHasOptionsMenu(true)

        return binding.root
    }

    private fun subscribeUi(adapter: RoomDetailHomeUnitListAdapter) {
        roomDetailViewModel.homeUnitsMap.observe(viewLifecycleOwner, Observer<MutableMap<String, HomeUnit<Any?>>> { homeUnitsMap ->
            Timber.d("subscribeUi homeUnitsMap: $homeUnitsMap")
            adapter.submitList(homeUnitsMap.values.toList())
        })
    }

    /*private fun subscribeUi(adapter: RoomDetailHomeUnitListAdapter) {
        roomDetailViewModel.homeUnits.observe(viewLifecycleOwner, Observer<Pair<ChildEventType, HomeUnit<Any>>> { pair ->
            pair?.let { (action, unit) ->
                Timber.d("subscribeUi action: $action; unit: $unit")
                when (action) {
                    ChildEventType.NODE_ACTION_CHANGED -> {
                        val idx = adapter.getItemIdx(unit)
                        Timber.d("homeUnitsDataObserver NODE_ACTION_CHANGED: idx: $idx")
                        if (idx >= 0) {
                            adapter.homeUnits[idx] = unit
                            adapter.notifyItemChanged(idx)
                            Timber.d("subscribeUi NODE_ACTION_CHANGED: unit updated")
                        }
                    }
                    ChildEventType.NODE_ACTION_ADDED -> {
                        Timber.d("homeUnitsDataObserver NODE_ACTION_ADDED list: ${adapter.homeUnits}")
                        val idx = adapter.homeUnits.addSorted(unit)
                        if (idx >= 0) {
                            Timber.d("homeUnitsDataObserver NODE_ACTION_ADDED idx: $idx")
                            adapter.notifyItemInserted(idx)
                        }
                    }
                    ChildEventType.NODE_ACTION_DELETED -> {
                        val idx = adapter.getItemIdx(unit)
                        Timber.d("homeUnitsDataObserver NODE_ACTION_DELETED idx: $idx")
                        if (idx >= 0) {
                            val result = adapter.homeUnits.removeAt(idx)
                            result.let {
                                adapter.notifyItemRemoved(idx)
                                Timber.d("homeUnitsDataObserver NODE_ACTION_DELETED: $result")
                            }
                        }
                    }
                    else -> {
                        Timber.e("homeUnitsDataObserver unsupported action: $action")
                    }
                }
            }
        })
    }

    private fun MutableList<HomeUnit<Any>>.addSorted(homeUnit: HomeUnit<Any>): Int {
        if (contains(homeUnit)) {
            Timber.e("addSorted this unit is already on the list")
            return -1
        }
        forEachIndexed { index, unit ->
            if (unit.name >= homeUnit.name) {
                add(index, homeUnit)
                return index
            }
        }
        add(homeUnit)
        return size - 1
    }*/

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_room_detail, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        when (roomDetailViewModel.isEditMode.value) {
            true -> {
                menu.findItem((R.id.action_finish))?.isVisible = true
                menu.findItem((R.id.action_edit))?.isVisible = false
            }
            else -> {
                menu.findItem((R.id.action_finish))?.isVisible = false
                menu.findItem((R.id.action_edit))?.isVisible = true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit -> {
                Timber.e("onOptionsItemSelected EDIT : ${roomDetailViewModel.isEditMode}")
                roomDetailViewModel.isEditMode.value = true
                return true
            }
           R.id.action_finish -> {
                roomDetailViewModel.isEditMode.value = false
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
