package com.krisbiketeam.smarthomeraspbpi3.ui

import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.core.util.Pair
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.analytics.FirebaseAnalytics
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.Analytics
import com.krisbiketeam.smarthomeraspbpi3.common.FULL_DAY_IN_MILLIS
import com.krisbiketeam.smarthomeraspbpi3.common.getOnlyDateLocalTime
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentLogsBinding
import com.krisbiketeam.smarthomeraspbpi3.utils.toLogsFloat
import com.krisbiketeam.smarthomeraspbpi3.utils.toLogsLong
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.LogsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


@ExperimentalCoroutinesApi
class LogsFragment : androidx.fragment.app.Fragment() {

    private val analytics: Analytics by inject()

    private val logsViewModel: LogsViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val binding: FragmentLogsBinding = FragmentLogsBinding.inflate(
                inflater, container, false)

        binding.fragmentLogsLineChart.apply {
            setHardwareAccelerationEnabled(true)
            setNoDataText(getString(R.string.logs_fragment_no_data))
            isKeepPositionOnRotation = true
            description = null
            legend.isWordWrapEnabled = true

            xAxis.apply {
                setLabelCount(4, false)
                //granularity = 24*60 * 60 * 1000f // minimum axis-step (interval) is 1
                val timeFormat = SimpleDateFormat("EE d HH:mm:ss", Locale.getDefault())
                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase): String {
                        return timeFormat.format(Date(value.toLogsLong()))
                    }
                }
            }
            //axisLeft.axisMinimum = 0f
            setDrawGridBackground(false)
            invalidate()
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    subscribeLogsData(binding.fragmentLogsLineChart)
                }

                launch {
                    subscribeFilterMenuItems()
                }
            }
        }

        setHasOptionsMenu(true)

        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundleOf(
                FirebaseAnalytics.Param.SCREEN_NAME to this::class.simpleName,
                FirebaseAnalytics.Param.SCREEN_CLASS to this::class.qualifiedName
        ))
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_logs, menu)
        menu.findItem(R.id.action_filter).subMenu.apply {
            clear()
            var type: String? = null
            logsViewModel.menuItemHwUnitListFlow.value.forEach { (hwUnitPair, itemId, checked) ->
                if (type != hwUnitPair.first.type) {
                    add("\t${hwUnitPair.first.type}")
                    type = hwUnitPair.first.type
                }
                val menuItem = add(hwUnitPair.first.type.hashCode(), itemId, Menu.NONE, hwUnitPair.first.name.plus(hwUnitPair.second?.let { "_$it" }
                        ?: ""))
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
            else -> if (logsViewModel.addFilter(item.itemId)) true else super.onOptionsItemSelected(item)
        }
    }

    @ExperimentalCoroutinesApi
    private suspend fun subscribeFilterMenuItems() {
        logsViewModel.menuItemHwUnitListFlow.collect {
            Timber.d("subscribeFilterMenuItems  size:${it.size}")
            activity?.invalidateOptionsMenu()
        }
    }

    @ExperimentalCoroutinesApi
    private suspend fun subscribeLogsData(combinedChart: Chart<*>) {
        logsViewModel.logsData.collect { lineData ->
            Timber.d("subscribeLogsData lineData: $lineData")
            combinedChart.data = lineData
            combinedChart.xAxis.axisMinimum = lineData.xMin.toLogsLong().getOnlyDateLocalTime().toLogsFloat()
            combinedChart.xAxis.axisMaximum = (lineData.xMax.toLogsLong().getOnlyDateLocalTime() + FULL_DAY_IN_MILLIS).toLogsFloat()
            combinedChart.invalidate() // refresh
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
