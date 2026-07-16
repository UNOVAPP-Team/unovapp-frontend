# Demande backend — Feed personnalisé (algorithme de recommandation)

**Contexte.** Aujourd'hui `GET /feed?type=foryou` renvoie **la même liste à tout le monde**
(vraisemblablement les vidéos les plus récentes). Résultat : deux utilisateurs voient
exactement le même contenu dans le même ordre, revoient les vidéos déjà vues, et l'onglet
« Pour toi » ne porte pas son nom.

Sur une plateforme de vidéo courte, **le feed EST le produit** : c'est lui qui détermine la
rétention. Cette demande décrit un algorithme réaliste, livrable en 3 étapes, sans machine
learning au départ.

L'app est **déjà prête** : elle envoie les vues (`POST /videos/:id/view` avec
`watched_seconds` / `total_seconds`), les likes, les partages, les sauvegardes, et lit
`GET /feed?type=foryou`. **Aucun changement d'API n'est nécessaire** pour les étapes 1 et 2 —
seul le contenu de la réponse change.

---

## Étape 1 🔴 — Ne plus jamais montrer deux fois la même vidéo (1 jour)

C'est le minimum vital, et le plus gros gain immédiat.

- Table `video_views(user_id, video_id, watched_seconds, total_seconds, created_at)`
  (probablement déjà alimentée par `POST /videos/:id/view`).
- Dans `/feed?type=foryou` : **exclure les vidéos déjà vues** par l'utilisateur
  (`NOT EXISTS (SELECT 1 FROM video_views ...)`), et **exclure ses propres vidéos**.
- Exception : si l'utilisateur a tout vu, on peut recycler les vidéos vues il y a > 7 jours,
  triées par score (cf. étape 2) — mieux que de renvoyer une liste vide.

## Étape 2 🟠 — Score de qualité + fraîcheur, avec un peu d'aléatoire (2-3 jours)

Un feed « pour toi » sans ML mais qui fonctionne : classer les vidéos non vues par un score.

```
score = engagement_rate * poids_qualité
      + freshness_boost
      + affinity_boost      (créateurs suivis / hashtags aimés)
      + exploration_noise   (aléatoire ~10-20 %)
```

Avec :

| Composante | Calcul suggéré | Pourquoi |
|---|---|---|
| `engagement_rate` | `(likes + 2×commentaires + 3×partages + 2×sauvegardes) / max(vues, 10)` | Un partage vaut plus qu'un like : c'est le signal le plus fort. Le `max(vues,10)` évite qu'une vidéo à 1 vue / 1 like ait un score parfait. |
| **`completion_rate`** | `AVG(watched_seconds / total_seconds)` sur les vues | **Le signal le plus prédictif sur TikTok.** Une vidéo regardée jusqu'au bout est une bonne vidéo. À pondérer fortement (×2 ou ×3). |
| `freshness_boost` | `exp(-age_heures / 48)` ou paliers (24 h : +50 %, 72 h : +20 %) | Sans ça, les vieilles vidéos populaires écrasent les nouvelles → les nouveaux créateurs n'ont aucune chance. |
| `affinity_boost` | +40 % si créateur suivi ; +20 % si hashtag déjà liké/vu en entier | Personnalisation réelle par utilisateur. |
| `exploration_noise` | `random() * 0.15` ou tirage pondéré | **Indispensable** : sans aléatoire, tout le monde converge vers les mêmes vidéos et le feed se fige. |

**Boost « nouvelle vidéo »** (cold start) : toute vidéo publiée depuis < 2 h reçoit un lot de
vues garanties (ex. injectée dans le feed de N utilisateurs) pour mesurer son engagement réel.
Sans ça, une bonne vidéo d'un petit créateur ne démarre jamais.

Requête type (PostgreSQL, à adapter) :

```sql
SELECT v.*,
  ( (v.likes_count + 2*v.comments_count + 3*v.shares_count + 2*v.saves_count)::float
      / GREATEST(v.views_count, 10)
    + 2.0 * COALESCE(vs.avg_completion, 0)                     -- completion rate
    + 1.5 * exp(-EXTRACT(EPOCH FROM (now() - v.created_at)) / 172800)  -- fraîcheur (48h)
    + CASE WHEN f.followed_id IS NOT NULL THEN 0.4 ELSE 0 END  -- créateur suivi
    + random() * 0.15                                          -- exploration
  ) AS score
FROM videos v
LEFT JOIN video_stats vs   ON vs.video_id = v.id
LEFT JOIN follows f        ON f.follower_id = :user_id AND f.followed_id = v.creator_id
WHERE v.status = 'published'
  AND v.visibility = 'public'
  AND v.creator_id <> :user_id
  AND NOT EXISTS (SELECT 1 FROM video_views w WHERE w.user_id = :user_id AND w.video_id = v.id)
ORDER BY score DESC
LIMIT :limit;
```

⚠️ **Performance** : ce calcul ne doit pas s'exécuter à chaque requête sur toute la table.
Recommandé : une vue matérialisée `video_scores` rafraîchie toutes les 5-15 min pour la partie
globale (engagement, fraîcheur), et seule la partie personnalisée (exclusion des vues, affinité)
calculée à la volée. Index nécessaires : `video_views(user_id, video_id)`,
`videos(status, created_at)`.

## Étape 3 🟡 — Diversité et signaux négatifs (plus tard)

- **Pas plus de 2 vidéos consécutives du même créateur** (règle anti-monotonie appliquée après
  le tri).
- **Signaux négatifs** : « Pas intéressé » (l'app a déjà le bouton dans le menu ⋮) et le
  *skip rapide* (`watched_seconds / total_seconds < 0.2`) → pénaliser ce créateur/hashtag pour
  cet utilisateur.
- **Centres d'intérêt** : l'app collecte déjà `POST /users/me/interests` à l'onboarding —
  aujourd'hui **ils ne sont pas utilisés dans le feed**. Les utiliser au moins pour le tout
  premier feed d'un nouvel utilisateur (cold start utilisateur).

---

## Ce que l'app fait / fera de son côté

- ✅ Envoie déjà `watched_seconds` / `total_seconds` à chaque vue → le backend a la matière
  pour calculer le `completion_rate`.
- ✅ Sépare déjà `foryou` (algorithmique) et `following` (chronologique, créateurs suivis).
- ✅ Pagine par curseur → l'algorithme peut renvoyer des pages successives cohérentes
  (⚠️ le curseur doit rester stable pendant une session de scroll, sinon des doublons
  apparaissent : privilégier un curseur opaque contenant le seed d'aléatoire + l'offset).
