package com.krisbiketeam.smarthomeraspbpi3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.krisbiketeam.smarthomeraspbpi3.ui.compose.navigation.SmartNavGraph
import com.krisbiketeam.smarthomeraspbpi3.ui.compose.theme.SmartHomeRaspbPi3Theme
import com.krisbiketeam.smarthomeraspbpi3.usecases.ReloginLastUserWithHomeUseCase
import org.koin.android.ext.android.inject

class SmartActivity : ComponentActivity() {

    val reloginLastUserWithHomeUseCase by inject<ReloginLastUserWithHomeUseCase>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val status = reloginLastUserWithHomeUseCase()
        setContent {
            SmartHomeRaspbPi3Theme {
                // We could pass startDestination obtained form reloginLastUserWithHomeUseCase to start with Login screen if we are not logged in
                SmartNavGraph()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SmartHomeRaspbPi3Theme {
        SmartNavGraph()
    }
}