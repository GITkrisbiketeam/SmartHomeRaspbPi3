package com.krisbiketeam.smarthomeraspbpi3.ui

import android.arch.lifecycle.Observer
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.view.*
import androidx.navigation.fragment.findNavController
import com.krisbiketeam.data.storage.ChildEventType
import com.krisbiketeam.data.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.adapters.HomeUnitListAdapter
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentRoomDetailBinding
import com.krisbiketeam.smarthomeraspbpi3.di.Params.ROOM_NAME
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.RoomDetailViewModel
import org.koin.android.ext.android.setProperty
import org.koin.android.viewmodel.ext.android.getViewModel
import timber.log.Timber

/**
 * A fragment representing a single Room detail screen.
 */
class RoomDetailFragment : Fragment() {

    private lateinit var roomDetailViewModel: RoomDetailViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // set ROOM_NAME property fo Koin injection
        setProperty(ROOM_NAME, RoomDetailFragmentArgs.fromBundle(arguments).roomName)
        roomDetailViewModel = getViewModel()

        val binding: FragmentRoomDetailBinding = DataBindingUtil.inflate<FragmentRoomDetailBinding>(
                inflater, R.layout.fragment_room_detail, container, false).apply {
            viewModel = roomDetailViewModel
            setLifecycleOwner(this@RoomDetailFragment)
            fab.setOnClickListener {
                val direction = RoomDetailFragmentDirections.ActionRoomDetailFragmentToHomeUnitDetailFragment(
                        roomDetailViewModel.room.value?.name ?: "", "", "")
                findNavController().navigate(direction)
            }
            val adapter = HomeUnitListAdapter()
            homeUnitList.adapter = adapter
            subscribeUi(adapter)
        }

        roomDetailViewModel.room.observe(viewLifecycleOwner, Observer {

        })

        roomDetailViewModel.isEditMode.observe(viewLifecycleOwner, Observer {
            activity?.invalidateOptionsMenu()
        })

        setHasOptionsMenu(true)

        return binding.root
    }

    private fun subscribeUi(adapter: HomeUnitListAdapter) {
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
                        Timber.d("homeUnitsDataObserver NODE_ACTION_ADDED")
                        /*val idx = adapter.homeUnits.addSorted(unit)
                        Timber.d("homeUnitsDataObserver NODE_ACTION_ADDED idx: $idx")
                        adapter.notifyItemInserted(idx)*/
                        adapter.homeUnits.add(unit)
                        adapter.notifyItemInserted(adapter.itemCount - 1)
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
    fun MutableList<HomeUnit<Any>>.addSorted(homeUnit: HomeUnit<Any>): Int{
        forEachIndexed { index, unit ->
            Timber.e("addSorted index: $index unit.name: ${unit.name} homeUnit.name: ${homeUnit.name}")
            if (unit.name.compareTo(homeUnit.name)<0) {
                add(index, homeUnit)
                return index
            }
        }
        add(homeUnit)
        return 0
    }
    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.menu_room_detail, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        super.onPrepareOptionsMenu(menu)
        when (roomDetailViewModel.isEditMode.value) {
            true -> {
                menu?.findItem((R.id.action_discard))?.isVisible = true
                menu?.findItem((R.id.action_save))?.isVisible = true
                menu?.findItem((R.id.action_edit))?.isVisible = false
            }
            else -> {
                menu?.findItem((R.id.action_discard))?.isVisible = false
                menu?.findItem((R.id.action_save))?.isVisible = false
                menu?.findItem((R.id.action_edit))?.isVisible = true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.action_edit -> {
                Timber.e("onOptionsItemSelected EDIT : ${roomDetailViewModel.isEditMode}")
                roomDetailViewModel.isEditMode.value = true
                return true
            }
            R.id.action_save -> {
                return true
            }
            R.id.action_discard -> {
                roomDetailViewModel.isEditMode.value = false
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
