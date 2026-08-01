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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
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
import com.unovapp.android.ui.theme.EmberMotion
import com.unovapp.android.ui.theme.appear
import com.unovapp.android.ui.theme.arcOpen
import com.unovapp.android.ui.theme.breathe
import com.unovapp.android.ui.theme.pressable
import com.unovapp.android.ui.theme.riseIn
import com.unovapp.android.ui.theme.staggerDelay
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
    bottomInset: androidx.compose.ui.unit.Dp = 0.dp,
    /**
     * Vrai quand l'Orbe a convoqué l'état éveillé. La maquette (écran 02) n'y montre
     * NI le bloc créateur NI la légende : la place revient à l'arc et à la navigation.
     * On les retire donc, au lieu de les laisser transparaître sous le voile.
     */
    awakened: Boolean = false
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
            // Bloc créateur + légende : présents au repos, retirés à l'éveil (écran 02).
            if (!awakened) {
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
            }

            // Ligne de temps + étincelles + durée — visible dans les deux états.
            EmberTimeline(
                progress = progress,
                durationMs = if (durationMs > 0) durationMs else video.durationSec * 1000L,
                sparkSeed = video.id.hashCode(),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)
            )

            // À l'éveil, la rangée de navigation occupe cette hauteur (dessinée par-dessus
            // en overlay) : on lui réserve la place pour qu'elle ne recouvre rien.
            if (awakened) Spacer(Modifier.height(96.dp))

            // AU REPOS : rien que l'Orbe (maquette écran 01). « Un seul point, sous ton
            // pouce. Pas de barre d'onglets, pas de menu à apprendre. » Les actions et la
            // navigation n'existent que dans l'état éveillé, convoquées par l'Orbe.
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Orbe(onClick = onOrbe, hasBadge = true, modifier = Modifier.riseIn())
            }
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

/**
 * Entrée d'un item de l'arc : il **grandit depuis l'Orbe** (§3.4) — l'origine du
 * mouvement est le point touché, pas le centre de l'écran (R1).
 *
 * [k] = rang depuis le bas (0 = Braise, la plus proche de l'Orbe). Plus l'item est
 * haut, plus il part de loin : la translation initiale le ramène vers l'Orbe, en
 * bas et vers la gauche. Le stagger de 28 ms fait partir la Braise en premier.
 */
@Composable
private fun Modifier.emanate(k: Int): Modifier = appear(
    dxFrom = (-18 - k * 5).dp,
    dyFrom = (48 + k * 34).dp,
    scaleFrom = 0.72f,
    durationMs = EmberMotion.DurBase,
    delayMs = staggerDelay(k, 28)
)

/**
 * `reactFanIn` (§7.6) — l'ÉVENTAIL de la colonne de réactions. Les items sortent
 * par la droite en montant : plus l'item est haut, plus il vient de loin (+18 →
 * +33 px), et tous montent de 12 px (R4 : la chaleur monte).
 *
 * [k] = rang depuis le BAS (0 = Braise, qui part la première — même instant que
 * Studio dans l'arc de navigation). Stagger 28 ms, scale de départ .86.
 */
@Composable
private fun Modifier.reactFanIn(k: Int): Modifier = appear(
    dxFrom = (18 + k * 3).dp,
    dyFrom = 12.dp,
    scaleFrom = 0.86f,
    durationMs = EmberMotion.DurBase,
    delayMs = staggerDelay(k, 28)
)

/* ---------- Une action de l'arc éveillé (étiquette à gauche, pastille à droite) ---------- */

