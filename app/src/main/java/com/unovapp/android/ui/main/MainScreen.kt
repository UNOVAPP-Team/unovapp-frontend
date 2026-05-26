package com.unovapp.android.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.unovapp.android.ui.battle.BattleScreen
import com.unovapp.android.ui.components.BottomNav
import com.unovapp.android.ui.components.MainTab
import com.unovapp.android.ui.feed.FeedScreen
import com.unovapp.android.ui.inbox.InboxScreen
import com.unovapp.android.ui.profile.ProfileScreen
import com.unovapp.android.ui.search.SearchScreen
import com.unovapp.android.ui.theme.UnovAppTheme
import com.unovapp.android.ui.wallet.WalletScreen

/** Overlays plein écran qui passent au-dessus du Scaffold + BottomNav. */
private enum class Overlay { Battle, Wallet }

/**
 * Host post-connexion : Scaffold avec BottomNav, swap entre Feed / Search / Inbox / Profile
 * selon l'onglet courant. Battle et Wallet sont des overlays full-screen au-dessus
 * (le BottomNav reste en mémoire mais visuellement masqué).
 *
 * `rememberSaveable` sur l'onglet pour qu'il survive à la rotation et au process death.
 */
@Composable
fun MainScreen(
    onOpenCreate: () -> Unit = {}
) {
    UnovAppTheme {
        var tab by rememberSaveable { mutableStateOf(MainTab.Feed) }
        var overlay by remember { mutableStateOf<Overlay?>(null) }

        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050505))) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color(0xFF050505),
                contentColor = Color.White,
                bottomBar = {
                    BottomNav(
                        active = tab,
                        onTabChange = { tab = it },
                        onCreate = onOpenCreate
                    )
                }
            ) { padding ->
                when (tab) {
                    MainTab.Feed -> FeedScreen(
                        contentPadding = padding,
                        onOpenWallet = { overlay = Overlay.Wallet }
                    )
                    MainTab.Search -> SearchScreen(contentPadding = padding)
                    MainTab.Inbox -> InboxScreen(contentPadding = padding)
                    MainTab.Profile -> ProfileScreen(
                        contentPadding = padding,
                        onOpenBattle = { overlay = Overlay.Battle },
                        onOpenWallet = { overlay = Overlay.Wallet }
                    )
                }
            }

            // Battle overlay — fade in/out (immersif, pas de slide)
            AnimatedVisibility(
                visible = overlay == Overlay.Battle,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                BattleScreen(onClose = { overlay = null })
            }

            // Wallet overlay — slide vertical (façon modal)
            AnimatedVisibility(
                visible = overlay == Overlay.Wallet,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                WalletScreen(onClose = { overlay = null })
            }
        }
    }
}
