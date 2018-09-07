package com.krisbiketeam.smarthomeraspbpi3.ui

import android.os.Bundle
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import androidx.navigation.Navigation
import com.krisbiketeam.smarthomeraspbpi3.R
import timber.log.Timber

/**
 * A fragment representing settings screen.
 */
class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.settings_fragment_preference, rootKey)
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        Timber.d("onPreferenceTreeClick preference: $preference")
        activity?.let {
            when(preference?.key){
                getString(R.string.settings_wifi_fragment_key) -> {
                    Timber.d("onPreferenceTreeClick go to WifiSettings")
                    val direction = SettingsFragmentDirections.ActionSettingsFragmentToWifiSettingsFragment()
                    Navigation.findNavController(it, R.id.home_nav_fragment).navigate(direction)
                    return true
                }
                getString(R.string.settings_login_fragment_key) -> {
                    Timber.d("onPreferenceTreeClick go to LoginSettings")
                    val direction = SettingsFragmentDirections.ActionSettingsFragmentToLoginSettingsFragment()
                    Navigation.findNavController(it, R.id.home_nav_fragment).navigate(direction)
                    return true
                }
                else -> {
                    return super.onPreferenceTreeClick(preference)
                }
            }
        }
        return super.onPreferenceTreeClick(preference)
    }
}
