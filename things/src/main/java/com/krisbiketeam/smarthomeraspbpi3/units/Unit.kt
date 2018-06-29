package com.krisbiketeam.smarthomeraspbpi3.units

interface Unit : AutoCloseable{

    fun connect()

    fun readValue(): Any?

}