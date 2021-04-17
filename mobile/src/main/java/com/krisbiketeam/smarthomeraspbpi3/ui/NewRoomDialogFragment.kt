package com.krisbiketeam.smarthomeraspbpi3.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.analytics.FirebaseAnalytics
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.Analytics
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentNewRoomDialogFragmentBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.NewRoomDialogViewModel
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel


class NewRoomFragmentDialog : Fragment() {
    private val newRoomViewModel by viewModel<NewRoomDialogViewModel>()

    private val analytics: Analytics by inject()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val binding: FragmentNewRoomDialogFragmentBinding = DataBindingUtil.inflate<FragmentNewRoomDialogFragmentBinding>(
                inflater, R.layout.fragment_new_room_dialog_fragment, container, false).apply {
            viewModel = newRoomViewModel
            lifecycleOwner = this@NewRoomFragmentDialog
            cancelButton.setOnClickListener {
                findNavController().navigateUp()
            }
            confirmButton.setOnClickListener {
                newRoomViewModel.saveNewRoom()
                findNavController().navigateUp()
            }
        }

        setHasOptionsMenu(false)

        analytics.firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundleOf(
                FirebaseAnalytics.Param.SCREEN_NAME to this::class.simpleName
        ))

        return binding.root
    }
}
