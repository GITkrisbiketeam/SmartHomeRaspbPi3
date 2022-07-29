package com.krisbiketeam.smarthomeraspbpi3.ui

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.LightSwitchHomeUnit
import com.krisbiketeam.smarthomeraspbpi3.databinding.*
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.HomeUnitLightSwitchDetailViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

@ExperimentalCoroutinesApi
class HomeUnitLightSwitchDetailFragment : HomeUnitDetailFragmentBase<LightSwitchHomeUnit<Any>>() {

    private val args: HomeUnitLightSwitchDetailFragmentArgs by navArgs()

    private var additionalValueFieldsBindings: FragmentHomeUnitLightSwitchAdditionalValueFieldsBinding? =
        null
    private var additionalHwUnitBindings: FragmentHomeUnitLightSwitchAdditionalHwUnitsBinding? =
        null

    override val homeUnitDetailViewModel: HomeUnitLightSwitchDetailViewModel by viewModel {
        parametersOf(
            args.roomName,
            args.homeUnitName
        )
    }

    override fun bindAdditionalValueFields(inflater: LayoutInflater, container: ViewGroup?): View? {
        additionalValueFieldsBindings =
            DataBindingUtil.inflate<FragmentHomeUnitLightSwitchAdditionalValueFieldsBinding>(
                inflater,
                R.layout.fragment_home_unit_light_switch_additional_value_fields,
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
            DataBindingUtil.inflate<FragmentHomeUnitLightSwitchAdditionalHwUnitsBinding>(
                inflater,
                R.layout.fragment_home_unit_light_switch_additional_hw_units,
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
            secondHwUnitNameSpinner.setOnLongClickListener {
                val hwUnitName = homeUnitDetailViewModel.switchHwUnitName.value
                if (hwUnitName != null && homeUnitDetailViewModel.isEditMode.value) {
                    findNavController().navigate(
                        HomeUnitLightSwitchDetailFragmentDirections.actionHomeUnitLightSwitchDetailFragmentToAddEditHwUnitFragment(
                            hwUnitName
                        )
                    )
                    true
                } else {
                    false
                }
            }
            secondHwUnitNameSpinner.setOnClickListener {
                val hwUnitName = homeUnitDetailViewModel.switchHwUnitName.value
                if (hwUnitName != null && !homeUnitDetailViewModel.isEditMode.value) {
                    findNavController().navigate(
                        HomeUnitLightSwitchDetailFragmentDirections.actionHomeUnitLightSwitchDetailFragmentToAddEditHwUnitFragment(
                            hwUnitName
                        )
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        additionalValueFieldsBindings = null
        super.onDestroyView()
    }
}
