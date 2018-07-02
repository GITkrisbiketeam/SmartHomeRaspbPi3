package com.krisbiketeam.data.nearby

interface NearbyService {
    fun sendData(data: Any)
    fun dataSendResultListener(dataSendResultListener: DataSendResultListener)
    fun dataReceivedListener(dataReceiverListener: DataReceiverListener)

    interface DataSendResultListener {
        fun onSuccess()
        fun onFailure(exception: Exception)
    }

    interface DataReceiverListener {
        fun onDataReceived(data: ByteArray?)
    }
}
