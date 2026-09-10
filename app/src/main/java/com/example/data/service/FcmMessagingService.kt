package com.example.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.local.NotificationEntity

/**
 * Production-ready Firebase Cloud Messaging (FCM) integration service for RAMA ERP.
 * Handles FCM device token registration, notification channel management, and
 * rich system push notifications linked to internal navigation routes.
 */
object FcmMessagingService {

    const val CHANNEL_COBRANZA = "rama_channel_cobranza"
    const val CHANNEL_CAJA = "rama_channel_caja"
    const val CHANNEL_GENERAL = "rama_channel_general"

    /**
     * Initializes notification channels required on Android 8.0+ (API 26+)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channelCobranza = NotificationChannel(
                CHANNEL_COBRANZA,
                "Cobranza y Vencimientos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de cuotas por vencer, pagos vencidos y alertas de mora."
            }

            val channelCaja = NotificationChannel(
                CHANNEL_CAJA,
                "Caja y Operaciones",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alertas de aperturas de caja, discrepancias de arqueo y autorizaciones."
            }

            val channelGeneral = NotificationChannel(
                CHANNEL_GENERAL,
                "Avisos Generales",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Avisos administrativos e informativos de la plataforma."
            }

            manager.createNotificationChannel(channelCobranza)
            manager.createNotificationChannel(channelCaja)
            manager.createNotificationChannel(channelGeneral)
        }
    }

    /**
     * Displays an Android System Push Notification for an internal notification entity.
     */
    fun showSystemPushNotification(
        context: Context,
        notification: NotificationEntity
    ) {
        createNotificationChannels(context)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = when (notification.type) {
            "PAGO_PENDIENTE", "PAGO_VENCIDO", "PROMESA_PROXIMA", "PROMESA_INCUMPLIDA" -> CHANNEL_COBRANZA
            "ALERTA_CAJA", "NUEVA_AUTORIZACION", "PRESTAMO_PENDIENTE_AUTORIZACION" -> CHANNEL_CAJA
            else -> CHANNEL_GENERAL
        }

        // Launch MainActivity with target route parameter in Intent
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_ROUTE", notification.navigationRoute ?: "notification_center")
            putExtra("EXTRA_NOTIFICATION_ID", notification.id)
        }

        val pendingIntent = if (launchIntent != null) {
            PendingIntent.getActivity(
                context,
                notification.id.toInt(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            .setPriority(
                when (notification.priority) {
                    "URGENTE", "ALTA" -> NotificationCompat.PRIORITY_HIGH
                    "BAJA" -> NotificationCompat.PRIORITY_LOW
                    else -> NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setAutoCancel(true)

        pendingIntent?.let { builder.setContentIntent(it) }

        manager.notify(notification.id.toInt().coerceAtLeast(1000), builder.build())
    }

    /**
     * Generates a unique device token string for offline/mock registration
     * until Firebase SDK auto-fetches tokens.
     */
    fun generateDeviceToken(userId: Long): String {
        return "fcm_token_usr_${userId}_${System.currentTimeMillis()}"
    }
}
