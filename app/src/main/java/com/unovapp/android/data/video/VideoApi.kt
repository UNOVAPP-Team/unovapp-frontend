package com.unovapp.android.data.video

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Service vidéo — passerelle unique `/api/v1/` (feed, métadonnées, upload TUS). Backend Go (Gin).
 *
 * Seuls les endpoints REST sans streaming sont ici.
 * L'upload TUS (PATCH chunked) passe directement par OkHttpClient dans [VideoRepository].
 */
interface VideoApi {

    /**
     * Feed principal — cursor-based. JWT optionnel (enrichit is_liked/is_saved/is_following).
     * type = "foryou" (public) | "following" (créateurs suivis — exige le Bearer).
     */
    @GET("feed")
    suspend fun feed(
        @Query("cursor") cursor: String? = null,
        @Query("limit")  limit: Int      = 10,
        @Query("type")   type: String?   = null
    ): FeedResponse

    /** Métadonnées complètes d'une vidéo (inclut hls_manifest_url + status). */
    @GET("videos/{id}")
    suspend fun getVideo(@Path("id") id: String): FeedVideoDto

    /** Édition d'une vidéo (créateur uniquement) : description, visibilité, commentaires. */
    @PATCH("videos/{id}")
    suspend fun updateVideo(@Path("id") id: String, @Body body: UpdateVideoRequest): FeedVideoDto

    /** Vidéos liées (hashtags communs, même créateur, tendance). Auth optionnelle. */
    @GET("videos/{id}/related")
    suspend fun relatedVideos(@Path("id") id: String, @Query("limit") limit: Int = 10): RelatedResponse

    /** Feed d'un hashtag (avec ou sans #). Auth optionnelle. */
    @GET("hashtags/{name}/videos")
    suspend fun hashtagVideos(
        @Path("name") name: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit")  limit: Int = 10
    ): FeedResponse

    /** Miniature personnalisée : URL pré-signée S3 (créateur uniquement). */
    @POST("videos/{id}/thumbnail/presign")
    suspend fun thumbnailPresign(@Path("id") id: String, @Body body: ThumbnailPresignRequest): ThumbnailPresignResponse

    /** Confirme la miniature uploadée sur S3 → vidéo à jour (thumbnail_url remplacé). */
    @PUT("videos/{id}/thumbnail")
    suspend fun thumbnailConfirm(@Path("id") id: String, @Body body: ThumbnailConfirmRequest): FeedVideoDto

    /**
     * Crée une session d'upload TUS.
     * Headers requis côté appelant : Upload-Length (taille totale en octets).
     */
    @POST("videos/upload")
    suspend fun createUploadSession(
        @Header("Upload-Length")   length: Long,
        @Header("Upload-Metadata") metadata: String? = null
    ): UploadSessionDto

    /** Supprime (soft delete) une vidéo — seul le créateur peut le faire. */
    @DELETE("videos/{id}")
    suspend fun deleteVideo(@Path("id") id: String)
}
