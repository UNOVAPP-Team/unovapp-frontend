package com.unovapp.android.ui.feed

/** Un type de cadeau reçu par une vidéo — utilisé dans le GiftBreakdownSheet. */
data class GiftReceived(val emoji: String, val name: String, val count: Int)

/**
 * Modèle UI d'un item du feed. Mirror temporaire de la future réponse `GET /api/v1/feed`.
 * Quand le backend sera up (Mois 2 Jour 4), on remplacera la source par un FeedRepository.
 */
data class FeedVideoUi(
    val id: String,
    /** URL de LECTURE — peut être une master playlist synthétisée en `data:` URI (ABR). */
    val hlsUrl: String,
    /** URL concrète de la meilleure rendition — téléchargement, partage, copie de lien. */
    val downloadUrl: String = "",
    val creatorUsername: String,
    val creatorAvatarIdx: Int,
    val description: String,
    val likesFmt: String,
    val commentsFmt: String,
    val sharesFmt: String,
    val isFollowing: Boolean,
    val isLiked: Boolean = false,
    val giftsFmt: String = "0",
    val giftBreakdown: List<GiftReceived> = emptyList(),
    /** UUID du créateur — utilisé pour la navigation vers son profil. Vide sur les mocks. */
    val creatorId: String = "",
    /** Nombre de likes brut (source du compteur optimiste). `likesFmt` en est l'affichage formaté. */
    val likesCount: Int = 0,
    /** Photo de profil réelle du créateur (résolue via /users/:id). Null → avatar à initiales. */
    val avatarUrl: String? = null,
    /** Nombre de cadeaux reçus (compteur local optimiste — pas d'endpoint cadeau côté backend). */
    val giftsCount: Int = 0,
    /** Miniature de la vidéo — sert de fond flouté plein écran (façon TikTok). */
    val thumbnailUrl: String? = null,
    /** Date de publication ISO-8601 (created_at) — source du temps relatif « il y a … ». */
    val createdAt: String = "",
    /** Vidéo sauvegardée par l'utilisateur (Sprint 1). */
    val isSaved: Boolean = false,
    /** Nombre de partages réel (Sprint 1). */
    val sharesCount: Int = 0,
    /** Hashtags dérivés de la description (Sprint 1/3). */
    val hashtags: List<String> = emptyList(),
    /** Visibilité ("public"/"private") — pour l'écran d'édition de sa propre vidéo. */
    val visibility: String? = null,
    /** Nombre de commentaires brut (source du compteur optimiste après ajout). */
    val commentsCount: Int = 0,
    /** Nombre de vues brut + affichage formaté (compteur façon TikTok). */
    val viewsCount: Int = 0,
    val viewsFmt: String = "0",
    /** Durée annoncée par l'API (s) — repli pour le chip temps du scrub si le player n'a pas
     *  encore résolu la durée exacte. */
    val durationSec: Int = 0
) {
    /** URL à partager/télécharger : la rendition concrète, jamais la data-URI de lecture. */
    val shareableUrl: String get() = downloadUrl.ifBlank { hlsUrl }
}

/**
 * Liste de streams HLS publics + métadonnées fictives — utilisée tant que le `/feed` API
 * stub n'est pas câblé. Suffisant pour valider ExoPlayer, le snap-to-page et l'auto-play.
 *
 * Tous ces streams sont gratuits et publics, prévus pour le test des players HLS :
 *  - Mux : test streams dédiés aux validations player (HLS standard, ABR fonctionnel).
 *  - Apple BipBop : référence du protocole HLS.
 *  - Akamai sample Sintel : long form, utile pour tester la lecture continue.
 *
 * Le visuel sera "16:9 letterboxé" — le format vertical 9:16 viendra avec les vidéos
 * réelles transcodées par notre pipeline.
 */
val MockFeedVideos: List<FeedVideoUi> = listOf(
    FeedVideoUi(
        id = "v1",
        hlsUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
        creatorUsername = "aminata.cot",
        creatorAvatarIdx = 0,
        description = "Quand maman entend le wifi qui revient 😂 #benin #marche",
        likesFmt = "12,4 K",
        commentsFmt = "847",
        sharesFmt = "231",
        isFollowing = false,
        isLiked = false,
        giftsFmt = "2,3 K",
        giftBreakdown = listOf(
            GiftReceived("🚀", "Fusée", 12),
            GiftReceived("👑", "Couronne", 45),
            GiftReceived("💎", "Diamant", 89),
            GiftReceived("🦁", "Lion", 34),
            GiftReceived("🌹", "Rose", 127),
            GiftReceived("🔥", "Flamme", 312),
        )
    ),
    FeedVideoUi(
        id = "v2",
        hlsUrl = "https://test-streams.mux.dev/test_001/stream.m3u8",
        creatorUsername = "kossi.dance",
        creatorAvatarIdx = 1,
        description = "Nouvelle choré Zangbeto 🥁 reproduis & tag-moi #challenge",
        likesFmt = "31,2 K",
        commentsFmt = "2,1 K",
        sharesFmt = "892",
        isFollowing = false,
        isLiked = true,
        giftsFmt = "891",
        giftBreakdown = listOf(
            GiftReceived("🎁", "Surprise", 23),
            GiftReceived("⭐", "Étoile", 67),
            GiftReceived("❤️", "Cœur", 201),
        )
    ),
    FeedVideoUi(
        id = "v3",
        hlsUrl = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
        creatorUsername = "reezy_naija",
        creatorAvatarIdx = 2,
        description = "POV : Vendredi soir à Lagos. Le son monte 🔊",
        likesFmt = "8,9 K",
        commentsFmt = "412",
        sharesFmt = "156",
        isFollowing = false,
        giftsFmt = "234",
        giftBreakdown = listOf(
            GiftReceived("🌹", "Rose", 98),
            GiftReceived("❤️", "Cœur", 136),
        )
    ),
    FeedVideoUi(
        id = "v4",
        hlsUrl = "https://test-streams.mux.dev/pts_shift/master.m3u8",
        creatorUsername = "mariama_dak",
        creatorAvatarIdx = 3,
        description = "Recette du yassa poulet — la vraie 🇸🇳 #cuisine #senegal",
        likesFmt = "5,6 K",
        commentsFmt = "289",
        sharesFmt = "412",
        isFollowing = true,
        giftsFmt = "45",
        giftBreakdown = listOf(
            GiftReceived("🌹", "Rose", 45),
        )
    ),
    FeedVideoUi(
        id = "v5",
        hlsUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_ts/master.m3u8",
        creatorUsername = "le_chef_moise",
        creatorAvatarIdx = 4,
        description = "Tutoriel : monter une vidéo en 30 secondes sur UNOVAPP. C'est cadeau.",
        likesFmt = "47,8 K",
        commentsFmt = "3,4 K",
        sharesFmt = "1,2 K",
        isFollowing = false,
        giftsFmt = "5,1 K",
        giftBreakdown = listOf(
            GiftReceived("🚀", "Fusée", 28),
            GiftReceived("👑", "Couronne", 112),
            GiftReceived("💎", "Diamant", 234),
            GiftReceived("🦁", "Lion", 87),
            GiftReceived("🎁", "Surprise", 198),
            GiftReceived("🔥", "Flamme", 441),
            GiftReceived("🌹", "Rose", 617),
        )
    )
)
