package com.krisbiketeam.smarthomeraspbpi3

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.krisbiketeam.smarthomeraspbpi3.compose.SmartApp
import com.krisbiketeam.smarthomeraspbpi3.compose.theme.SmartHomeRaspbPi3Theme
import com.krisbiketeam.smarthomeraspbpi3.usecases.ReloginLastUserWithHomeUseCase
import org.koin.android.ext.android.inject

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class SmartActivity : ComponentActivity() {

    val reloginLastUserWithHomeUseCase by inject<ReloginLastUserWithHomeUseCase>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val status = reloginLastUserWithHomeUseCase()
        setContent {
            SmartHomeRaspbPi3Theme {
                // We could pass startDestination obtained form reloginLastUserWithHomeUseCase to start with Login screen if we are not logged in
                SmartApp(calculateWindowSizeClass(this)/*, "startDestination"*/)
            }
        }
        requestPermissions()
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    0)
            }
        }
    }
}
