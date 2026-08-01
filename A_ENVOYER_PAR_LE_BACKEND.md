# Ce que le backend doit exposer pour finir la maquette « La braise, pas le bruit »

> Établi en migrant l'app écran par écran sur les maquettes `jpg/` et les specs
> d'animation / Flux. **Chaque point ci-dessous bloque un élément visible de la
> maquette que nous avons dû retirer ou laisser vide** — nous n'affichons jamais
> de donnée inventée.
>
> Contrats vérifiés en direct contre `https://api.unovapp.com/api/v1` le 2026-08-01.

---

## 🔴 P1 — bloque une promesse produit centrale

### 1. L'ancrage temporel des Étincelles

**Le manque.** `CommentDto` (GET `/videos/:id/comments`) ne porte aucun champ de
position dans la vidéo. Vérifié : `id, video_id, user_id, content, parent_id,
username, avatar_url, likes_count, is_liked, replies_count, is_pinned, is_author,
mentions, created_at`.

**Ce que ça casse.** L'Étincelle **est** le commentaire ancré à un instant — c'est
la différence revendiquée avec un commentaire ordinaire. Sans ce champ :
- la bulle du Flux ne peut pas écrire « étincelle à 0:04 » (spec Flux §5.2) ;
- l'écran Étincelles ne peut pas grouper « À 0:04 — LE PAS DE DANSE » (écran 03) ;
- **les points d'étincelle sur la ligne de temps sont aujourd'hui des positions
  aléatoires** — un placeholder visuel, pas une donnée.

**Demandé.**
```jsonc
// POST /videos/:id/comments  — en entrée
{ "content": "Trop fort !", "anchor_ms": 4200 }

// GET /videos/:id/comments  — en sortie, par commentaire
{ "anchor_ms": 4200 }   // null si commentaire non ancré
```
Et idéalement, sur le feed, les positions pour dessiner la ligne sans charger
tous les commentaires :
```jsonc
"spark_anchors_ms": [1700, 7600, 12100]
```

---

### 2. Le Brasier est entièrement simulé côté app

**Le manque.** Aucun endpoint. Votes, compte à rebours et cadeaux sont générés
localement avec `Random` dans `BattleScreen.kt`.

**Demandé.** Le service temps réel annoncé (WebSocket) avec au minimum :
`battle.state` (camps, scores, secondes restantes), `vote.added`, `gift.sent`,
`battle.ended`. Tant qu'il n'existe pas, cet écran ne montre rien de réel.

---

## 🟠 P2 — un élément de maquette reste vide

### 3. La couleur de signature

Choisie à l'onboarding (écran 04) et affichée dans Univers, elle **n'est stockée
nulle part côté serveur** : elle vit en mémoire sur l'appareil et se perd au
changement de téléphone. Or c'est présenté à l'utilisateur comme son identité.

**Demandé.** Un champ sur le profil, ex. `signature_color: "#FF4D00"`, en
lecture (`GET /users/me`) et écriture (`PATCH /users/:id`).

### 4. La localisation

La maquette écrit « @beingar • **Cotonou** » (Univers) et « **Cotonou** · son
original » (Flux). Aucun champ de ville sur l'utilisateur ni sur la vidéo.
Nous l'avons retiré plutôt que de coder « Cotonou » en dur.

**Demandé.** `city` (ou `location`) sur le profil, et/ou sur la vidéo si le lieu
de tournage doit s'afficher.

### 5. Les Séries

L'anneau « Séries » d'Univers et la section « SÉRIES » (écran 06) n'ont aucun
support. Nous affichons « Vidéos » à la place plutôt qu'un compteur toujours à 0.

**Demandé.** CRUD de séries + `series_count` sur le profil, et
`GET /users/:id/series` → `{ title, episodes_count, followers_count, is_draft }`.

### 6. L'Empreinte (thèmes du créateur)

Écran 06 : tuiles « Danse · 14 vidéos · la plus chaude », « Cuisine · 6 »,
« Foi · 4 ». Cela suppose une **agrégation par thème** des vidéos d'un créateur.
Les `hashtags` existent sur la vidéo, mais rien ne les regroupe ni ne les classe
par chaleur.

**Demandé.** `GET /users/:id/empreinte` → `[{ theme, videos_count, heat_rank }]`.

### 7. Le panneau « CE MOMENT » du Flux

