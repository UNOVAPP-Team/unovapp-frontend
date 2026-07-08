# 📘 Guide d'intégration API — UNOVAPP Backend

> **Contrat API pour l'équipe Frontend Android.**
> Généré le **2026-06-19**, **mis à jour le 2026-06-26** (migration Render → **VPS Hostinger**,
> passerelle unique). À partir du **code réel** des 6 services
> (`auth`, `user`, `social` en NestJS · `video` & `notification` en Go · `transcoding` en Python).
> Tu dois pouvoir tout intégrer sans poser de question : chaque endpoint a sa méthode,
> son chemin exact, son auth, ses paramètres typés, un exemple de réponse réelle et ses erreurs.

> ✅ **Chaîne vidéo validée end-to-end en production (2026-06-20).**
> Parcours prouvé par un vrai upload : `POST /videos/upload` (TUS) → envoi des chunks →
> transcodage **H.265 réel** → **4 renditions HLS** (144p/240p/480p/720p) + thumbnail sur
> Supabase → `status: published` → **vidéo lisible dans le feed** (`master.m3u8` HTTP 200,
> `CODECS="hvc1"`, segments `.ts` servis). **Vous pouvez intégrer l'upload et la lecture.**
> ⚠️ **Deux points à connaître** (sans gravité) : (1) le transcodage H.265 est **séquentiel
> sur 2 vCPU** (quelques minutes pour un clip court, davantage pour 90 s/720p) → prévoir un écran « traitement en cours » ;
> (2) **suivez l'avancement via `GET /api/v1/videos/:id`**, **PAS** via le service transcoding
> (non exposé par la passerelle). Voir l'encadré « Cycle de vie d'une vidéo ».

---

## 🌍 URL de base (production — VPS Frankfurt, **passerelle unique**)

> 🆕 **Depuis le 2026-06-26, tout passe par UNE seule URL.** Un reverse proxy Nginx route
> automatiquement vers le bon service selon le chemin. **Fini les 6 sous-domaines Render.**

```
http://152.239.118.90/api/v1
```

| Domaine fonctionnel | Préfixe de chemin | Service interne |
|---|---|---|
| Authentification | `/api/v1/auth/*` | auth (NestJS) |
| Profils & follow & avatar | `/api/v1/users/*` | user (NestJS) |
| Likes / commentaires / vues / recherche | `/api/v1/videos/:id/like`·`/comments`·`/view`, `/api/v1/search` | social (NestJS) |
| Vidéos (upload, métadonnées) & feed | `/api/v1/feed`, `/api/v1/videos/*` | video (Go) |

> ✅ **Tu n'as plus à savoir quel service héberge quoi** : une seule base URL, le serveur
> route en interne. Le « double hôte » pour `/videos/...` d'avant **n'existe plus** côté client.

> ✅ **Toujours allumé** : contrairement au plan Render Free, les services **ne se mettent
> plus en veille** — plus de réveil de 30-50 s, réponse immédiate à tout moment.

> ⚠️ **HTTP (pas encore HTTPS)** : pas encore de nom de domaine → le trafic est en clair.
> Sur Android, autorise le cleartext vers cette IP le temps qu'on ajoute un domaine + SSL :
> ```xml
> <!-- res/xml/network_security_config.xml -->
> <network-security-config>
>   <domain-config cleartextTrafficPermitted="true">
>     <domain includeSubdomains="false">152.239.118.90</domain>
>   </domain-config>
> </network-security-config>
> ```

> ℹ️ **Transcoding & Notification** ne sont **pas** exposés par la passerelle (ce sont des
> consumers RabbitMQ internes). Rien à appeler directement : suis l'avancement d'une vidéo
> via `GET /api/v1/videos/:id`. Le **Swagger** des services NestJS n'est pas exposé publiquement.

---

## 🔑 Convention générale

- **Header d'authentification** (tous les endpoints protégés) :
  ```
  Authorization: Bearer <accessToken>
  ```
- **Cycle de vie des tokens** :
  - `accessToken` : valable **1 h** (`JWT_SECRET`).
  - `refreshToken` : valable **7 jours** (`JWT_REFRESH_SECRET`), **rotation à chaque refresh**.
  - Sur un `401`, appelle `POST /auth/refresh` avec `{ refreshToken }`. Si le refresh
    échoue (401) → la session est morte → redirige vers l'écran de login.
- Le **même `JWT_SECRET`** est partagé par auth/user/social/video : un token émis par
  auth est accepté partout. Le payload JWT contient **`{ sub: userId, email }`** —
  ⚠️ **pas de `username`** (les services le résolvent côté serveur si besoin).
