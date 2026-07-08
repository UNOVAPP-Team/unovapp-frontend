# 📘 Guide Frontend — SPRINT 3 (COMPLET)

> **Pour l'équipe Frontend Android.** Tous les endpoints du Sprint 3, **déployés et validés en production le 2026-07-04**.
> ✅ **14/14 items livrés** (22 à 35) — item 34 (`DB_SYNC=false`) était déjà fait avant le sprint (Phase 2).
> Guide complet : `GUIDE_FRONTEND.md` · Sprint 1 : `GUIDE_FRONTEND_SPRINT1.md` · Sprint 2 : `GUIDE_FRONTEND_SPRINT2.md`.

## 🌍 Base & auth
```
http://152.239.118.90/api/v1
```
- Auth : `Authorization: Bearer <accessToken>` sur les endpoints protégés.
- 🌍 **Nouveau** : tous les messages d'erreur peuvent être obtenus en anglais via `?lang=en` ou l'en-tête `Accept-Language: en` (item 30, détails plus bas).

---

## 🔄 Changements vs Sprint 2

| Élément | Changement |
|---|---|
| **Profil** (`GET /users/:id`, `GET /users/me`) | Nouveaux champs : `website_url`, `videos_count`, `total_views_received`, `total_likes_received`, `total_comments_received`, `total_shares_received` — items 22 et 28. |
| **PATCH `/users/:id`** | Accepte désormais `website_url` (URL avec schéma `http(s)://` obligatoire) — item 28. |
| **Notifications** | Toute nouvelle route `/notifications` — historique in-app des push déjà reçus (like, commentaire, mention, follow, vidéo prête/échouée...) — item 23. |
| **Vidéos / feed** | Hashtags désormais **indexés réellement** (tables dédiées) → nouveau endpoint `GET /hashtags/:name/videos` et recherche `#tag` plus précise — item 24. |
| **Commentaires** (`POST .../comments`, `GET .../comments`) | Nouveau champ `mentions: string[]` (les `@username` détectés dans le texte) ; les utilisateurs mentionnés reçoivent une notification — item 25. |
| **Commentaires** | Nouvel endpoint pour épingler un commentaire (créateur de la vidéo uniquement) ; `GET .../comments` trie désormais le commentaire épinglé en premier — item 26. |
| **Vidéos** | Nouvel endpoint `GET /videos/:id/related` — vidéos "liées" (hashtags communs, même créateur, ou tendance) — item 27. |
| **Feed / profil** | Nouveaux en-têtes `Cache-Control` + `ETag` sur `GET /feed`, `GET /videos/:id`, `GET /users/:id`, `GET /users/me` — un `304` sans body si rien n'a changé (économie de données en 2G/3G) — item 29. |
| **Recherche** (`GET /search`) | Nouveaux filtres `hashtag=`, `min_duration=`, `max_duration=`, `sort=` combinables avec `q=` — item 33. |
| **Tous les endpoints** | Messages d'erreur traduisibles en anglais (`?lang=en`) — item 30. Health checks enrichis (utile pour le monitoring, pas pour l'app) — item 31. |

---

## 22. Stats créateur (vues/likes/commentaires/partages)
Pas de nouvel endpoint — `GET /users/:id` et `GET /users/me` renvoient 4 nouveaux champs (agrégats sur les vidéos publiées du créateur) :
```json
{
  "videos_count": 12,
  "total_views_received": 5200,
  "total_likes_received": 340,
  "total_comments_received": 87,
  "total_shares_received": 23
}
```

---

## 23. Notifications in-app

### `GET /notifications?cursor=&limit=` — historique
- **Auth** : **requise**.
- **Query** : `cursor` (pagination) · `limit` (défaut 20, max 50).
- **Réponse 200** :
  ```json
  {
    "data": [{
      "id": "uuid", "type": "video.liked", "title": "akossi a aimé ta vidéo ❤️",
      "body": "Ma vidéo de test", "data": { "type": "video_liked", "video_id": "uuid" },
      "is_read": false, "created_at": "2026-07-03T10:00:00Z"
    }],
    "next_cursor": "opaque_ou_vide", "has_more": false, "unread_count": 3
  }
  ```
- Une entrée est créée pour **chaque** push envoyé (même si le push FCM lui-même échoue) — l'historique in-app est toujours fiable, utilisez `unread_count` pour le badge.

