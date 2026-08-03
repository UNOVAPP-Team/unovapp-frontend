# Ce qui reste à implémenter — côté frontend

> État au **3 août 2026**, version **2.4 (15)**.
> Établi en migrant l'app écran par écran sur les maquettes `jpg/`, la spec
> d'animation et la spec exhaustive du Flux.
>
> **Convention de ce document.** Chaque tâche indique si elle est faisable
> **maintenant** (tout est côté client) ou si elle **attend le backend** (voir
> `A_ENVOYER_PAR_LE_BACKEND.md`). Rien n'est marqué « fait » sans avoir été vu à
> l'écran — les items compilés mais non vérifiés sont signalés comme tels.

---

## 📊 Vue d'ensemble

| Écran | Migré | Vérifié à l'écran | Reste |
|---|---|---|---|
| 01/02 Flux repos + éveil | ✅ | ✅ | CE MOMENT, montée timeline |
| 03 Étincelles | ✅ | ⚠️ compilé | groupement par instant (backend) |
| 04 Explorer | ✅ | ✅ | Ondes/Horizons réels (backend) |
| 05 Pulsations | ✅ | ✅ | détail du pouls (backend) |
| 06 Univers | ⚠️ partiel | ✅ | EMPREINTE, SÉRIES, couverture, onglets |
| 07 Studio | ⚠️ partiel | ⚠️ compilé | RÉALISATEUR, scènes |
| 08 Cercles | ✅ démo | ❌ | tout le backend |
| 09 Réglages | ✅ | ⚠️ compilé | section Rythme |
| 10 Brasier | ⚠️ partiel | ⚠️ compilé | temps réel (backend) |
| 11 Réserve | ✅ | ⚠️ compilé | ledger Flammes |
| 12 Flux MVP | ✅ | ✅ | — |
| 13 La Forge | ✅ démo | ❌ | tout le backend |
| Onboarding (13 écrans) | ✅ | ✅ | OTP téléphone réel |

---

## 🔨 Faisable MAINTENANT (aucune dépendance backend)

### F1 — Flux : la montée de la timeline en éveil
**Spec Flux §5.3.** En passant en état éveillé, la ligne de temps doit **monter de
74 px** (`bottom: 122 → 196`) en **320 ms `--ease-soft`**, simultanément à
l'extinction du bloc créateur. Aujourd'hui elle reste en place.
*Purement visuel. ~30 min.*

### F2 — Univers : retirer la photo de couverture et les onglets
La maquette écran 06 ne montre **ni bannière de couverture, ni onglets**
(Vidéos / Favoris / Playlists / Challenges). L'app affiche encore les deux.
⚠️ **Décision produit requise** : supprimer les onglets fait perdre l'accès aux
Favoris et Playlists. À arbitrer avant de coder.

### F3 — Univers : section EMPREINTE
Tuiles thématiques (grande tuile « Danse · 14 vidéos · la plus chaude », puis
« Cuisine », « Foi ») + carte d'insight lagune. Le mock existe déjà dans
`data/mock/MockContent.kt` (`MOCK_EMPREINTE`, `MOCK_EMPREINTE_INSIGHT`).
*Reste à écrire l'UI. ~2 h.*

### F4 — Univers : section SÉRIES
Deux cartes (« Danse Afro » avec badge `S1 · 6 ÉP.`, « Rires de rue » en
brouillon) + action « Nouvelle série ». Mock prêt : `MOCK_SERIES`.
*~1 h.*

### F5 — Studio : carte « TON RÉALISATEUR » + scènes
Carte lagune avec l'idée et le plan de tournage, puis les trois chips de scène
(`1 · ACCROCHE 3 s`, `2 · DÉMO 8 s`, `3 · APPEL 4 s`) et le sélecteur bas.
Mock prêt : `MOCK_REALISATEUR_IDEA`, `MOCK_REALISATEUR_PLAN`, `MOCK_SCENES`.
*~2 h.*

### F6 — Réglages : section « Rythme »
Les trois interrupteurs de permission IA, en carte **lagune** avec le point.
`SettingsGroup` accepte déjà `lagune = true` — le paramètre a été écrit pour ça.
Mock prêt : `MOCK_RYTHME`.
⚠️ Ces toggles ne commanderont rien tant que l'API n'expose pas les préférences.
À poser en démo assumée (bandeau) ou à attendre — **ton choix**.

### F7 — Flux : panneau « CE MOMENT »
Panneau haut-gauche, 210 px de large, trois lignes (son / lieu / défi) avec
leurs icônes. Mock prêt : `MOCK_CE_MOMENT`. Entrée en `riseIn` 20 px, délai 80 ms.
*~1 h 30.*

### F8 — Cercles et La Forge : rendre les actions cliquables
Tous les `onClick` de ces deux écrans sont **vides** (`pressable(onClick = {})`).
Un bouton qui ne fait rien est indistinguable d'un bug. À minima : ouvrir une
feuille « Bientôt disponible » plutôt que d'absorber le tap en silence.

