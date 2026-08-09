package com.unovapp.android.ui.feed

import com.unovapp.android.data.video.FeedVideoDto
import kotlin.math.abs

/** Format compact partagé (feed, profil, grilles). */
fun formatFeedCount(n: Int): String = when {
    n >= 1_000_000 -> String.format("%.1f M", n / 1_000_000f).replace('.', ',')
    n >= 1_000     -> String.format("%.1f K", n / 1_000f).replace('.', ',')
    else           -> n.toString()
}

/**
 * Mapper partagé DTO → modèle UI du feed. Utilisé par le feed et par les grilles/lecture
 * du profil (vidéos, aimées, sauvegardées). [username]/[avatarUrl] sont résolus par l'appelant
 * (cache créateurs) ou laissés null → repli sur l'id.
 */
fun FeedVideoDto.toFeedVideoUi(
    videoBaseUrl: String,
    username: String? = null,
    avatarUrl: String? = null
): FeedVideoUi = FeedVideoUi(
    id = id,
    creatorId = creatorId,
    hlsUrl = playbackUrl(videoBaseUrl),
    downloadUrl = bestStreamUrl(videoBaseUrl),
    durationSec = durationSeconds,
    // Le pseudo est résolu dans un second temps (resolveCreators). En attendant on
    // laisse VIDE : l'ancien repli affichait les 8 premiers caractères de l'UUID, et
    // l'utilisateur lisait « @f6bb5768 » — un identifiant technique présenté comme un
    // pseudo. Mieux vaut ne rien montrer une seconde que montrer ça.
    creatorUsername = username.orEmpty(),
    creatorAvatarIdx = abs(creatorId.hashCode()) % 5,
    avatarUrl = avatarUrl,
    thumbnailUrl = thumbnailUrl,
    description = description ?: "",
    likesCount = likesCount,
    likesFmt = formatFeedCount(likesCount),
    commentsCount = commentsCount,
    commentsFmt = formatFeedCount(commentsCount),
    sharesFmt = formatFeedCount(sharesCount),
    sharesCount = sharesCount,
    viewsCount = viewsCount,
    viewsFmt = formatFeedCount(viewsCount),
    isFollowing = isFollowingCreator,
    isLiked = isLiked,
    isSaved = isSaved,
    hashtags = hashtags,
    visibility = visibility,
    giftsFmt = "0",
    createdAt = createdAt,
    // Ancres réelles des Étincelles. Le champ arrive vide tant que le backend ne l'a
    // pas livré → la ligne de temps reste nue, au lieu des points aléatoires d'avant.
    sparkAnchorsMs = sparkAnchorsMs
)
