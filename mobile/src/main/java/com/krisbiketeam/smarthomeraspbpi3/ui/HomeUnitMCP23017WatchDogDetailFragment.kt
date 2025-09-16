package com.krisbiketeam.smarthomeraspbpi3.ui

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.MCP23017WatchDogHomeUnit
import com.krisbiketeam.smarthomeraspbpi3.databinding.*
import com.krisbiketeam.smarthomeraspbpi3.utils.showTimePicker
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.HomeUnitMCP23017WatchDogDetailViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

@ExperimentalCoroutinesApi
class HomeUnitMCP23017WatchDogDetailFragment : HomeUnitDetailFragmentBase<MCP23017WatchDogHomeUnit<Any>>() {

    private val args: HomeUnitMCP23017WatchDogDetailFragmentArgs by navArgs()

    private var additionalValueFieldsBindings: FragmentHomeUnitMcp23017WatchDogAdditionalValueFieldsBinding? =
        null
    private var additionalHwUnitBindings: FragmentHomeUnitMcp23017WatchDogAdditionalHwUnitsBinding? =
        null

    override val homeUnitDetailViewModel: HomeUnitMCP23017WatchDogDetailViewModel by viewModel {
        parametersOf(
            args.roomName,
            args.homeUnitName
        )
    }

    override fun bindAdditionalValueFields(inflater: LayoutInflater, container: ViewGroup?): View? {
        additionalValueFieldsBindings =
            DataBindingUtil.inflate<FragmentHomeUnitMcp23017WatchDogAdditionalValueFieldsBinding>(
                inflater,
                R.layout.fragment_home_unit_mcp23017_watch_dog_additional_value_fields,
                container,
                false
            ).apply {
                viewModel = homeUnitDetailViewModel
                lifecycleOwner = viewLifecycleOwner
            }
        return additionalValueFieldsBindings?.root
    }

    override fun bindAdditionalHwUnits(inflater: LayoutInflater, container: ViewGroup?): View? {
        additionalHwUnitBindings =
            DataBindingUtil.inflate<FragmentHomeUnitMcp23017WatchDogAdditionalHwUnitsBinding>(
                inflater,
                R.layout.fragment_home_unit_mcp23017_watch_dog_additional_hw_units,
                container,
                false
            ).apply {
                viewModel = homeUnitDetailViewModel
                lifecycleOwner = viewLifecycleOwner
            }
        return additionalHwUnitBindings?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        additionalValueFieldsBindings?.run {
            homeUnitValueSwitch.setOnCheckedChangeListener { _, isChecked ->
                homeUnitDetailViewModel.setValueFromSwitch(isChecked)
            }
        }
        additionalHwUnitBindings?.run {
            inputHwUnitNameSpinner.setOnLongClickListener {
                val hwUnitName = homeUnitDetailViewModel.inputHwUnitName.value
                if (hwUnitName != null && homeUnitDetailViewModel.isEditMode.value) {
                    findNavController().navigate(
                        HomeUnitMCP23017WatchDogDetailFragmentDirections.actionHomeUnitMcp23017WatchDogDetailFragmentToAddEditHwUnitFragment(
                            hwUnitName
                        )
                    )
                    true
                } else {
                    false
                }
            }
            inputHwUnitNameSpinner.setOnClickListener {
                val hwUnitName = homeUnitDetailViewModel.inputHwUnitName.value
                if (hwUnitName != null && !homeUnitDetailViewModel.isEditMode.value) {
                    findNavController().navigate(
                        HomeUnitMCP23017WatchDogDetailFragmentDirections.actionHomeUnitMcp23017WatchDogDetailFragmentToAddEditHwUnitFragment(
                            hwUnitName
                        )
                    )
                }
            }

            watchDogTimeoutGroup.setOnClickListener {
                showTimePicker(context, homeUnitDetailViewModel.watchDogTimeout)
            }
            watchDogDelayGroup.setOnClickListener {
                showTimePicker(context, homeUnitDetailViewModel.watchDogDelay)
            }
        }
    }

    override fun onDestroyView() {
        additionalValueFieldsBindings = null
        additionalHwUnitBindings = null
        super.onDestroyView()
    }

    override fun navigateToddEditHwUnitFragment(hwUnitName: String) {
        findNavController().navigate(
            HomeUnitMCP23017WatchDogDetailFragmentDirections.actionHomeUnitMcp23017WatchDogDetailFragmentToAddEditHwUnitFragment(
                hwUnitName
            )
        )
    }
}
