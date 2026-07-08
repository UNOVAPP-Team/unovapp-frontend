package com.unovapp.android.ui.feed

import android.content.Context
import android.content.Intent

/**
 * Ouvre la feuille de partage Android (ACTION_SEND) pour une vidéo du feed.
 *
 * Tant que le pipeline de deep links n'existe pas, on partage le titre + le lien du flux.
 * Quand le backend exposera des URLs canoniques (ex. https://unovapp.com/v/{id}), il suffira
 * de remplacer [FeedVideoUi.hlsUrl] par cette URL de partage.
 */
fun shareVideo(context: Context, video: FeedVideoUi) {
    val shareText = buildString {
        append("Regarde cette vidéo de @${video.creatorUsername} sur UNOVAPP 🎬\n\n")
        append(video.description)
        append("\n\n")
        append(video.shareableUrl)
    }

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Vidéo UNOVAPP de @${video.creatorUsername}")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }

    context.startActivity(
        Intent.createChooser(sendIntent, "Partager via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}
