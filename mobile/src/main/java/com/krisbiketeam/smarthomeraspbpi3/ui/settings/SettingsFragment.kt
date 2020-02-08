package com.krisbiketeam.smarthomeraspbpi3.ui.settings

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
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
        preferenceManager
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        Timber.d("onPreferenceTreeClick preference: $preference")
        activity?.let {
            when (preference?.key) {
                getString(R.string.settings_wifi_fragment_key)                     -> {
                    Timber.d("onPreferenceTreeClick go to WifiSettings")
                    val direction =
                            SettingsFragmentDirections.actionSettingsFragmentToWifiSettingsFragment()
                    Navigation.findNavController(it, R.id.home_nav_fragment).navigate(direction)
                    return true
                }
                getString(R.string.settings_login_fragment_key)                    -> {
                    Timber.d("onPreferenceTreeClick go to LoginSettings")
                    val direction =
                            SettingsFragmentDirections.actionSettingsFragmentToLoginSettingsFragment()
                    Navigation.findNavController(it, R.id.home_nav_fragment).navigate(direction)
                    return true
                }
                getString(R.string.settings_home_fragment_key)                     -> {
                    Timber.d("onPreferenceTreeClick go to HomeSettings")
                    val direction =
                            SettingsFragmentDirections.actionSettingsFragmentToHomeSettingsFragment()
                    Navigation.findNavController(it, R.id.home_nav_fragment).navigate(direction)
                    return true
                }
                getString(R.string.settings_hw_unit_list_fragment_key)             -> {
                    Timber.d("onPreferenceTreeClick go to HwUnitList")
                    val direction =
                            SettingsFragmentDirections.actionSettingsFragmentToHwUnitListFragment()
                    Navigation.findNavController(it, R.id.home_nav_fragment).navigate(direction)
                    return true
                }
                getString(R.string.settings_hw_unit_error_event_list_fragment_key) -> {
                    Timber.d("onPreferenceTreeClick go to HwUnitErrorEventList")
                    val direction =
                            SettingsFragmentDirections.actionSettingsFragmentToHwUnitErrorEventListFragment()
                    Navigation.findNavController(it, R.id.home_nav_fragment).navigate(direction)
                    return true
                }
                else                                                               -> {
                    return super.onPreferenceTreeClick(preference)
                }
            }
        }
        return super.onPreferenceTreeClick(preference)
    }
}
