package com.unovapp.android.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unovapp.android.TokenDataStore
import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.notification.NotificationRepository
import com.unovapp.android.data.notification.UnreadNotificationsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Alimente le **badge de la bottom bar** : expose le compteur non-lu partagé et le rafraîchit
 * depuis le backend à l'ouverture et à chaque retour au premier plan.
 *
 * Le compteur lui-même vit dans [UnreadNotificationsStore] (singleton) — cet écran et l'Inbox
 * partagent donc le même état : lire une notification dans l'Inbox met le badge à jour en direct.
 */
@HiltViewModel
class NotificationBadgeViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val tokenStore: TokenDataStore,
    private val store: UnreadNotificationsStore
) : ViewModel() {

    val unread: StateFlow<Int> = store.count

    init { refresh() }

    /** Relit le `unread_count` autoritatif du backend (silencieux, sans spinner). */
    fun refresh() {
        viewModelScope.launch {
            // Pas connecté → rien à compter (endpoint à Bearer requis).
            if (tokenStore.readAccessToken() == null) return@launch
            when (val r = repository.list()) {
                is NetworkResult.Success -> store.set(r.data.unreadCount)
                is NetworkResult.Failure -> Unit   // on garde la dernière valeur connue
            }
        }
    }
}
