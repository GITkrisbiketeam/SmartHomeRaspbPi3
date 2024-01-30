package com.krisbiketeam.smarthomeraspbpi3.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.databinding.OnRebindCallback
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.Analytics
import com.krisbiketeam.smarthomeraspbpi3.common.RemoteConnectionState
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentSettingsHomeBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.settings.HomeSettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class HomeSettingsFragment : Fragment() {
    private val homeSettingsViewModel by viewModel<HomeSettingsViewModel>()

    private val analytics: Analytics by inject()

    private lateinit var binding: FragmentSettingsHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate<FragmentSettingsHomeBinding>(
            inflater,
            R.layout.fragment_settings_home,
            container, false
        ).apply {
            viewModel = homeSettingsViewModel

            homeName.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    setupHomeName()
                    return@OnEditorActionListener true
                }
                false
            })
            lifecycleScope.launch {
                homeSettingsViewModel.homeName.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
                    .flowOn(Dispatchers.IO).collect {
                        binding.homeNameLayout.error = null
                    }
            }
            lifecycleScope.launch {
                homeSettingsViewModel.bleConnectionState.flowWithLifecycle(
                    lifecycle,
                    Lifecycle.State.RESUMED
                ).flowOn(Dispatchers.IO).collect { state ->
                    when (state) {
                        RemoteConnectionState.ERROR -> {
                            binding.homeNameLayout.error =
                                getString(R.string.cannot_setup_home_name)
                            binding.homeName.requestFocus()
                        }

                        RemoteConnectionState.INIT -> {
                        }

                        RemoteConnectionState.CONNECTING -> {
                        }

                        RemoteConnectionState.DONE -> {
                            findNavController().navigateUp()
                        }
                    }
                }
            }

            loginConnectButton.setOnClickListener { setupHomeName() }

            addOnRebindCallback(object : OnRebindCallback<ViewDataBinding>() {
                override fun onPreBind(binding: ViewDataBinding?): Boolean {
                    Timber.d("onPreBind")
                    binding?.let {
                        TransitionManager.beginDelayedTransition(binding.root as ViewGroup)
                    }
                    return super.onPreBind(binding)
                }
            })

            lifecycleOwner = viewLifecycleOwner
        }

        analytics.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW, bundleOf(
                FirebaseAnalytics.Param.SCREEN_NAME to this::class.simpleName
            )
        )

        return binding.root
    }

    private fun setupHomeName() {
        binding.homeNameLayout.error = null

        val homeNameStr = binding.homeName.text.toString()

        if (homeNameStr.isEmpty()) {
            binding.homeNameLayout.error = getString(R.string.error_field_required)
            binding.homeName.requestFocus()
        } else {
            homeSettingsViewModel.setupHomeName(homeNameStr)
        }
    }

}
