package com.krisbiketeam.smarthomeraspbpi3.ui

import androidx.lifecycle.Observer
import androidx.databinding.DataBindingUtil
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import android.view.*
import androidx.navigation.fragment.findNavController
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentHomeUnitDetailBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.HomeUnitDetailViewModel
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class HomeUnitDetailFragment : Fragment() {
    private val homeUnitDetailViewModel: HomeUnitDetailViewModel by viewModel {
        parametersOf(
                arguments?.let { HomeUnitDetailFragmentArgs.fromBundle(it).roomName} ?: "",
                arguments?.let { HomeUnitDetailFragmentArgs.fromBundle(it).homeUnitName} ?: "",
                arguments?.let { HomeUnitDetailFragmentArgs.fromBundle(it).homeUnitType} ?: "")
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val rootBinding = DataBindingUtil.inflate<FragmentHomeUnitDetailBinding>(
                inflater, R.layout.fragment_home_unit_detail, container, false).apply {
            viewModel = homeUnitDetailViewModel
            lifecycleOwner = this@HomeUnitDetailFragment
        }

        homeUnitDetailViewModel.isEditMode.observe(viewLifecycleOwner, Observer { isEditMode ->
            // in Edit Mode we need to listen for homeUnitList, as there is no reference in xml layout to trigger its observer, but can we find some better way???
            Timber.d("onCreateView isEditMode: $isEditMode")
            if (isEditMode == true) {
                homeUnitDetailViewModel.homeUnitList.observe(viewLifecycleOwner, Observer { })
            } else {
                homeUnitDetailViewModel.homeUnitList.removeObservers(viewLifecycleOwner)
            }
            activity?.invalidateOptionsMenu()
            // Animate Layout edit mode change
            TransitionManager.beginDelayedTransition(rootBinding.root as ViewGroup, Fade())
        })
        homeUnitDetailViewModel.unitTaskList.observe(viewLifecycleOwner, Observer { taskList ->
            taskList?.let {
                Timber.d("onCreateView unitTaskList Observer it: $it")
                // Update UnitTask list
                homeUnitDetailViewModel.unitTaskListAdapter.submitList(it.values.toMutableList())
            }
        })

        setHasOptionsMenu(true)

        return rootBinding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home_unit_details, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        when (homeUnitDetailViewModel.isEditMode.value) {
            true -> {
                menu.findItem((R.id.action_discard))?.isVisible = true
                menu.findItem((R.id.action_save))?.isVisible = true
                menu.findItem((R.id.action_delete))?.isVisible =
                        arguments?.let {
                            HomeUnitDetailFragmentArgs.fromBundle(it).homeUnitName.isNotEmpty()
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
        if (homeUnitDetailViewModel.showProgress.value == true) {
            return false
        }
        return when (item.itemId) {
            R.id.action_edit -> {
                Timber.e("action_edit")
                homeUnitDetailViewModel.actionEdit()
                return true
            }
            R.id.action_save -> {
                val (messageId, positiveButtonId) = homeUnitDetailViewModel.actionSave()
                Timber.d("action_save ${getString(messageId)}")
                if (messageId > 0) {
                    positiveButtonId?.let { buttonId ->
                        showDialog(messageId, buttonId) {
                            homeUnitDetailViewModel.saveChanges()?.addOnCompleteListener {
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
                //TODO do sth with this mess
                if (homeUnitDetailViewModel.noChangesMade()) {
                    if (homeUnitDetailViewModel.actionDiscard()) {
                        // navigate back Up from this Fragment
                        findNavController().navigateUp()
                    }
                } else {
                    showDialog(R.string.add_edit_home_unit_discard_changes, R.string.menu_discard) {
                        if (homeUnitDetailViewModel.actionDiscard()) {
                            // navigate back Up from this Fragment
                            findNavController().navigateUp()
                        }
                    }
                }
                return true
            }
            R.id.action_delete -> {
                showDialog(R.string.add_edit_home_unit_delete_home_unit_prompt, R.string.menu_delete) {
                    homeUnitDetailViewModel.deleteHomeUnit()?.addOnCompleteListener {
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
