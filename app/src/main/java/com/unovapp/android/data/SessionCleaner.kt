package com.unovapp.android.data

import android.content.Context
import androidx.work.WorkManager
import com.unovapp.android.TokenDataStore
import com.unovapp.android.data.notification.UnreadNotificationsStore
import com.unovapp.android.data.user.FollowStore
import com.unovapp.android.data.user.SelfStatsStore
import com.unovapp.android.data.user.UserProfileStore
import com.unovapp.android.data.video.FeedDiskCache
import com.unovapp.android.data.video.FeedPrefetchWorker
import com.unovapp.android.data.video.PrefetchStore
import com.unovapp.android.notif.ActivityPollWorker
import com.unovapp.android.ui.feed.ReactionMemory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Efface **toute trace locale** de l'utilisateur à la déconnexion — l'appareil doit revenir à
 * l'état « personne ne s'est jamais connecté » : identifiants, graphe de suivi, feed en cache,
 * réactions, badge, préchargements, profils, plus les tâches de fond qui n'ont plus lieu d'être.
 *
 * Pendant : la persistance de session (tokens chiffrés dans [TokenDataStore]) fait qu'après une
 * première connexion, l'utilisateur entre directement dans le feed à chaque lancement, sans
 * jamais avoir à se reconnecter (cf. MainActivity : startRoute = Main si un token existe).
 * Ce nettoyage est la contrepartie exacte : à la déconnexion, tout disparaît.
 */
@Singleton
class SessionCleaner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenStore: TokenDataStore,
    private val followStore: FollowStore,
    private val prefetchStore: PrefetchStore,
    private val unreadStore: UnreadNotificationsStore,
    private val feedDiskCache: FeedDiskCache,
    private val profileStore: UserProfileStore,
    private val selfStatsStore: SelfStatsStore
) {
    suspend fun wipe() {
        // 1) Identité / session (EncryptedSharedPreferences : access, refresh, userId, username).
        tokenStore.clear()

        // 2) Données de contenu attachées au compte.
        followStore.clearAllPersisted()   // graphe de suivi (mémoire + disque, tous comptes)
        feedDiskCache.clear()             // dernier feed mis en cache
        ReactionMemory.clearAll()         // réactions par vidéo (mémoire + disque)
        prefetchStore.clear()             // IDs de vidéos préchargées
        unreadStore.clear()               // badge notifications
        profileStore.clear()              // profils résolus en mémoire
        selfStatsStore.reset()            // deltas de stats locaux

        // 3) Préférences résiduelles liées à la session.
        context.getSharedPreferences("notif_poll", Context.MODE_PRIVATE).edit().clear().apply()

        // 4) Tâches de fond qui n'ont plus de raison de tourner sans compte connecté.
        runCatching {
            val wm = WorkManager.getInstance(context)
            wm.cancelUniqueWork(ActivityPollWorker.WORK_NAME)
            wm.cancelUniqueWork(FeedPrefetchWorker.WORK_NAME)
        }
    }
}
