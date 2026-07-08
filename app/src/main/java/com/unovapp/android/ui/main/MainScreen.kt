package com.unovapp.android.ui.main

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import com.unovapp.android.ui.create.CreateScreen
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

/** Overlays plein écran qui passent au-dessus de la BottomNav. */
private enum class Overlay { Battle, Wallet }

/**
 * Destinations « people » empilables (recherche → profil → abonnés → autre profil…).
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
 * Host post-connexion. La BottomNav est rendue en overlay via Box.align(BottomCenter)
 * plutôt que dans un Scaffold bottomBar — le Scaffold contraint sa zone de contenu à la
 * hauteur écran moins la barre, ce qui empêche le Feed vidéo d'être vraiment plein écran.
 * Avec cette architecture Box, le feed remplit 100 % de l'écran et la nav flotte par-dessus.
 */
@Composable
fun MainScreen(
    onOpenCreate: () -> Unit = {},
    onLoggedOut: () -> Unit = {}
) {
    UnovAppTheme {
        val context = LocalContext.current
        var tab by rememberSaveable { mutableStateOf(MainTab.Feed) }
        val tabHistory = remember { mutableStateListOf<MainTab>() }
        var overlay by remember { mutableStateOf<Overlay?>(null) }
        var showCreate by remember { mutableStateOf(false) }
        val people = remember { mutableStateListOf<People>() }
        fun popPeople() { if (people.isNotEmpty()) people.removeAt(people.lastIndex) }

        // Navigation avec historique : mémorise l'onglet courant avant d'en changer.
        fun navigateToTab(newTab: MainTab) {
            if (newTab == tab) return
            tabHistory.add(tab)
            tab = newTab
        }
        // Remonte d'un onglet dans l'historique (ou Feed si l'historique est vide).
        fun popTab() {
            tab = if (tabHistory.isNotEmpty()) tabHistory.removeAt(tabHistory.lastIndex)
                  else MainTab.Feed
        }

        // Double-back-to-exit depuis l'écran d'accueil (Feed sans historique).
        var exitConfirmed by remember { mutableStateOf(false) }
        LaunchedEffect(exitConfirmed) {
            if (exitConfirmed) { delay(2_000); exitConfirmed = false }
        }

        // ── Gestion du bouton Retour (hardware + geste système) ──────────────────
        // Les BackHandler sont évalués du dernier registré au premier (priorité croissante).

        // Priorité 1 (basse) — pile d'onglets ou retour au Feed si historique vide.
        BackHandler(
            enabled = (tabHistory.isNotEmpty() || tab != MainTab.Feed) &&
                      people.isEmpty() && overlay == null && !showCreate
        ) { popTab() }
        // Priorité 2 — pile « people » (profil visité, abonnés/abonnements…)
        BackHandler(enabled = people.isNotEmpty()) { popPeople() }
        // Priorité 3 — overlay plein écran (Battle, Wallet) → le fermer d'abord
        BackHandler(enabled = overlay != null) { overlay = null }
        // Priorité 4 — écran de création (toujours fermable avec Retour)
        BackHandler(enabled = showCreate) { showCreate = false }
        // Priorité 5 (haute) — Feed sans historique : confirmer avant de quitter l'app.
        BackHandler(
            enabled = tab == MainTab.Feed && tabHistory.isEmpty() &&
                      people.isEmpty() && overlay == null && !showCreate
        ) {
            if (exitConfirmed) {
                (context as? ComponentActivity)?.finish()
            } else {
                exitConfirmed = true
                Toast.makeText(context, "Appuyez à nouveau pour quitter", Toast.LENGTH_SHORT).show()
            }
        }

        // Padding à passer aux onglets non-Feed (Search, Inbox, Profile) pour que leur
        // contenu scrollable s'arrête au-dessus de la BottomNav.
        // 78.dp = hauteur visuelle de la pilule (6 top + 60 Row + 12 bottom).
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val screenPadding = PaddingValues(bottom = navBarBottom + 78.dp)

        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
        ) {
            // Contenu principal — remplit 100 % de l'écran sans contrainte Scaffold
            AnimatedContent(
                targetState = tab,
                modifier = Modifier.fillMaxSize(),
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
                        onOpenWallet = { overlay = Overlay.Wallet },
                        // Tap avatar/pseudo d'un créateur → son profil + stats (pile people).
                        onOpenProfile = { creatorId -> people.add(People.Profile(creatorId)) },
                        // Met le feed en pause (image + son) dès qu'un overlay le recouvre :
                        // création vidéo, wallet, battle, profil visité…
                        active = !showCreate && overlay == null && people.isEmpty()
                    )
                    MainTab.Search -> SearchScreen(
                        contentPadding = screenPadding,
                        onOpenUser = { people.add(People.Profile(it.id)) }
                    )
                    MainTab.Inbox -> com.unovapp.android.ui.notifications.NotificationsScreen(contentPadding = screenPadding)
                    MainTab.Profile -> ProfileScreen(
                        contentPadding = screenPadding,
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

            // BottomNav flotte au-dessus du contenu — fond transparent (retiré de BottomNav.kt)
            // laisse la vidéo visible derrière sur le Feed.
            BottomNav(
                active = tab,
                onTabChange = { navigateToTab(it) },
                onCreate = { showCreate = true },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

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

            // Studio de création — bloom depuis le bouton + (scale origin bas-centre)
            AnimatedVisibility(
                visible = showCreate,
                enter = scaleIn(
                    animationSpec   = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
                    initialScale    = 0.88f,
                    transformOrigin = TransformOrigin(0.5f, 1.0f)
                ) + fadeIn(tween(300)),
                exit = scaleOut(
                    animationSpec   = tween(240, easing = UnovMotion.Accelerate),
                    targetScale     = 0.92f,
                    transformOrigin = TransformOrigin(0.5f, 1.0f)
                ) + fadeOut(tween(220))
            ) {
                CreateScreen(onClose = { showCreate = false })
            }

            // Navigation « people » : profils visités + listes abonnés/abonnements.
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
