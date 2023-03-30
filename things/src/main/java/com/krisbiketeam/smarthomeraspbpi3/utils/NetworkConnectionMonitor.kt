package com.krisbiketeam.smarthomeraspbpi3.utils

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import timber.log.Timber


interface NetworkConnectionListener {
    fun onNetworkAvailable(available: Boolean)
}

class NetworkConnectionMonitor(activity: Activity) : ConnectivityManager.NetworkCallback() {

    private val networkRequest: NetworkRequest =
            NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED).build()

    private val connectivityManager: ConnectivityManager =
            activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var networkConnectionListener: NetworkConnectionListener? = null

    val isNetworkConnected =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.run {
                (hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || hasTransport(
                        NetworkCapabilities.TRANSPORT_CELLULAR) || hasTransport(
                        NetworkCapabilities.TRANSPORT_ETHERNET)) && hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET) && hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } ?: false

    fun startListen(listener: NetworkConnectionListener) {
        networkConnectionListener = listener
        connectivityManager.registerNetworkCallback(networkRequest, this)
    }

    fun stopListen() {
        connectivityManager.unregisterNetworkCallback(this)
        networkConnectionListener = null
    }

    override fun onLost(network: Network) {
        Timber.e("onLost network: $network")
        networkConnectionListener?.onNetworkAvailable(false)
    }

    override fun onUnavailable() {
        Timber.e("onUnavailable")
        networkConnectionListener?.onNetworkAvailable(false)
    }

    @Suppress("DEPRECATION")
    override fun onAvailable(network: Network) {
        networkConnectionListener?.onNetworkAvailable(true)
    }
}