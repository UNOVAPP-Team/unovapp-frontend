# Demande backend — Challenges (créés par les utilisateurs)

**Contexte.** La maquette produit introduit une fonctionnalité centrale : **chaque utilisateur
peut créer son propre challenge** (nom, description, dates, règles, récompense, frais de
participation), les autres y participent en publiant des vidéos, et le créateur gagne en
visibilité.

Côté app, tout est **déjà construit** :
- l'écran « Créer un challenge » (formulaire complet, validation, sélection d'image, dates) ;
- l'onglet « Challenges » du profil (carte CTA + carrousel « Mes challenges ») ;
- les modèles Kotlin (`ChallengeForm`, `ChallengeCard`).

⚠️ **Il n'existe aujourd'hui aucun endpoint `challenges`** → le formulaire ne peut pas publier.
Le challenge créé reste local à la session, et l'utilisateur est prévenu par un message.

---

## 1. 🔴 Créer un challenge

### `POST /challenges`
- **Auth** : requise.
- **Body** :
  ```json
  {
    "name": "Danse Afro 2026",
    "description": "Montrez vos meilleurs pas de danse afro...",
    "cover_key": "challenges/<uuid>/cover.jpg",   // via presign, cf. §2
    "start_date": "2026-07-10T00:00:00Z",
    "end_date": "2026-08-10T23:59:59Z",
    "rules": "Vidéo de 30s minimum, hashtag obligatoire...",
    "audience": "everyone",          // everyone | followers | invited
    "min_age": null,                 // null | 13 | 16 | 18
    "reward_type": "money",          // money | jetons | gift | visibility  (optionnel)
    "reward_value": "100000 FCFA",   // optionnel
    "entry_fee_enabled": true,       // optionnel
    "entry_fee_amount": 500          // FCFA, requis si entry_fee_enabled
  }
  ```
- **Réponse 201** : l'objet challenge complet (cf. §3), avec un `hashtag` généré
  (ex. `#DanseAfro2026`) et `participants_count: 0`.
- **Validations attendues** : `end_date > start_date`, `start_date >= aujourd'hui`,
  nom unique (ou hashtag unique), description ≤ 200 car., règles ≤ 300 car.

### `POST /challenges/:id/cover/presign`
Même mécanique que le presign avatar/miniature déjà en place (S3 pré-signé) — l'app envoie
l'image directement au stockage, puis confirme.

## 2. 🔴 Lister les challenges

### `GET /users/:id/challenges?cursor=&limit=`
Challenges **créés par** cet utilisateur → alimente le carrousel « Mes challenges » du profil
(et du profil visité). Réponse paginée standard (`data` / `next_cursor` / `has_more`).

### `GET /challenges?filter=active|trending|new&cursor=&limit=`
Découverte (écran dédié, à venir).

### `GET /challenges/:id`
Détail d'un challenge + ses vidéos participantes.

## 3. Forme d'un challenge (réponse)

```json
{
  "id": "uuid",
  "creator_id": "uuid",
  "name": "Danse Afro 2026",
  "hashtag": "#DanseAfro2026",
  "description": "...",
  "cover_url": "https://.../cover.jpg",
  "start_date": "2026-07-10T00:00:00Z",
  "end_date": "2026-08-10T23:59:59Z",
  "rules": "...",
  "audience": "everyone",
  "min_age": null,
  "reward_type": "money",
  "reward_value": "100000 FCFA",
  "entry_fee_enabled": true,
  "entry_fee_amount": 500,
  "status": "active",              // draft | active | ended | cancelled
  "participants_count": 12500,
  "is_participating": false,        // pour l'utilisateur courant
  "created_at": "2026-07-08T10:00:00Z"
}
```

`status` doit être calculé côté serveur à partir des dates (`active` entre start et end,
`ended` après) — l'app affiche le badge « Actif » sur cette base.

## 4. 🟠 Participer

### `POST /challenges/:id/join`
Inscrit l'utilisateur. Si `entry_fee_enabled`, **débite les frais** (cf. §5) et échoue en 402
si le solde est insuffisant.

### Lier une vidéo à un challenge
Ajouter un champ optionnel `challenge_id` au `POST /videos/upload` (métadonnées TUS) **ou** un
endpoint `POST /challenges/:id/videos { video_id }`. Les vidéos participantes remontent alors
dans `GET /challenges/:id`.

## 5. 🟠 Frais de participation & récompense (⚠️ argent réel)

La maquette annonce : *« UNOVAPP prélève des frais de service sur chaque participation. »*
Cela implique un **flux financier** qui n'existe pas encore :

- Le **wallet est purement local côté app** aujourd'hui (aucun endpoint `/wallet`).
  Impossible de débiter réellement quoi que ce soit.
- Il faut donc, **avant d'activer les frais** : un wallet backend (solde, transactions),
  l'intégration Mobile Money (MTN/Moov), le calcul et la retenue de la commission UNOVAPP,
  et le versement de la récompense au gagnant.
- **Recommandation** : livrer les challenges **sans frais de participation** en v1
  (`entry_fee_enabled` toujours `false`), et n'activer cette section qu'une fois le wallet
  et le Mobile Money en place. La récompense peut rester **déclarative** (le créateur
  s'engage à la verser) dans un premier temps — à condition de le dire clairement dans l'UI.

## 6. Conséquences produit à trancher

- **Qui arbitre le gagnant ?** (le créateur ? les votes ? les vues ?) — à définir avant d'ouvrir
  les récompenses en argent, sinon litiges garantis.
- **Modération** : un challenge est un contenu public créé par un utilisateur → il doit être
  signalable et modérable (comme les vidéos).
- **Frais de service** : le pourcentage prélevé par UNOVAPP doit être une constante backend
  visible dans les CGU.
