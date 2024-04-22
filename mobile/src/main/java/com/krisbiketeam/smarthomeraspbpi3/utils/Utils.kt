package com.krisbiketeam.smarthomeraspbpi3.utils

import android.content.Context
import androidx.annotation.ColorInt
import com.krisbiketeam.smarthomeraspbpi3.R
import com.krisbiketeam.smarthomeraspbpi3.common.FULL_DAY_IN_MILLIS
import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.RemoteLog
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.DurationUnit
import kotlin.time.toDuration

const val LOGS_CHART_TIME_PREFIX: Long = 1620000000000

fun getLastUpdateTime(context: Context, lastUpdateTime: Long?): String {
    if (lastUpdateTime == null) {
        return "N/A"
    }
    val date = Date(lastUpdateTime)

    // calculate days from unit time to now 1000 milliseconds * 60 seconds * 60 minutes * 24 hours = 86400000L
    val days = ((System.currentTimeMillis() - date.time) / FULL_DAY_IN_MILLIS).toInt()

    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    return if (days > 0) {
        context.resources.getQuantityString(
            R.plurals.last_update_time, days,
            timeFormat.format(date), days
        )
    } else {
        context.resources.getString(R.string.last_update_time, timeFormat.format(date))
    }
}

fun Long?.getUpdatedAgo(context: Context): String {
    if (this == null) {
        return "N/A"
    }
    // calculate days from unit time to now 1000 milliseconds * 60 seconds * 60 minutes * 24 hours = 86400000L
    val time = System.currentTimeMillis() - this

    val date = time.toDuration(DurationUnit.MILLISECONDS)
    return if (date.inWholeDays > 0) {
        val days = date.inWholeDays.toInt()
        context.resources.getQuantityString(R.plurals.updated_days_ago, days, days)
    } else if (date.inWholeHours > 0) {
        val hours = date.inWholeHours.toInt()
        context.resources.getString(R.string.updated_hours_ago, hours)
    } else if (date.inWholeMinutes > 0) {
        val minutes = date.inWholeMinutes.toInt()
        context.resources.getString(R.string.updated_minutes_ago, minutes)
    } else {
        val seconds = date.inWholeSeconds.toInt()
        context.resources.getString(R.string.updated_seconds_ago, seconds)
    }
}

fun Any?.getStringValue(): String {
    return when (this) {
        is Float -> String.format("%.1f", this)
        else -> toString()
    }
}

fun getDayTime(lastUpdateTime: Long?): String {
    if (lastUpdateTime == null) {
        return "N/A"
    }
    val seconds = (lastUpdateTime / 1000) % 60
    val minutes = (lastUpdateTime / (1000 * 60) % 60)
    val hours = (lastUpdateTime / (1000 * 60 * 60) % 25)

    return String.format("%d:%02d:%02d", hours, minutes, seconds)
}

fun showTimePicker(context: Context?, stateFlow: MutableStateFlow<Long?>) {
    context?.let {
        val (currentHours, currentMinutes, currentSeconds) = stateFlow.value?.run {
            Triple(
                (this / (1000 * 60 * 60) % 24).toInt(),
                ((this / (1000 * 60) % 60)).toInt(),
                ((this / 1000) % 60).toInt()
            )
        } ?: Triple(0, 0, 0)
        TimeDurationPicker(context, { hours: Int, minutes: Int, seconds: Int ->
            stateFlow.value =
                (seconds * 1000 + minutes * 1000 * 60 + hours * 60 * 60 * 1000).toLong()
        }, currentHours, currentMinutes, currentSeconds).show()
    }
}

fun Number.toLogsFloat(): Float {
    val tmp: Float = if (this is Long) {
        val tmp2 = (this - LOGS_CHART_TIME_PREFIX) / 1000
        tmp2.toFloat()
    } else {
        this.toFloat()
    }
    return tmp
}

fun Float.toLogsLong(): Long {
    return toLong() * 1000 + LOGS_CHART_TIME_PREFIX
}

@ColorInt
fun RemoteLog.getTextColor(context: Context): Int {
    return context.getColor(
        when (priority) {
            "DEBUG" -> R.color.thingsAppLogsColorDebug
            "INFO" -> R.color.thingsAppLogsColorInfo
            "WARN" -> R.color.thingsAppLogsColorWarn
            "ERROR" -> R.color.thingsAppLogsColorError
            else -> R.color.thingsAppLogsColorVerbose
        }
    )
}