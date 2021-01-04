package com.krisbiketeam.smarthomeraspbpi3.common

import com.krisbiketeam.smarthomeraspbpi3.common.storage.dto.HomeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KProperty

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

fun updateValueMinMax(homeUnit: HomeUnit<Any?>, unitValue: Any?, updateTime: Long) {
    homeUnit.value = unitValue
    homeUnit.lastUpdateTime = updateTime
    when (unitValue) {
        is Float -> {
            if (unitValue <= (homeUnit.min.takeIf { it is Number? } as Number?)?.toFloat() ?: Float.MAX_VALUE) {
                homeUnit.min = unitValue
                homeUnit.minLastUpdateTime = updateTime
            }
            if (unitValue >= (homeUnit.max.takeIf { it is Number? } as Number?)?.toFloat() ?: Float.MIN_VALUE) {
                homeUnit.max = unitValue
                homeUnit.maxLastUpdateTime = updateTime
            }
        }
    }
}