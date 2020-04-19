package com.krisbiketeam.smarthomeraspbpi3.common.nearby

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.nearby.connection.PayloadTransferUpdate.Status.FAILURE
import com.google.android.gms.nearby.connection.PayloadTransferUpdate.Status.SUCCESS
import com.krisbiketeam.smarthomeraspbpi3.common.auth.FirebaseCredentials
import com.krisbiketeam.smarthomeraspbpi3.common.auth.WifiCredentials
import com.squareup.moshi.Moshi
import timber.log.Timber

private const val NICK_NAME = "SmartHome Raspberry Pi3"
private const val SERVICE_ID = "com.krisbiketeam.smarthomeraspbpi3"
private const val CLIENT_ID = "clientId"

//TODO: Add Stop/Pause/Resume
class NearbyServiceProvider(private val context: Context, private val moshi: Moshi) : NearbyService {
    private var dataSendResultListener: NearbyService.DataSendResultListener? = null
    private var dataReceiverListener: NearbyService.DataReceiverListener? = null
    private var dataToBeSent: String? = null

    init {
        Timber.d("init")
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Timber.d("onEndpointFound: $endpointId info: $info")
            requestConnection(endpointId)
        }

        override fun onEndpointLost(endpointId: String) {
            Timber.w("onEndpointLost: $endpointId")
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            Timber.d("connectionResult from $endpointId result:$result")
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Timber.d("connectionResult STATUS_OK")
                    // We're connected! Can now start sending and receiving data.
                    sendDataPayload(endpointId)
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    // The connection was rejected by one or both sides.
                    Timber.d("connectionResult STATUS_CONNECTION_REJECTED")
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    // The connection was rejected by one or both sides.
                    Timber.d("connectionResult STATUS_ERROR")
                }
                else -> {
                    // The connection was broken before it was accepted.
                }
            }
            Timber.d("connectionResult statusCode: ${result.status.statusCode} statusMessage: ${result.status.statusMessage}")

        }

        override fun onDisconnected(endpointId: String) {
            Timber.w("onDisconnected from $endpointId")
            dataSendResultListener?.onFailure(Exception("Disconnected"))
        }

        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Timber.d("onConnectionInitiated from $endpointId connectionInfo: $connectionInfo")
            Nearby.getConnectionsClient(context).acceptConnection(endpointId, payloadCallback)
            // TODO: Can we safely stop Advertising????
            dataReceiverListener?.run {
                Nearby.getConnectionsClient(context).stopAdvertising()
            }

            // TODO: Can we safely stop Discovery????
            dataSendResultListener?.run {
                Nearby.getConnectionsClient(context).stopDiscovery()
            }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            Timber.d("onPayloadReceived $payload")
            payload.let {
                dataReceiverListener?.onDataReceived(payload.asBytes())
                Nearby.getConnectionsClient(context).stopAllEndpoints()
                dataReceiverListener = null
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            when (update.status) {
                SUCCESS -> {
                    dataSendResultListener?.onSuccess()
                    Nearby.getConnectionsClient(context).stopAllEndpoints()
                    dataSendResultListener = null
                    Timber.d("onPayloadTransferUpdate: SUCCESS")
                }
                FAILURE -> {
                    dataSendResultListener?.onFailure(Exception("onPayloadTransferUpdate: FAILED"))
                    dataSendResultListener = null
                }
            }
        }
    }

    override fun stop() {
        Timber.d("stop")
        pause()
        dataSendResultListener = null
        dataReceiverListener = null
        dataToBeSent = null
    }

    override fun pause() {
        Timber.d("pause")
        Nearby.getConnectionsClient(context).stopAdvertising()
        Nearby.getConnectionsClient(context).stopDiscovery()
        Nearby.getConnectionsClient(context).stopAllEndpoints()
    }

    override fun resume() {
        dataSendResultListener?.run {
            dataToBeSent?.run{
                Timber.d("resume")
                startDiscovery()
            }
        }
        dataReceiverListener?.let{
            Timber.d("resume")
            startAdvertising()
        }
    }

    private fun sendDataPayload(endpointId: String) {
        dataToBeSent?.run {
            Nearby.getConnectionsClient(context).sendPayload(endpointId, Payload.fromBytes(dataToBeSent?.toByteArray()
                    ?: ByteArray(0)))
        }
    }

    private fun requestConnection(endpointId: String) {
        Nearby.getConnectionsClient(context).requestConnection(
                CLIENT_ID,
                endpointId,
                connectionLifecycleCallback)
                .addOnSuccessListener {
                    // We successfully requested a connection. Now both sides
                    // must accept before the connection is established.
                    Timber.d("requestConnection:SUCCESS")
                }
                .addOnFailureListener {
                    // Nearby Connections failed to request the connection.
                    Timber.w("requestConnection:FAILURE ${it.stackTrace}")
                }

    }

    private fun startAdvertising() {
        Timber.d("Starting Advertising")

        Nearby.getConnectionsClient(context).startAdvertising(
                NICK_NAME,
                SERVICE_ID,
                connectionLifecycleCallback,
                AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build())
                .addOnSuccessListener {
                    Timber.d("startAdvertising:onResult: SUCCESS")
                }
                .addOnFailureListener {
                    Timber.w("Advertising failed! $it")
                }
    }

    private fun startDiscovery() {
        Timber.d("Starting discovery")

        Nearby.getConnectionsClient(context).startDiscovery(
                SERVICE_ID,
                endpointDiscoveryCallback,
                DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build())
                .addOnSuccessListener {
                    Timber.d("startDiscovery:SUCCESS")
                }
                .addOnFailureListener {
                    Timber.w("startDiscovery:FAILURE $it")
                    dataSendResultListener?.onFailure(it)
                }
    }

    override fun sendData(data: Any) {
        Timber.d("sendData data: $data")

        when (data) {
            is WifiCredentials -> {
                val adapter = moshi.adapter(WifiCredentials::class.java)
                dataToBeSent = adapter.toJson(data)
            }
            is FirebaseCredentials -> {
                val adapter = moshi.adapter(FirebaseCredentials::class.java)
                dataToBeSent = adapter.toJson(data)
            }
            is String -> {
                val adapter = moshi.adapter(String::class.java)
                dataToBeSent = adapter.toJson(data)
            }
        }

        startDiscovery()
    }

    override fun dataSendResultListener(dataSendResultListener: NearbyService.DataSendResultListener) {
        this.dataSendResultListener = dataSendResultListener
    }

    override fun dataReceivedListener(dataReceiverListener: NearbyService.DataReceiverListener) {
        Timber.d("dataReceivedListener")
        this.dataReceiverListener = dataReceiverListener
        startAdvertising()
    }

    override fun isActive(): Boolean {
        return dataSendResultListener != null || dataReceiverListener != null
    }
}
