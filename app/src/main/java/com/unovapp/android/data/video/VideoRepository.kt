package com.unovapp.android.data.video

import com.unovapp.android.BuildConfig
import com.unovapp.android.data.network.ApiError
import com.unovapp.android.data.network.NetworkResult
import com.unovapp.android.data.network.safeCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.InputStream

interface VideoRepository {
    suspend fun feed(cursor: String? = null, limit: Int = 10, type: String? = null): NetworkResult<FeedResponse>
    suspend fun getVideo(id: String): NetworkResult<FeedVideoDto>
    suspend fun createUploadSession(fileSize: Long, metadata: String? = null): NetworkResult<UploadSessionDto>
    suspend fun deleteVideo(id: String): NetworkResult<Unit>
    suspend fun relatedVideos(id: String, limit: Int = 10): NetworkResult<RelatedResponse>
    suspend fun hashtagVideos(name: String, cursor: String? = null): NetworkResult<FeedResponse>
    suspend fun updateVideo(
        id: String,
        description: String? = null,
        visibility: String? = null,
        allowComments: Boolean? = null,
        hashtags: List<String>? = null
    ): NetworkResult<FeedVideoDto>
    /** Miniature personnalisée : presign → PUT S3 → confirm. Retourne la vidéo à jour. */
    suspend fun uploadThumbnail(videoId: String, contentType: String, bytes: ByteArray): NetworkResult<FeedVideoDto>

    /**
     * Upload TUS résumable complet : crée la session puis envoie le fichier par chunks de
     * 512 Ko (`PATCH /videos/upload/:id`). Retourne le `video_id` de la vidéo créée
     * (status `processing` → suivre via [getVideo] jusqu'à `published`).
     *
     * @param length    taille totale du fichier en octets (header `Upload-Length`).
     * @param metadata  valeur `Upload-Metadata` (paires `clé <base64>` séparées par des virgules), ou null.
     * @param openStream fabrique un flux de lecture du fichier (appelée une fois).
     * @param onProgress fraction [0f..1f] d'octets envoyés.
     */
    suspend fun uploadVideo(
        length: Long,
        metadata: String?,
        openStream: () -> InputStream?,
        onProgress: (Float) -> Unit
    ): NetworkResult<String>

    /**
     * Poll `GET /videos/:id` jusqu'à ce que la vidéo soit `published` (ou `failed`), avec timeout.
     * Renvoie le DTO final si publiée, une erreur métier si `failed` ou timeout.
     */
    suspend fun awaitPublished(
        videoId: String,
        intervalMs: Long = 4_000L,
        timeoutMs: Long = 5 * 60_000L
    ): NetworkResult<FeedVideoDto>
}

