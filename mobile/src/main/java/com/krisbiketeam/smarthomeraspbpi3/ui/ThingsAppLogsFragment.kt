package com.krisbiketeam.smarthomeraspbpi3.ui

import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.core.util.Pair
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.analytics.FirebaseAnalytics
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.Analytics
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentThingsAppLogsBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.ThingsAppLogsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber


@ExperimentalCoroutinesApi
class ThingsAppLogsFragment : androidx.fragment.app.Fragment() {

    private val analytics: Analytics by inject()

    private val logsViewModel: ThingsAppLogsViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val binding: FragmentThingsAppLogsBinding = FragmentThingsAppLogsBinding.inflate(
                inflater, container, false).apply {
            viewModel = logsViewModel
            lifecycleOwner = viewLifecycleOwner
        }

        subscribeThingsAppLogsData()

        subscribeRemoteLogLevelMenuItems()

        setHasOptionsMenu(true)

        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundleOf(
                FirebaseAnalytics.Param.SCREEN_NAME to this::class.simpleName,
                FirebaseAnalytics.Param.SCREEN_CLASS to this::class.qualifiedName
        ))
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_things_app_logs, menu)

        menu.findItem(R.id.action_remote_log_level).subMenu.apply {
            clear()
            logsViewModel.menuItemRemoteLogListFlow.value.forEach { (levelName, itemId, checked) ->
                val menuItem = add(R.id.action_remote_log_level, itemId, Menu.NONE, levelName)
                menuItem.isCheckable = true
                menuItem.isChecked = checked
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_all -> {
                logsViewModel.clearLogs()
                true
            }
            R.id.action_date_picker -> {
                openDateRangePicker()
                true
            }
            else -> when {
                logsViewModel.setLogLevel(item.itemId) -> {
                    true
                }
                else -> {
                    super.onOptionsItemSelected(item)
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    private fun subscribeThingsAppLogsData() {
        lifecycleScope.launch {
            logsViewModel.logsData.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED).flowOn(Dispatchers.IO).collect { logsList ->
                Timber.d("subscribeLogsData logsList: ${logsList.size}")
                logsViewModel.thingsAppLogsListAdapter.submitList(logsList)
            }
        }
    }

    @ExperimentalCoroutinesApi
    private fun subscribeRemoteLogLevelMenuItems() {
        lifecycleScope.launch {
            logsViewModel.menuItemRemoteLogListFlow.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED).flowOn(Dispatchers.IO).collect {
                Timber.d("subscribeFilterMenuItems  size:${it.size}")
                activity?.invalidateOptionsMenu()
            }
        }
    }

    private fun openDateRangePicker() {
        val dateRangePicker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Select dates")
                        .setSelection(Pair(logsViewModel.startRangeFlow.value,
                                logsViewModel.endRangeFlow.value))
                        .setCalendarConstraints(CalendarConstraints.Builder().setEnd(System.currentTimeMillis()).build())
                        .build()
        dateRangePicker.addOnPositiveButtonClickListener { selectionRangePair ->
            logsViewModel.startRangeFlow.value = selectionRangePair.first
            logsViewModel.endRangeFlow.value = selectionRangePair.second
        }
        dateRangePicker.show(childFragmentManager, null)
    }
}
