package com.unovapp.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.unovapp.android.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.annotation.StringRes
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients
import com.unovapp.android.ui.theme.UnovMotion

enum class MainTab(@StringRes val labelRes: Int, val icon: ImageVector) {
    Feed(R.string.nav_feed, Icons.Outlined.Home),
    Search(R.string.nav_search, Icons.Outlined.Search),
    Inbox(R.string.nav_inbox, Icons.Outlined.Notifications),
    Profile(R.string.nav_profile, Icons.Outlined.Person)
}

/**
 * BottomNav animée — signature visuelle d'UNOVAPP.
 *
 *  - Icône active : pop scale spring + halo or radial flouté derrière (effet "élu").
 *  - Indicator bar : grandit horizontalement sur l'item actif (expandHorizontally).
 *  - Couleur de tint animée pour le passage actif/inactif (pas de cut sec).
 *  - Bouton + : pulse permanent subtil pour orienter l'œil vers la création.
 *  - Tap haptic + scale via `unovTap` pour chaque item.
 */
@Composable
fun BottomNav(
    active: MainTab,
    onTabChange: (MainTab) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(18.dp, RoundedCornerShape(30.dp), clip = false)
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF1D1D1F), Color(0xFF0C0C0C)))
                )
                .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(30.dp))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavPill(tab = MainTab.Feed, active = active, onClick = onTabChange)
            NavPill(tab = MainTab.Search, active = active, onClick = onTabChange)
            CreateButton(onClick = onCreate)
            NavPill(tab = MainTab.Inbox, active = active, onClick = onTabChange)
            NavPill(tab = MainTab.Profile, active = active, onClick = onTabChange)
        }
    }
}

/**
 * Onglet "expressive" : au repos = icône seule ; actif = pilule dorée qui s'étend (morph fluide
 * via animateContentSize) pour révéler le label. Tint + fond animés, pop tactile au press.
 */
@Composable
private fun NavPill(
    tab: MainTab,
    active: MainTab,
    onClick: (MainTab) -> Unit
) {
    val isActive = tab == active
    val label = stringResource(tab.labelRes)

    val tint by animateColorAsState(
        targetValue = if (isActive) UnovColors.Accent else UnovColors.TextMute,
        animationSpec = UnovMotion.standard(),
        label = "navTint"
    )
    val bg by animateColorAsState(
        targetValue = if (isActive) UnovColors.Accent.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = UnovMotion.standard(),
        label = "navBg"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .unovTap(onClick = { onClick(tab) }, pressedScale = 0.9f)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .padding(horizontal = if (isActive) 14.dp else 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        if (isActive) {
            Text(
                text = label,
                color = tint,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp,
                maxLines = 1
            )
        }
    }
}

/**
 * Bouton "+" central — gradient or. Au repos, pulse ambient lent (12 px halo qui respire)
 * pour attirer l'œil vers la création de contenu (le moteur du produit).
 */
@Composable
private fun CreateButton(onClick: () -> Unit) {
    // Pulse ambient sur le halo derrière, pas sur le bouton lui-même
    val transition = rememberInfiniteTransition(label = "createPulse")
    val haloScale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "createHaloScale"
    )
    val haloAlpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "createHaloAlpha"
    )

    Box(contentAlignment = Alignment.Center) {
        // Halo qui respire — donne du "vivant" au point central de la nav
        Box(
            modifier = Modifier
                .size(56.dp)
                .scale(haloScale)
                .blur(18.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            UnovColors.Accent.copy(alpha = haloAlpha),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(UnovGradients.Gold)
                .unovTap(onClick = onClick, pressedScale = 0.90f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.nav_create),
                tint = Color(0xFF0D0D0D),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