- **Préfixe** : `/api/v1` pour tous les endpoints exposés par la passerelle.
- **Pagination** : *cursor-based* pour les feeds/commentaires (`cursor` + `next_cursor` + `has_more`) ;
  *page-based* pour les listes users (`page` + `limit` + `total`).

### Format des erreurs (⚠️ il diffère selon la techno — c'est l'état réel du code)

| Techno | Services | Format réel |
|---|---|---|
| **NestJS** | auth, user, social | `{ "statusCode": 409, "message": "...", "error": "Conflict" }` — `message` peut être une **chaîne** OU un **tableau de chaînes** (erreurs de validation 400). |
| **Go (Gin)** | video | `{ "code": "NOT_FOUND", "message": "Vidéo introuvable" }` — certains endpoints renvoient seulement `{ "message": "..." }`. |
| **Python (FastAPI)** | transcoding | `{ "error": "Vidéo introuvable" }` (service interne). |

> 📌 La convention cible du projet est `{ code, message, details }` (cf. CLAUDE.md),
> mais elle **n'est pas encore uniformisée** dans le code. Code défensivement côté Android :
> lis `message` en priorité, et `code`/`error`/`statusCode` quand présents.

---

## 🔐 AUTH SERVICE — chemins `/api/v1/auth/*`

> Flow d'inscription = **OTP par email** : `register` ne crée PAS le compte, il envoie un
> code à 6 chiffres ; le compte n'est créé qu'après `verify-email`.

### POST /auth/register — démarrer l'inscription (envoie l'OTP email)
- **Auth** : Non
- **Body** :
  | Champ | Type | Obligatoire | Règles |
  |---|---|---|---|
  | `email` | string | Oui | format email valide |
  | `username` | string | Oui | 3 à 20 caractères |
  | `password` | string | Oui | ≥ 8 caractères |
  | `phone_number` | string | Non | ex. `0156245050` ou `+2290156245050` |
- **Réponse 200** : `{ "message": "Code envoyé à votre email" }`
- **Comportement** : ne crée **pas** encore le compte. OTP **6 chiffres valable 15 min**.
- **Erreurs** : `400` validation.

### POST /auth/verify-email — valider l'OTP et créer le compte
- **Auth** : Non
- **Body** : `{ "email": string, "otp_code": string (6 chiffres) }`
- **Réponse 200** (compte créé) : `{ "accessToken": "...", "refreshToken": "..." }`
- **Erreurs** : `400` (aucune inscription / code expiré / code incorrect) · `409` (email/username/téléphone déjà utilisé)

### POST /auth/login
- **Auth** : Non · **Rate limit : 5 / min**
- **Body** : `{ "email": string, "password": string }`
- **Réponse 200** : `{ "accessToken": "...", "refreshToken": "..." }`
- **Erreurs** : `401` identifiants incorrects · `429`

### POST /auth/refresh
- **Auth** : Non · **Body** : `{ "refreshToken": string }` ⚠️ uniquement ce champ
- **Réponse 200** : `{ "accessToken": "...", "refreshToken": "..." }` (rotation)
- **Erreurs** : `401` invalide / expiré / révoqué

### POST /auth/logout
- **Auth** : **Oui** · blackliste l'accessToken (Redis TTL 1 h) + efface le refresh serveur
- **Réponse 204** · **Erreurs** : `401`

### POST /auth/me
- **Auth** : **Oui** · **Réponse 200** : `{ "userId": "<uuid>", "email": "..." }`
- > Pour le profil complet → `GET /users/me`.

### POST /auth/send-otp — OTP **SMS** (vérification téléphone)
- **Auth** : Non · **Rate limit : 5 / min** · **Body** : `{ "phone_number": string }`
- **Réponse 200** : `{ "message": "Code OTP envoyé par SMS" }` (Infobip, code 5 min)

### POST /auth/verify-otp
- **Auth** : **Oui** · **Body** : `{ "phone_number": string, "code": string }`
- **Réponse 200** : `{ "verified": true }` · **Erreurs** : `400` / `401`

### POST /auth/forgot-password — code de réinitialisation (OTP email)
- **Auth** : Non · **Rate limit : 3 / min** · **Body** : `{ "email": string }`
- **Réponse 200** : réponse générique (anti-énumération). Code 6 chiffres 15 min si le compte existe (hors Google).

### POST /auth/reset-password — valider le code + nouveau mot de passe (1 étape)
- **Auth** : Non · **Rate limit : 5 / min**
- **Body** :
  | Champ | Type | Règles |
  |---|---|---|
  | `email` | string | email du compte |
  | `otp_code` | string | 6 chiffres |
  | `newPassword` | string | ≥ 8 caractères |
