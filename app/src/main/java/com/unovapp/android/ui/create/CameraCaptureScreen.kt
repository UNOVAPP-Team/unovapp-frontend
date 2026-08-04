package com.unovapp.android.ui.create

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.unovapp.android.data.mock.MOCK_REALISATEUR_IDEA
import com.unovapp.android.data.mock.MOCK_REALISATEUR_PLAN
import com.unovapp.android.data.mock.MOCK_SCENES
import com.unovapp.android.data.mock.MockScene
import com.unovapp.android.ui.components.DemoBanner
import com.unovapp.android.ui.components.EmberBody
import com.unovapp.android.ui.components.unovTap
import com.unovapp.android.ui.components.RecordingPulse
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import com.unovapp.android.ui.theme.Ember
import com.unovapp.android.ui.theme.EmberDim
import com.unovapp.android.ui.theme.EmberFont
import com.unovapp.android.ui.theme.EmberMotion
import com.unovapp.android.ui.theme.UnovColors
import com.unovapp.android.ui.theme.breathe
import com.unovapp.android.ui.theme.UnovGradients
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.Close
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import java.io.File

/**
 * Entrée de la capture vidéo. Demande d'abord l'autorisation caméra (+ micro) à
 * l'utilisateur ; si refusée, propose l'import depuis la galerie. Aucune capture ne
 * démarre tant que l'utilisateur n'a pas explicitement accordé l'accès.
 */
@Composable
fun CameraCaptureScreen(
    onVideoCaptured: (Uri) -> Unit,
    onPickFromGallery: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    fun hasCamera() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    var cameraGranted by remember { mutableStateOf(hasCamera()) }
    var audioGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var asked by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        cameraGranted = result[Manifest.permission.CAMERA] == true || hasCamera()
        audioGranted = result[Manifest.permission.RECORD_AUDIO] == true
    }

    // Demande l'accès dès l'ouverture, une seule fois.
    LaunchedEffect(Unit) {
        if (!cameraGranted) {
            asked = true
            permLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (cameraGranted) {
            CameraRecorder(
                audioEnabled = audioGranted,
                onVideoCaptured = onVideoCaptured,
                onPickFromGallery = onPickFromGallery,
                onClose = onClose
            )
        } else {
            CameraPermissionPrompt(
                alreadyAsked = asked,
                onRequest = {
                    permLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                },
                onPickFromGallery = onPickFromGallery,
                onClose = onClose
            )
        }
    }
}

