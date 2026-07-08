package com.unovapp.android.data.video

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistance de la première page du feed sur disque (JSON dans filesDir).
 *
 * Objectif : **démarrage instantané à froid**. À l'ouverture de l'app, le dernier feed connu
 * s'affiche immédiatement — et comme les premières secondes de ces vidéos sont généralement
 * encore dans le cache disque ExoPlayer (512 Mo, LRU), la première vidéo démarre SANS RÉSEAU,
 * pendant que le feed frais se charge en arrière-plan et remplace la liste.
 *
 * On persiste les DTO (pas les modèles UI) : le mapping applique les règles à jour
 * (playbackUrl/ABR, formats) à chaque lecture.
 */
@Singleton
class FeedDiskCache @Inject constructor(@ApplicationContext context: Context) {

    private val file = File(context.filesDir, "feed_first_page.json")
    private val gson = Gson()

    suspend fun load(): List<FeedVideoDto> = withContext(Dispatchers.IO) {
        if (!file.exists()) emptyList()
        else runCatching {
            val type = object : TypeToken<List<FeedVideoDto>>() {}.type
            gson.fromJson<List<FeedVideoDto>>(file.readText(), type) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    suspend fun save(videos: List<FeedVideoDto>) {
        withContext(Dispatchers.IO) {
            runCatching { file.writeText(gson.toJson(videos)) }
        }
    }
}
