package com.unovapp.android.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.unovapp.android.ui.components.Avatar
import com.unovapp.android.ui.theme.Ember
import com.unovapp.android.ui.theme.EmberDim
import kotlin.math.abs

/* ═══════════════════════════════════════════════════════════════════════════
 *  FEED — nouvelle maquette « La braise, pas le bruit » (écran 12, Flux MVP)
 *  Reproduit la disposition exacte de la maquette sur le pipeline vidéo existant.
 * ═══════════════════════════════════════════════════════════════════════════ */

/** Modes de tri du feed MVP (toggle en haut à droite) — sans algorithme (pas d'IA). */
enum class EmberFeedMode { Suivis, Braise }

/**
 * Une page du Flux, version maquette. Vidéo plein écran ; au repos, seul le bloc créateur,
 * la légende, la ligne de temps (avec étincelles) et la rangée Souffler · Orbe · Étinceler.
 */
@Composable
fun EmberFeedItem(
    video: FeedVideoUi,
    isCurrentPage: Boolean,
    player: ExoPlayer?,
    progress: Float,
    durationMs: Long,
    isFollowing: Boolean,
    isSelf: Boolean,
    reacted: Boolean,
    showPauseIndicator: Boolean,
    onTogglePlay: () -> Unit,
    onSouffler: (videoId: String) -> Unit,
    onEtinceler: () -> Unit,
    onSuivre: (creatorId: String) -> Unit,
    onOpenProfile: (creatorId: String) -> Unit,
    onOrbe: () -> Unit,
    bottomInset: androidx.compose.ui.unit.Dp = 0.dp
) {
    // Double-tap = souffler (réaction) avec pop de flamme sous le doigt.
    var flamePop by remember(video.id) { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ember.Bg)
            .pointerInput(video.id) {
                detectTapGestures(
                    onTap = { onTogglePlay() },
                    onDoubleTap = { onSouffler(video.id); flamePop++ }
                )
            }
    ) {
        // Surface vidéo partagée (pipeline existant).
        FeedVideoSurface(
            player = player,
            thumbnailUrl = video.thumbnailUrl,
            modifier = Modifier.fillMaxSize()
        )

        // État de chargement honnête « La braise couve » (buffering de la page courante).
        if (isCurrentPage && player != null) {
            val state = player.playbackState
            if (state == Player.STATE_BUFFERING || state == Player.STATE_IDLE) {
                BraiseCouve(modifier = Modifier.align(Alignment.Center))
            }
        }

        // Voile bas — lisibilité du texte sans assombrir le contenu (doc §3).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.62f to Color.Transparent,
                        1f to Ember.Bg.copy(alpha = 0.92f)
                    )
                )
        )

        // Indicateur de pause central.
        AnimatedVisibility(
            visible = showPauseIndicator,
            enter = scaleIn(initialScale = 0.7f) + fadeIn(tween(120)),
            exit = fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier.size(74.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, "Lecture", tint = Ember.Text.copy(alpha = 0.92f), modifier = Modifier.size(42.dp))
            }
        }

        // Flamme au double-tap.
        AnimatedVisibility(
            visible = flamePop > 0,
            enter = scaleIn(initialScale = 0f) + fadeIn(tween(100)),
            exit = scaleOut(targetScale = 1.4f, animationSpec = tween(360)) + fadeOut(tween(360)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(Icons.Filled.LocalFireDepartment, null, tint = Ember.Braise, modifier = Modifier.size(128.dp))
        }

        // ── HUD bas : bloc créateur + légende + ligne de temps + actions ──────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = bottomInset)
        ) {
            CreatorBlock(
                video = video,
                isFollowing = isFollowing,
                isSelf = isSelf,
                onSuivre = { onSuivre(video.creatorId) },
                onOpenProfile = { onOpenProfile(video.creatorId) },
                modifier = Modifier.padding(start = 20.dp, end = 20.dp)
            )

            // Légende.
            if (video.description.isNotBlank()) {
                Text(
                    text = video.description,
                    color = Ember.Text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)
                )
            }

            // Ligne de temps + étincelles + durée.
            EmberTimeline(
                progress = progress,
                durationMs = if (durationMs > 0) durationMs else video.durationSec * 1000L,
                sparkSeed = video.id.hashCode(),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)
            )

            // Rangée d'actions fixe : Souffler · Orbe · Étinceler (MVP, doc écran 12).
            EmberActionRow(
                reacted = reacted,
                onSouffler = { onSouffler(video.id); flamePop++ },
                onEtinceler = onEtinceler,
                onOrbe = onOrbe,
                modifier = Modifier.padding(top = 18.dp, bottom = 10.dp)
            )
        }
    }
}

