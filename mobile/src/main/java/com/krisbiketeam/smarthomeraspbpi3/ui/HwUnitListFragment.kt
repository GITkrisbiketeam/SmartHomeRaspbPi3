package com.krisbiketeam.smarthomeraspbpi3.ui

import androidx.lifecycle.Observer
import androidx.databinding.DataBindingUtil
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.*
import androidx.navigation.fragment.findNavController
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentHwUnitListBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.HwUnitListViewModel
import org.koin.android.viewmodel.ext.android.viewModel


class HwUnitListFragment : Fragment() {

    private val hwUnitListViewModel by viewModel<HwUnitListViewModel>()

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
            hwUnitList.observe(viewLifecycleOwner, Observer { hwUnitList ->
                hwUnitListAdapter.submitList(hwUnitList)
            })
        }

        setHasOptionsMenu(true)

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
            else -> super.onOptionsItemSelected(item)
        }
    }
}
