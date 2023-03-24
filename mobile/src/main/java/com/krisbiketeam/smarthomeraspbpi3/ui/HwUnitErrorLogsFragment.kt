package com.krisbiketeam.smarthomeraspbpi3.ui

import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.firebase.analytics.FirebaseAnalytics
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.Analytics
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentHwUnitErrorLogsBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.HwUnitErrorLogsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber


@ExperimentalCoroutinesApi
class HwUnitErrorLogsFragment : androidx.fragment.app.Fragment() {

    private val analytics: Analytics by inject()

    private val logsViewModel: HwUnitErrorLogsViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val binding: FragmentHwUnitErrorLogsBinding = FragmentHwUnitErrorLogsBinding.inflate(
                inflater, container, false).apply {
            viewModel = logsViewModel
            lifecycleOwner = viewLifecycleOwner
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch(Dispatchers.IO) {
                    logsViewModel.logsData.collect { logsList ->
                        Timber.d("subscribeLogsData logsList: ${logsList.size}")
                        logsViewModel.hwUnitErrorLogsListAdapter.submitList(logsList)
                    }
                }
                launch(Dispatchers.IO) {
                    logsViewModel.menuItemHwUnitListFlow.collect {
                            Timber.d("subscribeFilterMenuItems  size:${it.size}")
                            activity?.invalidateOptionsMenu()
                        }
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
        inflater.inflate(R.menu.menu_hw_unit_error_logs, menu)

        menu.findItem(R.id.action_filter).subMenu.apply {
            clear()
            logsViewModel.menuItemHwUnitListFlow.value.forEach { (hwUnitPair, itemId, checked) ->
                add(hwUnitPair.type.hashCode(), itemId, Menu.NONE, hwUnitPair.name).apply {
                    isCheckable = true
                    isChecked = checked
                }
            }
        }
        menu.findItem(R.id.action_select_all)?.isChecked = logsViewModel.isSelectAll()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_all -> {
                logsViewModel.clearLogs()
                true
            }
            R.id.action_select_all -> {
                logsViewModel.selectAll(!item.isChecked)
                true
            }
            else -> when {
                logsViewModel.addFilter(item.itemId) -> {
                    true
                }
                else -> {
                    super.onOptionsItemSelected(item)
                }
            }
        }
    }
}
