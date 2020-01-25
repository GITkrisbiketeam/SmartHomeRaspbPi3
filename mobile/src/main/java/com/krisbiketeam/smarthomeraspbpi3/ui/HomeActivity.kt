package com.krisbiketeam.smarthomeraspbpi3.ui

import androidx.databinding.DataBindingUtil
import android.os.Bundle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.FirebaseHomeInformationRepository
import com.krisbiketeam.smarthomeraspbpi3.common.storage.NotSecureStorage
import com.krisbiketeam.smarthomeraspbpi3.common.storage.SecureStorage
import com.krisbiketeam.smarthomeraspbpi3.databinding.ActivityHomeBinding
import com.krisbiketeam.smarthomeraspbpi3.databinding.NavHeaderBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.NavigationViewModel
import org.koin.android.viewmodel.ext.android.viewModel
import timber.log.Timber

class HomeActivity  : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var secureStorage: SecureStorage

    private val navigationViewModel by viewModel<NavigationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityHomeBinding = DataBindingUtil.setContentView(this,
                R.layout.activity_home)
        drawerLayout = binding.drawerLayout

        DataBindingUtil.inflate<NavHeaderBinding>(
                layoutInflater, R.layout.nav_header, binding.navigationView, false).apply {
            binding.navigationView.addHeaderView(root)
            viewModel = navigationViewModel
            lifecycleOwner = this@HomeActivity
        }

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.home_nav_fragment)

        // Set up ActionBar
        setupActionBarWithNavController(navController, drawerLayout)

        // Set up navigation menu
        binding.navigationView.setupWithNavController(navController)

        secureStorage = NotSecureStorage(this)

        if (secureStorage.homeName.isEmpty()){
            Timber.d("No Home Name defined, starting HomeSettingsFragment")
            navController.navigate(R.id.home_settings_fragment)
        } else {
            FirebaseHomeInformationRepository.setHomeReference(secureStorage.homeName)
        }

        if (!secureStorage.isAuthenticated()) {
            Timber.d("No Home Name defined, starting HomeSettingsFragment")
            navController.navigate(R.id.login_settings_fragment)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(
                Navigation.findNavController(this, R.id.home_nav_fragment), drawerLayout)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}