package com.krisbiketeam.smarthomeraspbpi3.ui

import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.adapters.RoomWithHomeUnitListAdapter
import com.krisbiketeam.smarthomeraspbpi3.common.Analytics
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentRoomListBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.RoomListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import timber.log.Timber


class RoomListFragment : Fragment() {

    private val roomListViewModel by viewModel<RoomListViewModel>()

    private val analytics: Analytics by inject()

    @ExperimentalCoroutinesApi
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentRoomListBinding = DataBindingUtil.inflate<FragmentRoomListBinding>(
                inflater, R.layout.fragment_room_list, container, false).apply {
            viewModel = roomListViewModel
            lifecycleOwner = this@RoomListFragment
            fab.setOnClickListener {
                val direction = RoomListFragmentDirections.actionRoomListFragmentToNewRoomDialogFragment()
                findNavController().navigate(direction)
            }
            val adapter = RoomWithHomeUnitListAdapter()
            roomList.layoutManager = GridLayoutManager(requireContext(), 2)
            roomList.adapter = adapter
            subscribeRoomHomeUnitList(adapter)
        }
        roomListViewModel.isEditMode.observe(viewLifecycleOwner, {
            activity?.invalidateOptionsMenu()
        })
        setHasOptionsMenu(true)

        analytics.firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundleOf(
                FirebaseAnalytics.Param.SCREEN_NAME to this::class.simpleName
        ))

        return binding.root
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_room_list, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        when (roomListViewModel.isEditMode.value) {
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
                Timber.e("onOptionsItemSelected EDIT : ${roomListViewModel.isEditMode}")
                roomListViewModel.isEditMode.value = true
                return true
            }
            R.id.action_finish -> {
                roomListViewModel.isEditMode.value = false
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @ExperimentalCoroutinesApi
    private fun subscribeRoomHomeUnitList(adapter: RoomWithHomeUnitListAdapter) {
        /*roomListViewModel.roomHomeUnitsMap.observe(viewLifecycleOwner, Observer { roomHomeUnitsMap ->
            Timber.d("subscribeUi roomHomeUnitsMap: $roomHomeUnitsMap")
            val roomHomeUnitListSorted = roomHomeUnitsMap.values.sortedWith(Comparator { a, b ->
                when {
                    a is Room && b is HomeUnit<*> -> -1
                    a is HomeUnit<*> && b is Room -> 1
                    a is HomeUnit<*> && b is String -> 1
                    a is Room && b is String -> -1
                    else -> 0
                }
            })
            adapter.submitList(roomHomeUnitListSorted)
        })*/
        roomListViewModel.roomWithHomeUnitsListFromFlow.observe(viewLifecycleOwner,
                                                                { roomWithHomeUnitsList ->
                                                                    Timber.d("subscribeUi roomWithHomeUnitsList: $roomWithHomeUnitsList")
                                                                    adapter.submitList(roomWithHomeUnitsList)
                                                                })
    }
}