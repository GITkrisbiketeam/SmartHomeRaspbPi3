package com.krisbiketeam.smarthomeraspbpi3.ui

import android.arch.lifecycle.Observer
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import androidx.navigation.fragment.findNavController
import com.krisbiketeam.data.storage.ChildEventType
import com.krisbiketeam.data.storage.dto.StorageUnit
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.adapters.StorageUnitListAdapter
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentRoomDetailBinding
import com.krisbiketeam.smarthomeraspbpi3.di.Params.ROOM_NAME
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.RoomDetailViewModel
import org.koin.android.architecture.ext.getViewModel
import org.koin.android.ext.android.setProperty
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
                val direction = RoomDetailFragmentDirections.ActionRoomDetailFragmentToStorageUnitDetailFragment(
                        roomDetailViewModel.room.value?.name ?: "", "", "")
                findNavController().navigate(direction)
            }
            val adapter = StorageUnitListAdapter()
            storageUnitList.adapter = adapter
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

    private fun subscribeUi(adapter: StorageUnitListAdapter) {
        roomDetailViewModel.storageUnits.observe(viewLifecycleOwner, Observer<Pair<ChildEventType, StorageUnit<Any>>> { pair ->
            pair?.let { (action, unit) ->
                Timber.d("subscribeUi action: $action; unit: $unit")
                when (action) {
                    ChildEventType.NODE_ACTION_CHANGED -> {
                        val idx = adapter.getItemIdx(unit)
                        Timber.d("subscribeUi NODE_ACTION_CHANGED: idx: $idx")
                        if (idx >= 0) {
                            adapter.storageUnits[idx] = unit
                            adapter.notifyItemChanged(idx)
                            Timber.d("subscribeUi NODE_ACTION_CHANGED: unit updated")
                        }
                    }
                    ChildEventType.NODE_ACTION_ADDED -> {
                        Timber.d("storageUnitsDataObserver NODE_ACTION_ADDED")
                        adapter.storageUnits.add(unit)
                        adapter.notifyItemInserted(adapter.itemCount - 1)
                    }
                    ChildEventType.NODE_ACTION_DELETED -> {
                        val idx = adapter.getItemIdx(unit)
                        Timber.d("storageUnitsDataObserver NODE_ACTION_DELETED idx: $idx")
                        if (idx >= 0) {
                            val result = adapter.storageUnits.removeAt(idx)
                            result.let {
                                adapter.notifyItemRemoved(idx)
                                Timber.d("storageUnitsDataObserver NODE_ACTION_DELETED: $result")
                            }
                        }
                    }
                    else -> {
                        Timber.e("storageUnitsDataObserver unsupported action: $action")
                    }
                }
            }
        })
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