### `PATCH /notifications/:id/read` — marquer une notification comme lue
- **Auth** : **requise**, scopée par propriétaire.
- **Réponse 200** : `{ "message": "Notification marquée comme lue" }`.
- **Erreurs** : `403` si la notification n'est pas la vôtre ou n'existe pas.

### `POST /notifications/read-all` — tout marquer comme lu
- **Auth** : **requise** · **Réponse 200** : `{ "message": "..." }`.

---

## 24. Hashtags — indexation réelle

### `GET /hashtags/:name/videos?cursor=&limit=` — feed par hashtag
- **Auth** : optionnelle (comme `/feed`) · `:name` avec ou sans `#` (`benin` ou `#benin`).
- **Réponse 200** : même forme qu'un item de `/feed`, cursor pagination.
- Ne renvoie que les vidéos **publiées et publiques** taguées avec ce hashtag exact.

### `GET /search?q=#benin`
- Toujours disponible, équivalent à `GET /search?hashtag=benin` (item 33).

---

## 25. Mentions `@username` dans les commentaires
Aucun nouvel endpoint — `POST /videos/:id/comments` et `GET /videos/:id/comments` renvoient désormais `mentions: string[]` :
```json
{ "id": "uuid", "content": "Trop fort @akossi_creator 🔥", "mentions": ["akossi_creator"], ... }
```
- Parsing uniquement (pas de vérification d'existence côté client). Si le username correspond à un compte actif existant (et que ce n'est pas l'auteur lui-même), cette personne reçoit une notification `comment_mentioned` (push + in-app, item 23).

---

## 26. Épingler un commentaire

### `PATCH /videos/:id/comments/:commentId/pin`
- **Auth** : **requise**, réservé au **créateur de la vidéo** (pas à l'auteur du commentaire).
- **Réponse 200** : `{ "is_pinned": true }` (toggle — un 2ᵉ appel désépingle).
- Un seul commentaire épinglé à la fois par vidéo — en épingler un nouveau désépingle automatiquement l'ancien.
- `GET /videos/:id/comments` trie désormais le commentaire épinglé **en premier**, peu importe sa date.
- **Erreurs** : `401` · `403` si vous n'êtes pas le créateur · `404`.

---

## 27. Vidéos liées

### `GET /videos/:id/related?limit=`
- **Auth** : optionnelle (comme `/feed`) · **Query** : `limit` (défaut 10, max 20).
- **Réponse 200** : `{ "data": [...mêmes champs qu'un item de /feed...] }` — **pas de pagination curseur**, liste ponctuelle à taille fixe.
- Pas de vraie IA de recommandation (à venir) : heuristique — (1) vidéos partageant un hashtag, (2) autres vidéos du même créateur, (3) vidéos tendance pour compléter si besoin.
- **Erreurs** : `404` si la vidéo cible n'existe pas.

---

## 28. Site web sur le profil (`website_url`)
`PATCH /users/:id` accepte un nouveau champ optionnel :
```json
{ "website_url": "https://akossi.com" }
```
- Doit être une URL valide **avec schéma** (`http://` ou `https://` obligatoire) — sinon `400`.
- Exposé par `GET /users/:id` et `GET /users/me` (`null` si non renseigné).

---

## 29. Cache HTTP (Cache-Control + ETag)
`GET /feed`, `GET /videos/:id`, `GET /users/:id`, `GET /users/me` renvoient désormais :
```
Cache-Control: private, max-age=30   (ou public si la réponse ne dépend pas de votre JWT)
ETag: "a1b2c3..."
```
- Renvoyez l'en-tête `If-None-Match: <etag reçu>` sur l'appel suivant : si rien n'a changé, l'API répond `304` **sans body** — économie de données appréciable en 2G/3G. La plupart des libs HTTP Android (OkHttp avec un cache configuré) gèrent ça automatiquement si vous activez le cache HTTP standard.

---

## 30. Messages d'erreur en anglais
Ajoutez `?lang=en` à n'importe quelle requête, ou l'en-tête `Accept-Language: en`, pour recevoir le champ `message` des erreurs en anglais plutôt qu'en français. Par défaut (aucun des deux) : français, comportement inchangé.
⚠️ Traduction **best-effort** (dictionnaire des messages les plus courants) — ne faites jamais de correspondance exacte de texte côté app, basez votre logique sur le code HTTP (et `code` quand il est présent).

---

## 31. Health checks enrichis
Pas pertinent pour l'app — utile pour le monitoring uniquement. `GET .../health` de chaque service renvoie maintenant `dependencies: { database, redis, rabbitmq }` (`"up"`/`"down"`) et un `503` si un composant est en panne, au lieu d'un simple `{"status":"ok"}` qui ne vérifiait rien de réel.

---

## 32. Index BDD (invisible côté app)
Optimisations internes (temps de réponse), aucun changement de contrat API.

---

## 33. Recherche avancée

### `GET /search?q=&hashtag=&min_duration=&max_duration=&sort=`
- `hashtag` : sans le `#` — équivalent à `q=#tag`.
- `min_duration` / `max_duration` : en secondes, bornes incluses.
- `sort` : `recent` (plus récent d'abord) ou `popular` (défaut, `likes_count` décroissant).
- Combinables entre eux **et** avec `q` (texte libre sur la description).
- Dès qu'un `hashtag` ou une durée est fourni, la réponse ne contient **jamais** de `users` (ces filtres ne concernent que des vidéos).
- Exemple : `GET /search?q=dance&hashtag=benin&min_duration=15&sort=recent`.

---

## 34. `DB_SYNC=false`
Aucun changement côté app — fait en Phase 2, avant même le début de ce sprint.

## 35. Audit sécurité
Aucun changement de contrat API — durcissement interne (vérification JWT). Rapport complet : `RAPPORT_AUDIT_SECURITE_SPRINT3.md` (repo backend, pas pertinent pour l'intégration frontend).

---

## 📱 Exemples Kotlin (Retrofit)
```kotlin
// ── Notifications (item 23) ─────────────────────────────
@GET("notifications")
suspend fun getNotifications(@Query("cursor") cursor: String? = null, @Query("limit") limit: Int = 20): NotificationsResponse
data class NotificationsResponse(val data: List<NotificationItem>, val next_cursor: String, val has_more: Boolean, val unread_count: Int)
data class NotificationItem(val id: String, val type: String, val title: String, val body: String, val data: Map<String, String>, val is_read: Boolean, val created_at: String)

@PATCH("notifications/{id}/read")
suspend fun markNotificationRead(@Path("id") id: String): MessageResponse
@POST("notifications/read-all")
suspend fun markAllNotificationsRead(): MessageResponse

// ── Hashtags (item 24) ───────────────────────────────────
@GET("hashtags/{name}/videos")
suspend fun getHashtagFeed(@Path("name") name: String, @Query("cursor") cursor: String? = null): FeedResponse

// ── Pin commentaire (item 26) ────────────────────────────
@PATCH("videos/{id}/comments/{commentId}/pin")
suspend fun togglePinComment(@Path("id") videoId: String, @Path("commentId") commentId: String): PinResponse
data class PinResponse(val is_pinned: Boolean)

// ── Vidéos liées (item 27) ───────────────────────────────
@GET("videos/{id}/related")
suspend fun getRelatedVideos(@Path("id") id: String, @Query("limit") limit: Int = 10): RelatedResponse
data class RelatedResponse(val data: List<FeedItem>)

// ── website_url (item 28) : ajouter au body de PATCH /users/{id} existant ──
data class UpdateProfileBody(
    val display_name: String? = null,
    val bio: String? = null,
    val website_url: String? = null, // NOUVEAU
)

// ── Recherche avancée (item 33) ──────────────────────────
@GET("search")
suspend fun search(
    @Query("q") q: String? = null,
    @Query("hashtag") hashtag: String? = null,
    @Query("min_duration") minDuration: Int? = null,
    @Query("max_duration") maxDuration: Int? = null,
    @Query("sort") sort: String? = null, // "recent" | "popular"
): SearchResponse
```

## 📋 Codes d'erreur
| Code | Cas |
|---|---|
| `400` | `website_url` sans schéma http(s) |
| `401` | token requis manquant/expiré |
| `403` | notification d'un autre utilisateur · pas le créateur de la vidéo (pin) |
| `404` | notification / vidéo introuvable |

> ✅ Tous les endpoints ci-dessus ont été **testés en production** le 2026-07-04, y compris un backfill des hashtags sur les vidéos de démo existantes pour valider `GET /hashtags/:name/videos` avec de vraies données.

---

## ✅ Sprint 3 — bilan final
Les 14 items (22-35) sont livrés, déployés et validés en prod. **Prochaine étape : lancement v1.0** (Google Play, cf. `ROADMAP.md`) — pas de Sprint 4 planifié à ce jour, le backlog restant (Battle, Mobile Money, Live, IA GMM...) relève de la Phase 2+ hors MVP.
