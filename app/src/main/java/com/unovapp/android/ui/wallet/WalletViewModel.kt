package com.unovapp.android.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.user.UserRepository
import com.unovapp.android.data.wallet.WalletStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Expose le solde de jetons partagé ([WalletStore]) et les opérations (dépense, recharge).
 * Plusieurs instances (feed, wallet…) partagent le même [WalletStore] singleton → solde cohérent.
 */
@HiltViewModel
class WalletViewModel @Inject constructor(
    private val store: WalletStore,
    private val userRepository: UserRepository
) : ViewModel() {

    val balance: StateFlow<Long> = store.balance

    init { seedFromBackend() }

    private fun seedFromBackend() {
        viewModelScope.launch {
            when (val r = userRepository.fetchMe()) {
                is NetworkResult.Success -> store.seed(r.data.walletBalance.toLong())
                is NetworkResult.Failure -> Unit // on garde 0 ; l'utilisateur pourra recharger
            }
        }
    }

    /** Tente de dépenser [price] jetons (envoi de cadeau). */
    fun trySpend(price: Long): Boolean = store.trySpend(price)

    /** Crédite des jetons après une recharge réussie. */
    fun credit(jetons: Long) = store.add(jetons)
}
