package com.krisbiketeam.smarthomeraspbpi3.ui

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.adapters.RoomDetailHomeUnitListAdapter
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentRoomDetailBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.RoomDetailViewModel
import org.koin.android.ext.android.inject
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
            lifecycleOwner = this@RoomDetailFragment
            fab.setOnClickListener {
                val direction = RoomDetailFragmentDirections.actionRoomDetailFragmentToHomeUnitDetailFragment(
                        roomDetailViewModel.room.value?.name ?: "", "", "")
                findNavController().navigate(direction)
            }
            val adapter: RoomDetailHomeUnitListAdapter by inject()
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_room_detail, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        when (roomDetailViewModel.isEditMode.value) {
            true -> {
                menu.findItem((R.id.action_discard))?.isVisible = true
                menu.findItem((R.id.action_save))?.isVisible = true
                menu.findItem((R.id.action_delete))?.isVisible =
                        arguments?.let {
                            RoomDetailFragmentArgs.fromBundle(it).roomName.isNotEmpty()
                        } ?: false
                menu.findItem((R.id.action_edit))?.isVisible = false
            }
            else -> {
                menu.findItem((R.id.action_discard))?.isVisible = false
                menu.findItem((R.id.action_save))?.isVisible = false
                menu.findItem((R.id.action_delete))?.isVisible = false
                menu.findItem((R.id.action_edit))?.isVisible = true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // If showing progress do not allow app bar actions
        if (roomDetailViewModel.showProgress.value == true) {
            return false
        }
        return when (item.itemId) {
            R.id.action_edit -> {
                Timber.e("onOptionsItemSelected EDIT : ${roomDetailViewModel.isEditMode}")
                roomDetailViewModel.isEditMode.value = true
                return true
            }
            R.id.action_save -> {
                val (messageId, positiveButtonId) = roomDetailViewModel.actionSave()
                Timber.d("action_save ${getString(messageId)}")
                if (messageId > 0) {
                    positiveButtonId?.let { buttonId ->
                        showDialog(messageId, buttonId) {
                            roomDetailViewModel.saveChanges().addOnCompleteListener {
                                // navigate back Up from this Fragment
                                findNavController().navigateUp()
                            }
                        }
                    } ?: view?.run {
                        Snackbar.make(this, messageId, Snackbar.LENGTH_SHORT).show()
                    }
                } else {
                    //hmm, this should not happen
                    Timber.e("action_save we got empty message This should not happen")
                }
                return true
            }
            R.id.action_discard -> {
                if (roomDetailViewModel.noChangesMade()) {
                    // navigate back Up from this Fragment
                    findNavController().navigateUp()
                } else {
                    showDialog(R.string.add_edit_home_unit_discard_changes, R.string.menu_discard) {
                        roomDetailViewModel.actionDiscard()
                        // navigate back Up from this Fragment
                        findNavController().navigateUp()
                    }
                }
                return true
            }
            R.id.action_delete -> {
                showDialog(R.string.add_edit_home_unit_delete_home_unit_prompt, R.string.menu_delete) {
                    roomDetailViewModel.actionDeleteHomeUnit().addOnCompleteListener {
                        // navigate back Up from this Fragment
                        findNavController().navigateUp()
                    }
                }
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDialog(messageId: Int, positiveButtonId: Int, positiveButtonInvoked: () -> Unit) {
        context?.let {

            AlertDialog.Builder(it)
                    .setMessage(messageId)
                    .setPositiveButton(positiveButtonId) { _, _ -> positiveButtonInvoked() }
                    .setNegativeButton(R.string.cancel) { _, _ -> }
                    .show()
        }
    }
}
