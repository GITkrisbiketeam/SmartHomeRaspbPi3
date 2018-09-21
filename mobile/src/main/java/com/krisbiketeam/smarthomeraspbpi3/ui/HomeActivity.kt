package com.krisbiketeam.smarthomeraspbpi3.ui

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.databinding.ActivityHomeBinding
import com.krisbiketeam.smarthomeraspbpi3.databinding.NavHeaderBinding
import com.krisbiketeam.smarthomeraspbpi3.viewmodels.NavigationViewModel
import org.koin.android.viewmodel.ext.android.viewModel

class HomeActivity  : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout

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
            setLifecycleOwner(this@HomeActivity)
        }

        val navController = Navigation.findNavController(this, R.id.home_nav_fragment)

        // Set up ActionBar
        setSupportActionBar(binding.toolbar)
        NavigationUI.setupActionBarWithNavController(this, navController, drawerLayout)

        // Set up navigation menu
        binding.navigationView.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(drawerLayout,
                Navigation.findNavController(this, R.id.home_nav_fragment))
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}