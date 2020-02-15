package com.krisbiketeam.smarthomeraspbpi3.ui

import androidx.lifecycle.Observer
import androidx.databinding.DataBindingUtil
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.*
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentHwUnitErrorEventListBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.HwUnitErrorEventListViewModel
import org.koin.android.viewmodel.ext.android.viewModel


class HwUnitErrorEventListFragment : Fragment() {

    private val hwUnitErrorEventListViewModel by viewModel<HwUnitErrorEventListViewModel>()

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
            hwUnitErrorEventList.observe(viewLifecycleOwner, Observer { hwUnitList ->
                hwUnitErrorEventListAdapter.submitList(hwUnitList)
            })
        }

        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_hw_unit_error_event_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_all -> {
                FirebaseHomeInformationRepository.clearHwErrors()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