Spec Flux §5.1, trois lignes : le **son** (« DJ Bossou — Nuit d'Or » + action
« Garder ce son »), le **lieu** (« Cotonou, Fidjrossè · 12 vidéos près de toi »),
le **défi** (« #ZangbetoChallenge · Rejoindre »). Aucun n'est exposé : la vidéo
n'a ni son associé, ni lieu, ni défi lié.

**Demandé.** Sur la vidéo : `sound { id, title, artist }`, `place { name,
nearby_count }`, `challenge { tag, joinable }`.

### 8. Les Ondes et les Horizons d'Explorer

Écran 04 : « ONDES DU BÉNIN » (nom, **sens de la tendance**, volume) et
« HORIZONS ». Aujourd'hui **entièrement codés en dur** dans `SearchScreen.kt`.

**Demandé.** `GET /trends` → `[{ tag, volume, direction: "up"|"stable"|"down",
scope }]`, et `GET /horizons` → `[{ title, videos_count, duration_s }]`.

### 9. Le détail des Pulsations

Écran 05 : « +2 étincelles · 1 nouveau fidèle · **pic à 20 h** », la courbe
d'activité, et le bouton « Voir le récap — 12 s ». `GET /notifications` ne
renvoie que la liste et `unread_count`.

**Demandé.** `GET /pulse/today` → `{ braises_received, sparks, new_followers,
peak_hour, activity_curve: [...], recap_video_url }`.

---

## 🟡 P3 — fonctionnalités entières absentes

| Écran | État |
|---|---|
| **08 Cercles** (messagerie entre fidèles) | Aucun écran, aucun endpoint. Non migré. |
| **13 La Forge** (place de marché marques ↔ créateurs) | Idem. Non migré. |

Nous n'avons pas posé de coquille visuelle : elle n'afficherait que du vide ou
du faux.

---

## 🔵 Tout ce qui touche à l'IA (couleur lagune)

La lagune `#6FD9C4` est **réservée à l'IA** dans la charte. Ces éléments sont
donc dessinés mais inertes, ou retirés :

| Élément | Écran | État |
|---|---|---|
| Chip « Rythme doux · matin » | Flux repos | non affiché |
| « Studio · **suggéré** » | Flux éveil | affiché en lagune, mais **la suggestion n'est pas calculée** — c'est toujours Studio |
| Réglages « Rythme » (3 interrupteurs de permission IA) | Réglages 09 | **non affichés** : des toggles qui ne commandent rien seraient un mensonge |
| « TON RÉALISATEUR » (plan de tournage) | Studio 07 | non affiché |
| Carte d'insight (« ton humour fait rester 2× plus longtemps ») | Univers 06 | non affichée |
| Recherche sémantique | Explorer 04 | le champ est en lagune, mais la recherche reste lexicale |
| `GET /feed?type=for_you` | Flux | **existe et fonctionne** ✅ (corrigé : nous envoyions `foryou` sans underscore) |

**Demandé.** Un contrat clair sur ce que l'IA renvoie, et surtout : les
**permissions** de l'utilisateur (`GET`/`PATCH /users/me/ai-preferences`), parce
que la charte promet qu'on les lui demande *avant*, pas six mois plus tard.

---

## ⚠️ Deux régressions constatées

1. **`GET /users/:id/videos` renvoyait `{"data":[]}`** alors que `videos_count`
   était > 0. À revérifier — la grille de profil dépend entièrement de cet appel.
2. **Livraison vidéo.** Mesuré : l'origine plafonnait autour de 100 Ko/s et
   renvoyait `Cache-Control: no-cache`. Le client compense (ABR, cache disque,
   préchargement), mais le vrai correctif est un CDN + un `Cache-Control` long
   sur des URLs immuables.

---

## ✅ Vérifié comme déjà conforme — ne rien changer

- `POST /auth/login` renvoie bien `{accessToken, refreshToken}` seuls
- Champ de connexion nommé `email`
- `POST /auth/me` (et non GET)
- Pagination par curseur sur le feed
- `unread_count` dans `GET /notifications` (la route `/notifications/unread-count`
  n'existe pas — nous ne l'appelons pas)
- Rotation du refresh token
- `users/me/liked` et `users/me/saved` = alias de `users/:id/...`
- Médias en URLs absolues R2
