# Demande backend — Endpoints Feed & Vidéos (déblocage frontend)

**Contexte.** Le frontend Android est déjà connecté à l'Auth, au profil (`/users/me`), à la
recherche/suivi d'utilisateurs et au refresh de token. **Le feed reste mocké** car le service
backend n'expose pas encore les vidéos. Les actions sociales (`/videos/{id}/like|comments|view`)
existent mais sont **inutilisables** sans IDs de vidéos réels.

Merci de livrer les endpoints ci-dessous. Conventions à garder : **JWT Bearer**, **snake_case**,
pagination `{ data, total, page, limit }`, dates ISO-8601.

---

## 1. 🔴 Lister le feed — `GET /api/v1/videos`
Le plus prioritaire (débloque tout le feed).

- **Auth** : Bearer (optionnel pour "Pour toi", requis pour "Abonnements").
- **Query** : `feed=for_you|following` · `page` (def. 1) · `limit` (def. 10) · `cursor` (optionnel, pour scroll infini).
- **Réponse 200** :
```json
{
  "data": [
    {
      "id": "uuid",
      "hls_url": "https://cdn.../video.m3u8",
      "thumbnail_url": "https://cdn.../thumb.jpg",
      "duration_sec": 42,
      "description": "Légende avec #hashtags",
      "created_at": "2026-06-15T18:00:00Z",
      "likes_count": 1240,
      "comments_count": 87,
      "shares_count": 12,
      "views_count": 53000,
      "liked_by_me": false,
      "creator": {
        "id": "uuid",
        "username": "aminata.cot",
        "display_name": "Aminata",
        "avatar_url": "https://cdn.../avatar.jpg",
        "is_verified": true,
        "followed_by_me": false
      }
    }
  ],
  "total": 230, "page": 1, "limit": 10
}
```

## 2. Détail d'une vidéo — `GET /api/v1/videos/{id}`
- **Auth** : Bearer optionnel.
- **Réponse 200** : même objet vidéo que ci-dessus. **404** si introuvable.

## 3. Upload d'une vidéo — `POST /api/v1/videos`
Idéalement en 2 temps (comme l'avatar `users/me/avatar/presign`) :
1. `POST /api/v1/videos/presign` → `{ "upload_url": "...", "video_id": "uuid" }` (URL signée pour PUT du fichier).
2. `POST /api/v1/videos` avec `{ "video_id": "uuid", "description": "...", "duration_sec": 42 }` → crée l'enregistrement, renvoie l'objet vidéo.
- **Auth** : Bearer requis.

---

## 4. À confirmer — formats des endpoints sociaux existants
Pour finaliser les DTO côté app (déjà câblés), merci de préciser :

- **`GET /videos/{id}/comments`** — structure exacte d'un commentaire :
```json
{ "data": [ { "id":"uuid", "content":"texte", "created_at":"...",
  "user": { "id":"uuid","username":"...","display_name":"...","avatar_url":"...","is_verified":false } } ],
  "total": 87, "page": 1, "limit": 20 }
```
- **`POST /videos/{id}/comments`** — corps attendu : `{ "content": "texte" }` ? Réponse = le commentaire créé ?
- **`POST /videos/{id}/like`** — renvoie quoi ? (ex. `{ "liked": true, "likes_count": 1241 }`). Existe-t-il un **DELETE** pour retirer le like ?
- **`POST /videos/{id}/view`** — corps/effet ? (comptage de vue)

---

## 5. Optionnel mais utile
- **Wallet/jetons** : `GET /api/v1/wallet` (solde) + `POST /api/v1/wallet/spend` (envoi cadeau) — actuellement le solde est local côté app.
- **Réactions multiples** (façon LinkedIn) : si possible, `POST /videos/{id}/react` avec `{ "type": "like|love|celebrate|support|insightful|funny" }`. Sinon l'app retombe sur `like` simple.

> Dès que (1) `GET /videos` et (4) sont livrés/confirmés, le frontend remplace le mock par un
> `FeedRepository` + `FeedViewModel` et tout le feed devient réel (commentaires, likes, vues,
> follow créateur) — c'est rapide côté app.
