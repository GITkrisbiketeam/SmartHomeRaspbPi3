package com.krisbiketeam.smarthomeraspbpi3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
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
    }
}
