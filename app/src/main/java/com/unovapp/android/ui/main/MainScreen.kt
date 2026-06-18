package com.unovapp.android.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.activity.compose.BackHandler
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.unovapp.android.ui.profile.ConnectionsScreen
import com.unovapp.android.ui.profile.ProfileScreen
import com.unovapp.android.ui.profile.UserProfileScreen
import com.unovapp.android.ui.search.SearchScreen
import com.unovapp.android.ui.theme.UnovAppTheme
import com.unovapp.android.ui.theme.UnovMotion
import com.unovapp.android.ui.wallet.WalletScreen

/** Overlays plein écran qui passent au-dessus du Scaffold + BottomNav. */
private enum class Overlay { Battle, Wallet }

/**
 * Destinations « people » empilables (recherche → profil → abonnés → autre profil…).
 * Une pile dédiée + BackHandler donnent une vraie navigation arrière, indépendante des onglets.
 */
private sealed interface People {
    data class Profile(val userId: String) : People
    data class Connections(
        val userId: String,
        val isSelf: Boolean,
        val username: String,
        val showFollowers: Boolean
    ) : People
}

/**
 * Host post-connexion : Scaffold avec BottomNav, swap entre Feed / Search / Inbox / Profile
 * selon l'onglet courant.
 *
 *  - Transition entre onglets : **shared-axis horizontal**, direction selon l'index
 *    (gauche-droite ou droite-gauche). Donne une sensation de continuité spatiale.
 *  - Overlays (Battle, Wallet) : transitions distinctes — fade pour Battle (immersif),
 *    slide vertical pour Wallet (façon modal qui descend).
 *
 * `rememberSaveable` sur l'onglet pour qu'il survive à la rotation et au process death.
 */
@Composable
fun MainScreen(
    onOpenCreate: () -> Unit = {},
    onLoggedOut: () -> Unit = {}
) {
    UnovAppTheme {
        var tab by rememberSaveable { mutableStateOf(MainTab.Feed) }
        var overlay by remember { mutableStateOf<Overlay?>(null) }
        // Pile de navigation « people » (profils visités / listes abonnés-abonnements).
        val people = remember { mutableStateListOf<People>() }
        fun popPeople() { if (people.isNotEmpty()) people.removeAt(people.lastIndex) }
        BackHandler(enabled = people.isNotEmpty()) { popPeople() }

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
                // AnimatedContent avec direction calculée — sens du slide depend de l'index
                // (Feed→Profile = slide gauche, Profile→Feed = slide droite). Donne le repère
                // spatial qu'on attend des bottom-nav modernes (Instagram, X).
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        val dir = if (forward) 1 else -1
                        (slideInHorizontally(UnovMotion.decelerate()) { dir * it / 6 } +
                            fadeIn(UnovMotion.standard()))
                            .togetherWith(
                                slideOutHorizontally(UnovMotion.accelerate()) { -dir * it / 6 } +
                                    fadeOut(UnovMotion.fast())
                            )
                    },
                    label = "tabContent"
                ) { current ->
                    when (current) {
                        MainTab.Feed -> FeedScreen(
                            contentPadding = padding,
                            onOpenWallet = { overlay = Overlay.Wallet }
                        )
                        MainTab.Search -> SearchScreen(
                            contentPadding = padding,
                            onOpenUser = { people.add(People.Profile(it.id)) }
                        )
                        MainTab.Inbox -> InboxScreen(contentPadding = padding)
                        MainTab.Profile -> ProfileScreen(
                            contentPadding = padding,
                            onOpenBattle = { overlay = Overlay.Battle },
                            onOpenWallet = { overlay = Overlay.Wallet },
                            onLoggedOut = onLoggedOut,
                            onOpenConnections = { userId, username, showFollowers ->
                                people.add(
                                    People.Connections(
                                        userId = userId,
                                        isSelf = true,
                                        username = username,
                                        showFollowers = showFollowers
                                    )
                                )
                            }
                        )
                    }
                }
            }

            // Battle overlay — fade in/out (immersif, pas de slide)
            AnimatedVisibility(
                visible = overlay == Overlay.Battle,
                enter = fadeIn(UnovMotion.standard()),
                exit = fadeOut(UnovMotion.fast())
            ) {
                BattleScreen(onClose = { overlay = null })
            }

            // Wallet overlay — slide vertical (façon modal qui descend)
            AnimatedVisibility(
                visible = overlay == Overlay.Wallet,
                enter = slideInVertically(UnovMotion.decelerate(UnovMotion.DurationSlow)) { it } +
                    fadeIn(UnovMotion.standard()),
                exit = slideOutVertically(UnovMotion.accelerate()) { it } +
                    fadeOut(UnovMotion.fast())
            ) {
                WalletScreen(onClose = { overlay = null })
            }

            // Navigation « people » : profils visités + listes abonnés/abonnements.
            // Rendue au-dessus de tout (y compris la BottomNav), avec slide horizontal type pile.
            AnimatedContent(
                targetState = people.lastOrNull(),
                transitionSpec = {
                    (slideInHorizontally(UnovMotion.decelerate()) { it } + fadeIn(UnovMotion.standard()))
                        .togetherWith(slideOutHorizontally(UnovMotion.accelerate()) { it } + fadeOut(UnovMotion.fast()))
                },
                label = "peopleNav"
            ) { dest ->
                when (dest) {
                    null -> Box(modifier = Modifier) {}
                    is People.Profile -> UserProfileScreen(
                        userId = dest.userId,
                        onBack = { popPeople() },
                        onOpenConnections = { uid, uname, showFollowers ->
                            people.add(
                                People.Connections(
                                    userId = uid,
                                    isSelf = false,
                                    username = uname,
                                    showFollowers = showFollowers
                                )
                            )
                        }
                    )
                    is People.Connections -> ConnectionsScreen(
                        userId = dest.userId,
                        username = dest.username,
                        isSelf = dest.isSelf,
                        showFollowers = dest.showFollowers,
                        onBack = { popPeople() },
                        onOpenUser = { id -> people.add(People.Profile(id)) }
                    )
                }
            }
        }
    }
}
