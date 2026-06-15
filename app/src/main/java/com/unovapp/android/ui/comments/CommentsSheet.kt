package com.unovapp.android.ui.comments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unovapp.android.ui.components.Avatar
import com.unovapp.android.ui.components.StaggerReveal
import com.unovapp.android.ui.components.unovTap
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovMotion

data class CommentUi(
    val id: String,
    val avatarIdx: Int,
    val username: String,
    val verified: Boolean,
    val pinned: Boolean,
    val text: String,
    val time: String,
    val likesFmt: String
)

private val MockComments = listOf(
    CommentUi("c1", 0, "aminata.cot", verified = true, pinned = true,
        "Trop fort 😂 vous m'avez tué", "2 h", "412"),
    CommentUi("c2", 1, "kossi.dance", verified = false, pinned = false,
        "C'est mon vendredi ça 🔥", "1 h", "89"),
    CommentUi("c3", 4, "samuel228", verified = false, pinned = false,
        "🇧🇯🇧🇯 fier de toi !", "3 h", "67"),
    CommentUi("c4", 2, "reezy_naija", verified = true, pinned = false,
        "Lagos vibes 🔥 keep it up bro", "5 h", "234"),
    CommentUi("c5", 3, "ange_ptn", verified = false, pinned = false,
        "Ma maman c'est exactement ça mdr", "1 h", "156"),
    CommentUi("c6", 5, "mariama_dak", verified = false, pinned = false,
        "Recette stp 🙏🙏", "30 min", "42")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(
    commentCountFmt: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf("") }
    val likes = remember { mutableStateMapOf<String, Boolean>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0E0E10),
        contentColor = UnovColors.Text,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(UnovColors.LineStrong)
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$commentCountFmt commentaires",
                color = UnovColors.Text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .unovTap(onClick = onDismiss, pressedScale = 0.9f)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Fermer",
                    tint = UnovColors.TextDim,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Divider()

        // Commentaires en stagger entry — chaque ligne arrive avec slide+fade séquentiel
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            itemsIndexed(MockComments, key = { _, c -> c.id }) { index, c ->
                StaggerReveal(index = index) {
                    CommentRow(
                        c = c,
                        liked = likes[c.id] ?: false,
                        onToggleLike = { likes[c.id] = !(likes[c.id] ?: false) }
                    )
                }
            }
        }

        Divider()

        // Rail d'emoji rapides — chaque emoji a son tap haptic + scale
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            listOf("❤️", "😂", "🔥", "👏", "😭", "🇧🇯").forEach { emoji ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .unovTap(onClick = { text += emoji }, pressedScale = 0.78f)
                        .padding(4.dp)
                ) {
                    Text(text = emoji, fontSize = 22.sp)
                }
            }
        }

        // Input bar : bouton Send qui s'éveille (couleur + scale + rotation subtle) dès qu'il y a du texte
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Avatar(idx = 5, name = "moi", size = 32.dp)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(999.dp))
                    .background(UnovColors.SurfaceAlt)
                    .padding(start = 14.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    cursorBrush = SolidColor(UnovColors.Accent),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 14.sp
                    ),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (text.isEmpty()) {
                                Text(
                                    text = "Ajouter un commentaire…",
                                    color = UnovColors.TextMute,
                                    fontSize = 14.sp
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                SendButton(active = text.isNotBlank(), onClick = { text = "" })
            }
        }
    }
}

/**
 * Bouton "envoyer" qui prend vie dès qu'il y a du texte :
 *  - couleur passe de gris à or (transition standard)
 *  - scale up à 1.15× avec spring bouncy (effet "ressort armé")
 *  - rotation -15° subtle pour suggérer le mouvement à venir
 */
@Composable
private fun SendButton(active: Boolean, onClick: () -> Unit) {
    val tint by animateColorAsState(
        targetValue = if (active) UnovColors.Accent else UnovColors.TextMute,
        animationSpec = UnovMotion.standard(),
        label = "sendTint"
    )
    val scale by animateFloatAsState(
        targetValue = if (active) 1.15f else 1f,
        animationSpec = UnovMotion.bouncy(),
        label = "sendScale"
    )
    val rotation by animateFloatAsState(
        targetValue = if (active) -15f else 0f,
        animationSpec = UnovMotion.smooth(),
        label = "sendRot"
    )
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(999.dp))
            .unovTap(onClick = onClick, enabled = active, pressedScale = 0.85f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Envoyer",
            tint = tint,
            modifier = Modifier
                .size(22.dp)
                .scale(scale)
                .rotate(rotation)
        )
    }
}

@Composable
private fun CommentRow(
    c: CommentUi,
    liked: Boolean,
    onToggleLike: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Avatar(idx = c.avatarIdx, name = c.username, size = 40.dp)
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "@${c.username}",
                    color = UnovColors.TextDim,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (c.verified) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Vérifié",
                        tint = UnovColors.Accent,
                        modifier = Modifier.size(12.dp)
                    )
                }
                if (c.pinned) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(UnovColors.SurfaceAlt)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Épinglé",
                            color = UnovColors.TextDim,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(
                text = c.text,
                color = UnovColors.Text,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
            Row(
                modifier = Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = c.time,
                    color = UnovColors.TextMute,
                    fontSize = 12.sp
                )
                Text(
                    text = "Répondre",
                    color = UnovColors.TextMute,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .unovTap(onClick = {}, pressedScale = 0.92f)
                )
            }
        }
        // Like avec crossfade outline ↔ filled + scale pop
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.unovTap(onClick = onToggleLike, pressedScale = 0.85f)
        ) {
            Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = !liked,
                    enter = scaleIn(initialScale = 0.6f, animationSpec = UnovMotion.bouncy()) + fadeIn(UnovMotion.fast()),
                    exit = scaleOut(targetScale = 0.6f, animationSpec = UnovMotion.snappy()) + fadeOut(UnovMotion.fast())
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = UnovColors.TextMute,
                        modifier = Modifier.size(18.dp)
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = liked,
                    enter = scaleIn(initialScale = 0.3f, animationSpec = UnovMotion.wobbly()) + fadeIn(UnovMotion.fast()),
                    exit = scaleOut(targetScale = 0.5f, animationSpec = UnovMotion.snappy()) + fadeOut(UnovMotion.fast())
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Aimé",
                        tint = UnovColors.Accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = c.likesFmt,
                color = if (liked) UnovColors.Accent else UnovColors.TextMute,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(UnovColors.Line)
    )
}
