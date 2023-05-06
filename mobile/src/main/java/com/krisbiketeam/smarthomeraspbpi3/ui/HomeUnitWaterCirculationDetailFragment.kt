package com.krisbiketeam.smarthomeraspbpi3.ui

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.WaterCirculationHomeUnit
import com.krisbiketeam.smarthomeraspbpi3.databinding.*
import com.krisbiketeam.smarthomeraspbpi3.utils.showTimePicker
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.HomeUnitWaterCirculationDetailViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

@ExperimentalCoroutinesApi
class HomeUnitWaterCirculationDetailFragment :
    HomeUnitDetailFragmentBase<WaterCirculationHomeUnit<Any>>() {

    private val args: HomeUnitWaterCirculationDetailFragmentArgs by navArgs()

    private var additionalValueFieldsBindings: FragmentHomeUnitWaterCirculationAdditionalValueFieldsBinding? =
        null
    private var additionalHwUnitBindings: FragmentHomeUnitWaterCirculationAdditionalHwUnitsBinding? =
        null

    override val homeUnitDetailViewModel: HomeUnitWaterCirculationDetailViewModel by viewModel {
        parametersOf(
            args.roomName,
            args.homeUnitName
        )
    }

    override fun bindAdditionalValueFields(inflater: LayoutInflater, container: ViewGroup?): View? {
        additionalValueFieldsBindings =
            DataBindingUtil.inflate<FragmentHomeUnitWaterCirculationAdditionalValueFieldsBinding>(
                inflater,
                R.layout.fragment_home_unit_water_circulation_additional_value_fields,
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
            DataBindingUtil.inflate<FragmentHomeUnitWaterCirculationAdditionalHwUnitsBinding>(
                inflater,
                R.layout.fragment_home_unit_water_circulation_additional_hw_units,
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
            homeUnitMinClearButton.setOnClickListener {
                homeUnitDetailViewModel.clearMinValue()
            }
            homeUnitMaxClearButton.setOnClickListener {
                homeUnitDetailViewModel.clearMaxValue()
            }
        }
        additionalHwUnitBindings?.run {
            motionHwUnitNameSpinner.setOnLongClickListener {
                val hwUnitName = homeUnitDetailViewModel.motionHwUnitName.value
                if (hwUnitName != null && homeUnitDetailViewModel.isEditMode.value) {
                    findNavController().navigate(
                        HomeUnitWaterCirculationDetailFragmentDirections.actionHomeUnitWaterCirculationDetailFragmentToAddEditHwUnitFragment(
                            hwUnitName
                        )
                    )
                    true
                } else {
                    false
                }
            }
            motionHwUnitNameSpinner.setOnClickListener {
                val hwUnitName = homeUnitDetailViewModel.motionHwUnitName.value
                if (hwUnitName != null && !homeUnitDetailViewModel.isEditMode.value) {
                    findNavController().navigate(
                        HomeUnitWaterCirculationDetailFragmentDirections.actionHomeUnitWaterCirculationDetailFragmentToAddEditHwUnitFragment(
                            hwUnitName
                        )
                    )
                }
            }

            temperatureHwUnitNameSpinner.setOnLongClickListener {
                val hwUnitName = homeUnitDetailViewModel.temperatureHwUnitName.value
                if (hwUnitName != null && homeUnitDetailViewModel.isEditMode.value) {
                    findNavController().navigate(
                        HomeUnitWaterCirculationDetailFragmentDirections.actionHomeUnitWaterCirculationDetailFragmentToAddEditHwUnitFragment(
                            hwUnitName
                        )
                    )
                    true
                } else {
                    false
                }
            }
            temperatureHwUnitNameSpinner.setOnClickListener {
                val hwUnitName = homeUnitDetailViewModel.temperatureHwUnitName.value
                if (hwUnitName != null && !homeUnitDetailViewModel.isEditMode.value) {
                    findNavController().navigate(
                        HomeUnitWaterCirculationDetailFragmentDirections.actionHomeUnitWaterCirculationDetailFragmentToAddEditHwUnitFragment(
                            hwUnitName
                        )
                    )
                }
            }

            circulationDurationGroup.setOnClickListener {
                showTimePicker(context, homeUnitDetailViewModel.actionTimeout)
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
            HomeUnitWaterCirculationDetailFragmentDirections.actionHomeUnitWaterCirculationDetailFragmentToAddEditHwUnitFragment(
                hwUnitName
            )
        )
    }
}
