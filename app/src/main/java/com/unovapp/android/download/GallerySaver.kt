package com.unovapp.android.download

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import java.io.File

/**
 * Insère un fichier vidéo dans la galerie via MediaStore (Movies/UNOVAPP). Aucune permission
 * requise sur Android 10+ (stockage cloisonné). Retourne true si l'enregistrement a réussi.
 */
fun saveVideoToGallery(context: Context, file: File, videoId: String): Boolean {
    val resolver = context.contentResolver
    val name = "UNOVAPP_${sanitizeId(videoId)}_${System.currentTimeMillis()}.mp4"
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

internal fun sanitizeId(s: String) = s.filter { it.isLetterOrDigit() }.take(24).ifBlank { "video" }
