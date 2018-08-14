package com.krisbiketeam.smarthomeraspbpi3.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.RoomListViewModel
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.adapters.RoomListAdapter
import com.krisbiketeam.smarthomeraspbpi3.utilities.InjectorUtils

class RoomListFragment : Fragment() {

    private lateinit var viewModel: RoomListViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_room_list, container, false)
        val context = context ?: return view

        val factory = InjectorUtils.provideRoomListViewModelFactory()
        viewModel = ViewModelProviders.of(this, factory).get(RoomListViewModel::class.java)

        val adapter = RoomListAdapter()
        view.findViewById<RecyclerView>(R.id.room_list).adapter = adapter
        subscribeUi(adapter)

        setHasOptionsMenu(true)
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.menu_room_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.filter_zone -> {
                updateData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun subscribeUi(adapter: RoomListAdapter) {
        viewModel.getRooms().observe(viewLifecycleOwner, Observer { rooms ->
            rooms?.let{adapter.submitList(rooms)}
        })
    }

    private fun updateData() {
        with(viewModel) {
            if (isFiltered()) {
                clearGrowZoneNumber()
            } else {
                setGrowZoneNumber(9)
            }
        }
    }
}