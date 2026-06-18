package com.unovapp.android.data.user

import com.unovapp.android.data.network.NetworkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Point d'entrée unique pour suivre / ne plus suivre un utilisateur.
 *
 *  - Met à jour le [FollowStore] **immédiatement** (UI optimiste, partagée par tous les écrans).
 *  - Lance l'appel réseau sur un scope applicatif (et non celui d'un ViewModel) : l'action aboutit
 *    même si l'écran déclencheur est détruit entre-temps.
 *  - **Rollback** automatique de l'état partagé si l'API échoue.
 */
@Singleton
class FollowManager @Inject constructor(
    private val userRepository: UserRepository,
    private val store: FollowStore
) {
    // Scope app-long : un follow ne doit pas être annulé parce qu'on quitte l'écran.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun toggle(id: String) = setFollowing(id, !store.isFollowing(id))

    fun setFollowing(id: String, follow: Boolean) {
        if (store.isFollowing(id) == follow) return
        store.setFollowing(id, follow) // optimiste
        scope.launch {
            val r = if (follow) userRepository.follow(id) else userRepository.unfollow(id)
            if (r is NetworkResult.Failure) {
                store.setFollowing(id, !follow) // rollback
            }
        }
    }
}
