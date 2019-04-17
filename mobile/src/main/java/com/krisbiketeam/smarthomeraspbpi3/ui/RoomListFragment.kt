package com.krisbiketeam.smarthomeraspbpi3.ui

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.adapters.RoomListAdapter
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentRoomListBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.RoomListViewModel
import org.koin.android.viewmodel.ext.android.viewModel
import timber.log.Timber

class RoomListFragment : Fragment() {

    private val roomListViewModel by viewModel<RoomListViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentRoomListBinding = DataBindingUtil.inflate<FragmentRoomListBinding>(
                inflater, R.layout.fragment_room_list, container, false).apply {
            viewModel = roomListViewModel
            setLifecycleOwner(this@RoomListFragment)
            fab.setOnClickListener {
                val direction = RoomListFragmentDirections.actionRoomListFragmentToNewRoomDialogFragment()
                findNavController().navigate(direction)
            }
            val adapter = RoomListAdapter()
            roomList.adapter = adapter
            subscribeUi(adapter)
        }
        roomListViewModel.isEditMode.observe(viewLifecycleOwner, Observer {
            activity?.invalidateOptionsMenu()
        })
        setHasOptionsMenu(true)
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


    private fun subscribeUi(adapter: RoomListAdapter) {
        roomListViewModel.roomList.observe(viewLifecycleOwner, Observer { rooms ->
            rooms?.let{adapter.submitList(rooms)}
        })
    }
}