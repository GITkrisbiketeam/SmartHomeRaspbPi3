package com.krisbiketeam.smarthomeraspbpi3

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

interface NetworkConnectionListener {
    fun onNetworkAvailable(available: Boolean)
}

class NetworkConnectionMonitor(activity: Activity) : ConnectivityManager.NetworkCallback() {

    private val networkRequest: NetworkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()

    private val connectivityManager: ConnectivityManager =
            activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var networkConnectionListener: NetworkConnectionListener? = null


    fun startListen(listener: NetworkConnectionListener) {
        networkConnectionListener = listener
        connectivityManager.registerNetworkCallback(networkRequest, this)
    }

    fun stopListen() {
        connectivityManager.unregisterNetworkCallback(this)
        networkConnectionListener = null
    }

    fun isNetworkConnected(): Boolean {
        return connectivityManager.activeNetworkInfo?.isConnected ?: false
    }

    fun isWifiConnected(): Boolean {
        return connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                ?: false
    }

    override fun onUnavailable() {
        networkConnectionListener?.onNetworkAvailable(false)
    }

    override fun onAvailable(network: Network?) {
        networkConnectionListener?.onNetworkAvailable(true)
    }
}