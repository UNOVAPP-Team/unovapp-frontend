package com.unovapp.android.download

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.unovapp.android.notif.Notifs
import java.io.File

/**
 * Service de premier plan qui télécharge une vidéo (HLS → mp4 via Media3 Transformer) puis
 * l'enregistre dans la galerie. Comme le travail vit dans le service (et non dans un composable),
 * **fermer la feuille de partage n'interrompt plus le téléchargement** — il continue en arrière-plan,
 * avec une notification de progression, et une notification de fin.
 */
class DownloadService : Service() {

    private var transformer: Transformer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var polling: Runnable? = null
    private var output: File? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_URL)
        val videoId = intent?.getStringExtra(EXTRA_ID) ?: "video"

        // Un seul téléchargement à la fois : on ignore une nouvelle demande si déjà occupé.
        if (transformer != null) return START_NOT_STICKY
        if (url.isNullOrBlank()) { stopSelf(); return START_NOT_STICKY }

        startForegroundCompat()
        DownloadCenter.set(DownloadCenter.State(videoId = videoId, progress = 0, running = true))
        startExport(url, videoId)
        return START_NOT_STICKY
    }

    private fun startForegroundCompat() {
        val notif = Notifs.downloadProgress(this, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(Notifs.DOWNLOAD_NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(Notifs.DOWNLOAD_NOTIF_ID, notif)
        }
    }

    private fun startExport(url: String, videoId: String) {
        val tmp = File(cacheDir, "dl_${sanitizeId(videoId)}.mp4").also { if (it.exists()) it.delete() }
        output = tmp
        val t = Transformer.Builder(this)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    val ok = runCatching { saveVideoToGallery(applicationContext, tmp, videoId) }.getOrDefault(false)
                    finish(ok)
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    finish(false)
                }
            })
            .build()
        transformer = t
        t.start(MediaItem.fromUri(url), tmp.absolutePath)
        startPolling()
    }

    private fun startPolling() {
        val r = object : Runnable {
            override fun run() {
                val t = transformer ?: return
                val holder = ProgressHolder()
                val p = if (t.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) holder.progress else -1
                if (p in 0..100) {
                    DownloadCenter.update(p)
                    if (Notifs.hasPermission(this@DownloadService)) {
                        NotificationManagerCompat.from(this@DownloadService)
                            .notify(Notifs.DOWNLOAD_NOTIF_ID, Notifs.downloadProgress(this@DownloadService, p))
                    }
                }
                handler.postDelayed(this, 450)
            }
        }
        polling = r
        handler.post(r)
    }

    private fun finish(success: Boolean) {
        polling?.let { handler.removeCallbacks(it) }
        polling = null
        output?.let { runCatching { it.delete() } }
        output = null
        transformer = null
        DownloadCenter.clear()
        Notifs.downloadDone(this, success)
        stopForegroundCompat()
        stopSelf()
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION") stopForeground(true)
        }
    }

    override fun onDestroy() {
        polling?.let { handler.removeCallbacks(it) }
        runCatching { transformer?.cancel() }
        transformer = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_ID = "id"

        fun start(context: Context, videoId: String, url: String) {
            val intent = Intent(context, DownloadService::class.java)
                .putExtra(EXTRA_URL, url)
                .putExtra(EXTRA_ID, videoId)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }
    }
}
