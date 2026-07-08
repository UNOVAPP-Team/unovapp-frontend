@file:OptIn(ExperimentalFoundationApi::class)

package com.unovapp.android.ui.create

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.TextStyle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import com.unovapp.android.ui.feed.VideoPlayer
import com.unovapp.android.ui.components.unovTap
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.UnovGradients
import com.unovapp.android.ui.theme.UnovMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/* ─────────────────────────── Particles ─────────────────────────── */

private data class Particle(val bx: Float, val by: Float, val r: Float, val speed: Float, val phase: Float)

private val PARTICLES: List<Particle> = java.util.Random(0xACC1D3L).let { rng ->
    List(22) {
        Particle(
            bx    = rng.nextFloat(),
            by    = rng.nextFloat(),
            r     = 1.5f + rng.nextFloat() * 3f,
            speed = 0.40f + rng.nextFloat() * 0.50f,
            phase = rng.nextFloat() * (2f * PI.toFloat())
        )
    }
}

private val HASHTAGS = listOf("#viral", "#tendance", "#bénin", "#unovapp", "#dance", "#fyp", "#music")

/* ─────────────────────────── Step enum ─────────────────────────── */

private enum class CreateStep { Capture, Preview, Details }

/* ═══════════════════════ CreateScreen ══════════════════════════════
 *
 *  Entrée : scale depuis le bas (origine = bouton +) + fade.
 *  Fond   : 22 particules dorées en mouvement sinusoïdal lent.
 *  Flow   : Landing → Preview → Details (slide vertical + fade).
 *
 * ════════════════════════════════════════════════════════════════ */
@Composable
fun CreateScreen(onClose: () -> Unit) {
    var step by remember { mutableStateOf(CreateStep.Capture) }

    val createVm: CreateViewModel = hiltViewModel()
    val createState by createVm.state.collectAsStateWithLifecycle()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var description by remember { mutableStateOf("") }

    // Sélecteur vidéo système (galerie / fichiers). Sur sélection → étape Aperçu.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedUri = uri
            step = CreateStep.Preview
        }
    }
    val openPicker: () -> Unit = { picker.launch("video/*") }
    val doPublish: () -> Unit = { selectedUri?.let { createVm.publish(it, description) } }

    // Quand la vidéo est publiée (transcodage terminé / lancé), on ferme l'écran.
    LaunchedEffect(createState.phase) {
        if (createState.phase == PublishPhase.Published) onClose()
    }
    // Réinitialise l'état de publication quand l'écran disparaît.
    DisposableEffect(Unit) { onDispose { createVm.reset() } }

    /* Ambient shared timer — particules flottantes. */
    val ambient = rememberInfiniteTransition(label = "ambient")
    val ticker by ambient.animateFloat(
        initialValue = 0f,
        targetValue  = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "ticker"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .drawBehind {
                PARTICLES.forEach { p ->
                    val px    = p.bx * size.width  + sin(ticker * p.speed + p.phase) * size.width  * 0.042f
                    val py    = p.by * size.height + cos(ticker * p.speed * 0.7f + p.phase) * size.height * 0.042f
                    val alpha = (0.04f + (sin(ticker + p.phase) + 1f) * 0.055f).coerceIn(0f, 0.20f)
                    val rad   = (p.r * density * (1f + sin(ticker * p.speed * 1.3f + p.phase) * 0.25f)).coerceAtLeast(2f)
                    drawCircle(UnovColors.Accent.copy(alpha = alpha), radius = rad, center = Offset(px, py))
                }
            }
    ) {
        /* ── Contenu des étapes ── */
        if (step == CreateStep.Capture) {
            // Capture plein écran (caméra in-app + galerie). Header masqué : la caméra a
            // ses propres contrôles (fermer, galerie, flip, enregistrer).
            CameraCaptureScreen(
                onVideoCaptured = { uri ->
                    selectedUri = uri
                    step = CreateStep.Preview
                },
                onPickFromGallery = openPicker,
                onClose = onClose
            )
        } else {
            AnimatedContent(
                targetState  = step,
                modifier     = Modifier.fillMaxSize(),
                transitionSpec = {
                    val fwd = targetState.ordinal > initialState.ordinal
                    val enterDir  = if (fwd) 1 else -1
                    val exitDir   = -enterDir
                    (slideInVertically(UnovMotion.decelerate()) { enterDir * it / 3 } + fadeIn(tween(300)))
                        .togetherWith(slideOutVertically(UnovMotion.accelerate()) { exitDir * it / 4 } + fadeOut(tween(200)))
                },
                label = "step"
            ) { s ->
                when (s) {
                    CreateStep.Preview -> PreviewStep(uri = selectedUri)
                    CreateStep.Details -> DetailsStep(
                        uri = selectedUri,
                        description = description,
                        onDescriptionChange = { description = it },
                        onPublish = doPublish
                    )
                    CreateStep.Capture -> Unit
                }
            }

            /* ── Header flottant (Aperçu / Détails uniquement) ── */
            CreateHeader(
                step   = step,
                onBack = {
                    when (step) {
                        CreateStep.Capture -> onClose()
                        CreateStep.Preview -> step = CreateStep.Capture
                        CreateStep.Details -> step = CreateStep.Preview
                    }
                },
                onNext = {
                    when (step) {
                        CreateStep.Capture -> {}
                        CreateStep.Preview -> step = CreateStep.Details
                        CreateStep.Details -> doPublish()
                    }
                }
            )
        }

        /* ── Overlay d'upload / traitement (au-dessus de tout) ── */
        if (createState.phase != PublishPhase.Idle) {
            UploadOverlay(
                phase    = createState.phase,
                progress = createState.progress,
                error    = createState.error,
                onRetry  = doPublish,
                onClose  = { createVm.reset(); onClose() }
            )
        }
    }
}