- **Réponse 200** : `{ "message": "Mot de passe réinitialisé avec succès" }`
- **Erreurs** : `400` (code invalide/expiré / incorrect / trop de tentatives — annulé après 5 essais) · `429`
- > Invalide toutes les sessions. 💡 Plus de lien email : **code OTP** (pensé mobile).

### PATCH /auth/change-password
- **Auth** : **Oui** · **Body** : `{ currentPassword, newPassword (≥8), confirmPassword }`
- **Réponse 200** : `{ "message": "..." }` · **Erreurs** : `400` / `401`

### POST /auth/google
- **Auth** : Non · **Body** : `{ "id_token": string }`
- **Réponse 200** : `{ accessToken, refreshToken, user, isNewUser }` (crée le compte si email Google inconnu)
- **Erreurs** : `400` / `401`

---

## 👤 USER SERVICE — chemins `/api/v1/users/*`

### GET /users/me — profil privé
- **Auth** : **Oui**
- **Réponse 200** :
  ```json
  {
    "id": "uuid", "username": "akossi_creator", "display_name": "Akossi Koffi",
    "bio": "...", "avatar_url": null, "followers_count": 1250, "following_count": 89,
    "is_verified": false, "subscription_tier": "free",
    "email": "akossi@example.com", "phone_number": "+2290156246963",
    "wallet_balance": 0, "created_at": "2026-05-01T10:00:00.000Z"
  }
  ```

### POST /users/me/avatar/presign — URL pré-signée d'upload avatar
- **Auth** : **Oui** · **Body** : `{ "contentType": "image/jpeg" | "image/png" | "image/webp" }`
- **Réponse 200** : `{ key, uploadUrl, method:"PUT", contentType, publicUrl, expiresIn }`
- **Erreurs** : `400` type non supporté · `401` · `503` S3 non configuré
- > Flow : `PUT` direct du fichier sur `uploadUrl`, puis confirmer via `PUT /users/me/avatar`.

### PUT /users/me/avatar — confirmer l'avatar uploadé
- **Auth** : **Oui** · **Body** : `{ "key": string }` · **Réponse 200** : profil avec `avatar_url`
- **Erreurs** : `401` · `403` · `404`

### GET /users/search?q=&page=&limit=
- **Auth** : Non · `q` min 2 car · `page` (1) · `limit` (20, max 50)
- **Réponse 200** : `{ "data": [...], "total", "page", "limit" }`

### GET /users/:id — profil public
- **Auth** : **Optionnelle** (`Bearer` → ajoute `is_following`)
- **Réponse 200** : comme `/users/me` sans email/phone/wallet, plus `is_following`
- **Erreurs** : `404`

### PATCH /users/:id — modifier son profil
- **Auth** : **Oui** — `:id` doit être **votre propre UUID** (sinon 403). ⚠️ Pas de `PATCH /users/me`.
- **Body** (partiel) :
  | Champ | Type | Règles |
  |---|---|---|
  | `display_name` | string | max 100 |
  | `bio` | string | max 500 |
  | `username` | string | 3 à 50 (409 si pris) |
  | `avatar_s3_key` | string | préférer le flux presign/confirm |

### POST /users/:id/follow · DELETE /users/:id/follow
- **Auth** : **Oui** · POST idempotent (aucune erreur si déjà suivi)
- **`:id`** = UUID de la cible — **récupéré depuis le feed (`creator_id`) OU depuis un profil (`id`)** : même endpoint dans les deux cas.
- **Erreurs** : `400` (se suivre soi-même) · `401` · `404`

### GET /users/:id/followers · GET /users/:id/following (?page=&limit=)
- **Auth** : Non · **Réponse 200** : liste paginée `{ data, total, page, limit }`

---

## 🎬 SOCIAL SERVICE — chemins `/api/v1/videos/:id/{like,comments,view}` + `/api/v1/search`

### POST /videos/:id/like — liker / unliker (toggle)
- **Auth** : **Oui** · **Réponse 201** : `{ "liked": true, "likes_count": "1" }`
- ⚠️ `likes_count` en **STRING** ici (parser en entier). Sur `GET /videos/:id` et `/feed` c'est un **nombre**.

### POST /videos/:id/comments — poster un commentaire
- **Auth** : **Oui** · **Body** : `{ content (1-500), parent_id? (UUID) }`
- **Réponse 201** : entité commentaire complète (avec `username` résolu serveur)
- **Erreurs** : `400` (>500) · `401` · `404` (parent introuvable)

