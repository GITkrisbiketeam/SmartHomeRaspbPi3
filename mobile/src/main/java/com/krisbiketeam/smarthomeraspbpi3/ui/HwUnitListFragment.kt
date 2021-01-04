package com.krisbiketeam.smarthomeraspbpi3.ui

import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.analytics.FirebaseAnalytics
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.Analytics
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentHwUnitListBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.HwUnitListViewModel
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel


class HwUnitListFragment : Fragment() {

    private val hwUnitListViewModel by viewModel<HwUnitListViewModel>()

    private val analytics: Analytics by inject()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentHwUnitListBinding>(
                inflater, R.layout.fragment_hw_unit_list, container, false).apply {
            viewModel = hwUnitListViewModel
            lifecycleOwner = this@HwUnitListFragment
        }

        hwUnitListViewModel.apply {
            hwUnitList.observe(viewLifecycleOwner, { hwUnitList ->
                hwUnitListAdapter.submitList(hwUnitList)
            })
        }

        setHasOptionsMenu(true)

        analytics.firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundleOf(
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