@Composable
private fun ArcAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** La Braise est la plus importante : plus grande, pleine, et elle respire. */
    primary: Boolean = false,
    labelColor: Color = Ember.TextDim,
    /**
     * Décalage vers l'intérieur qui donne sa COURBURE à l'arc. Mesuré sur la maquette
     * (écran 02) : les centres des pastilles décrivent un arc de cercle dont le point
     * le plus à gauche tombe sur Étinceler/Garder, et qui repart vers la droite en
     * montant comme en descendant. Sans ça, on obtient une colonne droite — ce que
     * la maquette ne montre pas.
     */
    arcInset: androidx.compose.ui.unit.Dp = 0.dp,
    icon: @Composable () -> Unit
) {
    Row(
        modifier = modifier.padding(end = arcInset).pressable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            label,
            color = labelColor,
            fontSize = 15.sp,
            fontWeight = if (primary) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1, softWrap = false
        )
        if (primary) {
            // La Braise (§7.5) : Ø 56, anneau de progression r=25 trait 2.5 rempli à
            // 66 %, noyau incandescent Ø 46 (inset 5). Seul item de la colonne à
            // respirer une fois entré.
            Box(
                modifier = Modifier.size(56.dp).breathe(amplitude = 1.035f, minAlpha = 0.75f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(56.dp)) {
                    val stroke = 2.5.dp.toPx()
                    val r = 25.dp.toPx()
                    val topLeft = Offset(size.width / 2f - r, size.height / 2f - r)
                    drawArc(
                        color = Ember.Braise.copy(alpha = 0.25f),
                        startAngle = 0f, sweepAngle = 360f, useCenter = false,
                        style = Stroke(stroke), topLeft = topLeft,
                        size = androidx.compose.ui.geometry.Size(r * 2, r * 2)
                    )
                    // 104/157.08 = 66 % — départ à midi, sens horaire.
                    drawArc(
                        color = Ember.Braise,
                        startAngle = -90f, sweepAngle = 360f * 0.66f, useCenter = false,
                        style = Stroke(stroke, cap = StrokeCap.Round), topLeft = topLeft,
                        size = androidx.compose.ui.geometry.Size(r * 2, r * 2)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFFF8A2A), Color(0xFFE63900)),
                                center = Offset(0.5f, 0.38f)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) { icon() }
            }
        } else {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape)
                    .background(Ember.Bg.copy(alpha = 0.55f))
                    .border(1.dp, Ember.Line, CircleShape),
                contentAlignment = Alignment.Center
            ) { icon() }
        }
    }
}