/* ---------- Bloc créateur (anneau de chaleur, @handle, Suivre) ---------- */

@Composable
private fun CreatorBlock(
    video: FeedVideoUi,
    isFollowing: Boolean,
    isSelf: Boolean,
    onSuivre: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeatRingAvatar(
            avatarUrl = video.avatarUrl,
            name = video.creatorUsername,
            avatarIdx = video.creatorAvatarIdx,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpenProfile
            )
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "@${video.creatorUsername}",
                    color = Ember.Text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp,
                    maxLines = 1
                )
                // Badge vérifié en braise (pastille pleine + coche).
                Box(
                    modifier = Modifier.size(18.dp).clip(CircleShape).background(Ember.Braise),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Verified, "Vérifié", tint = Ember.Bg, modifier = Modifier.size(13.dp))
                }
            }
            // « · son original » — layout fidèle à la maquette ; le lieu n'est affiché que
            // s'il est réel (pas de « Cotonou » codé en dur, cf. leçon anti-fausses-données).
            Text(
                text = "son original",
                color = Ember.TextDim,
                fontSize = 13.sp,
                maxLines = 1
            )
        }
        if (!isFollowing && !isSelf) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .border(1.dp, Ember.TextDim, RoundedCornerShape(999.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSuivre
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text("Suivre", color = Ember.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Avatar cerclé d'un anneau de chaleur (arc braise, façon anneau de progression). */
@Composable
private fun HeatRingAvatar(
    avatarUrl: String?,
    name: String,
    avatarIdx: Int,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(56.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx()
            // Anneau de fond discret.
            drawCircle(color = Ember.Line, radius = (size.minDimension - stroke) / 2f, style = Stroke(stroke))
            // Arc de chaleur (~270°) en braise.
            drawArc(
                brush = Ember.HeatRing,
                startAngle = -215f, sweepAngle = 250f, useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round),
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            )
        }
        Box(
            modifier = Modifier.size(46.dp).clip(CircleShape).background(Ember.SurfaceWarm),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(42.dp).clip(CircleShape)
                )
            } else {
                Avatar(idx = avatarIdx, name = name, size = 42.dp)
            }
        }
    }
}

/* ---------- Ligne de temps + étincelles ---------- */

@Composable
private fun EmberTimeline(
    progress: Float,
    durationMs: Long,
    sparkSeed: Int,
    modifier: Modifier = Modifier
) {
    // Positions des étincelles : marqueurs de la maquette. Placeholder visuel de la
    // fonctionnalité Étincelle (commentaires ancrés) — pas encore de données backend.
    val sparks = remember(sparkSeed) {
        val r = kotlin.random.Random(sparkSeed)
        listOf(0.18f + r.nextFloat() * 0.12f, 0.42f + r.nextFloat() * 0.12f, 0.66f + r.nextFloat() * 0.12f)
    }
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f).height(14.dp), contentAlignment = Alignment.CenterStart) {
            Canvas(modifier = Modifier.fillMaxWidth().height(3.dp)) {
                val y = size.height / 2f
                // Piste.
                drawLine(Ember.Line, Offset(0f, y), Offset(size.width, y), 3.dp.toPx(), StrokeCap.Round)
                // Progression en braise.
                drawLine(Ember.Braise, Offset(0f, y), Offset(size.width * progress.coerceIn(0f, 1f), y), 3.dp.toPx(), StrokeCap.Round)
            }
            // Points d'étincelle blancs.
            Canvas(modifier = Modifier.fillMaxWidth().height(14.dp)) {
                val y = size.height / 2f
                sparks.forEach { f ->
                    drawCircle(Ember.Text, radius = 4.dp.toPx(), center = Offset(size.width * f, y))
                }
            }
        }
        Text(
            text = formatClock(durationMs),
            color = Ember.TextDim,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 14.dp)
        )
    }
}

