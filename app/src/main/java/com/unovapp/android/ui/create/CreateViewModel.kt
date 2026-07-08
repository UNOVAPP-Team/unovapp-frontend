package com.unovapp.android.ui.create

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.video.FeedRefreshBus
import com.unovapp.android.data.video.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Étapes de publication d'une vidéo. */
enum class PublishPhase { Idle, Uploading, Processing, Published, Failed }

data class CreateUiState(
    val phase: PublishPhase = PublishPhase.Idle,
    /** Progression de l'upload [0f..1f] (phase Uploading). */
    val progress: Float = 0f,
    val videoId: String? = null,
    val error: String? = null
)

/**
 * Pilote l'upload d'une vidéo vers le backend (TUS) puis le suivi du transcodage :
 *  pick (Uri) → upload chunks → status `processing` → poll → `published`.
 */
@HiltViewModel
class CreateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoRepository: VideoRepository,
    private val feedRefreshBus: FeedRefreshBus
) : ViewModel() {

    private val _state = MutableStateFlow(CreateUiState())
    val state: StateFlow<CreateUiState> = _state.asStateFlow()

    /**
     * Lance l'upload de [uri] avec une [description] optionnelle, puis attend la fin du
     * transcodage. L'UI observe [state] pour afficher progression / traitement / succès.
     */
    fun publish(uri: Uri, description: String) {
        if (_state.value.phase == PublishPhase.Uploading || _state.value.phase == PublishPhase.Processing) return
        viewModelScope.launch {
            _state.update { it.copy(phase = PublishPhase.Uploading, progress = 0f, error = null) }

            val cr = context.contentResolver
            val length = withContext(Dispatchers.IO) { resolveSize(uri) }
            if (length <= 0L) {
                _state.update { it.copy(phase = PublishPhase.Failed, error = "Vidéo illisible ou vide.") }
                return@launch
            }
            // Garde-fou : un enregistrement cassé (finalisation incomplète) a une durée nulle/illisible.
            // Le transcodeur backend échoue sur ces fichiers → on bloque AVANT l'upload, avec un
            // message clair, au lieu de subir un « échec » après plusieurs minutes.
            val durationMs = withContext(Dispatchers.IO) { videoDurationMs(uri) }
            if (durationMs < 700L) {
                _state.update { it.copy(
                    phase = PublishPhase.Failed,
                    error = "Vidéo invalide ou trop courte. Réenregistre une vidéo d'au moins 1 seconde."
                ) }
                return@launch
            }
            val type = cr.getType(uri) ?: "video/mp4"
            val name = withContext(Dispatchers.IO) { resolveName(uri) } ?: "video.mp4"
            val metadata = buildMetadata(name, type, description)

            val up = videoRepository.uploadVideo(
                length = length,
                metadata = metadata,
                openStream = { cr.openInputStream(uri) },
                onProgress = { p -> _state.update { it.copy(progress = p) } }
            )

            when (up) {
                is NetworkResult.Failure ->
                    _state.update { it.copy(phase = PublishPhase.Failed, error = up.error.userMessage) }
                is NetworkResult.Success -> {
                    val videoId = up.data
                    _state.update { it.copy(phase = PublishPhase.Processing, progress = 1f, videoId = videoId) }
                    when (val done = videoRepository.awaitPublished(videoId)) {
                        is NetworkResult.Success -> {
                            _state.update { it.copy(phase = PublishPhase.Published) }
                            // Vidéo publiée → recharge le feed pour qu'elle y apparaisse.
                            feedRefreshBus.signalNewVideo()
                        }
                        is NetworkResult.Failure ->
                            // Timeout de traitement n'est pas un échec d'upload : la vidéo finira
                            // par apparaître. On marque Published pour laisser l'utilisateur sortir,
                            // sauf échec réel de transcodage.
                            if ((done.error as? com.unovapp.android.data.network.ApiError.Business)?.code == "TRANSCODE_FAILED") {
                                _state.update { it.copy(phase = PublishPhase.Failed, error = done.error.userMessage) }
                            } else {
                                _state.update { it.copy(phase = PublishPhase.Published, error = done.error.userMessage) }
                            }
                    }
                }
            }
        }
    }

    fun reset() = _state.update { CreateUiState() }

    /* ---------- Helpers ---------- */

    private fun resolveSize(uri: Uri): Long {
        // 1. Taille exacte via le descripteur de fichier.
        runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { return it.statSize }
        }
        // 2. Repli sur la colonne SIZE du ContentResolver.
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0 && !c.isNull(idx)) return c.getLong(idx)
                }
            }
        }
        return -1L
    }

    /** Durée de la vidéo en ms (0 si illisible/cassée). Sert à bloquer les fichiers à durée nulle. */
    private fun videoDurationMs(uri: Uri): Long {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(context, uri)
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            runCatching { mmr.release() }
        }
    }

    private fun resolveName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return c.getString(idx)
            }
        }
        null
    }.getOrNull()

    /** Construit la valeur `Upload-Metadata` TUS : `clé <base64>` séparées par des virgules. */
    private fun buildMetadata(filename: String, filetype: String, description: String): String {
        fun b64(s: String) = Base64.encodeToString(s.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val pairs = mutableListOf(
            "filename ${b64(filename)}",
            "filetype ${b64(filetype)}"
        )
        if (description.isNotBlank()) pairs += "description ${b64(description.trim())}"
        return pairs.joinToString(",")
    }
}
