package com.krisbiketeam.smarthomeraspbpi3.common.ble

import org.koin.core.annotation.KoinInternalApi
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.createScope
import org.koin.core.instance.InstanceContext
import org.koin.core.scope.Scope
import timber.log.Timber

class BluetoothScope : KoinScopeComponent {

    override val scope: Scope by lazy {
        Timber.i("BluetoothScope created")
        createScope(this)
    }

    fun close() {
        val bleService: BleService? = getIfAlreadyCreated()
        Timber.i("close bleService:$bleService")
        bleService?.stop()
        val bluetoothEnablerManager: BluetoothEnablerManager? = getIfAlreadyCreated()
        bluetoothEnablerManager?.disableBluetooth()

        scope.close()
    }

    @OptIn(KoinInternalApi::class)
    inline fun <reified T : Any> getIfAlreadyCreated(): T? {
        val instanceContext = InstanceContext(scope.getKoin().logger, scope)
        return scope.getKoin().instanceRegistry.instances.values
            .filter { factory ->
                factory.beanDefinition.scopeQualifier == instanceContext.scope.scopeQualifier
            }
            .filter { factory ->
                factory.beanDefinition.primaryType == T::class || factory.beanDefinition.secondaryTypes.contains(
                    T::class
                )
            }
            .distinct().firstOrNull()?.run {
                if (this.isCreated(instanceContext)) {
                    get(instanceContext) as T?
                } else {
                    null
                }
            }
    }
}