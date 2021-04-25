package com.krisbiketeam.smarthomeraspbpi3.common

import android.os.Bundle
import androidx.annotation.Size
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase

class Analytics {
    private val firebaseAnalytics: FirebaseAnalytics? = Firebase.analytics

    fun logEvent(@Size(min = 1L, max = 40L) name: String, params: Bundle?) {
        firebaseAnalytics?.logEvent(name, params)
    }

    fun logEvent(name: String, block: com.google.firebase.analytics.ktx.ParametersBuilder.() -> Unit) {
        firebaseAnalytics?.logEvent(name, block)
    }

}