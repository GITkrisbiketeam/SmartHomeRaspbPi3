package com.krisbiketeam.smarthomeraspbpi3.utils

import android.app.TimePickerDialog
import android.content.Context
import android.widget.TimePicker
import androidx.lifecycle.MutableLiveData
import com.krisbiketeam.smarthomeraspbpi3.R
import java.text.SimpleDateFormat
import java.util.*


fun getLastUpdateTime(context: Context, lastUpdateTime: Long?): String {
    if (lastUpdateTime == null) {
        return "N/A"
    }
    val date = Date(lastUpdateTime)

    // calculate days from unit time to now 1000 milliseconds * 60 seconds * 60 minutes * 24 hours = 86400000L
    val days = ((System.currentTimeMillis() - date.time) / 86400000L).toInt()

    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    timeFormat.format(date)
    return if (days > 0) {
        context.resources.getQuantityString(R.plurals.last_update_time, days,
                                            timeFormat.format(date), days)
    } else {
        context.resources.getString(R.string.last_update_time, timeFormat.format(date))
    }
}

fun showTimePicker(context: Context?, liveData: MutableLiveData<Long?>) {
    // TODO: Add proper Time Picker
    context?.let {
        TimePickerDialog(context, { _: TimePicker, hourOfDay: Int, minute: Int ->
            liveData.value = (minute * 1000 + hourOfDay * 60 * 1000).toLong()
        }, 0, 0, true).show()
    }
}