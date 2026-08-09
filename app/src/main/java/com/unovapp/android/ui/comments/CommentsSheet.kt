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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.unovapp.android.ui.components.ShimmerBox
import androidx.compose.foundation.border
import com.unovapp.android.ui.theme.Ember
import com.unovapp.android.ui.theme.EmberFont
import com.unovapp.android.ui.theme.riseIn
import com.unovapp.android.ui.theme.staggerDelay
import com.unovapp.android.ui.components.unovTap
import com.unovapp.android.ui.components.GoldShimmerBox
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovMotion

/** L'étincelle du composeur — gerbe à centre creux, comme dans le feed. */
@Composable
private fun EtincelleGlyph(modifier: Modifier = Modifier, color: Color = Ember.BraiseClair) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val c = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val rOut = size.minDimension / 2f
        val rIn = rOut * 0.26f
        for (i in 0 until 8) {
            val a = Math.toRadians((i * 45).toDouble())
            val dx = kotlin.math.cos(a).toFloat(); val dy = kotlin.math.sin(a).toFloat()
            drawLine(
                color,
                androidx.compose.ui.geometry.Offset(c.x + dx * rIn, c.y + dy * rIn),
                androidx.compose.ui.geometry.Offset(c.x + dx * rOut, c.y + dy * rOut),
                1.7.dp.toPx(),
                androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

data class CommentUi(
    val id: String,
    val avatarIdx: Int,
    val username: String,
    val verified: Boolean,
    val pinned: Boolean,
    val text: String,
    val time: String,
    val likesFmt: String,
    val avatarUrl: String? = null,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val repliesCount: Int = 0,
    val isAuthor: Boolean = false,
    val mentions: List<String> = emptyList(),
    /** Instant de la vidéo où l'Étincelle est plantée (ms). Null = non ancrée. */
    val anchorMs: Long? = null
)

/**
 * Rangée squelette (shimmer) affichée pendant le chargement des commentaires.
 * Remplace les anciens commentaires mockés qui apparaissaient brièvement avant les vraies
 * données — trompeur pour l'utilisateur ; le squelette annonce un chargement, sans mentir.
 */
@Composable
private fun CommentSkeletonRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GoldShimmerBox(modifier = Modifier.size(36.dp), shape = CircleShape)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GoldShimmerBox(modifier = Modifier.height(12.dp).fillMaxWidth(0.35f))
            GoldShimmerBox(modifier = Modifier.height(12.dp).fillMaxWidth(0.8f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(
    videoId: String,
    commentCountFmt: String,
    onDismiss: () -> Unit,
    isVideoOwner: Boolean = false,
    /**
     * Position de lecture au moment où la feuille s'ouvre, en ms. C'est l'instant
     * où l'Étincelle se plantera : « Dis-le à 0:06 ». Null hors du Flux (profil,
     * notifications…) — on ne plante alors rien nulle part.
     */
    anchorMs: Long? = null,
    vm: CommentsViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf("") }
    val likes = remember { mutableStateMapOf<String, Boolean>() }
    val state by vm.state.collectAsState()

    LaunchedEffect(videoId) { vm.load(videoId) }

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
            // « Étincelles » + le compte en braise (maquette écran 03).
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Étincelles",
                    color = Ember.Text,
                    fontFamily = EmberFont,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = commentCountFmt,
                    color = Ember.Braise,
                    fontFamily = EmberFont,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
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

        val displayComments = state.comments
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Chargement initial : squelettes shimmer (jamais de fausses données).
            if (state.isLoading && displayComments.isEmpty()) {
                items(5) { CommentSkeletonRow() }
            }
            itemsIndexed(displayComments, key = { _, c -> c.id }) { index, c ->
                // §Écran 03 — groupes de moments : riseIn 14 px, stagger 40 ms, plafond 8.
                CommentRow(
                    c = c,
                    liked = c.isLiked,
                    onToggleLike = { vm.toggleCommentLike(c.id) },
                    canPin = isVideoOwner,
                    onTogglePin = { vm.togglePin(c.id) },
                    modifier = Modifier.riseIn(delayMs = staggerDelay(index), key = c.id)
                )
            }
            if (!state.isLoading && displayComments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucun commentaire pour l'instant",
                            color = UnovColors.TextMute,
                            fontSize = 14.sp
                        )
                    }
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
            // Composeur de la maquette : étincelle à gauche, invite « Laisse une
            // étincelle… », bouton d'envoi en pastille de feu.
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(999.dp))
                    .border(1.dp, Ember.Braise.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                    .padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EtincelleGlyph(Modifier.size(18.dp))
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    cursorBrush = SolidColor(Ember.Braise),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Ember.Text,
                        fontFamily = EmberFont,
                        fontSize = 15.sp
                    ),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (text.isEmpty()) {
                                // On annonce l'instant AVANT d'écrire : l'utilisateur doit
                                // savoir où son étincelle va se planter (« Dis-le à 0:06 »).
                                Text(
                                    text = anchorMs?.let { "Dis-le à ${formatAnchor(it)}…" }
                                        ?: "Laisse une étincelle…",
                                    color = Ember.TextMute,
                                    fontFamily = EmberFont,
                                    fontSize = 15.sp
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                SendButton(
                    active = text.isNotBlank() && !state.isSending,
                    onClick = {
                        vm.send(videoId, text, anchorMs)
                        text = ""
                    }
                )
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
    onToggleLike: () -> Unit,
    canPin: Boolean = false,
    onTogglePin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
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
                            .background(UnovColors.Accent.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("📌 Épinglé", color = UnovColors.Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (c.isAuthor) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(UnovColors.SurfaceAlt)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Auteur", color = UnovColors.TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                // L'INSTANT d'abord, en braise : une Étincelle appartient à un moment
                // de la vidéo, et c'est ce qui la distingue d'un commentaire ordinaire.
                // Affiché seulement s'il existe vraiment — jamais d'instant inventé.
                c.anchorMs?.let { ms ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        EtincelleGlyph(modifier = Modifier.size(11.dp))
                        Text(
                            text = formatAnchor(ms),
                            color = Ember.BraiseClair,
                            fontFamily = EmberFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
                if (c.repliesCount > 0) {
                    Text("${c.repliesCount} réponse${if (c.repliesCount > 1) "s" else ""}", color = UnovColors.TextMute, fontSize = 12.sp)
                }
                if (canPin) {
                    Text(
                        if (c.pinned) "Désépingler" else "Épingler",
                        color = UnovColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).unovTap(onClick = onTogglePin, pressedScale = 0.92f)
                    )
                }
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

/** « 0:06 » — l'instant d'une Étincelle, au format de la maquette. */
private fun formatAnchor(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