### GET /videos/:id/comments?cursor=&limit=
- **Auth** : Non · **Réponse 200** : `{ data:[...], next_cursor, has_more }`

### DELETE /videos/:id/comments/:commentId
- **Auth** : **Oui** (auteur uniquement) · **Réponse 204** · **Erreurs** : `401` / `403` / `404`

### POST /videos/:id/view
- **Auth** : Non · **Réponse 200** : `{ "message": "Vue enregistrée" }`

### GET /search?q=&cursor=
- **Auth** : Non · `q` min 2 car · **Réponse 200** : `{ "videos": [...], "users": [...] }`

---

## 📹 VIDEO SERVICE — chemins `/api/v1/feed` + `/api/v1/videos/*`

> Upload **TUS résumable**, feed, métadonnées, manifest HLS. Service **Go (Gin)**.

### GET /feed?cursor=&limit=
- **Auth** : **Optionnelle** (`Bearer` → `is_liked`) · `limit` (10, max 50)
- **Réponse 200** :
  ```json
  {
    "data": [{
      "id": "uuid", "creator_id": "uuid", "description": "...",
      "duration_seconds": 42, "renditions": { "240p": "...", "480p": "..." },
      "thumbnail_url": "...", "likes_count": 120, "comments_count": 8,
      "views_count": 3400, "is_liked": false, "created_at": "..."
    }],
    "next_cursor": "uuid-ou-vide", "has_more": true
  }
  ```

### POST /videos/upload — créer une session TUS
- **Auth** : **Oui**
- **Headers** : `Upload-Length` (octets, max 500 Mo) · `Upload-Metadata` (optionnel, paires base64)
- **Réponse 201** (+ headers TUS) : `{ upload_id, video_id, upload_url, expires_at }`. Crée la vidéo `status:"processing"`.

### HEAD /videos/upload/:uploadID — état de l'upload (reprise)
- **Auth** : Non · **Réponse 200** : headers `Upload-Offset`, `Upload-Length`, `Tus-Resumable`

### PATCH /videos/upload/:uploadID — envoyer un chunk
- **Auth** : **Oui** (créateur)
- **Headers** : `Tus-Resumable: 1.0.0` · `Content-Type: application/offset+octet-stream` · `Upload-Offset` · `Upload-Checksum` (optionnel)
- **Body** : chunk binaire (max 512 Ko) · **Réponse 204** (à la fin → event `video.uploaded`)
- **Erreurs** : `412` / `415` / `400` / `403` / `404` / `413` / `409` (offset incorrect)

### GET /videos/:id — métadonnées
- **Auth** : Non
- **Réponse 200** :
  ```json
  {
    "id": "uuid", "creator_id": "uuid", "description": "...",
    "duration_seconds": 42, "status": "published",
    "renditions": { "144p": "...", "240p": "...", "480p": "...", "720p": "..." },
    "thumbnail_url": "...", "hls_manifest_url": ".../master.m3u8",
    "views_count": 3400, "likes_count": 120, "comments_count": 8, "created_at": "..."
  }
  ```

### GET /videos/:id/manifest — manifest HLS (lecture)
- **Auth** : Non · `302` vers le HLS si prêt · `202` si en cours · `404` sinon

### DELETE /videos/:id — supprimer (soft delete) sa vidéo
- **Auth** : **Oui** (créateur) · **Réponse 204** · **Erreurs** : `403`

---

## 🎞️ TRANSCODING & 🔔 NOTIFICATION — internes (non exposés par la passerelle)

> **Transcoding** (Python/FFmpeg) et **Notification** (Go) sont des **consumers RabbitMQ internes** :
> **rien à appeler depuis l'app**. Le transcodage génère les 4 renditions HLS + thumbnail
> (suivre via `GET /api/v1/videos/:id`). Les notifications partent en push FCM automatiquement.

### Push FCM — événements (pour info)
| Événement backend | `data.type` | Contenu |
|---|---|---|
| `video.transcoded` | `video_ready` | vidéo prête |
| `video.liked` | `video_liked` | like reçu |
| `video.commented` | `video_commented` | nouveau commentaire |
| `user.followed` | `new_follower` | nouvel abonné |

> 👉 **Ciblage par TOPIC** : après login, `FirebaseMessaging.getInstance().subscribeToTopic("user-" + userId)`.
> Aucun endpoint d'enregistrement de device token (MVP). ⚠️ Push en **simulation** tant que `FCM_PROJECT_ID` vide.

---

## 🚦 Statut des services (au 2026-06-27 — **VPS Hostinger**, Docker Compose)

