package com.unovapp.android.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.components.unovTap
import com.unovapp.android.ui.theme.UnovAppTheme
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients
import com.unovapp.android.ui.theme.UnovMotion
import kotlinx.coroutines.delay

data class TrendingTag(val tag: String, val viewsFmt: String)
data class SearchVideo(val gradientIndex: Int, val viewsFmt: String)

private val CATEGORIES = listOf(
    "Tendances", "Cotonou", "Musique", "Danse", "Cuisine", "Mode", "Comédie", "Sport"
)

private val MOCK_TRENDING = listOf(
    TrendingTag("vendredisoir", "1,2 M"),
    TrendingTag("ZangbetoChallenge", "890 K"),
    TrendingTag("MamansBenin", "672 K"),
    TrendingTag("LagosNight", "540 K"),
    TrendingTag("YassaPoulet", "412 K"),
    TrendingTag("DjBossou", "290 K")
)

private val MOCK_VIDEOS = listOf(
    SearchVideo(0, "1,2 M"),
    SearchVideo(2, "412 K"),
    SearchVideo(4, "89 K"),
    SearchVideo(1, "234 K"),
    SearchVideo(3, "67 K"),
    SearchVideo(5, "31 K")
)

@Composable
fun SearchScreen(contentPadding: PaddingValues = PaddingValues(0.dp)) {
    UnovAppTheme {
        var activeCategory by remember { mutableStateOf(CATEGORIES.first()) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(contentPadding)
        ) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)) {
                SearchBar()
                Spacer(modifier = Modifier.height(14.dp))
                CategoryPills(
                    active = activeCategory,
                    onSelect = { activeCategory = it }
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp)
            ) {
                item { SectionHeader(icon = Icons.AutoMirrored.Outlined.TrendingUp, label = "Tendances Bénin") }
                item { TrendingGrid(tags = MOCK_TRENDING) }
                item { Spacer(modifier = Modifier.height(22.dp)) }
                item { SectionHeader(icon = null, label = "Vidéos populaires") }
                item { VideoGrid(videos = MOCK_VIDEOS) }
            }
        }
    }
}

/* ---------- SearchBar avec focus border animée ---------- */

@Composable
private fun SearchBar() {
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (focused) UnovColors.Accent.copy(alpha = 0.7f) else UnovColors.Line,
        animationSpec = UnovMotion.standard(),
        label = "searchBorder"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(UnovColors.Surface)
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .unovTap(onClick = { focused = !focused }, pressedScale = 0.98f)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = if (focused) UnovColors.Accent else UnovColors.Text,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "Recherche créateurs, sons, hashtags…",
            color = UnovColors.TextMute,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Outlined.QrCode,
            contentDescription = "QR",
            tint = UnovColors.TextDim,
            modifier = Modifier.size(18.dp)
        )
    }
}

/* ---------- Categories : indicator bar magnetic (scale-in spring) ---------- */

@Composable
private fun CategoryPills(active: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CATEGORIES.forEach { cat ->
            val isActive = cat == active
            val tint by animateColorAsState(
                targetValue = if (isActive) UnovColors.Text else UnovColors.TextMute,
                animationSpec = UnovMotion.standard(),
                label = "catTint"
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .unovTap(onClick = { onSelect(cat) }, pressedScale = 0.94f)
                    .padding(vertical = 4.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = cat,
                        color = tint,
                        fontSize = 14.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
                    )
                    AnimatedVisibility(
                        visible = isActive,
                        enter = scaleIn(initialScale = 0.3f, animationSpec = UnovMotion.bouncy()) +
                            fadeIn(UnovMotion.fast()),
                        exit = scaleOut(targetScale = 0.3f, animationSpec = UnovMotion.snappy()) +
                            fadeOut(UnovMotion.fast())
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(width = 20.dp, height = 2.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(UnovColors.Accent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    label: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = UnovColors.TextMute,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = label.uppercase(),
            color = UnovColors.TextMute,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    }
}

/* ---------- Trending grid : stagger entry ---------- */

@Composable
private fun TrendingGrid(tags: List<TrendingTag>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.chunked(2).forEachIndexed { rowIdx, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { colIdx, tag ->
                    val flatIdx = rowIdx * 2 + colIdx
                    val rank = tags.indexOf(tag) + 1
                    StaggerReveal(index = flatIdx, modifier = Modifier.weight(1f)) {
                        TrendingTile(rank = rank, tag = tag)
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TrendingTile(rank: Int, tag: TrendingTag) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(UnovColors.Surface)
            .border(1.dp, UnovColors.Line, RoundedCornerShape(16.dp))
            .unovTap(onClick = {})
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Text(
                text = "#$rank",
                color = UnovColors.Accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = tag.tag,
                color = UnovColors.Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${tag.viewsFmt} vues",
                color = UnovColors.TextMute,
                fontSize = 11.sp
            )
        }
        Icon(
            imageVector = Icons.Outlined.LocalFireDepartment,
            contentDescription = null,
            tint = UnovColors.Accent,
            modifier = Modifier.size(18.dp)
        )
    }
}

/* ---------- Video grid : stagger entry + scale on tap ---------- */

@Composable
private fun VideoGrid(videos: List<SearchVideo>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        videos.chunked(3).forEachIndexed { rowIdx, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEachIndexed { colIdx, video ->
                    val flatIdx = rowIdx * 3 + colIdx
                    StaggerReveal(
                        index = flatIdx + MOCK_TRENDING.size, // décale après les trending
                        modifier = Modifier.weight(1f)
                    ) {
                        VideoTile(video = video)
                    }
                }
                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun VideoTile(video: SearchVideo) {
    Box(
        modifier = Modifier
            .aspectRatio(9f / 14f)
            .clip(RoundedCornerShape(8.dp))
            .background(UnovGradients.videoBg(video.gradientIndex))
            .unovTap(onClick = {}, pressedScale = 0.94f)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Visibility,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = video.viewsFmt,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Cascade d'entrée : chaque item apparaît avec fade + slide-up + scale, retardé selon son index.
 * Le délai est plafonné à [UnovMotion.StaggerMaxItems] pour éviter une cascade interminable
 * sur les longues listes.
 *
 * Usage : enveloppe le composable enfant avec `index` = position dans la liste.
 */
@Composable
private fun StaggerReveal(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val cappedIndex = index.coerceAtMost(UnovMotion.StaggerMaxItems)

    LaunchedEffect(Unit) {
        delay((cappedIndex * UnovMotion.StaggerDelayMs).toLong())
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = UnovMotion.smooth(),
        label = "staggerScale"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(UnovMotion.standard()) +
            slideInVertically(UnovMotion.decelerate()) { it / 4 },
        modifier = modifier
    ) {
        Box(modifier = Modifier.scale(scale)) {
            content()
        }
    }
}