@Composable
private fun SideCircle(onClick: () -> Unit, modifier: Modifier = Modifier, icon: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .size(50.dp)
            .clip(CircleShape)
            .border(1.dp, Ember.Line, CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { icon() }
}

/** L'Orbe — point d'ancrage de navigation (utilisé hors socle, ex. rangée convoquée). */
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

/**
 * L'ÉTINCELLE — une gerbe : 8 rais fins qui convergent vers un centre CREUX.
 * À ne pas confondre avec la Lueur (soleil à disque plein) : ce sont deux notions
 * différentes du produit, elles doivent se distinguer d'un coup d'œil à 22 dp.
 * Ici : rais longs (du centre presque jusqu'au bord), trait fin, aucun disque.
 */
@Composable
private fun SparkIcon(modifier: Modifier = Modifier, color: Color = Ember.Text) {
    Canvas(modifier = modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val rOut = size.minDimension / 2f
        val rIn = rOut * 0.26f
        val stroke = 1.7.dp.toPx()
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
        // Elle est ÉLASTIQUE (weight, fill=false) et tronque son texte : c'est elle qui cède
        // la place, jamais la bascule. Avec une largeur figée, la bascule se retrouvait
        // comprimée et « Braise » se cassait en trois lignes.
        if (ecoActive) {
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Ember.Surface.copy(alpha = 0.7f))
                    .border(1.dp, Ember.Braise.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SparkIcon(modifier = Modifier.size(15.dp), color = Ember.BraiseClair)
                Text(
                    "Éco · 240p — le réseau faiblit",
                    color = Ember.BraiseClair, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Spacer(Modifier.width(1.dp))
        }

        Spacer(Modifier.width(10.dp))

        // Toggle Suivis / Braise (les deux seuls tris, sans algorithme).
        // Ne se comprime jamais : chaque libellé tient sur une ligne.
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
                maxLines = 1, softWrap = false,
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
                maxLines = 1, softWrap = false,
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
    onBraise: () -> Unit = {},
    onLueur: () -> Unit = {},
    onEtinceler: () -> Unit = {},
    onGarder: () -> Unit = {},
    onOffrir: () -> Unit = {},
    onEnvoyer: () -> Unit = {},
    /**
     * L'arc de réactions n'a de sens que sur le Flux (il agit sur une vidéo).
     * Ailleurs, l'Orbe ne convoque que la navigation.
     */
    showReactions: Boolean = true,
    /** Destination active, pour l'accent braise dans la rangée. */
    activeTab: com.unovapp.android.ui.components.MainTab = com.unovapp.android.ui.components.MainTab.Feed,
    modifier: Modifier = Modifier
) {
    // Voile — 160 ms, opacité .35 (§Écran 02). Un tap dessus referme.
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(EmberMotion.DurQuick)),
        exit = fadeOut(tween(EmberMotion.DurQuick)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Voile d'éveil : rgba(6,4,2,.42) — valeur normative de la spec Flux
                // (§3.5), qui tranche l'écart entre la maquette et ANIMATIONS.md (.35).
                .background(Color(0x06040200).copy(alpha = 0.42f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(EmberMotion.DurBase, easing = EmberMotion.EaseOut)),
        // L'interface se retire D'UN BLOC, sans stagger, et se rétracte vers l'Orbe :
        // 160 ms EaseIn, origine en bas-centre — là où se trouve l'Orbe (§Écran 02).
        exit = fadeOut(tween(EmberMotion.DurQuick, easing = EmberMotion.EaseIn)) +
            scaleOut(
                targetScale = 0.9f,
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f),
                animationSpec = tween(EmberMotion.DurQuick, easing = EmberMotion.EaseIn)
            ),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ── L'ARC DE RÉACTIONS : vertical, collé à droite, il se déplie VERS LE HAUT
            //    depuis l'Orbe. Ordre d'arrivée : la Braise d'abord (la plus importante),
            //    puis on remonte — stagger 28 ms (§Écran 02).
            if (showReactions) Column(
                modifier = Modifier
                    // Dégagement calculé : Orbe (~74) + rangée de nav (~78) + marge.
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = bottomInset + 196.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // `emanate(k)` : l'item naît À L'ORBE et s'étire jusqu'à sa place (§3.4,
                // « transform-origin = la position de l'Orbe »). Plus il est haut dans
                // l'arc, plus il vient de loin — d'où le facteur k. Stagger 28 ms, la
                // Braise partant EN PREMIÈRE (k = 0).
                ArcAction("Envoyer", onEnvoyer, Modifier.reactFanIn(5), arcInset = 0.dp) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Envoyer", tint = Ember.Text, modifier = Modifier.size(22.dp))
                }
                ArcAction("Offrir", onOffrir, Modifier.reactFanIn(4), arcInset = 14.dp) {
                    Icon(Icons.Outlined.CardGiftcard, "Offrir", tint = Ember.Text, modifier = Modifier.size(22.dp))
                }
                ArcAction("Garder", onGarder, Modifier.reactFanIn(3), arcInset = 22.dp) {
                    Icon(Icons.Outlined.BookmarkBorder, "Garder", tint = Ember.Text, modifier = Modifier.size(22.dp))
                }
                ArcAction("Étinceler", onEtinceler, Modifier.reactFanIn(2), arcInset = 22.dp) {
                    SparkIcon(modifier = Modifier.size(22.dp))
                }
                ArcAction("Lueur · gratuit", onLueur, Modifier.reactFanIn(1), arcInset = 22.dp) {
                    LueurIcon(modifier = Modifier.size(22.dp))
                }
                // La Braise arrive EN PREMIER et reste le seul élément qui boucle.
                ArcAction(
                    "Braise · jeton", onBraise,
                    Modifier.reactFanIn(0),
                    primary = true,
                    labelColor = Ember.Braise,
                    arcInset = 14.dp
                ) {
                    Icon(Icons.Filled.LocalFireDepartment, "Braise", tint = Ember.BraisePale2, modifier = Modifier.size(26.dp))
                }
            }

            // ── LA RANGÉE DE NAVIGATION : ce n'est PAS une rangée droite, c'est un ARC.
            //
            //    Centres relevés sur la maquette (écran 02) :
            //      Flux (126,1394) · Explorer (242,1364) · + (385,1346)
            //      Pulsations (530,1364) · Univers (647,1394)
            //    Un cercle ajusté dessus donne un centre à (386, 2077) et R ≈ 731 px,
            //    pour un gabarit large de 780 px — soit R ≈ 0,94 × largeur d'écran.
            //    Signature du cercle : les écarts horizontaux sont PLUS LARGES près du
            //    sommet (143/145 px) qu'aux extrémités (116/117 px). Une rangée à
            //    espacement régulier ne peut pas produire ça.
            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = bottomInset + 84.dp)
            ) {
                // Géométrie NORMATIVE (spec Flux §6.3/§6.4) : écartement horizontal
                // constant de 68 dp entre centres, envergure 272, flèche 24. Les cinq
                // centres tombent sur un cercle R = 397,3 centré 288 dp sous l'Orbe.
                // Valeurs fixes et non proportionnelles : la spec raisonne sur un cadre
                // de 390 dp, et un arc qui s'étirerait avec l'écran cesserait d'être
                // atteignable au pouce — c'est sa raison d'être.
                val dxOuter = 136.dp                        // Flux / Univers
                val dxMid = 68.dp                           // Explorer / Pulsations
                val sagOuter = 24.dp                        // flèche de l'arc
                val sagMid = 6.dp                           // 676 → 682

                // Les items sont ancrés en bas-centre ; `sagOuter` sert de ligne de base
                // pour que le sommet (le « + ») soit le point le plus haut.

                NavDest(
                    "Flux", active = activeTab == com.unovapp.android.ui.components.MainTab.Feed,
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .offset(x = -dxOuter, y = sagOuter - sagOuter).emanate(2)
                ) { if (activeTab == com.unovapp.android.ui.components.MainTab.Feed) onDismiss() else onNavigate(com.unovapp.android.ui.components.MainTab.Feed) }

                NavDest(
                    "Explorer", active = activeTab == com.unovapp.android.ui.components.MainTab.Search,
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .offset(x = -dxMid, y = sagMid - sagOuter).emanate(1)
                ) { onNavigate(com.unovapp.android.ui.components.MainTab.Search) }

                // Studio au SOMMET de l'arc. En lagune parce que c'est la destination
                // SUGGÉRÉE par l'IA (§6.5) — d'où le libellé « Studio · suggéré » et le
                // halo lagune. Même gabarit que ses voisines : seules sa position et sa
                // couleur le distinguent.
                NavDest(
                    label = "Studio · suggéré",
                    ringColor = Ember.Lagune,
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .offset(y = -sagOuter).emanate(0),
                    onClick = onCreate
                )

                NavDest(
                    "Pulsations", active = activeTab == com.unovapp.android.ui.components.MainTab.Inbox, badge = true,
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .offset(x = dxMid, y = sagMid - sagOuter).emanate(1)
                ) { onNavigate(com.unovapp.android.ui.components.MainTab.Inbox) }

                NavDest(
                    "Univers", active = activeTab == com.unovapp.android.ui.components.MainTab.Profile,
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .offset(x = dxOuter, y = sagOuter - sagOuter).emanate(2)
                ) { onNavigate(com.unovapp.android.ui.components.MainTab.Profile) }
            }
        }
    }
}

