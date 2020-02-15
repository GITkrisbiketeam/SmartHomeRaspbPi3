package com.krisbiketeam.smarthomeraspbpi3.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.databinding.OnRebindCallback
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.transition.TransitionManager
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.MyLiveDataState
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentSettingsHomeBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.settings.HomeSettingsViewModel
import org.koin.android.viewmodel.ext.android.viewModel
import timber.log.Timber

class HomeSettingsFragment : Fragment() {
    private val homeSettingsViewModel by viewModel<HomeSettingsViewModel>()

    private lateinit var binding: FragmentSettingsHomeBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate<FragmentSettingsHomeBinding>(inflater,
                                                                       R.layout.fragment_settings_home,
                                                                       container, false).apply {
            viewModel = homeSettingsViewModel

            homeName.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    setupHomeName()
                    return@OnEditorActionListener true
                }
                false
            })
            homeSettingsViewModel.homeName.observe(viewLifecycleOwner, Observer {
                binding.homeNameLayout.error = null
            })

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

            lifecycleOwner = this@HomeSettingsFragment
        }

        homeSettingsViewModel.nearByState.observe(viewLifecycleOwner, Observer { pair ->
            pair?.let { (state, data) ->

                when (state) {
                    MyLiveDataState.ERROR      -> {
                        if (data is Exception) {
                            Timber.e(data, "Request failed")
                        }
                        binding.homeNameLayout.error = getString(R.string.cannot_setup_home_name)
                        binding.homeName.requestFocus()
                    }

                    MyLiveDataState.INIT       -> {
                    }
                    MyLiveDataState.CONNECTING -> {
                    }
                    MyLiveDataState.DONE       -> {
                        activity?.let {
                            Navigation.findNavController(it, R.id.home_nav_fragment).navigateUp()
                        }
                    }
                }
            }
        })

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
