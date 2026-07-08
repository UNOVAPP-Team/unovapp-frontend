package com.unovapp.android.ui.feed

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.TrackSelector
import androidx.media3.exoplayer.upstream.BandwidthMeter
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter

/**
 * Pool de lecteurs façon TikTok, préchargement piloté par **[DefaultPreloadManager]** (Media3 1.8) —
 * l'API officielle "short-form feed", même architecture que le demo-shortform de Media3.
 *
 * Ce qui change vs l'ancien préfetch artisanal :
 *  1. **Préchargement centralisé et priorisé** : le manager charge les sources par ordre de
 *     proximité (page+1 : 3 s, page+2 : 1,5 s, page-1 : pistes seulement) et ne précharge
 *     JAMAIS pendant que la vidéo courante bufferise → la 1ʳᵉ vidéo démarre au plus vite,
 *     sans le listener STATE_READY qu'on maintenait à la main.
 *  2. **Composants partagés** : les [size] lecteurs sont construits par le même Builder →
 *     un SEUL thread de lecture, un seul TrackSelector, un seul BandwidthMeter (estimation
 *     ABR commune, injectable pour la persister entre sessions).
 *  3. **MediaCodec asynchrone** (`forceEnableMediaCodecAsynchronousQueueing`) + **scheduling
 *     dynamique** (`experimentalSetDynamicSchedulingEnabled`) : moins de frames perdues sur
 *     les appareils modestes, moins de réveils CPU → lecture plus fluide, moins de batterie.
 *
 * Les pages voisines (±1) gardent leur lecteur préparé/attaché à une surface (première frame
 * déjà rendue = zéro flash au swipe) ; la page +2 vit uniquement dans le preload manager
 * (données bufferisées, pas de codec occupé).
 *
 * Association page → lecteur : `slot = page mod size`. Garantie « une seule vidéo à la fois » :
 * un SEUL lecteur a `playWhenReady=true` + le son ; les voisins sont préparés mais en pause.
 */
