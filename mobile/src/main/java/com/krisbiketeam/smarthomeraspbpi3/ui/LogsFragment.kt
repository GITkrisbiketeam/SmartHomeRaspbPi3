package com.krisbiketeam.smarthomeraspbpi3.ui

import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.charts.CombinedChart.DrawOrder
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.analytics.FirebaseAnalytics
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.Analytics
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentLogsBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.LogsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


class LogsFragment : androidx.fragment.app.Fragment() {

    private val analytics: Analytics by inject()

    private val logsViewModel: LogsViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
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
                        return timeFormat.format(Date(value.toLong()))
                    }
                }
            }
            //axisLeft.axisMinimum = 0f
            //drawOrder = arrayOf(DrawOrder.LINE,DrawOrder.BUBBLE)
            setDrawGridBackground(false)
            invalidate()
        }


        subscribeLogsData(binding.fragmentLogsLineChart)

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
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_all -> {
                logsViewModel.clearLogs()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @ExperimentalCoroutinesApi
    private fun subscribeLogsData(combinedChart: Chart<*>) {
        logsViewModel.logsData.observe(viewLifecycleOwner,
                { lineData ->
                    Timber.d("subscribeLogsData lineData: $lineData")
                    combinedChart.data = lineData;
                    combinedChart.invalidate(); // refresh
                })
    }
}
