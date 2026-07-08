package com.unovapp.android.notif

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.unovapp.android.MainActivity
import com.unovapp.android.R

/**
 * Point unique des notifications système UNOVAPP : création des canaux, construction des
 * notifications (téléchargement + activité) et vérification de la permission POST_NOTIFICATIONS.
 */
object Notifs {
    const val CHANNEL_DOWNLOADS = "downloads"
    const val CHANNEL_ACTIVITY = "activity"
    const val DOWNLOAD_NOTIF_ID = 1001
    private const val DOWNLOAD_DONE_NOTIF_ID = 1002
    private var activityNotifId = 2000

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_DOWNLOADS, "Téléchargements", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Progression des téléchargements de vidéos"
                setShowBadge(false)
            }
        )
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_ACTIVITY, "Activité", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "J'aime, commentaires, nouveaux abonnés"
            }
        )
    }

    fun downloadProgress(context: Context, progress: Int): Notification =
        NotificationCompat.Builder(context, CHANNEL_DOWNLOADS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Téléchargement de la vidéo")
            .setContentText(if (progress in 0..99) "$progress %" else "En cours…")
            .setProgress(100, progress.coerceIn(0, 100), progress < 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    fun downloadDone(context: Context, success: Boolean) {
        if (!hasPermission(context)) return
        val notif = NotificationCompat.Builder(context, CHANNEL_DOWNLOADS)
            .setSmallIcon(if (success) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
            .setContentTitle(if (success) "Vidéo enregistrée" else "Échec du téléchargement")
            .setContentText(if (success) "Disponible dans ta galerie (Movies/UNOVAPP)" else "Réessaie plus tard")
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(DOWNLOAD_DONE_NOTIF_ID, notif)
    }

    /** Notification d'activité (like, commentaire, abonné…) — issue des données backend réelles. */
    fun activity(context: Context, title: String, body: String) {
        if (!hasPermission(context)) return
        val notif = NotificationCompat.Builder(context, CHANNEL_ACTIVITY)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(activityNotifId++, notif)
    }

    private fun openApp(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
}
