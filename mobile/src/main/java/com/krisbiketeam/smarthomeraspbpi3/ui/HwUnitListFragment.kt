package com.krisbiketeam.smarthomeraspbpi3.ui

import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.analytics.FirebaseAnalytics
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.Analytics
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentHwUnitListBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.HwUnitListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel


@ExperimentalCoroutinesApi
class HwUnitListFragment : Fragment() {

    private val hwUnitListViewModel by viewModel<HwUnitListViewModel>()

    private val analytics: Analytics by inject()

    private val repository: FirebaseHomeInformationRepository by inject()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val binding = DataBindingUtil.inflate<FragmentHwUnitListBinding>(
                inflater, R.layout.fragment_hw_unit_list, container, false).apply {
            viewModel = hwUnitListViewModel
            lifecycleOwner = viewLifecycleOwner
        }

        hwUnitListViewModel.apply {
            lifecycleScope.launch {
                hwUnitList.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED).flowOn(Dispatchers.IO).collect { hwUnitList ->
                    /*val startTime = 1624665600000
                    val endTime = 1641945600000
                    withContext(Dispatchers.IO){
                        hwUnitList.forEach { hwUnit ->
                            for(time in startTime .. endTime step FULL_DAY_IN_MILLIS){
                                Timber.d("clear logs:${hwUnit.name} time:$time ")
                                repository.clearHwUnitLogs(hwUnit.name, time.toString())?.addOnCanceledListener {
                                    Timber.e("addOnCanceledListener clear logs:${hwUnit.name} time:$time ")
                                }?.addOnFailureListener {
                                    Timber.e("addOnFailureListener clear logs:${hwUnit.name} time:$time ")
                                }?.addOnSuccessListener {
                                    Timber.e("addOnSuccessListener clear logs:${hwUnit.name} time:$time ")
                                }
                            }
                        }
                    }*/

                    hwUnitListAdapter.submitList(hwUnitList)
                }
            }
        }

        setHasOptionsMenu(true)

        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundleOf(
                FirebaseAnalytics.Param.SCREEN_NAME to this::class.simpleName
        ))

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_hw_unit_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add -> {
                findNavController().navigate(HwUnitListFragmentDirections.actionHwUnitListFragmentToAddEditHwUnitFragment(""))
                true
            }
            R.id.action_restart_all -> {
                hwUnitListViewModel.restartAllHwUnits()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
