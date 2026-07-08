# 📘 Guide Frontend — SPRINT 1 (endpoints critiques)

> **Pour l'équipe Frontend Android.** Nouveautés du **Sprint 1** (2026-06-28), déployées et **validées en production** (10/10).

## 🌍 Base & auth
```
http://152.239.118.90/api/v1
```
- Auth : `Authorization: Bearer <accessToken>`. Pagination cursor : `{ data:[...], next_cursor, has_more }` ; `?cursor=`, `?limit=` (défaut 10, max 50).

## 🔄 Changements vs version précédente
| Élément | Changement |
|---|---|
| **Feed** | + `shares_count`, `is_saved`, `is_following_creator`, `hashtags[]` ; nouveau `?type=foryou\|following` |
| **Commentaires** | + `user_id`, `avatar_url`, `likes_count`, `is_liked`, `replies_count`, `is_pinned`, `is_author` |
| **Profil** (`GET /users/:id`) | + `videos_count`, `total_likes_received`, `is_blocked` (false au S1), `is_following` |
| **Vue** | body optionnel `{ watched_seconds, total_seconds }` ; JWT optionnel ; rétrocompatible |
| **Nouveaux** | `GET /users/:id/videos`, `/users/me/liked`, `/users/me/saved`, `POST /videos/:id/save`, `PATCH /videos/:id`, `DELETE /users/me` |

> ✅ Aucune rupture (tout additif).

## 1. GET /users/:id/videos
Auth optionnelle (Bearer → is_liked/is_saved). `:id` = UUID ou `me`. Query : cursor, limit. → format feed. Vidéos `published` (le propriétaire voit aussi ses `private`).

## 2. GET /users/me/liked
Auth requise (`me` uniquement, 403 sinon). → format feed, triées par date de like.

## 3. Save
- `POST /videos/:id/save` (Auth) → `{ "saved": true }` (toggle).
- `GET /users/me/saved` (Auth, me) → format feed (`is_saved:true`).

## 4. Feed enrichi — GET /feed?type=foryou|following
Auth optionnelle (`type=following` exige le Bearer → 401 sinon). `foryou` = vidéos publiques ; `following` = créateurs suivis (chronologique).
```json
{
  "data": [{
    "id": "uuid", "creator_id": "uuid", "description": "Ma demo #benin #Test",
    "duration_seconds": 6, "renditions": {...}, "thumbnail_url": "...",
    "likes_count": 2, "comments_count": 1, "views_count": 9, "shares_count": 0,
    "hashtags": ["benin","test"], "is_liked": true, "is_saved": true,
    "is_following_creator": false, "created_at": "..."
  }],
  "next_cursor": "...", "has_more": false
}
```
⚠️ `hashtags` dérivé de `description`.

## 5. PATCH /videos/:id
Auth requise (créateur, 403 sinon). Body (optionnels) : `description`, `visibility` (public|private, 400 sinon), `allow_comments` (bool), `hashtags[]` ⚠️ **accepté mais IGNORÉ au S1** (indexation = Sprint 3 ; éditer `description`). → vidéo mise à jour.

## 6. DELETE /users/me (RGPD)
Auth requise → **204**. Soft delete + anonymisation (email→`deleted_<id>@deleted.com`, téléphone/nom/bio/avatars effacés) + suppression follows + révocation refresh. **Irréversible** (login impossible après, 401). ⚠️ Obligatoire Google Play — confirmation forte côté app.

## 8. POST /videos/:id/view — vue + complétion
Auth optionnelle. Body optionnel `{ watched_seconds, total_seconds }` → enregistre le taux de complétion. Sans body = simple incrément. → `{ "message": "Vue enregistrée" }`.

## 9. GET /videos/:id/comments — enrichis
```json
{
  "data": [{
    "id": "uuid", "user_id": "uuid", "username": "akossi", "avatar_url": "...",
    "content": "...", "parent_id": null, "likes_count": 0, "is_liked": false,
    "replies_count": 0, "is_pinned": false, "is_author": false, "created_at": "..."
  }],
  "next_cursor": null, "has_more": false
}
```
`is_author` = ce commentaire est le tien. ⚠️ `likes_count`/`is_liked` à 0/false au S1 → like de commentaire = Sprint 2.

## 10. GET /users/:id — profil enrichi
Auth optionnelle (Bearer → is_following). Nouveaux : `videos_count`, `total_likes_received`, `is_blocked` (false au S1, branché Sprint 2), `is_following`.

## 📋 Codes d'erreur
| Code | Cas |
|---|---|
| 400 | visibility invalide ; body de vue invalide |
| 401 | token manquant/expiré ; ?type=following sans Bearer ; login après suppression |
| 403 | PATCH/liked/saved d'un autre utilisateur |
| 404 | vidéo/compte introuvable |

> ✅ Tous testés en production (10/10).