/* ─────────────────────── Overlay upload / traitement ─────────────────────── */

@Composable
private fun UploadOverlay(
    phase: PublishPhase,
    progress: Float,
    error: String?,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    val pct = (progress * 100).roundToInt()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.86f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            when (phase) {
                PublishPhase.Uploading -> {
                    @Suppress("DEPRECATION")
                    CircularProgressIndicator(
                        progress = progress.coerceIn(0f, 1f),
                        color = UnovColors.Accent,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(20.dp))
                    Text("Envoi de ta vidéo… $pct%", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Garde l'app ouverte.", color = UnovColors.TextMute, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
                PublishPhase.Processing -> {
                    CircularProgressIndicator(color = UnovColors.Accent, strokeWidth = 3.dp, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(20.dp))
                    Text("Traitement en cours…", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "On convertit ta vidéo en plusieurs qualités. Ça peut prendre quelques minutes.",
                        color = UnovColors.TextMute, fontSize = 12.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Spacer(Modifier.height(20.dp))
                    GhostNextButton("Continuer en arrière-plan", onClose)
                }
                PublishPhase.Failed -> {
                    Text("Échec de la publication", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(error, color = UnovColors.TextMute, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GhostNextButton("Fermer", onClose)
                        GoldNextButton("Réessayer", onRetry)
                    }
                }
                else -> Unit
            }
        }
    }
}

/* ─────────────────────────── Header ─────────────────────────────── */

@Composable
private fun CreateHeader(step: CreateStep, onBack: () -> Unit, onNext: () -> Unit) {
    val noRipple = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        /* Retour */
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
                .unovTap(onBack, pressedScale = 0.88f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector     = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Retour",
                tint            = Color.White,
                modifier        = Modifier.size(20.dp)
            )
        }

        /* Points de progression */
        StepDots(current = step)

        /* Suivant / Publier */
        when (step) {
            CreateStep.Capture -> Spacer(Modifier.width(40.dp))
            CreateStep.Preview -> GhostNextButton("Suivant", onNext)
            CreateStep.Details -> GoldNextButton ("Publier", onNext)
        }
    }
}

@Composable
private fun StepDots(current: CreateStep) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        CreateStep.values().forEach { s ->
            val active = s == current
            val width by animateFloatAsState(
                targetValue  = if (active) 22f else 6f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                label        = "dotW"
            )
            val alpha by animateFloatAsState(
                targetValue  = if (active) 1f else 0.35f,
                animationSpec = tween(300),
                label        = "dotA"
            )
            Box(
                modifier = Modifier
                    .width(width.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) UnovGradients.Gold else Brush.linearGradient(listOf(UnovColors.TextMute.copy(alpha = alpha), UnovColors.TextMute.copy(alpha = alpha))))
            )
        }
    }
}

