package com.krisbiketeam.smarthomeraspbpi3.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.analytics.FirebaseAnalytics
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.Analytics
import com.krisbiketeam.smarthomeraspbpi3.common.storage.firebaseTables.HomeUnitType
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentHomeUnitTypeChooserDialogFragmentBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.HomeUnitTypeChooserDialogViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel


class HomeUnitTypeChooserDialogFragment : Fragment() {
    private val args: HomeUnitTypeChooserDialogFragmentArgs by navArgs()

    private val homeUnitTypeChooserDialogViewModel by viewModel<HomeUnitTypeChooserDialogViewModel>()

    private val analytics: Analytics by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentHomeUnitTypeChooserDialogFragmentBinding =
            DataBindingUtil.inflate<FragmentHomeUnitTypeChooserDialogFragmentBinding>(
                inflater, R.layout.fragment_home_unit_type_chooser_dialog_fragment, container, false
            ).apply {
                viewModel = homeUnitTypeChooserDialogViewModel
                lifecycleOwner = viewLifecycleOwner
                cancelButton.setOnClickListener {
                    findNavController().navigateUp()
                }
                confirmButton.setOnClickListener {
                    findNavController().run {
                        when (homeUnitTypeChooserDialogViewModel.type.value) {
                            HomeUnitType.HOME_LIGHT_SWITCHES -> navigate(
                                HomeUnitTypeChooserDialogFragmentDirections.actionHomeUnitTypeChooserDialogFragmentToHomeUnitLightSwitchDetailFragment(
                                    args.roomName
                                )
                            )
                            HomeUnitType.HOME_WATER_CIRCULATION -> navigate(
                                HomeUnitTypeChooserDialogFragmentDirections.actionHomeUnitTypeChooserDialogFragmentToHomeUnitWaterCirculationDetailFragment(
                                    args.roomName
                                )
                            )
                            HomeUnitType.HOME_MCP23017_WATCH_DOG -> navigate(
                                HomeUnitTypeChooserDialogFragmentDirections.actionHomeUnitTypeChooserDialogFragmentToHomeUnitMcp23017WatchDogDetailFragment(
                                    args.roomName
                                )
                            )
                            HomeUnitType.UNKNOWN -> Unit
                            else -> navigate(
                                HomeUnitTypeChooserDialogFragmentDirections.actionHomeUnitTypeChooserDialogFragmentToHomeUnitGenericDetailFragment(
                                    args.roomName, homeUnitType = homeUnitTypeChooserDialogViewModel.type.value
                                )
                            )
                        }
                    }
                }
            }

        setHasOptionsMenu(false)

        analytics.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW, bundleOf(
                FirebaseAnalytics.Param.SCREEN_NAME to this::class.simpleName
            )
        )

        return binding.root
    }
}