> Infra : VPS Ubuntu 24.04, 2 vCPU / 8 Go RAM, Frankfurt. 6 conteneurs Docker derrière Nginx.
> PostgreSQL **Supabase**, Redis **Upstash**, RabbitMQ **CloudAMQP**, stockage **Supabase S3**.
> ✅ Parcours validé end-to-end : register → OTP email Brevo → verify → login → profil →
> **upload vidéo TUS → transcodage 4 renditions HLS → lecture (HTTP 200) → vue/like/commentaire**.

| Dépendance | Provider | État |
|---|---|---|
| **Base de données** | Supabase PostgreSQL | 🟢 Actif |
| **Email** (OTP, reset) | Brevo | 🟢 Actif (⚠️ vérifier spams) |
| **Stockage** (avatars, vidéos) | Supabase S3 | 🟢 Actif |
| **Cache / sessions** | Upstash Redis | 🟢 Actif |
| **File de messages** | CloudAMQP (RabbitMQ) | 🟢 Actif |
| **SMS** (OTP téléphone) | Infobip | 🟠 à confirmer au 1er envoi |
| **Push** (notifications) | Firebase FCM | 🟠 simulation (FCM_PROJECT_ID vide) |

---

## 🔄 Flows complets (récap pratique)

### 1. Inscription (OTP email)
1. `POST /auth/register` `{ email, username, password, phone_number? }` → `{ message }` (compte **pas encore** créé).
2. L'utilisateur reçoit un code 6 chiffres par email.
3. `POST /auth/verify-email` `{ email, otp_code }` → `{ accessToken, refreshToken }` (compte créé).
4. *(Optionnel)* `POST /auth/send-otp` → `POST /auth/verify-otp` pour valider le téléphone.

### 2. Connexion
1. `POST /auth/login` → `{ accessToken, refreshToken }`. Stocker les deux.
2. Sur `401` → `POST /auth/refresh` `{ refreshToken }`. Échec refresh → login.

### 3. Profil
1. `GET /users/me` → profil complet **+ récupérer l'`id`**.
2. `PATCH /users/{id}` (l'UUID réel, **pas** `me`).
3. Avatar : `POST /users/me/avatar/presign` → `PUT` sur `uploadUrl` → `PUT /users/me/avatar`.

### 4. Upload vidéo (TUS)
1. `POST /videos/upload` (header `Upload-Length`) → `{ upload_id, upload_url, video_id }`.
2. Boucle `PATCH /videos/upload/{upload_id}` par chunks de 512 Ko jusqu'à `Upload-Offset == Upload-Length`.
3. Reprise : `HEAD /videos/upload/{upload_id}` → lire `Upload-Offset`.
4. Suivre le transcodage : `GET /videos/{id}` (champ `status`) ou `GET /videos/{id}/manifest`.

> #### ⏳ Cycle de vie d'une vidéo
> `processing` → `published` (`hls_manifest_url` + `renditions` remplis) → ou `failed`.
> - Écran « traitement en cours » tant que `status == "processing"`, puis poller `GET /api/v1/videos/{id}`.
> - Transcodage séquentiel sur 2 vCPU (quelques minutes/clip).
> - Push FCM `video_ready` émis quand la vidéo est prête.

### 5. Feed & interactions
1. `GET /feed?cursor=` (JWT optionnel pour `is_liked`).
2. Sur une vidéo : `POST /videos/{id}/view` · `POST /videos/{id}/like` · `POST /videos/{id}/comments`.
3. **Suivre un créateur** : `POST /users/{id}/follow` (l'`id` vient du feed `creator_id` **ou** d'un profil).

> ✅ Tous ces appels utilisent la **même base URL** `http://152.239.118.90/api/v1` — la passerelle route en interne.

---

## 📋 Codes d'erreur — référence rapide

| Code | Signification | Action frontend recommandée |
|---|---|---|
| `400` | Validation échouée | Afficher les erreurs par champ (`message` parfois = tableau) |
| `401` | Token invalide/expiré/révoqué | Tenter `POST /auth/refresh`, sinon → login |
| `403` | Accès interdit (pas votre ressource) | Ne pas réessayer |
| `404` | Ressource introuvable | Afficher état vide |
| `409` | Conflit (email/username/téléphone déjà utilisé) | Afficher le `message` |
| `412` / `415` | En-têtes TUS manquants/invalides | Corriger les headers TUS |
| `413` | Trop volumineux (> 500 Mo ou chunk > 512 Ko) | Réduire/redécouper |
| `429` | Rate limit dépassé | Attendre, désactiver le bouton |
| `503` | Service externe non configuré | Réessayer plus tard |
