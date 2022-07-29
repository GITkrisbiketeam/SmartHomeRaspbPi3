package com.krisbiketeam.smarthomeraspbpi3.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.GenericHomeUnit
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentHomeUnitGenericAdditionalHwUnitsBinding
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentHomeUnitGenericAdditionalValueFieldsBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.HomeUnitGenericDetailViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

@ExperimentalCoroutinesApi
class HomeUnitGenericDetailFragment : HomeUnitDetailFragmentBase<GenericHomeUnit<Any>>() {

    private val args: HomeUnitGenericDetailFragmentArgs by navArgs()

    private var additionalValueFieldsBindings: FragmentHomeUnitGenericAdditionalValueFieldsBinding? =
        null
    private var additionalHwUnitBindings: FragmentHomeUnitGenericAdditionalHwUnitsBinding? = null

    override val homeUnitDetailViewModel: HomeUnitGenericDetailViewModel by viewModel {
        parametersOf(
            args.roomName,
            args.homeUnitName,
            args.homeUnitType
        )
    }

    override fun bindAdditionalValueFields(inflater: LayoutInflater, container: ViewGroup?): View? {
        additionalValueFieldsBindings =
            DataBindingUtil.inflate<FragmentHomeUnitGenericAdditionalValueFieldsBinding>(
                inflater,
                R.layout.fragment_home_unit_generic_additional_value_fields,
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
            DataBindingUtil.inflate<FragmentHomeUnitGenericAdditionalHwUnitsBinding>(
                inflater, R.layout.fragment_home_unit_generic_additional_hw_units, container, false
            ).apply {
                viewModel = homeUnitDetailViewModel
                lifecycleOwner = viewLifecycleOwner
            }
        return additionalHwUnitBindings?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        additionalValueFieldsBindings?.run {
            homeUnitMinClearButton.setOnClickListener {
                homeUnitDetailViewModel.clearMinValue()
            }
            homeUnitMaxClearButton.setOnClickListener {
                homeUnitDetailViewModel.clearMaxValue()
            }
            homeUnitValueSwitch.setOnCheckedChangeListener { _, isChecked ->
                homeUnitDetailViewModel.setValueFromSwitch(isChecked)
            }
        }
        additionalHwUnitBindings?.run {
            secondHwUnitNameSpinner.setOnLongClickListener {
                val hwUnitName = homeUnitDetailViewModel.secondHwUnitName.value
                if (hwUnitName != null && homeUnitDetailViewModel.isEditMode.value) {
                    findNavController().navigate(
                        HomeUnitGenericDetailFragmentDirections.actionHomeUnitGenericDetailFragmentToAddEditHwUnitFragment(
                            hwUnitName
                        )
                    )
                    true
                } else {
                    false
                }
            }
            secondHwUnitNameSpinner.setOnClickListener {
                val hwUnitName = homeUnitDetailViewModel.secondHwUnitName.value
                if (hwUnitName != null && !homeUnitDetailViewModel.isEditMode.value) {
                    findNavController().navigate(
                        HomeUnitGenericDetailFragmentDirections.actionHomeUnitGenericDetailFragmentToAddEditHwUnitFragment(
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

    override fun navigateToddEditHwUnitFragment(hwUnitName: String) {
        findNavController().navigate(
            HomeUnitGenericDetailFragmentDirections.actionHomeUnitGenericDetailFragmentToAddEditHwUnitFragment(
                hwUnitName
            )
        )
    }
}
