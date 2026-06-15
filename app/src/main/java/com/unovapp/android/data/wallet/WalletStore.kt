package com.unovapp.android.data.wallet

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Source unique de vérité du solde en **jetons** (monnaie interne).
 * Partagée par tout l'app (feed/cadeaux, wallet/recharge, profil) → un seul solde cohérent.
 *
 * Amorcée une fois depuis `/users/me` (wallet_balance). Mise à jour localement à la dépense
 * (envoi de cadeau) et à la recharge. À synchroniser avec un futur `GET/POST /wallet` backend.
 */
@Singleton
class WalletStore @Inject constructor() {

    private val _balance = MutableStateFlow(0L)
    val balance: StateFlow<Long> = _balance.asStateFlow()

    private var seeded = false

    /** Initialise le solde depuis le backend (une seule fois). */
    fun seed(value: Long) {
        if (!seeded) {
            _balance.value = value.coerceAtLeast(0L)
            seeded = true
        }
    }

    fun canAfford(price: Long): Boolean = _balance.value >= price

    /** Débite [price] jetons si le solde suffit. Retourne true si la dépense a eu lieu. */
    fun trySpend(price: Long): Boolean {
        if (_balance.value < price) return false
        _balance.value -= price
        return true
    }

    /** Crédite des jetons (après une recharge). */
    fun add(jetons: Long) {
        _balance.value += jetons.coerceAtLeast(0L)
    }
}
