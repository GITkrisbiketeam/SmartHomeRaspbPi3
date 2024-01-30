package com.krisbiketeam.smarthomeraspbpi3.common.ble

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

interface BluetoothContext : CoroutineContext.Element {

    companion object Key : CoroutineContext.Key<BluetoothContext>

    override val key: CoroutineContext.Key<*> get() = Key

    val bluetoothScope: BluetoothScope

}

class DefaultBluetoothContext : BluetoothContext, KoinComponent {
    override val bluetoothScope: BluetoothScope by inject()
}

suspend fun <T> withBluetoothContext(
    block: suspend CoroutineScope.() -> T,
): T {
    val bluetoothContext = DefaultBluetoothContext()
    return try {
        withContext(bluetoothContext, block)
    } finally {
        Timber.e("withBluetoothContext finally")
        bluetoothContext.bluetoothScope.close()
        bluetoothContext.cancel()
    }
}

suspend fun getBluetoothContext(): BluetoothContext = coroutineScope {
    coroutineContext[BluetoothContext]
        ?: throw IllegalStateException("Not called witih BluetoothContext")
}

inline fun <reified T : Any> BluetoothContext.get(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): T {
    return bluetoothScope.scope.get(T::class, qualifier, parameters)
}