@Composable
private fun GhostNextButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, UnovColors.Accent.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
            .unovTap(onClick, pressedScale = 0.92f)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = UnovColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GoldNextButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(UnovGradients.Gold)
            .unovTap(onClick, pressedScale = 0.92f)
            .padding(horizontal = 18.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color(0xFF0D0D0D), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/* ═══════════════════════ ÉTAPE 1 — CHOISIR ════════════════════════ */

@Composable
private fun LandingStep(onPick: () -> Unit) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 20.dp)
    ) {
        Spacer(Modifier.height(72.dp))

        /* Titre */
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(
                "STUDIO",
                color        = UnovColors.Accent,
                fontSize     = 10.sp,
                fontWeight   = FontWeight.Bold,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Nouveau contenu",
                color      = Color.White,
                fontSize   = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
            Text(
                "Filme ou importe ta prochaine vidéo virale.",
                color    = UnovColors.TextDim,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Spacer(Modifier.height(28.dp))

        /* Cartes source */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CameraCard(
                appeared = appeared,
                modifier = Modifier.weight(1f),
                onClick  = onPick
            )
            GalleryCard(
                appeared = appeared,
                modifier = Modifier.weight(1f),
                onClick  = onPick
            )
        }

        Spacer(Modifier.height(32.dp))

        /* Divider "Récentes" */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.weight(1f).height(0.5.dp).background(UnovColors.LineStrong))
            Text("Récentes", color = UnovColors.TextMute, fontSize = 11.sp, letterSpacing = 0.8.sp)
            Box(Modifier.weight(1f).height(0.5.dp).background(UnovColors.LineStrong))
        }

        Spacer(Modifier.height(14.dp))

        /* Grille récente — 2 rangées × 3 colonnes */
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(0..2, 3..5).forEachIndexed { rowIdx, range ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    range.forEach { idx ->
                        RecentTile(
                            gradIdx  = idx,
                            rowDelay = rowIdx * 100,
                            appeared = appeared,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/* Carte Caméra — cercle pulsant + icône + bordure tournante */
@Composable
private fun CameraCard(appeared: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val ring = rememberInfiniteTransition(label = "camRing")
    val ringAngle by ring.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "ringAngle"
    )
    val pulse = rememberInfiniteTransition(label = "camPulse")
    val pulseScale by pulse.animateFloat(
        0.85f, 1.15f,
        infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseScale"
    )
    val pulseAlpha by pulse.animateFloat(
        0.20f, 0.55f,
        infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    val entrance by animateFloatAsState(
        if (appeared) 1f else 0f,
        spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "camEntrance"
    )

    Box(
        modifier = modifier
            .graphicsLayer { alpha = entrance; scaleX = 0.82f + 0.18f * entrance; scaleY = scaleX }
            .height(210.dp)
            .drawBehind {
                // Bordure tournante sweep gradient
                rotate(ringAngle) {
                    drawRoundRect(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color.Transparent,
                                UnovColors.Accent.copy(0.9f),
                                UnovColors.AccentDeep.copy(0.5f),
                                Color.Transparent,
                                Color.Transparent
                            )
                        ),
                        cornerRadius = CornerRadius(20.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0C0C0C))
            .unovTap(onClick = onClick, pressedScale = 0.94f),
        contentAlignment = Alignment.Center
    ) {
        /* Halo pulsant */
        Box(
            modifier = Modifier
                .size(90.dp)
                .scale(pulseScale)
                .background(
                    Brush.radialGradient(listOf(UnovColors.Accent.copy(pulseAlpha), Color.Transparent)),
                    CircleShape
                )
        )
        /* Cercle intérieur + icône */
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(UnovColors.Accent.copy(0.18f), UnovColors.AccentDeep.copy(0.06f))
                        )
                    )
                    .border(1.5.dp, UnovColors.Accent.copy(0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.CameraAlt, null, tint = UnovColors.Accent, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Caméra", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("Filmer", color = UnovColors.TextMute, fontSize = 10.sp)
        }
    }
}

/* Carte Galerie — mosaïque 3 × 3 + label */
@Composable
private fun GalleryCard(appeared: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val ring = rememberInfiniteTransition(label = "galRing")
    val ringAngle by ring.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "galRingAngle"
    )
    val entrance by animateFloatAsState(
        if (appeared) 1f else 0f,
        spring(0.72f, Spring.StiffnessMediumLow, visibilityThreshold = 0.001f),
        label = "galEntrance"
    )

    Box(
        modifier = modifier
            .graphicsLayer { alpha = entrance; scaleX = 0.82f + 0.18f * entrance; scaleY = scaleX }
            .height(210.dp)
            .drawBehind {
                rotate(ringAngle - 45f) {
                    drawRoundRect(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color.Transparent,
                                Color.Transparent,
                                UnovColors.AccentDeep.copy(0.6f),
                                UnovColors.Accent.copy(0.85f),
                                Color.Transparent
                            )
                        ),
                        cornerRadius = CornerRadius(20.dp.toPx()),
                        style = Stroke(2.dp.toPx())
                    )
                }
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0A0A0A))
            .unovTap(onClick = onClick, pressedScale = 0.94f)
    ) {
        /* Grille 3×3 remplit la carte */
        Column(Modifier.fillMaxSize().padding(4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            (0..2).forEach { row ->
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    (0..2).forEach { col ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(6.dp))
                                .background(UnovGradients.videoBg(row * 3 + col))
                        )
                    }
                }
            }
        }
        /* Overlay dégradé bas + label */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.85f)))
                )
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Galerie", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Importer", color = UnovColors.TextMute, fontSize = 10.sp)
            }
        }
        /* Icône top-right */
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(0.55f))
                .border(1.dp, Color.White.copy(0.20f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(13.dp))
        }
    }
}