/** UI affichée tant que l'autorisation caméra n'est pas accordée. */
@Composable
private fun CameraPermissionPrompt(
    alreadyAsked: Boolean,
    onRequest: () -> Unit,
    onPickFromGallery: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(UnovColors.Accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Videocam, null, tint = UnovColors.Accent, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Accès à la caméra",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "UNOVAPP a besoin de ta caméra et de ton micro pour enregistrer une vidéo. " +
                "Tu peux aussi importer une vidéo existante.",
            color = UnovColors.TextDim,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
        Spacer(Modifier.height(24.dp))
        // Bouton principal : autoriser la caméra.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(UnovGradients.Gold)
                .unovTap(onClick = onRequest, pressedScale = 0.96f)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Autoriser la caméra", color = Color(0xFF0D0D0D), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        // Repli : importer depuis la galerie.
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .unovTap(onClick = onPickFromGallery, pressedScale = 0.96f)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.PhotoLibrary, null, tint = UnovColors.Text, modifier = Modifier.size(18.dp))
            Text("Importer une vidéo", color = UnovColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Fermer",
            color = UnovColors.TextMute,
            fontSize = 13.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .unovTap(onClick = onClose, pressedScale = 0.95f)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

private const val DEFAULT_DURATION_SEC = 60

/** Caméra live + enregistrement (CameraX). */
@Composable
private fun CameraRecorder(
    audioEnabled: Boolean,
    onVideoCaptured: (Uri) -> Unit,
    onPickFromGallery: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val recorder = remember {
        Recorder.Builder()
            // 1080p en priorité : la source uploadée fixe le plafond de qualité de TOUTE la
            // chaîne (transcodage compris). Repli automatique 720p/480p si l'appareil ne
            // supporte pas la FHD — jamais d'échec de config.
            .setQualitySelector(
                QualitySelector.fromOrderedList(
                    listOf(Quality.FHD, Quality.HD, Quality.SD),
                    FallbackStrategy.higherQualityOrLowerThan(Quality.FHD)
                )
            )
            .build()
    }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }
    var providerRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableStateOf(0L) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    // Le plan de tournage (écran 07) : la scène active et la durée choisie
    // (15 / 60 / 90 s) bornent l'enregistrement.
    var sceneIndex by remember { mutableStateOf(0) }
    var durationSec by remember { mutableStateOf(DEFAULT_DURATION_SEC) }
    val maxDurationMs = durationSec * 1000L

    // (Re)lie la caméra au cycle de vie à chaque changement d'objectif.
    LaunchedEffect(lensFacing) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            providerRef = provider
            // Oriente l'enregistrement selon l'affichage : téléphone tenu à la verticale →
            // vidéo stockée en 9:16 vertical (donc plein écran façon TikTok, pas une bande paysage).
            videoCapture.targetRotation = previewView.display?.rotation ?: android.view.Surface.ROTATION_0
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            try {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, videoCapture)
            } catch (e: Exception) {
                errorMsg = "Caméra indisponible. Importe une vidéo."
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Stoppe l'enregistrement et libère la caméra quand l'écran disparaît.
    DisposableEffect(Unit) {
        onDispose {
            recording?.stop()
            recording = null
            providerRef?.unbindAll()
        }
    }

    // Démarre / arrête l'enregistrement vers un fichier du cache.
    fun toggleRecording() {
        val active = recording
        if (active != null) {
            active.stop()
            recording = null
            return
        }
        val file = File(context.cacheDir, "unov_rec_${System.currentTimeMillis()}.mp4")
        var pending = videoCapture.output.prepareRecording(context, FileOutputOptions.Builder(file).build())
        if (audioEnabled) pending = pending.withAudioEnabled()
        recording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> { isRecording = true; elapsedMs = 0L }
                is VideoRecordEvent.Status -> {
                    elapsedMs = event.recordingStats.recordedDurationNanos / 1_000_000L
                    if (elapsedMs >= maxDurationMs) recording?.stop()
                }
                is VideoRecordEvent.Finalize -> {
                    isRecording = false
                    recording = null
                    val durMs = event.recordingStats.recordedDurationNanos / 1_000_000L
                    when {
                        // ERROR_NO_VALID_DATA / autres : fichier inexploitable.
                        event.hasError() -> {
                            errorMsg = "Échec de l'enregistrement. Réessaie."
                            runCatching { file.delete() }
                        }
                        // Clip trop court → souvent non finalisable (durée nulle) → on rejette tôt.
                        durMs < 1_000L -> {
                            errorMsg = "Trop court — maintiens l'enregistrement au moins 1 seconde."
                            runCatching { file.delete() }
                        }
                        else -> onVideoCaptured(Uri.fromFile(file))
                    }
                }
            }
        }
    }

    CameraRecorderUi(
        previewView = previewView,
        isRecording = isRecording,
        elapsedMs = elapsedMs,
        errorMsg = errorMsg,
        sceneIndex = sceneIndex,
        durationSec = durationSec,
        onSelectScene = { sceneIndex = it },
        onSelectDuration = { durationSec = it },
        onToggleRecording = { toggleRecording() },
        onFlip = { lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK },
        onPickFromGallery = onPickFromGallery,
        onClose = onClose
    )
}

