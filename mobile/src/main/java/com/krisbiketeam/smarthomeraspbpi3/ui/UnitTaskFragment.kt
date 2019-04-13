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
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentUnitTaskBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.UnitTaskViewModel
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber


class UnitTaskFragment : Fragment() {
    private val unitTaskViewModel: UnitTaskViewModel by viewModel {
        parametersOf(
                arguments?.let { UnitTaskFragmentArgs.fromBundle(it).taskName}?: "",
                arguments?.let { UnitTaskFragmentArgs.fromBundle(it).homeUnitName}?: "",
                arguments?.let { UnitTaskFragmentArgs.fromBundle(it).homeUnitType}?: "")
    }

    private lateinit var rootBinding: FragmentUnitTaskBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        rootBinding = DataBindingUtil.inflate<FragmentUnitTaskBinding>(
                inflater, R.layout.fragment_unit_task, container, false).apply {
            viewModel = unitTaskViewModel
            setLifecycleOwner(this@UnitTaskFragment)
        }

        unitTaskViewModel.isEditMode.observe(viewLifecycleOwner, Observer { isEditMode ->
            // in Edit Mode we need to listen for homeUnitList, as there is no reference in xml layout to trigger its observer, but can we find some better way
            if (isEditMode == true) {
                unitTaskViewModel.homeUnitNameList.observe(viewLifecycleOwner, Observer { })
            } else {
                unitTaskViewModel.homeUnitNameList.removeObservers(viewLifecycleOwner)
            }
            activity?.invalidateOptionsMenu()
            // Animate Layout edit mode change
            TransitionManager.beginDelayedTransition(rootBinding.root as ViewGroup, Fade())
        })

        setHasOptionsMenu(true)

        return rootBinding.root
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_unit_task, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        when (unitTaskViewModel.isEditMode.value) {
            true -> {
                menu.findItem((R.id.action_discard))?.isVisible = true
                menu.findItem((R.id.action_save))?.isVisible = true
                menu.findItem((R.id.action_delete))?.isVisible =
                        arguments?.let {
                            UnitTaskFragmentArgs.fromBundle(it).homeUnitName.isNotEmpty()
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
        if (unitTaskViewModel.showProgress.value == true) {
            return false
        }
        return when (item.itemId) {
            R.id.action_edit -> {
                Timber.e("action_edit")
                unitTaskViewModel.actionEdit()
                return true
            }
            R.id.action_save -> {
                val (messageId, positiveButtonId) = unitTaskViewModel.actionSave()
                Timber.d("action_save ${getString(messageId)}")
                if (messageId > 0) {
                    positiveButtonId?.let { buttonId ->
                        showDialog(messageId, buttonId) {
                            unitTaskViewModel.saveChanges()?.addOnCompleteListener {
                                // navigate back Up from this Fragment
                                findNavController().navigateUp()
                            }
                        }
                    } ?: Snackbar.make(rootBinding.root, messageId, Snackbar.LENGTH_SHORT).show()
                } else {
                    //hmm, this should not happen
                    Timber.e("action_save we got empty message This should not happen")
                }
                return true
            }
            R.id.action_discard -> {
                //TODO do sth with this mess
                if (unitTaskViewModel.noChangesMade()) {
                    if (unitTaskViewModel.actionDiscard()) {
                        // navigate back Up from this Fragment
                        findNavController().navigateUp()
                    }
                } else {
                    showDialog(R.string.add_edit_home_unit_discard_changes, R.string.menu_discard) {
                        if (unitTaskViewModel.actionDiscard()) {
                            // navigate back Up from this Fragment
                            findNavController().navigateUp()
                        }
                    }
                }
                return true
            }
            R.id.action_delete -> {
                showDialog(R.string.add_edit_home_unit_delete_home_unit_prompt, R.string.menu_delete) {
                    unitTaskViewModel.deleteHomeUnit()?.addOnCompleteListener {
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