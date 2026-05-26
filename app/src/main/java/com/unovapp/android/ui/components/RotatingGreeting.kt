package com.unovapp.android.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.unovapp.android.ui.theme.UnovColors
import kotlinx.coroutines.delay

private data class Greeting(val word: String, val lang: String)

private val GREETINGS = listOf(
    Greeting("Akwaaba", "Twi · Ghana"),
    Greeting("Bawo ni", "Yoruba · Nigeria"),
    Greeting("Karibu", "Swahili"),
    Greeting("Bienvenue", "Français"),
    Greeting("Welcome", "English"),
    Greeting("Sannu", "Hausa")
)

@Composable
fun RotatingGreeting(modifier: Modifier = Modifier) {
    var index by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1800)
            index = (index + 1) % GREETINGS.size
        }
    }

    AnimatedContent(
        targetState = index,
        transitionSpec = {
            (slideInVertically { it / 3 } + fadeIn()) togetherWith
                (slideOutVertically { -it / 3 } + fadeOut())
        },
        label = "greeting",
        modifier = modifier
    ) { i ->
        val g = GREETINGS[i]
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "— ${g.word}",
                color = UnovColors.Accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp
            )
            Text(
                text = g.lang.uppercase(),
                color = UnovColors.TextMute,
                fontSize = 10.sp,
                letterSpacing = 1.8.sp
            )
        }
    }
}
