package com.unovapp.android

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.BandwidthMeter
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

/**
 * Lecture vidéo façon TikTok : cache disque partagé + fabrique de sources média cache-backed.
 *
 * Le cache ([SimpleCache]) est un **singleton de processus** (une seule instance par dossier,
 * imposé par Media3) → fourni ici en @Singleton. Il alimente le préfetch : les segments
 * téléchargés à l'avance restent sur disque → reprise instantanée, zéro re-téléchargement.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlaybackModule {

    /**
     * 1 Go de cache disque LRU — l'origine étant lente (pas encore de CDN actif), chaque octet
     * déjà téléchargé est précieux : plus de vidéos des sessions passées survivent → le feed
     * restauré au démarrage rejoue depuis le disque, et revoir une vidéo ne re-télécharge rien.
     * (Dans cacheDir : Android peut purger si le stockage manque.)
     */
    private const val CACHE_SIZE_BYTES = 1024L * 1024 * 1024

    private const val PLAYBACK_PREFS = "playback_prefs"
    private const val KEY_BITRATE_ESTIMATE = "bitrate_estimate"
    /** On ne réécrit l'estimation au plus qu'une fois toutes les 10 s (I/O inutile sinon). */
    private const val SAVE_INTERVAL_MS = 10_000L

    /**
     * BandwidthMeter unique pour toute la lecture vidéo, avec **estimation de débit persistée** :
     * au premier lancement d'une session, l'ABR repart de la dernière estimation mesurée au lieu
     * de la valeur par défaut du pays → la première vidéo démarre directement à la bonne
     * résolution (ni trop basse = floue, ni trop haute = rebuffering).
     */
    @Provides
    @Singleton
    fun provideBandwidthMeter(@ApplicationContext context: Context): BandwidthMeter {
        val prefs = context.getSharedPreferences(PLAYBACK_PREFS, Context.MODE_PRIVATE)
        val saved = prefs.getLong(KEY_BITRATE_ESTIMATE, 0L)
        // Tout premier lancement (aucune mesure) : estimation volontairement PESSIMISTE →
        // l'ABR démarre sur une rendition légère = première frame quasi immédiate, puis monte
        // en qualité dès que le débit réel est mesuré. (Le défaut par pays de Media3 est trop
        // optimiste pour nos réseaux mobiles → il choisissait une qualité intenable.)
        // 800 kbps × bandwidthFraction 0,7 = ~560 kbps utiles → démarre en 240p (~500 kbps).
        val initial = if (saved > 0L) saved else 800_000L
        val meter = DefaultBandwidthMeter.Builder(context)
            .setInitialBitrateEstimate(initial)
            .build()
        var lastSaveMs = 0L
        meter.addEventListener(
            Handler(Looper.getMainLooper()),
            BandwidthMeter.EventListener { _, _, bitrateEstimate ->
                val now = SystemClock.elapsedRealtime()
                if (bitrateEstimate > 0L && now - lastSaveMs >= SAVE_INTERVAL_MS) {
                    lastSaveMs = now
                    prefs.edit().putLong(KEY_BITRATE_ESTIMATE, bitrateEstimate).apply()
                }
            }
        )
        return meter
    }

    @Provides
    @Singleton
    fun provideVideoCache(@ApplicationContext context: Context): SimpleCache {
        val dir = File(context.cacheDir, "media_cache")
        return SimpleCache(
            dir,
            LeastRecentlyUsedCacheEvictor(CACHE_SIZE_BYTES),
            StandaloneDatabaseProvider(context)
        )
    }

    @Provides
    @Singleton
    fun provideCacheDataSourceFactory(
        @ApplicationContext context: Context,
        cache: SimpleCache
    ): CacheDataSource.Factory {
        val upstream = DefaultDataSource.Factory(
            context,
            DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(15_000)
        )
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstream)
            // En cas d'erreur cache → on retombe sur le réseau au lieu de planter la lecture.
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    @Provides
    @Singleton
    fun provideMediaSourceFactory(
        cacheDataSourceFactory: CacheDataSource.Factory
    ): MediaSource.Factory = DefaultMediaSourceFactory(cacheDataSourceFactory)
}
