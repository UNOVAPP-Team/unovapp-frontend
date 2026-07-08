# 📘 Guide Frontend — SPRINT 2 (COMPLET)

> **Pour l'équipe Frontend Android.** Tous les endpoints du Sprint 2, déployés et **validés en production**.
> ✅ **Sprint 2 terminé le 2026-07-02** : 10 des 11 items livrés (11, 13, 18, 19, 20 le 2026-06-28 ; 21, 17, 15, 14, 16 le 2026-07-02).
> ❌ **Item 12 abandonné** (décision actée) : pas de `GET /auth/me` léger séparé — `GET /users/me` reste la seule source du profil complet, pour éviter deux endpoints qui racontent la même chose.
> Guide complet : `GUIDE_FRONTEND.md` · Sprint 1 : `GUIDE_FRONTEND_SPRINT1.md`.

## 🌍 Base & auth
```
http://152.239.118.90/api/v1
```
- Auth : `Authorization: Bearer <accessToken>` sur les endpoints protégés.

---

## 🔄 Changements vs Sprint 1

| Élément | Changement |
|---|---|
| **Commentaires** (`GET /videos/:id/comments`) | `likes_count` et `is_liked` deviennent **réels** (avant : toujours 0/false). Le **like de commentaire est maintenant actif** (item 11). |
| **Nouveaux endpoints (vague 1, 28/06)** | like commentaire, signalement (vidéo + commentaire), partage, check username, centres d'intérêt. |
| **Profil** (`GET /users/:id`) | `is_blocked` devient **réel** (avant : toujours `false`) — item 14. |
| **Feed** (`GET /feed`) | Exclut désormais automatiquement les vidéos des créateurs bloqués/qui vous ont bloqué — aucun changement de code frontend nécessaire, c'est filtré côté serveur — item 14. |
| **Commentaires** (`POST .../comments`) | Peut désormais renvoyer `403` si l'auteur de la vidéo vous a bloqué (ou l'inverse) — item 14. |
| **Connexion** (`POST /auth/login`, etc.) | ⚠️ **Changement de comportement important** : se connecter sur un nouvel appareil **ne déconnecte plus** les autres appareils (avant : mono-session, un seul appareil connecté à la fois). `POST /auth/logout` ne déconnecte que l'appareil courant. Nouveaux endpoints pour gérer les sessions actives — item 16. |
| **Nouveaux endpoints (vague 2, 02/07)** | alias vérif téléphone, miniature vidéo personnalisée, changement d'email, blocage utilisateur, sessions actives. |

---

## 11. Liker un commentaire — `POST /videos/:id/comments/:commentId/like`
- **Auth** : **requise** · toggle (like/unlike).
- **Réponse 200** : `{ "liked": true, "likes_count": 1 }` (2ᵉ appel → `{ "liked": false, "likes_count": 0 }`).
- **Erreurs** : `401` · `404` commentaire introuvable.
- > Met à jour `likes_count` du commentaire (visible dans `GET /videos/:id/comments`).

## 13. Signaler du contenu
### `POST /videos/:id/report` — signaler une vidéo
### `POST /videos/:id/comments/:commentId/report` — signaler un commentaire
- **Auth** : **requise**.
- **Body** : `{ "reason": "spam"|"violence"|"nudity"|"harassment"|"other", "description"?: string(≤500) }`.
- **Réponse 200/201** : `{ "reported": true }`.
- **Erreurs** : `400` reason invalide · `401`.

## 18. Tracking des partages — `POST /videos/:id/share`
- **Auth** : **optionnelle** (Bearer non requis).
- **Réponse 200** : `{ "shares_count": 4 }` (compteur incrémenté).
- **Erreurs** : `404` vidéo introuvable.
- > `shares_count` est aussi renvoyé dans chaque item du feed.

## 19. Disponibilité d'un username — `GET /users/check?username=isma`
- **Auth** : **non**.
- **Query** : `username` (3 à 20 caractères).
- **Réponse 200** : `{ "available": true }` (`false` si déjà pris **ou** longueur invalide).
- > À appeler en temps réel pendant l'inscription / le changement de pseudo.