@Composable
private fun CameraRecorderUi(
    previewView: PreviewView,
    isRecording: Boolean,
    elapsedMs: Long,
    errorMsg: String?,
    sceneIndex: Int,
    durationSec: Int,
    onSelectScene: (Int) -> Unit,
    onSelectDuration: (Int) -> Unit,
    onToggleRecording: () -> Unit,
    onFlip: () -> Unit,
    onPickFromGallery: () -> Unit,
    onClose: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Aperçu caméra live.
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Barre haute : fermer + minuteur d'enregistrement.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .unovTap(onClick = onClose, pressedScale = 0.9f),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Close, "Fermer", tint = Color.White, modifier = Modifier.size(22.dp))
            }
            if (isRecording) {
                // Le direct : pastille braise qui bat, pas un rouge d'alerte (§Écran 10).
                Row(
                    modifier = Modifier
                        .breathe(amplitude = 1.06f, minAlpha = 0.8f, periodMs = 1800)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Ember.Braise.copy(alpha = 0.9f))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Ember.Text))
                    Text(
                        formatElapsed(elapsedMs), color = Ember.Text,
                        fontFamily = EmberFont, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // Titre « Studio » de la maquette (écran 07).
                Text(
                    "Studio", color = Ember.Text,
                    fontFamily = EmberFont, fontSize = 20.sp, fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.size(40.dp))
        }

        // ── TON RÉALISATEUR (écran 07) — l'intention d'abord, le plan ensuite.
        //    Panneau LAGUNE : tout ce qui vient de l'IA porte sa couleur.
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 58.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DemoBanner(
                "Écran de démonstration — le Réalisateur arrive avec l'IA (août)."
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Ember.Lagune.copy(alpha = 0.08f))
                    .border(1.dp, Ember.Lagune.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Outlined.AutoAwesome, null,
                        tint = Ember.Lagune, modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Ton réalisateur", color = Ember.Lagune,
                        fontFamily = EmberFont, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
                Text(
                    text = MOCK_REALISATEUR_IDEA,
                    color = Ember.Text,
                    fontFamily = EmberFont,
                    fontSize = 16.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    lineHeight = 21.sp
                )
                EmberBody(
                    MOCK_REALISATEUR_PLAN,
                    color = Ember.Lagune,
                    size = 13,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 2.dp)
                )
                // Le plan en cartes — la scène active est en braise.
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MOCK_SCENES.forEachIndexed { i, scene ->
                        SceneCard(
                            scene = scene,
                            selected = i == sceneIndex,
                            onClick = { onSelectScene(i) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (errorMsg != null) {
            Text(
                errorMsg,
                color = Color.White,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 260.dp, start = 24.dp, end = 24.dp)
            )
        }

        // ── Sélecteurs en pilules au-dessus des contrôles : scène active + durée 15/60/90.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 152.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MOCK_SCENES.forEachIndexed { i, scene ->
                    StudioPill(
                        label = scene.label,
                        selected = i == sceneIndex,
                        onClick = { onSelectScene(i) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 60, 90).forEach { d ->
                    StudioPill(
                        label = "${d}s",
                        selected = d == durationSec,
                        onClick = { onSelectDuration(d) }
                    )
                }
            }
        }

        // Barre basse : galerie · bouton enregistrement · flip caméra.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 32.dp, vertical = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Galerie
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .unovTap(onClick = onPickFromGallery, pressedScale = 0.9f),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.PhotoLibrary, "Galerie", tint = Color.White, modifier = Modifier.size(22.dp))
            }

            // Bouton d'enregistrement 84 px (écran 07) avec anneau de progression et
            // légende contextuelle (« 3 s · accroche »). L'anneau se remplit pendant
            // l'enregistrement jusqu'à la durée choisie ; au repos il annonce la scène.
            val scene = MOCK_SCENES.getOrElse(sceneIndex) { MOCK_SCENES.first() }
            val progress = if (isRecording) {
                (elapsedMs.toFloat() / (durationSec * 1000L)).coerceIn(0f, 1f)
            } else {
                scene.seconds / 90f
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .breathe(amplitude = 1.02f, minAlpha = 0.92f, enabled = !isRecording)
                        .size(EmberDim.RecordButton)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .unovTap(onClick = onToggleRecording, pressedScale = 0.92f),
                    contentAlignment = Alignment.Center
                ) {
                    // Anneau de progression braise — la seule jauge de l'app (§3.3).
                    Canvas(modifier = Modifier.size(EmberDim.RecordButton)) {
                        val stroke = 3.dp.toPx()
                        drawArc(
                            color = Ember.Braise,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                            topLeft = Offset(stroke / 2f, stroke / 2f),
                            size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                        )
                    }
                    // Pulsation rouge pendant l'enregistrement.
                    RecordingPulse(
                        isActive = isRecording,
                        modifier = Modifier.size(EmberDim.RecordButton)
                    )
                    Box(
                        modifier = Modifier
                            .size(if (isRecording) 32.dp else 58.dp)
                            .clip(RoundedCornerShape(if (isRecording) 8.dp else 29.dp))
                            .background(UnovColors.Accent)
                    )
                }
                Text(
                    text = if (isRecording) formatElapsed(elapsedMs) else "${scene.seconds}s · ${scene.label.lowercase()}",
                    color = Ember.Text,
                    fontFamily = EmberFont,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            // Flip caméra
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .unovTap(onClick = onFlip, pressedScale = 0.9f),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Cameraswitch, "Changer de caméra", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

/** Carte du plan de tournage — la scène active est en braise. */
@Composable
private fun SceneCard(
    scene: MockScene,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) Ember.Braise.copy(alpha = 0.22f) else Ember.Surface.copy(alpha = 0.6f))
            .border(1.dp, if (selected) Ember.Braise else Ember.Line, shape)
            .unovTap(onClick = onClick, pressedScale = 0.94f)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "${scene.index}",
            color = if (selected) Ember.Braise else Ember.TextMute,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = scene.label,
            color = Ember.Text,
            fontFamily = EmberFont,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${scene.seconds}s · ${scene.shot}",
            color = if (selected) Ember.BraiseClair else Ember.TextDim,
            fontSize = 9.sp,
            maxLines = 1
        )
    }
}

/** Pilule du sélecteur (scène ou durée) — la sélection est en braise. */
@Composable
private fun StudioPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) Ember.Braise else Color.Black.copy(alpha = 0.45f))
            .border(1.dp, if (selected) Ember.Braise else Color.White.copy(alpha = 0.22f), shape)
            .unovTap(onClick = onClick, pressedScale = 0.92f)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Ember.Text,
            fontFamily = EmberFont,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatElapsed(ms: Long): String {
    val totalSec = (ms / 1000L).toInt()
    return "0:%02d".format(totalSec.coerceIn(0, 99))
}
