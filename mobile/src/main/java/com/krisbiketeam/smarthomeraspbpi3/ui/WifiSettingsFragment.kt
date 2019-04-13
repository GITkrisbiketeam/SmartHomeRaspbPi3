package com.krisbiketeam.smarthomeraspbpi3.ui

import androidx.lifecycle.Observer
import androidx.databinding.DataBindingUtil
import androidx.databinding.OnRebindCallback
import androidx.databinding.ViewDataBinding
import android.os.Bundle
import androidx.transition.TransitionManager
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.navigation.Navigation
import com.krisbiketeam.smarthomeraspbpi3.common.MyLiveDataState
import com.krisbiketeam.smarthomeraspbpi3.common.auth.WifiCredentials
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.databinding.FragmentSettingsWifiBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.WifiSettingsViewModel
import org.koin.android.viewmodel.ext.android.viewModel
import timber.log.Timber

class WifiSettingsFragment : Fragment() {
    private val wifiSettingsViewModel by viewModel<WifiSettingsViewModel>()

    private lateinit var binding: FragmentSettingsWifiBinding

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate<FragmentSettingsWifiBinding>(
                inflater, R.layout.fragment_settings_wifi, container, false).apply {
            viewModel = wifiSettingsViewModel

            password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptRemoteConnect()
                    return@OnEditorActionListener true
                }
                false
            })

            wifiConnectButton.setOnClickListener { attemptRemoteConnect() }

            addOnRebindCallback(object : OnRebindCallback<ViewDataBinding>() {
                override fun onPreBind(binding: ViewDataBinding?): Boolean {
                    Timber.d("onPreBind")
                    TransitionManager.beginDelayedTransition(
                            binding!!.root as ViewGroup)
                    return super.onPreBind(binding)
                }
            })

            setLifecycleOwner(this@WifiSettingsFragment)
        }

        wifiSettingsViewModel.nearByState.observe(viewLifecycleOwner, Observer { pair ->
            pair?.let { (state, data) ->

                when (state) {
                    MyLiveDataState.ERROR -> {
                        if (data is Exception) {
                            Timber.e(data, "Request failed")
                        }
                        binding.password.error = getString(R.string.error_incorrect_password)
                        binding.password.requestFocus()
                    }

                    MyLiveDataState.INIT -> {
                    }
                    MyLiveDataState.CONNECTING -> {
                    }
                    MyLiveDataState.DONE -> {
                        activity?.let {
                            Navigation.findNavController(it, R.id.home_nav_fragment).navigateUp()
                        }
                    }
                }
            }
        })


        return binding.root
    }

    private fun attemptRemoteConnect() {
        binding.ssid.error = null
        binding.password.error = null

        val ssidStr = binding.ssid.text.toString()
        val passwordStr = binding.password.text.toString()

        var cancel = false
        var focusView: View? = null

        if (!isPasswordValid(passwordStr)) {
            binding.password.error = getString(R.string.error_invalid_password)
            focusView = binding.password
            cancel = true
        }

        if (ssidStr.isEmpty()) {
            binding.ssid.error = getString(R.string.error_field_required)
            focusView = binding.ssid
            cancel = true
        }

        if (cancel) {
            focusView?.requestFocus()
        } else {
            wifiSettingsViewModel.sendData(WifiCredentials(ssidStr, passwordStr))
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.isNotEmpty() && password.length > 8
    }
}