## 20. Centres d'intérêt (onboarding)
### `POST /users/me/interests` — définir (remplace l'existant)
- **Auth** : **requise** · **Body** : `{ "categories": string[] }`.
- **Catégories autorisées** : `musique, sport, cuisine, culture, mode, humour, education, religion, politique, tech`.
- **Réponse 201** : `{ "categories": ["musique","sport"] }` (filtrées sur la liste autorisée, minuscules, dédupliquées ; max 10).
### `GET /users/me/interests` — récupérer
- **Auth** : **requise** · **Réponse 200** : `{ "categories": ["musique","sport"] }`.
- > Sert à initialiser le feed des nouveaux utilisateurs (à venir).

---

## 21. Alias vérification téléphone
Mêmes endpoints que `POST /auth/send-otp` / `POST /auth/verify-otp`, juste un nom plus explicite. **Aucune différence de comportement**, choisissez l'un ou l'autre nom.

### `POST /auth/send-phone-otp`
- **Auth** : non · **Rate limit** : 5/min.
- **Body** : `{ "phone_number": "0156245050" }` (format local `0XXXXXXXXX` ou international `+2290XXXXXXXXX`).
- **Réponse 200** : `{ "message": "Code OTP envoyé par SMS" }` (code à 6 chiffres, valable 5 min).
- **Erreurs** : `400` numéro invalide · `429` trop de demandes.

### `POST /auth/verify-phone`
- **Auth** : **requise**.
- **Body** : `{ "phone_number": "0156245050", "code": "123456" }`.
- **Réponse 200** : `{ "verified": true }`.
- **Erreurs** : `400` code invalide/expiré · `401`.

---

## 17. Miniature vidéo personnalisée
Même flux presign → upload direct S3 → confirmation que l'avatar/couverture de profil.

### `POST /videos/:id/thumbnail/presign` — obtenir l'URL d'upload
- **Auth** : **requise**, créateur de la vidéo uniquement.
- **Body** : `{ "contentType": "image/jpeg" }` (`image/jpeg`, `image/png` ou `image/webp` uniquement).
- **Réponse 200** :
  ```json
  {
    "key": "videos/<videoId>/custom_thumb.jpg",
    "uploadUrl": "https://.../unovapp-videos/videos/<id>/custom_thumb.jpg?X-Amz-Signature=...",
    "method": "PUT",
    "contentType": "image/jpeg",
    "publicUrl": "https://.../videos/<id>/custom_thumb.jpg",
    "expiresIn": 300
  }
  ```
