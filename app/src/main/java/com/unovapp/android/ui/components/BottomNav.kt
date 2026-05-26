package com.unovapp.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients

enum class MainTab(val label: String, val icon: ImageVector) {
    Feed("Feed", Icons.Outlined.Home),
    Search("Recherche", Icons.Outlined.Search),
    Inbox("Inbox", Icons.Outlined.Notifications),
    Profile("Profil", Icons.Outlined.Person)
}

@Composable
fun BottomNav(
    active: MainTab,
    onTabChange: (MainTab) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF050505))
            .border(width = 1.dp, color = UnovColors.Line, shape = RoundedCornerShape(0.dp))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(68.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        NavItem(tab = MainTab.Feed, active = active, onClick = onTabChange)
        NavItem(tab = MainTab.Search, active = active, onClick = onTabChange)

        // Gold "+" — déclenche le flux Create (capture vidéo)
        val createInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(UnovGradients.Gold)
                .clickable(
                    interactionSource = createInteraction,
                    indication = null,
                    onClick = onCreate
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Créer",
                tint = Color(0xFF0D0D0D),
                modifier = Modifier.size(22.dp)
            )
        }

        NavItem(tab = MainTab.Inbox, active = active, onClick = onTabChange)
        NavItem(tab = MainTab.Profile, active = active, onClick = onTabChange)
    }
}

@Composable
private fun NavItem(
    tab: MainTab,
    active: MainTab,
    onClick: (MainTab) -> Unit
) {
    val isActive = tab == active
    val tint = if (isActive) UnovColors.Accent else UnovColors.TextMute
    val noRipple = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .width(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = noRipple,
                indication = null,
                onClick = { onClick(tab) }
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = tab.label,
            color = tint,
            fontSize = 9.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
            letterSpacing = 0.4.sp
        )
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(width = 16.dp, height = 2.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(UnovColors.Accent)
            )
        }
    }
}