@Composable
private fun RecentTile(gradIdx: Int, rowDelay: Int, appeared: Boolean, modifier: Modifier) {
    val entrance by animateFloatAsState(
        if (appeared) 1f else 0f,
        tween(360, delayMillis = rowDelay + gradIdx * 55),
        label = "tileFade$gradIdx"
    )
    Box(
        modifier = modifier
            .aspectRatio(9f / 14f)
            .graphicsLayer { alpha = entrance; scaleX = 0.88f + 0.12f * entrance; scaleY = scaleX }
            .clip(RoundedCornerShape(8.dp))
            .background(UnovGradients.videoBg(gradIdx))
    ) {
        /* Durée simulée */
        Text(
            text     = "${15 + gradIdx * 7}s",
            color    = Color.White,
            fontSize = 9.sp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp)
        )
    }
}

/* ═══════════════════════ ÉTAPE 2 — AFFINER ════════════════════════ */

@Composable
private fun PreviewStep(uri: Uri?) {
    var muted by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (uri != null) {
            // Vraie lecture en boucle de la vidéo capturée/importée (ExoPlayer).
            VideoPlayer(
                url = uri.toString(),
                isPlaying = true,
                muted = muted,
                modifier = Modifier.fillMaxSize()
            )
        }
        // Toggle son.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .unovTap(onClick = { muted = !muted }, pressedScale = 0.9f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (muted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                contentDescription = "Activer/couper le son",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun VideoPlayButton() {
    val pulse = rememberInfiniteTransition(label = "playPulse")
    val pScale by pulse.animateFloat(
        0.88f, 1.05f,
        infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "playScale"
    )
    Box(
        modifier = Modifier
            .size(58.dp)
            .scale(pScale)
            .clip(CircleShape)
            .background(Color.White.copy(0.18f))
            .border(2.dp, Color.White.copy(0.50f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.PlayArrow, null, tint = Color.White, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun EditTool(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val noRipple = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = noRipple, indication = null) {}
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(UnovColors.Surface)
                .border(1.dp, UnovColors.LineStrong, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = UnovColors.Text, modifier = Modifier.size(20.dp))
        }
        Text(label, color = UnovColors.TextMute, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TrimBar(modifier: Modifier = Modifier) {
    val trimStartState = remember { mutableStateOf(0f) }
    val trimEndState   = remember { mutableStateOf(1f) }
    val barWidthPx     = remember { mutableStateOf(0f) }

    val trimStart = trimStartState.value
    val trimEnd   = trimEndState.value
    val barW      = barWidthPx.value

    Column(modifier = modifier.fillMaxWidth()) {
        /* Étiquettes temps */
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${(trimStart * 60).roundToInt()}s", color = UnovColors.TextMute, fontSize = 10.sp)
            Text("60s max", color = UnovColors.TextMute, fontSize = 10.sp)
            Text("${(trimEnd * 60).roundToInt()}s", color = UnovColors.TextMute, fontSize = 10.sp)
        }
        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .onGloballyPositioned { barWidthPx.value = it.size.width.toFloat() }
        ) {
            /* Piste de fond + région active dessinée en pixels via drawBehind (aucune conversion dp) */
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(999.dp))
                    .background(UnovColors.LineStrong)
                    .drawBehind {
                        drawRoundRect(
                            brush        = UnovGradients.Gold,
                            topLeft      = Offset(trimStart * size.width, 0f),
                            size         = androidx.compose.ui.geometry.Size((trimEnd - trimStart) * size.width, size.height),
                            cornerRadius = CornerRadius(size.height / 2f)
                        )
                    }
            )
            /* Handle début */
            TrimHandle(
                xFraction  = trimStart,
                barWidthPx = barW,
                onDrag     = { delta ->
                    if (barW > 0f)
                        trimStartState.value = (trimStart + delta / barW).coerceIn(0f, trimEndState.value - 0.08f)
                },
                modifier   = Modifier.align(Alignment.CenterStart)
            )
            /* Handle fin */
            TrimHandle(
                xFraction  = trimEnd,
                barWidthPx = barW,
                onDrag     = { delta ->
                    if (barW > 0f)
                        trimEndState.value = (trimEnd + delta / barW).coerceIn(trimStartState.value + 0.08f, 1f)
                },
                modifier   = Modifier.align(Alignment.CenterStart)
            )
        }
    }
}