/* ---------- Rangée d'actions : Souffler · Orbe · Étinceler ---------- */

@Composable
private fun EmberActionRow(
    reacted: Boolean,
    onSouffler: () -> Unit,
    onEtinceler: () -> Unit,
    onOrbe: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SideAction(label = "Souffler", onClick = onSouffler) {
            Icon(
                Icons.Filled.LocalFireDepartment, "Souffler",
                tint = if (reacted) Ember.Braise else Ember.Text,
                modifier = Modifier.size(26.dp)
            )
        }
        Orbe(onClick = onOrbe)
        SideAction(label = "Étinceler", onClick = onEtinceler) {
            SparkIcon(modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun SideAction(label: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .size(width = 64.dp, height = 66.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(top = 8.dp)
    ) {
        Box(
            modifier = Modifier.size(EmberDim.Touch).clip(CircleShape).border(1.dp, Ember.Line, CircleShape),
            contentAlignment = Alignment.Center
        ) { icon() }
        Text(label, color = Ember.TextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** L'Orbe — le point d'ancrage unique de la navigation (icône « onde », anneau braise). */
@Composable
fun Orbe(onClick: () -> Unit, hasBadge: Boolean = true, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(EmberDim.Orbe + 6.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(EmberDim.Orbe)
                .clip(CircleShape)
                .background(Ember.Bg.copy(alpha = 0.55f))
                .border(1.5.dp, Ember.Braise.copy(alpha = 0.85f), CircleShape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            WaveIcon(modifier = Modifier.size(26.dp))
        }
        // Badge lagune (activité non lue).
        if (hasBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(Ember.Lagune)
            )
        }
    }
}

/* ---------- Icônes dessinées (onde, étincelle, braise qui couve) ---------- */

/** L'onde — la signature graphique de l'Orbe (petite vague). */
@Composable
private fun WaveIcon(modifier: Modifier = Modifier, color: Color = Ember.Text) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.62f)
            cubicTo(w * 0.30f, h * 0.30f, w * 0.42f, h * 0.30f, w * 0.52f, h * 0.50f)
            cubicTo(w * 0.62f, h * 0.70f, w * 0.74f, h * 0.70f, w * 0.88f, h * 0.40f)
        }
        drawPath(path, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** L'étincelle — starburst radiant (8 rais). */
@Composable
private fun SparkIcon(modifier: Modifier = Modifier, color: Color = Ember.Text) {
    Canvas(modifier = modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val rOut = size.minDimension / 2f
        val rIn = rOut * 0.42f
        val stroke = 2.dp.toPx()
        for (i in 0 until 8) {
            val a = Math.toRadians((i * 45).toDouble())
            val dx = kotlin.math.cos(a).toFloat(); val dy = kotlin.math.sin(a).toFloat()
            drawLine(color, Offset(c.x + dx * rIn, c.y + dy * rIn), Offset(c.x + dx * rOut, c.y + dy * rOut), stroke, StrokeCap.Round)
        }
    }
}

/** État de chargement : anneau de braise + « La braise couve — 1,2 s ». */
@Composable
private fun BraiseCouve(modifier: Modifier = Modifier) {
    val t by rememberInfiniteTransition(label = "couve").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "couveRot"
    )
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(74.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = t }) {
                val stroke = 3.dp.toPx()
                drawArc(
                    brush = Ember.FireGradient,
                    startAngle = 0f, sweepAngle = 110f, useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                    topLeft = Offset(stroke, stroke),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke * 2, size.height - stroke * 2)
                )
            }
            Icon(Icons.Filled.LocalFireDepartment, null, tint = Ember.BraiseClair, modifier = Modifier.size(26.dp))
        }
        Text(
            "La braise couve — 1,2 s",
            color = Ember.TextDim,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 14.dp)
        )
    }
}

