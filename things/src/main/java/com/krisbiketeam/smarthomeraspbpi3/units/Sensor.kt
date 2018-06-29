package com.krisbiketeam.smarthomeraspbpi3.units

interface Sensor : Unit{

    fun registerListener(listener: HomeUnitListener)

    fun unregisterListener()

    /**
     * Interface definition for a callback to be invoked when a Sensor event occurs.
     */
    interface HomeUnitListener {
        /**
         * Called when a HomeUnit event occurs
         *
         * @param homeUnit the HomeUnit for which the event occurred
         * @param value Object with unit changed value
         */
        fun onUnitChanged(homeUnit: HomeUnit, value: Any?)
    }
}