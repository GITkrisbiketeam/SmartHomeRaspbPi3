package com.krisbiketeam.smarthomeraspbpi3.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.TimePicker
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.google.android.material.snackbar.Snackbar
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentAddEditHwUnitBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.AddEditHwUnitViewModel
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber


class AddEditHwUnitFragment : Fragment() {
    private val addEditHwUnitViewModel: AddEditHwUnitViewModel by viewModel {
        parametersOf(arguments?.let {
            AddEditHwUnitFragmentArgs.fromBundle(it).hwUnitName
        } ?: "")
    }

    private lateinit var rootBinding: FragmentAddEditHwUnitBinding

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        rootBinding = DataBindingUtil.inflate<FragmentAddEditHwUnitBinding>(
                inflater, R.layout.fragment_add_edit_hw_unit, container, false).apply {
            viewModel = addEditHwUnitViewModel
            setLifecycleOwner(this@AddEditHwUnitFragment)
            hwUnitSensorRefreshRate.setOnClickListener {
                showTimePicker()
            }
        }
        addEditHwUnitViewModel.isEditMode.observe(viewLifecycleOwner, Observer {
            activity?.invalidateOptionsMenu()
            // Animate Layout edit mode change
            TransitionManager.beginDelayedTransition(rootBinding.root as ViewGroup, Fade())
        })

        setHasOptionsMenu(true)

        return rootBinding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_add_edit_hw_unit, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        when (addEditHwUnitViewModel.isEditMode.value) {
            true -> {
                menu.findItem((R.id.action_discard))?.isVisible = true
                menu.findItem((R.id.action_save))?.isVisible = true
                menu.findItem((R.id.action_delete))?.isVisible =
                        arguments?.let {
                            AddEditHwUnitFragmentArgs.fromBundle(it).hwUnitName.isNotEmpty()
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
        if (addEditHwUnitViewModel.showProgress.value == true) {
            //return false
        }
        return when (item.itemId) {
            R.id.action_edit -> {
                Timber.e("action_edit")
                addEditHwUnitViewModel.actionEdit()
                return true
            }
            R.id.action_save -> {
                val (messageId, positiveButtonId) = addEditHwUnitViewModel.actionSave()
                Timber.d("action_save ${getString(messageId)}")
                if (messageId > 0) {
                    positiveButtonId?.let { buttonId ->
                        showDialog(messageId, buttonId) {
                            addEditHwUnitViewModel.saveChanges()?.addOnCompleteListener {
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
                if (addEditHwUnitViewModel.noChangesMade()) {
                    if (addEditHwUnitViewModel.actionDiscard()) {
                        // navigate back Up from this Fragment
                        findNavController().navigateUp()
                    }
                } else {
                    showDialog(R.string.add_edit_home_unit_discard_changes, R.string.menu_discard) {
                        if (addEditHwUnitViewModel.actionDiscard()) {
                            // navigate back Up from this Fragment
                            findNavController().navigateUp()
                        }
                    }
                }
                return true
            }
            R.id.action_delete -> {
                showDialog(R.string.add_edit_home_unit_delete_home_unit_prompt, R.string.menu_delete) {
                    addEditHwUnitViewModel.deleteHomeUnit()?.addOnCompleteListener {
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

    private fun showTimePicker() {
        // TODO: Add proper Time Picker
        context?.let {
            TimePickerDialog(context, { _: TimePicker, hourOfDay: Int, minute: Int ->
                addEditHwUnitViewModel.refreshRate.value = (minute * 1000 + hourOfDay * 60 * 1000).toLong()
            }, 0, 0, true).show()
        }
    }
}
