package com.krisbiketeam.smarthomeraspbpi3.units

interface Sensor<T> : BaseUnit<T> {

    fun registerListener(listener: HomeUnitListener<T>)

    fun unregisterListener()

    fun readValue(): T?

    /**
     * Interface definition for a callback to be invoked when a Sensor event occurs.
     */
    interface HomeUnitListener<in T> {
        /**
         * Called when a HomeUnit event occurs
         *
         * @param homeUnit the HomeUnit for which the event occurred
         * @param value Object with unit changed value
         */
        fun onUnitChanged(homeUnit: HomeUnit<out T>, value: T?)
    }
}