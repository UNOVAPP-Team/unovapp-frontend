package com.unovapp.android.data.video

import com.google.gson.annotations.SerializedName

data class FeedVideoDto(
    val id: String,
    @SerializedName("creator_id")    val creatorId: String,
    val description: String?         = null,
    @SerializedName("duration_seconds") val durationSeconds: Int = 0,
    val renditions: Map<String, String> = emptyMap(),
    @SerializedName("thumbnail_url") val thumbnailUrl: String?  = null,
    @SerializedName("hls_manifest_url") val hlsManifestUrl: String? = null,
    @SerializedName("likes_count")    val likesCount: Int        = 0,
    @SerializedName("comments_count") val commentsCount: Int     = 0,
    @SerializedName("views_count")    val viewsCount: Int        = 0,
    @SerializedName("is_liked")       val isLiked: Boolean       = false,
    @SerializedName("shares_count")   val sharesCount: Int       = 0,
    @SerializedName("is_saved")       val isSaved: Boolean       = false,
    @SerializedName("is_following_creator") val isFollowingCreator: Boolean = false,
    val hashtags: List<String>        = emptyList(),
    val visibility: String?           = null,
    @SerializedName("allow_comments") val allowComments: Boolean = true,
    val status: String                = "published",
    @SerializedName("created_at")     val createdAt: String      = ""
) {
    /** URL de la meilleure rendition concrète — téléchargement, partage, copie de lien. */
    fun bestStreamUrl(videoBaseUrl: String): String {
        val priorities = listOf("720p", "480p", "240p", "144p")
        for (q in priorities) renditions[q]?.let { return it }
        return hlsManifestUrl ?: "${videoBaseUrl}videos/$id/manifest"
    }

    /**
     * URL de LECTURE pour ExoPlayer. Le backend n'expose pas de master playlist (juste des
     * renditions séparées) et l'app forçait la plus haute → démarrage lent + saccades sur
     * réseau faible. Ici on **synthétise une master playlist multivariant** embarquée en
     * `data:` URI : ExoPlayer fait alors du vrai ABR — il démarre sur la rendition que le
     * débit mesuré permet (basse = démarrage quasi instantané) puis monte en qualité.
     */
    fun playbackUrl(videoBaseUrl: String): String {
        if (renditions.size < 2) return bestStreamUrl(videoBaseUrl)
        val master = buildString {
            append("#EXTM3U\n")
            renditions.entries
                .sortedBy { RENDITION_BANDWIDTH[it.key] ?: Int.MAX_VALUE }
                .forEach { (quality, url) ->
                    val bw = RENDITION_BANDWIDTH[quality] ?: 1_000_000
                    // CODECS (H.264 + AAC, sortie standard du pipeline ffmpeg) → préparation
                    // "chunkless" : ExoPlayer connaît les pistes sans télécharger de segment.
                    append("#EXT-X-STREAM-INF:BANDWIDTH=$bw,NAME=\"$quality\",CODECS=\"avc1.640028,mp4a.40.2\"\n")
                    append(url).append('\n')
                }
        }
        val b64 = android.util.Base64.encodeToString(master.toByteArray(), android.util.Base64.NO_WRAP)
        return "data:application/vnd.apple.mpegurl;base64,$b64"
    }

    private companion object {
        /**
         * Bitrates approximatifs par rendition (octets utiles mesurés sur le pipeline actuel :
         * 144p ≈ 240 kbps, 480p ≈ 900 kbps). Seul l'ORDRE et l'ordre de grandeur comptent
         * pour les décisions ABR — pas besoin des valeurs exactes par vidéo.
         */
        val RENDITION_BANDWIDTH = mapOf(
            "144p" to 250_000,
            "240p" to 500_000,
            "360p" to 800_000,
            "480p" to 1_200_000,
            "720p" to 2_500_000,
            "1080p" to 4_500_000
        )
    }
}

data class FeedResponse(
    val data: List<FeedVideoDto>          = emptyList(),
    @SerializedName("next_cursor") val nextCursor: String? = null,
    @SerializedName("has_more")    val hasMore: Boolean    = false
)

data class UploadSessionDto(
    @SerializedName("upload_id")  val uploadId: String,
    @SerializedName("video_id")   val videoId: String,
    @SerializedName("upload_url") val uploadUrl: String,
    @SerializedName("expires_at") val expiresAt: String
)

/** Réponse du toggle like (Social service). likes_count est une String côté backend. */
data class LikeResponse(
    val liked: Boolean,
    @SerializedName("likes_count") val likesCountStr: String = "0"
) {
    val likesCount: Int get() = likesCountStr.toIntOrNull() ?: 0
}

/* ---------- Sprint 1/2/3 : actions vidéo ---------- */

/** POST /videos/:id/save → { saved } (toggle). */
data class SaveResponse(val saved: Boolean = false)

/** POST /videos/:id/share → { shares_count }. */
data class ShareResponse(@SerializedName("shares_count") val sharesCount: Int = 0)

/** Corps de signalement (vidéo ou commentaire). reason ∈ spam|violence|nudity|harassment|other. */
data class ReportRequest(val reason: String, val description: String? = null)
data class ReportedResponse(val reported: Boolean = true)

/** PATCH /videos/:id/comments/:commentId/pin → { is_pinned } (toggle). */
data class PinResponse(@SerializedName("is_pinned") val isPinned: Boolean = false)

/** PATCH /videos/:id — édition (tous champs optionnels). */
data class UpdateVideoRequest(
    val description: String? = null,
    val visibility: String? = null,           // "public" | "private"
    @SerializedName("allow_comments") val allowComments: Boolean? = null,
    val hashtags: List<String>? = null
)

/** GET /videos/:id/related → { data:[...] } (pas de pagination). */
data class RelatedResponse(val data: List<FeedVideoDto> = emptyList())

/** POST /videos/:id/thumbnail/presign — même forme que le presign avatar. */
data class ThumbnailPresignRequest(val contentType: String)
data class ThumbnailPresignResponse(
    val key: String,
    val uploadUrl: String,
    val method: String = "PUT",
    val contentType: String = "image/jpeg",
    val publicUrl: String = "",
    val expiresIn: Int = 300
)
data class ThumbnailConfirmRequest(val key: String)

/** POST /videos/:id/view — body optionnel de complétion. */
data class ViewRequest(
    @SerializedName("watched_seconds") val watchedSeconds: Int? = null,
    @SerializedName("total_seconds")   val totalSeconds: Int? = null
)