- Le client fait ensuite un **PUT direct** sur `uploadUrl` avec le fichier image (pas via l'API UNOVAPP), header `Content-Type` = celui envoyé au presign.
- **Erreurs** : `400` type non supporté · `403` vous n'êtes pas le créateur · `404` vidéo introuvable.

### `PUT /videos/:id/thumbnail` — confirmer l'upload
- **Auth** : **requise**, créateur de la vidéo uniquement.
- **Body** : `{ "key": "videos/<videoId>/custom_thumb.jpg" }` (la `key` reçue au presign, telle quelle).
- **Réponse 200** : l'objet vidéo complet mis à jour (`thumbnail_url` pointe maintenant vers la nouvelle image — remplace immédiatement la miniature auto-générée).
- **Erreurs** : `403` clé invalide pour cette vidéo ou vous n'êtes pas le créateur · `404`.
- > Un nouvel upload **remplace** simplement l'ancienne miniature (même nom de fichier `custom_thumb.<ext>`) — pas besoin de gérer plusieurs versions.

---

## 15. Changement d'email
Flux en 2 temps, calqué sur le reset password : un code est envoyé sur la **nouvelle** adresse (pas l'ancienne) pour prouver qu'elle appartient bien à l'utilisateur.

### `POST /auth/change-email` — démarrer le changement
- **Auth** : **requise** · **Rate limit** : 5/min.
- **Body** : `{ "newEmail": "nouveau@example.com" }`.
- **Réponse 200** : `{ "message": "Code de vérification envoyé à nouveau@example.com" }`.
- **Erreurs** : `400` adresse identique à l'email actuel · `409` email déjà utilisé par un autre compte · `429`.

### `POST /auth/verify-email-change` — confirmer avec le code
- **Auth** : **requise** · **Rate limit** : 5/min.
- **Body** : `{ "otp_code": "123456" }` (code reçu sur la **nouvelle** adresse, valable 15 min, 5 tentatives max).
- **Réponse 200** : `{ "email": "nouveau@example.com" }` — l'email de connexion est changé, `GET /users/me` reflète le nouvel email dès l'appel suivant.
- **Erreurs** : `400` aucune demande en cours / code expiré / code incorrect / trop de tentatives · `409` email pris entre-temps par quelqu'un d'autre.

---

## 14. Blocage utilisateur

### `POST /users/:id/block` — bloquer
- **Auth** : **requise**.
- **Réponse 200** : `{ "message": "Utilisateur bloqué avec succès" }` (idempotent : rebloquer ne fait rien de plus).
- **Erreurs** : `400` impossible de se bloquer soi-même · `404` utilisateur cible introuvable.
- > Rompt automatiquement tout suivi (follow) existant, dans les deux sens.

### `DELETE /users/:id/block` — débloquer
- **Auth** : **requise**.
- **Réponse 200** : `{ "message": "Utilisateur débloqué" }`.

### Effets sur le reste de l'API (rien à coder côté app, tout est filtré serveur)
- `GET /users/:id` → `is_blocked` reflète maintenant la vraie relation (avez-vous bloqué cette personne ?).
- `GET /feed` (pour vous et "following") → les vidéos des utilisateurs bloqués/qui vous ont bloqué **n'apparaissent plus**, dans les deux sens.
- `POST /videos/:id/comments` → **`403`** si le créateur de la vidéo vous a bloqué ou si vous l'avez bloqué.
- `GET /videos/:id/comments` → les commentaires d'un utilisateur bloqué sont **masqués** de la liste.

---

## 16. Sessions actives (multi-appareils)

> ⚠️ **Changement de comportement à connaître** : avant ce Sprint, se connecter sur un nouvel appareil déconnectait silencieusement tous les autres (une seule session possible). **Ce n'est plus le cas** : chaque connexion (`login`, vérification d'email à l'inscription, Google) crée sa **propre session**, indépendante des autres. `POST /auth/logout` ne déconnecte que l'appareil courant.
> Aucun changement de code requis pour `login`/`refresh`/`logout` — le comportement change tout seul côté serveur. Les deux endpoints ci-dessous sont **nouveaux**, à intégrer dans un écran "Sessions actives" / "Appareils connectés" des paramètres du compte si souhaité.

### `GET /auth/sessions` — lister les appareils connectés
- **Auth** : **requise**.
- **Réponse 200** :
  ```json
  [
    { "id": "24292f0c-...", "device_info": "okhttp/4.12.0", "created_at": "2026-07-02T14:19:55.460Z", "last_used_at": "2026-07-02T15:02:11.002Z", "is_current": true },
    { "id": "d5bb438b-...", "device_info": "okhttp/4.12.0", "created_at": "2026-06-30T09:10:03.221Z", "last_used_at": "2026-07-01T08:44:20.918Z", "is_current": false }
  ]
  ```
- `device_info` = header `User-Agent` envoyé à la connexion (pensez à envoyer un User-Agent parlant côté app, ex. `"UNOVAPP-Android/1.2 (Pixel 7)"`, pour que l'utilisateur reconnaisse ses appareils dans la liste).
- `is_current` = la session utilisée pour cet appel précis.
- Ne renvoie **jamais** de token ni de hash.

### `DELETE /auth/sessions/:id` — déconnecter un appareil à distance
- **Auth** : **requise**.
- **Réponse 200** : `{ "message": "Session révoquée" }` — l'appareil visé est immédiatement déconnecté (son refresh token cesse de fonctionner).
- **Erreurs** : `400` session introuvable, déjà expirée, ou n'appartenant pas à vous.
- **Cas d'usage typique** : "J'ai perdu mon téléphone" → l'utilisateur se connecte depuis un autre appareil, liste ses sessions, révoque celle du téléphone perdu.

### Rappel sécurité (déjà en place, pas un changement)
`PATCH /auth/change-password` et `POST /auth/reset-password` déconnectent **tous** les appareils (pas seulement le courant) — comportement de sécurité volontaire, inchangé dans son intention mais désormais correct pour du multi-appareils.

---

## 📱 Exemples Kotlin (Retrofit)
```kotlin
@POST("videos/{id}/comments/{commentId}/like")
suspend fun likeComment(@Path("id") id: String, @Path("commentId") commentId: String): LikeResponse // { liked, likes_count }

@POST("videos/{id}/report")
suspend fun reportVideo(@Path("id") id: String, @Body body: ReportBody)
data class ReportBody(val reason: String, val description: String? = null)

@POST("videos/{id}/share")
suspend fun share(@Path("id") id: String): ShareResponse // { shares_count }

@GET("users/check")
suspend fun checkUsername(@Query("username") username: String): AvailabilityResponse // { available }

@POST("users/me/interests")
suspend fun setInterests(@Body body: InterestsBody): InterestsBody
data class InterestsBody(val categories: List<String>)

// ── Vague 2 (02/07) ──────────────────────────────────────

@POST("auth/send-phone-otp")
suspend fun sendPhoneOtp(@Body body: PhoneBody): MessageResponse
@POST("auth/verify-phone")
suspend fun verifyPhone(@Body body: VerifyPhoneBody): VerifiedResponse
data class PhoneBody(val phone_number: String)
data class VerifyPhoneBody(val phone_number: String, val code: String)

@POST("videos/{id}/thumbnail/presign")
suspend fun presignThumbnail(@Path("id") id: String, @Body body: PresignBody): PresignResponse
@PUT("videos/{id}/thumbnail")
suspend fun confirmThumbnail(@Path("id") id: String, @Body body: ConfirmThumbnailBody): VideoResponse
data class PresignBody(val contentType: String) // image/jpeg | image/png | image/webp
data class ConfirmThumbnailBody(val key: String)

@POST("auth/change-email")
suspend fun changeEmail(@Body body: ChangeEmailBody): MessageResponse
@POST("auth/verify-email-change")
suspend fun verifyEmailChange(@Body body: OtpCodeBody): EmailResponse
data class ChangeEmailBody(val newEmail: String)
data class OtpCodeBody(val otp_code: String)

@POST("users/{id}/block")
suspend fun blockUser(@Path("id") id: String): MessageResponse
@DELETE("users/{id}/block")
suspend fun unblockUser(@Path("id") id: String): MessageResponse

@GET("auth/sessions")
suspend fun listSessions(): List<SessionResponse>
@DELETE("auth/sessions/{id}")
suspend fun revokeSession(@Path("id") id: String): MessageResponse
data class SessionResponse(
    val id: String,
    val device_info: String?,
    val created_at: String,
    val last_used_at: String,
    val is_current: Boolean,
)
```

## 📋 Codes d'erreur
| Code | Cas |
|---|---|
| `400` | reason de signalement invalide · numéro/code téléphone invalide · type de miniature non supporté · email identique à l'actuel · code email expiré/incorrect/trop de tentatives · se bloquer soi-même · session introuvable/pas la vôtre |
| `401` | token requis manquant/expiré |
| `403` | pas le créateur de la vidéo (miniature) · clé de miniature invalide · **bloqué par l'auteur de la vidéo (ou l'inverse) en commentant** |
| `404` | vidéo / commentaire / utilisateur cible introuvable |
| `409` | nouvel email déjà utilisé (par un autre compte, ou pris entre-temps) |
| `429` | trop de demandes (rate limit) |

> ✅ Tous les endpoints ci-dessus (vague 1 **et** vague 2) ont été testés en production, y compris un scénario complet multi-appareils pour les sessions actives (connexions simultanées, refresh, logout ciblé, révocation à distance).

---

## ✅ Sprint 2 — bilan final
Tous les items livrés sauf le 12 (abandonné sciemment, cf. bandeau en haut de page). Le **Sprint 3** (items 22-35 : stats, hashtags indexés, notifications in-app, mentions, épingler un commentaire, contenus similaires, site web du profil, cache HTTP, i18n des erreurs, health enrichi, index BDD, recherche améliorée) n'a pas encore démarré.
