package com.krisbiketeam.smarthomeraspbpi3.common

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

class Analytics {
    val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics
}