/**
 * Une destination de l'arc de navigation.
 *
 * Les CINQ pastilles ont exactement la même structure et la même taille — mesuré
 * sur la maquette : les anneaux font tous 87 px (≈ 44 dp), le « + » compris. Il
 * n'est pas plus gros, il est seulement en lagune et posé au sommet de l'arc.
 * Structure commune = les centres des cercles s'alignent d'eux-mêmes ; c'est ce
 * qui manquait, et le « + » retombait sous ses voisins.
 *
 * [label] vide ⇒ pas de texte, mais la place reste réservée pour que l'alignement
 * par le bas reste comparable d'une pastille à l'autre.
 */
@Composable
private fun NavDest(
    label: String,
    active: Boolean = false,
    badge: Boolean = false,
    ringColor: Color? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = ringColor ?: if (active) Ember.Braise else Ember.Line
    val tint = ringColor ?: if (active) Ember.Braise else Ember.Text
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.pressable(onClick = onClick).padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                // La destination suggérée par l'IA porte un halo lagune (§6.5).
                .then(
                    if (ringColor != null) Modifier.background(
                        Brush.radialGradient(
                            listOf(Ember.Lagune.copy(alpha = 0.25f), Color.Transparent)
                        ),
                        CircleShape
                    ) else Modifier
                )
                .clip(CircleShape)
                .background(Color(0xFF110A05).copy(alpha = 0.85f))
                .border(if (ringColor != null) 1.dp else if (active) 1.5.dp else 1.dp, accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Tailles de rendu de la spec §6.5 : 17 dp pour Flux, 16 pour les autres.
            when {
                label == "Flux" -> WaveIcon(modifier = Modifier.size(17.dp), color = tint)
                label == "Explorer" -> Icon(Icons.Outlined.Search, label, tint = tint, modifier = Modifier.size(16.dp))
                label == "Pulsations" -> Icon(Icons.Outlined.NotificationsNone, label, tint = tint, modifier = Modifier.size(16.dp))
                label == "Univers" -> Icon(Icons.Outlined.PersonOutline, label, tint = tint, modifier = Modifier.size(16.dp))
                else -> Icon(Icons.Filled.Add, "Studio", tint = tint, modifier = Modifier.size(16.dp))
            }
            // Pastille de non-lu — braise, débordant légèrement du cercle.
            if (badge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(9.dp).clip(CircleShape).background(Ember.Braise)
                )
            }
        }
        // Libellé — vide pour le « + », mais la ligne est conservée : sans elle, sa
        // pastille descendrait par rapport aux autres (alignement par le bas).
        Text(
            label,
            color = if (active) Ember.Braise else Ember.TextDim,
            fontSize = 12.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, softWrap = false
        )
    }
}

/**
 * La LUEUR — un soleil : gros disque plein au centre, entouré de rais COURTS et
 * bien détachés. C'est le contraire de l'Étincelle (gerbe creuse à rais longs) —
 * la Lueur est douce et pleine, l'Étincelle est piquante et vide.
 */
@Composable
private fun LueurIcon(modifier: Modifier = Modifier, color: Color = Ember.Text) {
    Canvas(modifier = modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val rOut = size.minDimension / 2f
        // Disque central généreux (≈46 % du diamètre) : c'est lui qui signe la Lueur.
        drawCircle(color, radius = rOut * 0.46f, center = c)
        // Rais courts, nettement séparés du disque.
        val rIn = rOut * 0.68f
        for (i in 0 until 8) {
            val a = Math.toRadians((i * 45).toDouble())
            val dx = kotlin.math.cos(a).toFloat(); val dy = kotlin.math.sin(a).toFloat()
            drawLine(color, Offset(c.x + dx * rIn, c.y + dy * rIn), Offset(c.x + dx * rOut, c.y + dy * rOut), 2.2.dp.toPx(), StrokeCap.Round)
        }
    }
}

/* ---------- Utilitaire ---------- */

private fun formatClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
