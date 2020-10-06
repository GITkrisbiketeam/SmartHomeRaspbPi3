package com.krisbiketeam.smarthomeraspbpi3.ui

import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.firebase.analytics.FirebaseAnalytics
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.Analytics
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentHwUnitErrorEventListBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.HwUnitErrorEventListViewModel
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel


class HwUnitErrorEventListFragment : Fragment() {

    private val hwUnitErrorEventListViewModel by viewModel<HwUnitErrorEventListViewModel>()

    private val analytics: Analytics by inject()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentHwUnitErrorEventListBinding>(
                inflater, R.layout.fragment_hw_unit_error_event_list, container, false).apply {
            viewModel = hwUnitErrorEventListViewModel
            lifecycleOwner = this@HwUnitErrorEventListFragment
        }

        hwUnitErrorEventListViewModel.apply {
            hwUnitErrorEventList.observe(viewLifecycleOwner, { hwUnitList ->
                hwUnitErrorEventListAdapter.submitList(hwUnitList)
            })
        }

        setHasOptionsMenu(true)

        analytics.firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundleOf(
                FirebaseAnalytics.Param.SCREEN_NAME to this::class.simpleName
        ))

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_hw_unit_error_event_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_all -> {
                hwUnitErrorEventListViewModel.clearHwErrors()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