### F9 — Le Brasier : sortir la simulation du composable
Votes, compte à rebours et cadeaux sont générés avec `Random` **dans l'UI**
(`BattleScreen.kt`). À déplacer dans `MockContent.kt` ou un ViewModel de démo,
pour que le branchement WebSocket ne demande pas de réécrire l'écran.

### F10 — Accessibilité : libellés TalkBack
La checklist de la spec Flux demande des libellés sur **les 11 items de l'état
éveillé + l'Orbe**. Plusieurs `contentDescription` sont à `null`.

### F11 — Vérifier ce qui n'a jamais été vu à l'écran
Écrans compilés mais **jamais ouverts** sur l'appareil : Cercles, La Forge,
Étincelles, Réglages, Studio, Brasier, Réserve. À parcourir une fois chacun.

---

## ⛔ BLOQUÉ par le backend

Détail complet des contrats demandés dans `A_ENVOYER_PAR_LE_BACKEND.md`.

### B1 — Étincelles : le groupement par instant 🔴
Sans `anchor_ms` sur les commentaires, impossible de faire :
- l'attribution « étincelle à 0:04 » sur la bulle du Flux ;
- les sections « À 0:04 — LE PAS DE DANSE » de l'écran 03 ;
- **les points d'étincelle de la ligne de temps, aujourd'hui à des positions
  ALÉATOIRES** — c'est le point le plus gênant, une donnée visuellement crédible
  mais fausse.

### B2 — Brasier : le temps réel 🔴
Aucun endpoint. À brancher sur `battle.state` / `vote.added` / `gift.sent`.

### B3 — Cercles : toute la messagerie
Conversations, envoi, non-lus, sas « Nouvelles voix ».

### B4 — La Forge : toute la place de marché
Commandes, candidatures, séquestre, score de match IA.

### B5 — Couleur de signature
Choisie à l'onboarding et affichée dans Univers, mais **locale à l'appareil** :
elle se perd au changement de téléphone. Demande un champ `signature_color`.

### B6 — Localisation
« @beingar • **Cotonou** » (Univers) et « **Cotonou** · son original » (Flux).
Retiré partout plutôt que codé en dur.

### B7 — Ondes et Horizons d'Explorer
Aujourd'hui **codés en dur** dans `SearchScreen.kt`. À brancher sur `/trends`.

### B8 — Détail des Pulsations
« pic à 20 h », courbe d'activité, bouton « Voir le récap — 12 s ».

### B9 — Réserve : le ledger « Flammes »
La maquette montre l'historique signé (−3, +21, +110). Pas d'endpoint de
transactions.

### B10 — Tout le volet IA
Chip « Rythme doux · matin », suggestion réelle de l'Orbe (aujourd'hui toujours
Studio), plan du réalisateur, insight d'Univers, recherche sémantique.

### B11 — Onboarding : OTP téléphone
Les écrans Numéro et Signature existent mais ne sont **pas branchés** sur
`send-phone-otp` / `verify-phone` — l'onboarding file vers l'écran d'auth
classique. Les endpoints EXISTENT : c'est du travail frontend en attente, pas un
blocage. *→ à remonter en F.*

---

## 🐛 Dettes et points de vigilance

| Sujet | Détail |
|---|---|
| **Deux systèmes de couleur** | `UnovColors` (ancien orange) et `Ember` (braise) coexistent. Les écrans migrés utilisent Ember ; Auth, Create, Inbox et une partie de Profile sont encore sur l'ancien. À unifier. |
| **Deux systèmes de mouvement** | `UnovMotion` (legacy) et `EmberMotion` (spec). `StaggerReveal` legacy subsiste dans `AdvancedAnimations.kt` et `ProfileScreen.kt`. |
| **Police** | Les écrans migrés utilisent `EmberFont` (Hanken). Auth et Create gardent la police système, qui rend en manuscrit sur certains Xiaomi. |
| **`android-36` local** | Le build dépend d'un dossier `android-36` reconstitué à la main sur cette machine. **Une autre machine ou la CI échouera** — installer Android 16 via le SDK Manager. |
| **Mock non branché** | `MOCK_EMPREINTE`, `MOCK_SERIES`, `MOCK_REALISATEUR_*`, `MOCK_SCENES`, `MOCK_CE_MOMENT`, `MOCK_RYTHME` sont écrits mais **pas encore consommés** (tâches F3–F7). |
| **Animations jamais vues** | Le mouvement n'a pu être validé qu'à l'œil nu : pas d'outil vidéo sur ce poste, et une capture met ~1 s pour des animations de 240–520 ms. La Lueur en particulier reste à juger à l'usage. |

---

## 🎯 Ordre suggéré

1. **F11** — parcourir les 7 écrans jamais ouverts (peut révéler des surprises)
2. **F8** — rendre les actions de démo honnêtes (feuille « Bientôt »)
3. **F1** — montée de la timeline (rapide, visible)
4. **F3 + F4** — Univers complet, l'écran le plus regardé après le Flux
5. **F7** — CE MOMENT
6. **F5 + F6** — Studio et Rythme
7. **F2** — arbitrage produit sur couverture et onglets
8. **B11** — OTP téléphone (endpoints déjà dispos)
9. Le reste dès que le backend livre