class FeedPlayerPool(
    context: Context,
    mediaSourceFactory: MediaSource.Factory,
    bandwidthMeter: BandwidthMeter? = null,
    private val size: Int = 4
) {
    private companion object {
        /** Durée préchargée pour la page suivante (celle du prochain swipe). */
        const val PRELOAD_NEXT_MS = 5_000L
        /** Durée préchargée pour la page d'après (moins prioritaire). */
        const val PRELOAD_AFTER_NEXT_MS = 2_500L
    }

    /** Lu depuis le thread de préchargement du manager → volatile. */
    @Volatile
    private var current = -1
    private var playing = false
    private var muted = false

    /**
     * Cible de préchargement par distance à la page courante. Retourner null = ne pas
     * précharger (le manager libère les buffers de la source) → la mémoire reste bornée
     * même si la fenêtre a déjà défilé.
     */
    private val targetPreloadStatusControl =
        TargetPreloadStatusControl<Int, DefaultPreloadManager.PreloadStatus> { page ->
            when (page - current) {
                0 -> DefaultPreloadManager.PreloadStatus.specifiedRangeLoaded(PRELOAD_NEXT_MS)
                1 -> DefaultPreloadManager.PreloadStatus.specifiedRangeLoaded(PRELOAD_NEXT_MS)
                2 -> DefaultPreloadManager.PreloadStatus.specifiedRangeLoaded(PRELOAD_AFTER_NEXT_MS)
                -1 -> DefaultPreloadManager.PreloadStatus.TRACKS_SELECTED
                else -> null
            }
        }

    private val preloadBuilder = DefaultPreloadManager.Builder(context, targetPreloadStatusControl)
        .setMediaSourceFactory(mediaSourceFactory)
        .setBandwidthMeter(bandwidthMeter ?: DefaultBandwidthMeter.getSingletonInstance(context))
        // ABR réactif et gourmand en qualité : démarrage sur une rendition légère (cf.
        // estimation initiale basse dans PlaybackModule) puis montée dès 4 s de débit confirmé
        // (défaut Media3 : 15 s, trop lent pour des clips de 15-30 s). bandwidthFraction 0,8 :
        // on ose la qualité au-dessus dès que 80 % du débit mesuré la couvre — la descente
        // rapide (15 s) et le cache disque limitent le risque de rebuffering.
        .setTrackSelectorFactory(
            TrackSelector.Factory { ctx ->
                DefaultTrackSelector(
                    ctx,
                    AdaptiveTrackSelection.Factory(
                        /* minDurationForQualityIncreaseMs = */ 4_000,
                        /* maxDurationForQualityDecreaseMs = */ 15_000,
                        /* minDurationToRetainAfterDiscardMs = */ 25_000,
                        /* bandwidthFraction = */ 0.8f
                    )
                )
            }
        )
        .setRenderersFactory(
            DefaultRenderersFactory(context)
                // MediaCodec en file asynchrone même avant API 31 → moins de jank décodeur.
                .forceEnableMediaCodecAsynchronousQueueing()
                // Si le décodeur matériel préféré échoue, on retombe sur un autre au lieu d'une erreur.
                .setEnableDecoderFallback(true)
        )
        // Démarrage rapide (lecture dès ~0,4 s bufferisé) + buffer plafonné : les lecteurs ne
        // saturent pas la mémoire, le préchargement long est du ressort du manager.
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs = */ 10_000,
                    /* maxBufferMs = */ 25_000,
                    /* bufferForPlaybackMs = */ 400,
                    /* bufferForPlaybackAfterRebufferMs = */ 1_000
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
        )

    private val preloadManager: DefaultPreloadManager = preloadBuilder.build()

    private val players: List<ExoPlayer> = List(size) {
        preloadBuilder
            .buildExoPlayer(
                ExoPlayer.Builder(context)
                    // Scheduling adaptatif du travail de rendu (moins de réveils inutiles).
                    .experimentalSetDynamicSchedulingEnabled(true)
            )
            .apply { repeatMode = Player.REPEAT_MODE_ALL }
    }

    /** MediaItem géré par le preload manager pour chaque page de la fenêtre. */
    private val added = HashMap<Int, MediaItem>()

    /** URL actuellement chargée dans chaque slot (évite un re-`prepare` inutile). */
    private val boundUrl = arrayOfNulls<String>(size)

    private fun slot(page: Int): Int = ((page % size) + size) % size

    /**
     * Les master playlists synthétisées arrivent en `data:` URI (cf. [FeedVideoDto.playbackUrl]) :
     * pas d'extension → le type HLS doit être déclaré explicitement.
     */
    private fun mediaItemFor(url: String): MediaItem =
        if (url.startsWith("data:")) {
            MediaItem.Builder().setUri(url).setMimeType(MimeTypes.APPLICATION_M3U8).build()
        } else {
            MediaItem.fromUri(url)
        }

    /** Lecteur associé à une page — à attacher à sa PlayerView. */
    fun playerForPage(page: Int): ExoPlayer = players[slot(page)]

    /** Position/durée de la page active (pour la barre de progression). */
    fun currentPlayer(): ExoPlayer? = if (current >= 0) players[slot(current)] else null

    /**
     * Met à jour la fenêtre { courante-1 … courante+2 } : la courante est liée et jouée en
     * priorité, les voisines ±1 sont liées (surface prête), le reste est préchargé/libéré par
     * le manager selon [targetPreloadStatusControl]. À rappeler à chaque changement de page.
     */
    fun setWindow(currentPage: Int, urls: List<String?>, shouldPlay: Boolean) {
        current = currentPage
        playing = shouldPlay

        // 1) Fenêtre de préchargement gérée par le manager : purge hors fenêtre + ajouts.
        val window = (currentPage - 1)..(currentPage + 2)
        val it = added.entries.iterator()
        while (it.hasNext()) {
            val (page, item) = it.next()
            if (page !in window) {
                preloadManager.remove(item)
                it.remove()
            }
        }
        for (page in window) {
            val url = urls.getOrNull(page) ?: continue
            val known = added[page]
            if (known == null || known.localConfiguration?.uri?.toString() != url) {
                known?.let { old -> preloadManager.remove(old) }
                val item = mediaItemFor(url)
                preloadManager.add(item, page)
                added[page] = item
            }
        }
        preloadManager.setCurrentPlayingIndex(currentPage)

        // 2) Lecteurs : courante d'abord (elle prend la bande passante), puis voisines ±1
        //    (surface attachée par FeedItem → première frame déjà décodée au swipe).
        bind(currentPage, urls)
        applyPlayback()
        bind(currentPage + 1, urls)
        bind(currentPage - 1, urls)

        // 3) Lance/re-priorise le préchargement (page+1 : 3 s, page+2 : 1,5 s, page-1 : pistes).
        //    Le manager attend que les lecteurs aient fini de charger avant de précharger.
        preloadManager.invalidate()
    }

    private fun bind(page: Int, urls: List<String?>) {
        if (page < 0 || page >= urls.size) return
        val url = urls[page] ?: return
        val s = slot(page)
        if (boundUrl[s] == url) return
        val item = added[page] ?: mediaItemFor(url)
        // Récupère la source déjà préchargée (buffers réseau/disque conservés) ; à défaut,
        // le lecteur repart de l'URL — le cache disque amortit de toute façon.
        val preloaded = preloadManager.getMediaSource(item)
        if (preloaded != null) players[s].setMediaSource(preloaded) else players[s].setMediaItem(item)
        players[s].playWhenReady = false
        players[s].prepare()
        boundUrl[s] = url
    }

    fun setPlaying(value: Boolean) { playing = value; applyPlayback() }
    fun setMuted(value: Boolean) { muted = value; applyPlayback() }

    private fun applyPlayback() {
        val activeSlot = if (current >= 0) slot(current) else -1
        players.forEachIndexed { i, p ->
            val isActive = i == activeSlot
            p.volume = if (muted || !isActive) 0f else 1f
            p.playWhenReady = isActive && playing
            if (isActive && playing) p.play() else p.pause()
        }
    }

    /** Arrière-plan : tout en pause (coupe image + son). */
    fun pauseAll() { players.forEach { it.playWhenReady = false; it.pause() } }

    /** Retour au premier plan : réactive la page courante selon l'état interne. */
    fun resume() { applyPlayback() }

    fun release() {
        players.forEach { it.release() }
        preloadManager.release()
        added.clear()
    }
}
