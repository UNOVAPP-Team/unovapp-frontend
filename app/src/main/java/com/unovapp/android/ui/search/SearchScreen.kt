package com.unovapp.android.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.theme.UnovAppTheme
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients

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
            // Search header (search bar + categories)
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)) {
                SearchBar()
                Spacer(modifier = Modifier.height(14.dp))
                CategoryPills(
                    active = activeCategory,
                    onSelect = { activeCategory = it }
                )
            }

            // Scrollable content
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

@Composable
private fun SearchBar() {
    val noRipple = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(UnovColors.Surface)
            .border(1.dp, UnovColors.Line, RoundedCornerShape(999.dp))
            .clickable(interactionSource = noRipple, indication = null) {}
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = UnovColors.Text,
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
            val noRipple = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(
                        interactionSource = noRipple,
                        indication = null,
                        onClick = { onSelect(cat) }
                    )
                    .padding(vertical = 4.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = cat,
                        color = if (isActive) UnovColors.Text else UnovColors.TextMute,
                        fontSize = 14.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
                    )
                    if (isActive) {
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

@Composable
private fun TrendingGrid(tags: List<TrendingTag>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { idx, tag ->
                    val rank = tags.indexOf(tag) + 1
                    TrendingTile(
                        rank = rank,
                        tag = tag,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TrendingTile(
    rank: Int,
    tag: TrendingTag,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(UnovColors.Surface)
            .border(1.dp, UnovColors.Line, RoundedCornerShape(16.dp))
            .clickable {}
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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

@Composable
private fun VideoGrid(videos: List<SearchVideo>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        videos.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { video ->
                    VideoTile(video = video, modifier = Modifier.weight(1f))
                }
                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun VideoTile(video: SearchVideo, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(9f / 14f)
            .clip(RoundedCornerShape(8.dp))
            .background(UnovGradients.videoBg(video.gradientIndex))
            .clickable {}
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
