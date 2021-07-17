package com.krisbiketeam.smarthomeraspbpi3.common

import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KProperty

const val FULL_DAY_IN_MILLIS = 86400000L

fun <T> resetableLazy(initializer: () -> T) = ResetableDelegate(initializer)

class ResetableDelegate<T>(private val initializer: () -> T) {
    private val lazyRef: AtomicReference<Lazy<T>> = AtomicReference(lazy(initializer))

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return lazyRef.get().getValue(thisRef, property)
    }

    fun reset() {
        lazyRef.set(lazy(initializer))
    }
}

fun Long.getOnlyDateLocalTime(): Long {
    val today: Calendar = Calendar.getInstance()//TimeZone.getTimeZone("UTC"))
    today.timeInMillis = this
    today[Calendar.HOUR_OF_DAY] = 0
    today[Calendar.MINUTE] = 0
    today[Calendar.SECOND] = 0
    today[Calendar.MILLISECOND] = 0
    today.timeZone = TimeZone.getTimeZone("UTC")
    return today.timeInMillis
}

fun Long.getOnlyTodayLocalTime(): Long {
    val today: Calendar = Calendar.getInstance()//TimeZone.getTimeZone("UTC"))
    today.timeInMillis = this
    today[Calendar.YEAR] = 1970
    today[Calendar.MONTH] = 0
    today[Calendar.DATE] = 1
    today.timeZone = TimeZone.getTimeZone("UTC")
    return today.timeInMillis
}
