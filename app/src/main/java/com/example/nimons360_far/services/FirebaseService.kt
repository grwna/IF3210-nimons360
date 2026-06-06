package com.example.nimons360_far.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.nimons360_far.LauncherActivity
import com.example.nimons360_far.R
import com.example.nimons360_far.backend.utils.SessionManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FirebaseService : FirebaseMessagingService() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(this)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        
        // Send token if logged in
        val tokenString = sessionManager.fetchAuthToken()
        if (!tokenString.isNullOrEmpty()) {
            sendRegistrationToServer(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check user preferences
        if (!sessionManager.fetchNotificationPreference()) {
            Log.d(TAG, "Notifications are disabled in settings. Suppressing notification.")
            return
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            sendNotification(it.title ?: "Nimons360", it.body ?: "")
        }
    }

    private fun sendRegistrationToServer(token: String) {
        val notificationApi = com.example.nimons360_far.backend.network.ApiClient.getNotificationApi(this)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                notificationApi.subscribe(com.example.nimons360_far.backend.models.SubscribeRequest(token))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send token to server", e)
            }
        }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, LauncherActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, messageBody.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "nimons_default_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.nimons)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Nimons Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(messageBody.hashCode(), notificationBuilder.build())
    }

    companion object {
        private const val TAG = "FirebaseService"
    }
}
