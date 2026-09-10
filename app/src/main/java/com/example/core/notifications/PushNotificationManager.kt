package com.example.core.notifications

import android.content.Context
import com.example.core.logger.AppLogger
import com.example.util.NotificationHelper

object PushNotificationManager {

    fun init(context: Context) {
        try {
            NotificationHelper.createNotificationChannel(context)
            AppLogger.i("PushNotificationManager initialized successfully", tag = "PushNotificationManager")
        } catch (e: Exception) {
            AppLogger.e("Error initializing notification channel", e, tag = "PushNotificationManager")
        }
    }

    fun sendLocalAlert(context: Context, id: Int, title: String, message: String) {
        try {
            NotificationHelper.showNotification(context, id, title, message)
            AppLogger.i("Notification [$id] sent: $title", tag = "PushNotificationManager")
        } catch (e: Exception) {
            AppLogger.e("Error sending local alert notification", e, tag = "PushNotificationManager")
        }
    }
}