class VideoRepositoryImpl(
    private val api: VideoApi,
    private val okHttpClient: OkHttpClient
) : VideoRepository {

    override suspend fun feed(cursor: String?, limit: Int, type: String?): NetworkResult<FeedResponse> =
        safeCall { api.feed(cursor, limit, type) }

    override suspend fun getVideo(id: String): NetworkResult<FeedVideoDto> =
        safeCall { api.getVideo(id) }

    override suspend fun relatedVideos(id: String, limit: Int): NetworkResult<RelatedResponse> =
        safeCall { api.relatedVideos(id, limit) }

    override suspend fun hashtagVideos(name: String, cursor: String?): NetworkResult<FeedResponse> =
        safeCall { api.hashtagVideos(name, cursor) }

    override suspend fun updateVideo(
        id: String, description: String?, visibility: String?, allowComments: Boolean?, hashtags: List<String>?
    ): NetworkResult<FeedVideoDto> = safeCall {
        api.updateVideo(id, UpdateVideoRequest(description, visibility, allowComments, hashtags))
    }

    override suspend fun uploadThumbnail(videoId: String, contentType: String, bytes: ByteArray): NetworkResult<FeedVideoDto> =
        safeCall {
            val presign = api.thumbnailPresign(videoId, ThumbnailPresignRequest(contentType))
            val reqBody = bytes.toRequestBody(contentType.toMediaType())
            val request = Request.Builder().url(presign.uploadUrl).put(reqBody).header("Content-Type", contentType).build()
            withContext(Dispatchers.IO) {
                okHttpClient.newCall(request).execute().use { r -> if (!r.isSuccessful) throw IOException("S3 PUT ${r.code}") }
            }
            api.thumbnailConfirm(videoId, ThumbnailConfirmRequest(presign.key))
        }

    override suspend fun createUploadSession(fileSize: Long, metadata: String?): NetworkResult<UploadSessionDto> =
        safeCall { api.createUploadSession(fileSize, metadata) }

    override suspend fun deleteVideo(id: String): NetworkResult<Unit> =
        safeCall { api.deleteVideo(id); Unit }

    override suspend fun uploadVideo(
        length: Long,
        metadata: String?,
        openStream: () -> InputStream?,
        onProgress: (Float) -> Unit
    ): NetworkResult<String> = safeCall {
        // 1. Crée la session TUS (Bearer ajouté par AuthInterceptor → créateur).
        val session = api.createUploadSession(length, metadata)
        // 2. URL de PATCH : utilise upload_url si absolue, sinon reconstruit depuis la passerelle.
        val patchUrl = if (session.uploadUrl.startsWith("http")) {
            session.uploadUrl
        } else {
            BuildConfig.VIDEO_BASE_URL.trimEnd('/') + "/videos/upload/" + session.uploadId
        }

        withContext(Dispatchers.IO) {
            val input = openStream() ?: throw IOException("Flux vidéo illisible")
            input.use { ins ->
                val buffer = ByteArray(CHUNK_SIZE)
                var offset = 0L
                while (true) {
                    // Remplit le buffer jusqu'à CHUNK_SIZE (read() peut renvoyer moins).
                    var filled = 0
                    while (filled < CHUNK_SIZE) {
                        val r = ins.read(buffer, filled, CHUNK_SIZE - filled)
                        if (r == -1) break
                        filled += r
                    }
                    if (filled == 0) break

                    val body = buffer.toRequestBody(OFFSET_OCTET_STREAM, 0, filled)
                    val req = Request.Builder()
                        .url(patchUrl)
                        .patch(body)
                        .header("Tus-Resumable", "1.0.0")
                        .header("Upload-Offset", offset.toString())
                        .header("Content-Type", "application/offset+octet-stream")
                        .build()
                    okHttpClient.newCall(req).execute().use { resp ->
                        // TUS renvoie 204 No Content (200 toléré). Tout le reste = échec.
                        if (resp.code != 204 && resp.code != 200) {
                            throw IOException("Upload chunk HTTP ${resp.code} @offset=$offset")
                        }
                    }
                    offset += filled
                    if (length > 0) onProgress((offset.toFloat() / length).coerceIn(0f, 1f))
                    if (filled < CHUNK_SIZE) break // dernier chunk
                }
            }
        }
        session.videoId
    }

    override suspend fun awaitPublished(
        videoId: String,
        intervalMs: Long,
        timeoutMs: Long
    ): NetworkResult<FeedVideoDto> {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            when (val r = getVideo(videoId)) {
                is NetworkResult.Success -> when (r.data.status.lowercase()) {
                    "published", "ready" -> return r
                    "failed", "error"    -> return NetworkResult.Failure(
                        ApiError.Business(
                            code = "TRANSCODE_FAILED",
                            userMessage = "Le traitement de la vidéo a échoué.",
                            httpStatus = 422
                        )
                    )
                    else -> { /* processing → on continue à poller */ }
                }
                // Erreur transitoire (réseau, 404 le temps que la vidéo apparaisse) → on retente.
                is NetworkResult.Failure -> { /* ignore et retente */ }
            }
            delay(intervalMs)
        }
        return NetworkResult.Failure(
            ApiError.Business(
                code = "TRANSCODE_TIMEOUT",
                userMessage = "La vidéo est toujours en traitement. Elle apparaîtra dans le feed une fois prête.",
                httpStatus = 408
            )
        )
    }

    companion object {
        /** Chunk TUS — max 512 Ko côté backend. */
        private const val CHUNK_SIZE = 512 * 1024
        private val OFFSET_OCTET_STREAM = "application/offset+octet-stream".toMediaType()
    }
}
