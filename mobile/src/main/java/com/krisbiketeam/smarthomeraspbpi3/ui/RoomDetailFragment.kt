package com.krisbiketeam.smarthomeraspbpi3.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.adapters.StorageUnitAdapter
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentRoomDetailBinding
import com.krisbiketeam.smarthomeraspbpi3.utilities.InjectorUtils
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.RoomDetailViewModel
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
        val roomName = RoomDetailFragmentArgs.fromBundle(arguments).roomName

        val factory = InjectorUtils.provideRoomDetailViewModelFactory(roomName)
        roomDetailViewModel = ViewModelProviders.of(this, factory)
                .get(RoomDetailViewModel::class.java)

        val binding: FragmentRoomDetailBinding = DataBindingUtil.inflate<FragmentRoomDetailBinding>(
                inflater, R.layout.fragment_room_detail, container, false).apply {
            viewModel = roomDetailViewModel
            setLifecycleOwner(this@RoomDetailFragment)
            fab.setOnClickListener { view ->
                //Snackbar.make(view, R.string.added_plant_to_garden, Snackbar.LENGTH_LONG).show()
            }
            val adapter = StorageUnitAdapter()
            storageUnitList.adapter = adapter
            subscribeUi(adapter)
        }

        roomDetailViewModel.room.observe(this, Observer { room ->

        })

        roomDetailViewModel.isEditMode.observe(this, Observer { isEditMode ->
            activity?.let {
                it.invalidateOptionsMenu()
            }
        })

        setHasOptionsMenu(true)

        return binding.root
    }

    private fun subscribeUi(adapter: StorageUnitAdapter) {
        roomDetailViewModel.storageUnits.observe(viewLifecycleOwner, Observer { storageUnits ->
            storageUnits?.let{adapter.submitList(storageUnits)}
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.menu_room_detail, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        super.onPrepareOptionsMenu(menu)
        when(roomDetailViewModel.isEditMode.value) {
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

    companion object {

        /**
         * The fragment argument representing the item ID that this fragment
         * represents.
         */
        const val ARG_ITEM_ID = "item_id"

        /**
         * Create a new instance of RoomDetailFragment, initialized with a room Name.
         */
        fun newInstance(roomName: String): RoomDetailFragment {

            // Supply room ID as an argument.
            val bundle = Bundle().apply { putString(ARG_ITEM_ID, roomName) }
            return RoomDetailFragment().apply { arguments = bundle }
        }
    }
}
