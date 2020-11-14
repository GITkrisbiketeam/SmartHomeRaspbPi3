package com.krisbiketeam.smarthomeraspbpi3.utils

import android.content.Context
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit
import java.text.SimpleDateFormat
import java.util.*


fun getLastUpdateTime(context: Context, item: HomeUnit<Any?>): String {
    val date = Date(item.lastUpdateTime ?: 0)

    // calculate days from unit time to now 1000 milliseconds * 60 seconds * 60 minutes * 24 hours = 86400000L
    val days = ((System.currentTimeMillis() - date.time) / 86400000L).toInt()

    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    timeFormat.format(date)
    return if (days > 0) {
        context.resources.getQuantityString(R.plurals.last_update_time, days, timeFormat.format(date), days)
    } else {
        context.resources.getString(R.string.last_update_time, timeFormat.format(date))
    }
}