@Composable
private fun TrimHandle(xFraction: Float, barWidthPx: Float, onDrag: (Float) -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .offset { IntOffset(((xFraction * barWidthPx) - 11.dp.toPx()).roundToInt(), 0) }
            .size(22.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(2.dp, UnovColors.Accent, CircleShape)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, delta -> onDrag(delta) }
            }
    )
}

/* ═══════════════════════ ÉTAPE 3 — PUBLIER ════════════════════════ */

/** Miniature d'aperçu : extrait la 1ʳᵉ image réelle de la vidéo (local URI) via MediaMetadataRetriever. */
@Composable
private fun VideoThumbnail(uri: Uri?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(uri) {
        bitmap = if (uri == null) null else withContext(Dispatchers.IO) {
            runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } finally {
                    retriever.release()
                }
            }.getOrNull()
        }
    }
    Box(modifier) {
        bitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Aperçu de la vidéo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun DetailsStep(
    uri: Uri?,
    description: String,
    onDescriptionChange: (String) -> Unit,
    onPublish: () -> Unit
) {
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Spacer(Modifier.height(72.dp))

        /* Miniature (vraie 1ʳᵉ image de la vidéo) + champ de description côte à côte */
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            VideoThumbnail(
                uri = uri,
                modifier = Modifier
                    .size(width = 64.dp, height = 90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(UnovGradients.videoBg(1))
                    .border(1.dp, UnovColors.LineStrong, RoundedCornerShape(10.dp))
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(UnovColors.Surface)
                    .border(1.dp, UnovColors.LineStrong, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                if (description.isEmpty()) {
                    Text(
                        "Décris ta vidéo…  #hashtags  @mentions",
                        color    = UnovColors.TextMute,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
                BasicTextField(
                    value = description,
                    onValueChange = { onDescriptionChange(it.take(500)) },
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp, lineHeight = 20.sp),
                    cursorBrush = SolidColor(UnovColors.Accent),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(Modifier.height(26.dp))

        /* ── Suggestions hashtags ── */
        SectionLabel("Suggestions")
        Spacer(Modifier.height(10.dp))
        HashtagSuggestions()

        Spacer(Modifier.height(26.dp))

        /* ── Musique ── */
        SectionLabel("Musique")
        Spacer(Modifier.height(10.dp))
        MusicPickerRow()

        Spacer(Modifier.height(26.dp))

        /* ── Visibilité ── */
        SectionLabel("Visibilité")
        Spacer(Modifier.height(10.dp))
        PrivacySelector()

        Spacer(Modifier.height(36.dp))

        /* ── Bouton publier ── */
        PublishButton(onClick = onPublish)

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color        = UnovColors.TextMute,
        fontSize     = 10.sp,
        fontWeight   = FontWeight.Medium,
        letterSpacing = 2.sp
    )
}

/* Chips hashtag avec cascade d'entrée */
@Composable
private fun HashtagSuggestions() {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 8.dp)
    ) {
        itemsIndexed(HASHTAGS) { index, tag ->
            val entrance by animateFloatAsState(
                targetValue  = if (shown) 1f else 0f,
                animationSpec = tween(durationMillis = 320, delayMillis = 80 + index * 65),
                label        = "tag$index"
            )
            val noRipple = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha        = entrance
                        translationX = (1f - entrance) * 36.dp.toPx()
                    }
                    .clip(RoundedCornerShape(999.dp))
                    .background(UnovColors.Accent.copy(alpha = 0.10f))
                    .border(1.dp, UnovColors.Accent.copy(alpha = 0.30f), RoundedCornerShape(999.dp))
                    .clickable(interactionSource = noRipple, indication = null) {}
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(tag, color = UnovColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/* Vinyle rotatif + informations de la piste */
@Composable
private fun MusicPickerRow() {
    val noRipple = remember { MutableInteractionSource() }
    val vinyl    = rememberInfiniteTransition(label = "vinyl")
    val rotation by vinyl.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(4500, easing = LinearEasing)),
        label = "vinylRot"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(UnovColors.Surface)
            .border(1.dp, UnovColors.LineStrong, RoundedCornerShape(14.dp))
            .clickable(interactionSource = noRipple, indication = null) {}
            .padding(14.dp),
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        /* Vinyle */
        Box(
            modifier = Modifier
                .size(52.dp)
                .graphicsLayer { rotationZ = rotation }
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        listOf(Color(0xFF1A1A1A), Color(0xFF2C2C2C), Color(0xFF1A1A1A), Color(0xFF262626), Color(0xFF1A1A1A))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(UnovGradients.Gold)
            )
        }
        /* Infos piste */
        Column(modifier = Modifier.weight(1f)) {
            Text("Ajouter une musique", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("Sons originaux · Tendances", color = UnovColors.TextMute, fontSize = 11.sp)
        }
        Icon(Icons.Outlined.MusicNote, null, tint = UnovColors.Accent, modifier = Modifier.size(18.dp))
    }
}

/* Sélecteur de confidentialité segmenté */
@Composable
private fun PrivacySelector() {
    data class Option(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
    val options = listOf(
        Option("Tous",  Icons.Outlined.Public),
        Option("Amis",  Icons.Outlined.People),
        Option("Privé", Icons.Outlined.Lock)
    )
    var selected by remember { mutableStateOf(0) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(UnovColors.Surface)
            .border(1.dp, UnovColors.LineStrong, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        options.forEachIndexed { idx, opt ->
            val isActive  = idx == selected
            val bgAlpha by animateFloatAsState(if (isActive) 1f else 0f, tween(220), label = "privBg$idx")

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isActive) UnovGradients.Gold
                        else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .unovTap(onClick = { selected = idx }, pressedScale = 0.94f)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment    = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        opt.icon, null,
                        tint     = if (isActive) Color(0xFF0D0D0D) else UnovColors.TextMute,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        opt.label,
                        color      = if (isActive) Color(0xFF0D0D0D) else UnovColors.TextMute,
                        fontSize   = 12.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

/* Bouton Publier — dégradé or + balayage lumineux */
@Composable
private fun PublishButton(onClick: () -> Unit) {
    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val shimX by shimmer.animateFloat(
        initialValue  = -0.6f,
        targetValue   = 1.6f,
        animationSpec = infiniteRepeatable(tween(2200, delayMillis = 600, easing = LinearEasing)),
        label         = "shimX"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(UnovGradients.Gold)
            .drawBehind {
                val start = shimX * size.width
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.28f),
                            Color.Transparent
                        ),
                        start = Offset(start, 0f),
                        end   = Offset(start + size.width * 0.30f, size.height)
                    )
                )
            }
            .unovTap(onClick = onClick, pressedScale = 0.97f),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "PUBLIER",
            color        = Color(0xFF0D0D0D),
            fontSize     = 15.sp,
            fontWeight   = FontWeight.ExtraBold,
            letterSpacing = 2.4.sp,
            textAlign    = TextAlign.Center
        )
    }
}
