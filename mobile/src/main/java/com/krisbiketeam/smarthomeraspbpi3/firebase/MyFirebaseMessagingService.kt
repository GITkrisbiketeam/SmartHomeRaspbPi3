package com.krisbiketeam.smarthomeraspbpi3.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.support.v4.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.krisbiketeam.smarthomeraspbpi3.LoadActivity
import com.krisbiketeam.smarthomeraspbpi3.R
import timber.log.Timber
import java.lang.Exception


class MyFirebaseMessagingService : FirebaseMessagingService() {
    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Timber.d("onMessageReceived message: $remoteMessage")
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // Check if message contains a data payload.
        if (remoteMessage?.data?.size != 0) {
            Timber.d("Message data payload: " + remoteMessage?.data)

            if (/* Check if data needs to be processed by long running job */ true) {
                // For long-running tasks (10 seconds or more) use Firebase Job Dispatcher.
                scheduleJob()
            } else {
                // Handle message within 10 seconds
                handleNow()
            }

        }

        // Check if message contains a notification payload.
        remoteMessage?.notification?.let {
            Timber.d("Message Notification Body: " + it.body)
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String?) {
        Timber.d("onNewToken token: $token")
        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        token?.let(this::sendRegistrationToServer)

        super.onNewToken(token)
    }


    override fun onMessageSent(s: String?) {
        Timber.d("onMessageSent s: $s")
        super.onMessageSent(s)
    }

    override fun onDeletedMessages() {
        Timber.d("onDeletedMessages")
        super.onDeletedMessages()
    }

    override fun onSendError(s: String?, exception: Exception?) {
        Timber.d("onSendError s: $s, exception: $exception")
        super.onSendError(s, exception)
    }

    /**
     * Schedule a job using FirebaseJobDispatcher.
     */
    private fun scheduleJob() {
        // [START dispatch_job]
        /*val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(this))
        val myJob = dispatcher.newJobBuilder()
                .setService(MyJobService::class.java)
                .setTag("my-job-tag")
                .build()
        dispatcher.schedule(myJob)*/
        // [END dispatch_job]
    }

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private fun handleNow() {
        Timber.d("Short lived task is done.")
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private fun sendRegistrationToServer(token: String) {
        // TODO: Implement this method to send token to your app server.
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private fun sendNotification(messageBody: String) {
        val intent = Intent(this, LoadActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT)

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("FCM Message")
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }
}