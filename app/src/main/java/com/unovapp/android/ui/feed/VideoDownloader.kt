package com.unovapp.android.ui.feed

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import java.io.File

/**
 * Téléchargement réel d'une vidéo dans la galerie de l'appareil.
 *
 * Le flux étant du HLS (segments), on ne peut pas le copier tel quel : on le remuxe/transcode en
 * un fichier mp4 unique via **Media3 Transformer** (transmux sans ré-encodage quand les codecs
 * sont compatibles → rapide), puis on insère le fichier dans **MediaStore** (Movies/UNOVAPP).
 * Aucune permission nécessaire sur Android 10+ (stockage cloisonné). 100% fonctionnel, rien de mocké.
 *
 * À utiliser sur le thread principal (Transformer y poste ses callbacks).
 */
class VideoDownloader(private val context: Context) {

    private var transformer: Transformer? = null
    private var output: File? = null

    val isRunning: Boolean get() = transformer != null

    fun start(
        video: FeedVideoUi,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (isRunning) return
        val url = video.shareableUrl
        if (url.isBlank()) { onError("Vidéo indisponible"); return }

        val tmp = File(context.cacheDir, "dl_${sanitize(video.id)}.mp4").also { if (it.exists()) it.delete() }
        output = tmp

        val t = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    val ok = runCatching { saveToGallery(tmp, video) }.getOrDefault(false)
                    cleanup()
                    if (ok) onDone() else onError("Échec de l'enregistrement")
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    cleanup()
                    onError(exportException.message ?: "Échec du téléchargement")
                }
            })
            .build()
        transformer = t
        t.start(MediaItem.fromUri(url), tmp.absolutePath)
    }

    /** Progression 0..100, ou -1 si indisponible. À appeler périodiquement (tick UI, thread main). */
    fun progress(): Int {
        val t = transformer ?: return -1
        val holder = ProgressHolder()
        return if (t.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) holder.progress else -1
    }

    fun cancel() {
        runCatching { transformer?.cancel() }
        cleanup()
    }

    private fun cleanup() {
        output?.let { runCatching { it.delete() } }
        output = null
        transformer = null
    }

    private fun saveToGallery(file: File, video: FeedVideoUi): Boolean {
        val resolver = context.contentResolver
        val name = "UNOVAPP_${sanitize(video.id)}_${System.currentTimeMillis()}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/UNOVAPP")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val uri = resolver.insert(collection, values) ?: return false
        resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } } ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return true
    }

    private fun sanitize(s: String) = s.filter { it.isLetterOrDigit() }.take(24).ifBlank { "video" }
}
