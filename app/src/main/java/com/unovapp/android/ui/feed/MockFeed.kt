package com.unovapp.android.ui.feed

/**
 * Modèle UI d'un item du feed. Mirror temporaire de la future réponse `GET /api/v1/feed`.
 * Quand le backend sera up (Mois 2 Jour 4), on remplacera la source par un FeedRepository.
 */
data class FeedVideoUi(
    val id: String,
    val hlsUrl: String,
    val creatorUsername: String,
    val creatorAvatarIdx: Int,
    val description: String,
    val likesFmt: String,
    val commentsFmt: String,
    val sharesFmt: String,
    val isFollowing: Boolean,
    val isLiked: Boolean = false
)

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
        isFollowing = true,
        isLiked = false
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
        isLiked = true
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
        isFollowing = false
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
        isFollowing = true
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
        isFollowing = false
    )
)