/* ═══════════════════════ Chips du haut : Éco + Suivis/Braise ═══════════════════════ */

@Composable
fun EmberFeedTop(
    ecoActive: Boolean,
    mode: EmberFeedMode,
    onToggleMode: (EmberFeedMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Chip Éco (honnête : dégradation annoncée) — visible seulement quand le réseau faiblit.
        if (ecoActive) {
            Row(
                modifier = Modifier
                    .width(230.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Ember.Surface.copy(alpha = 0.7f))
                    .border(1.dp, Ember.Braise.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SparkIcon(modifier = Modifier.size(15.dp), color = Ember.BraiseClair)
                Text("Éco · 240p — le réseau faiblit", color = Ember.BraiseClair, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        } else {
            Spacer(Modifier.width(1.dp))
        }

        // Toggle Suivis / Braise (les deux seuls tris, sans algorithme).
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Ember.Surface.copy(alpha = 0.7f))
                .border(1.dp, Ember.Line, RoundedCornerShape(999.dp))
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Suivis",
                color = if (mode == EmberFeedMode.Suivis) Ember.Text else Ember.TextMute,
                fontSize = 14.sp,
                fontWeight = if (mode == EmberFeedMode.Suivis) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null
                ) { onToggleMode(EmberFeedMode.Suivis) }
            )
            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Ember.Braise))
            Text(
                "Braise",
                color = if (mode == EmberFeedMode.Braise) Ember.Text else Ember.TextMute,
                fontSize = 14.sp,
                fontWeight = if (mode == EmberFeedMode.Braise) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null
                ) { onToggleMode(EmberFeedMode.Braise) }
            )
        }
    }
}

/* ═══════════════════ Navigation de l'Orbe (Flux · Explorer · + · Pulsations · Univers) ═══ */

/**
 * Rangée de navigation convoquée par l'Orbe (doc écran 02). Remplace la barre d'onglets :
 * un voile assombrit le fond, la rangée monte du bas, le « + » central est en lagune.
 */
@Composable
fun OrbeNavRow(
    visible: Boolean,
    onDismiss: () -> Unit,
    onNavigate: (com.unovapp.android.ui.components.MainTab) -> Unit,
    onCreate: () -> Unit,
    bottomInset: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(160)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Ember.Bg.copy(alpha = 0.55f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
        )
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.9f),
        exit = fadeOut(tween(140)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = bottomInset + 18.dp, start = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavDest("Flux", active = true) { onDismiss() }
            NavDest("Explorer") { onNavigate(com.unovapp.android.ui.components.MainTab.Search) }
            // Le « + » central — création, en lagune (accent produit).
            Box(
                modifier = Modifier
                    .size(EmberDim.Orbe)
                    .clip(CircleShape)
                    .background(Ember.Bg.copy(alpha = 0.7f))
                    .border(1.5.dp, Ember.Lagune, CircleShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCreate),
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Ember.Lagune, fontSize = 28.sp, fontWeight = FontWeight.Light)
            }
            NavDest("Pulsations", badge = true) { onNavigate(com.unovapp.android.ui.components.MainTab.Inbox) }
            NavDest("Univers") { onNavigate(com.unovapp.android.ui.components.MainTab.Profile) }
        }
    }
}

@Composable
private fun NavDest(label: String, active: Boolean = false, badge: Boolean = false, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(EmberDim.Touch)
                .clip(CircleShape)
                .border(1.dp, if (active) Ember.Braise else Ember.Line, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (label == "Flux") WaveIcon(modifier = Modifier.size(22.dp), color = if (active) Ember.Braise else Ember.Text)
            else Text(label.first().toString(), color = Ember.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (badge) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).size(8.dp).clip(CircleShape).background(Ember.Braise)
                )
            }
        }
        Text(label, color = if (active) Ember.Braise else Ember.TextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/* ---------- Utilitaire ---------- */

private fun formatClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
