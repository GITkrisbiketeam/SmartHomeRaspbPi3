package com.krisbiketeam.smarthomeraspbpi3.ui

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.Analytics
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentHomeUnitDetailBaseBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.HomeUnitDetailViewModelBase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

@ExperimentalCoroutinesApi
abstract class HomeUnitDetailFragmentBase<T : HomeUnit<Any>> : Fragment() {

    abstract val homeUnitDetailViewModel: HomeUnitDetailViewModelBase<T>

    private var rootBinding: FragmentHomeUnitDetailBaseBinding? = null

    private val analytics: Analytics by inject()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val binding = DataBindingUtil.inflate<FragmentHomeUnitDetailBaseBinding>(
                inflater, R.layout.fragment_home_unit_detail_base, container, false).apply {
            viewModel = homeUnitDetailViewModel
            lifecycleOwner = viewLifecycleOwner

            setHasOptionsMenu(true)

            bindAdditionalValueFields(inflater, container)?.run(homeUnitGenericAdditionalValueFields::addView)
            bindAdditionalHwUnits(inflater, container)?.run(homeUnitGenericAdditionalHwUnits::addView)

            rootBinding = this
        }

        lifecycleScope.launch {
            homeUnitDetailViewModel.isEditMode.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED).flowOn(Dispatchers.IO).collect { isEditMode ->
                // in Edit Mode we need to listen for homeUnitList, as there is no reference in xml layout to trigger its observer, but can we find some better way???
                Timber.d("onCreateView isEditMode: $isEditMode")
                activity?.invalidateOptionsMenu()
                // Animate Layout edit mode change
                TransitionManager.beginDelayedTransition(binding.root as ViewGroup, Fade())
            }
        }
        lifecycleScope.launch {
            homeUnitDetailViewModel.unitTaskList.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED).flowOn(Dispatchers.IO).collect { taskListMap ->
                Timber.d("onCreateView unitTaskList Observer taskListMap: $taskListMap")
                // Update UnitTask list
                // Do not show default HOME_LIGHT_SWITCHES UnitTask (with UnitTask name same as HomeUnit name) responsible for linking two hwUnits
                homeUnitDetailViewModel.unitTaskListAdapter.submitList(taskListMap.values.toList())
            }
        }

        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundleOf(
                FirebaseAnalytics.Param.SCREEN_NAME to this::class.simpleName,
                FirebaseAnalytics.Param.ITEM_NAME to homeUnitDetailViewModel.room.value
        ))

        return binding.root
    }


    abstract fun bindAdditionalValueFields(inflater: LayoutInflater, container: ViewGroup?): View?

    abstract fun bindAdditionalHwUnits(inflater: LayoutInflater, container: ViewGroup?): View?

    abstract fun navigateToddEditHwUnitFragment(hwUnitName:String)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rootBinding?.run {
            hwUnitNameSpinner.setOnLongClickListener {
                val hwUnitName = homeUnitDetailViewModel.hwUnitName.value
                if (hwUnitName != null && homeUnitDetailViewModel.isEditMode.value) {
                    navigateToddEditHwUnitFragment(hwUnitName)
                    true
                } else {
                    false
                }
            }
            hwUnitNameSpinner.setOnClickListener {
                val hwUnitName = homeUnitDetailViewModel.hwUnitName.value
                if (hwUnitName != null && !homeUnitDetailViewModel.isEditMode.value) {
                    navigateToddEditHwUnitFragment(hwUnitName)
                }
            }
        }
    }

    override fun onDestroyView() {
        rootBinding = null
        super.onDestroyView()
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
                menu.findItem((R.id.action_delete))?.isVisible = homeUnitDetailViewModel.homeUnit != null
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
        if (homeUnitDetailViewModel.showProgress.value) {
            return super.onOptionsItemSelected(item)
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
