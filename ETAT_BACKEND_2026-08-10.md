# État du backend — relevé du 10 août 2026, 05:10

> Diagnostic exécuté depuis le poste de développement frontend, contre
> `https://api.unovapp.com/api/v1`. Chaque ligne est une requête réelle, pas une
> déduction.

## 🟢 Verdict : le serveur fonctionne

**Le serveur N'EST PAS à terre.** La panne constatée le 9 août était **transitoire** :
ce jour-là, TCP, HTTP et ICMP échouaient tous les trois sur `191.218.164.178`, alors
que trois hôtes témoins répondaient normalement. Le 10 août, tout répond.

Il n'y a donc **pas de preuve de panne à produire** — ce serait un document faux.
Ce fichier consigne à la place ce que les requêtes montrent réellement.

---

## 1. Disponibilité

| Requête | Résultat |
|---|---|
| `GET /health` | **200** · connect 1,18 s · total 3,44 s |
| `GET /auth/health` | **200** · total 0,55 s |
| `GET /users/health` | **200** · total 0,54 s |
| `GET /videos/health` | **200** · total 0,60 s |
| `GET /feed?limit=1` | **200** · total 0,55 s |
| `https://api.unovapp.com` (443) | **200** |
| `http://api.unovapp.com` (80) | **200** — encore ouvert, comme annoncé |
| `http://191.218.164.178` (IP directe) | **200** |

Hôtes témoins au même moment : `github.com` **200**, `google.com` **200**,
`api.github.com` **200** — le réseau local est sain, la comparaison est donc valide.

> ⚠️ La **première** connexion a mis 1,18 s à s'établir contre 0,16–0,17 s ensuite.
> Cohérent avec un réveil de connexion, pas avec un service en difficulté.

---

## 2. Leurs correctifs annoncés — tous confirmés

### `city` et `signature_color` (livrés le 05/08) ✅

`GET /users/me` sur le compte de test :

```json
"username": "thibaut_test"
"city": "Cotonou"
"signature_color": "#FF4D00"
"videos_count": 4
```

Les deux champs sont présents et renseignés. **Câblés côté app** (commit `0f39832`) :
la ville s'affiche à côté du pseudo, et la couleur de signature n'est plus locale à
l'appareil.

### `GET /users/:id/videos` — le bug de la grille de profil ✅

Testé **avec `limit=50`**, comme ils l'ont demandé (voir piège n°1) :

| Contexte | Vidéos renvoyées |
|---|---|
| **Anonyme** (sans jeton) | **4** |
| Connecté | **4** |
| `videos_count` du profil | **4** |

Les trois concordent. Le bug — qui vidait tout profil consulté sans jeton — **est
corrigé**. Rien à changer côté app.

---

## 3. Les trois pièges — vérifiés

### Piège n°1 : `limit > 50` retombe à 10, en silence ✅ confirmé

```
limit=10  -> 10 vidéos
limit=50  -> 50 vidéos
limit=100 -> 10 vidéos      ← ni 100, ni 50 : dix, sans erreur
```

**Ne nous concerne pas** : audit fait, toutes nos valeurs de `limit` sont ≤ 20
(20 pour les listes d'utilisateurs et notifications, 12 pour les grilles, 10 pour le
feed). Aucun appel de l'app ne franchit le plafond.

### Piège n°2 : `/notifications/unread-count` ✅ confirmé

```
HTTP 200 · corps = 0 octet
```

Un `JSON.parse` dessus lèverait une exception peu lisible. **Nous ne l'appelons pas** :
le compteur vient de `GET /notifications` → `"unread_count": 0`, qui répond correctement.

### Piège n°3 : l'adresse en clair ✅ sans objet pour nous

`http://api.unovapp.com` répond encore, mais **l'app est déjà passée en HTTPS** le
28 juillet, et l'exception cleartext a été retirée du `network_security_config`.
👉 **Ils peuvent activer la redirection définitive quand ils veulent** — nous ne
tapons plus l'URL en clair.

---

## 4. Recommandation IA — fonctionne

```
chrono  : b46405a1  3e36e92f  4ac755e3
for_you : 31aaf102  32ee3256  c015db30
```

Deux jeux de vidéos entièrement différents : `?type=for_you` est bien opérant.
(Rappel : nous envoyions `foryou` sans underscore, et le backend l'ignorait en
silence — corrigé de notre côté.)

---

## 5. Ce qui n'est pas encore livré — conforme au calendrier annoncé

| Champ | État | Prévu |
|---|---|---|
| `anchor_ms` (sur les commentaires) | **absent** | août |
| `spark_anchors_ms` (sur le feed) | **absent** | août |

**L'app est déjà prête** (commit `ec0702a`) : elle envoie `anchor_ms` dès aujourd'hui,
et lit `spark_anchors_ms` s'il apparaît. Le jour de la livraison, tout s'allume sans
redéploiement.

En attendant, la ligne de temps du Flux reste **nue** : les points d'étincelle étaient
auparavant tirés au hasard depuis l'id de la vidéo — crédibles mais faux. Une ligne
vide dit la vérité.

---

## 6. À leur transmettre

1. **La grille de profil est confirmée corrigée** — testée en anonyme avec `limit=50`,
   4 vidéos comme en base. Merci.
2. **L'app est en HTTPS depuis le 28 juillet** → la redirection définitive peut être
   activée.
3. **Réponses aux deux questions sur `anchor_ms`** (ils ne codent pas avant) :
   - *Ancre hors durée* → **rejet en 400**, d'accord. Une ancre au-delà de la vidéo
     n'est pas récupérable côté client ; autant la refuser à l'écriture. Nous filtrons
     déjà à l'affichage par sécurité.
   - *Réponses ancrées* → **pas d'ancre sur les réponses**, d'accord. L'ancre appartient
     au fil ; une réponse pointant un autre instant que l'étincelle à laquelle elle
     répond n'aurait pas de sens.
4. **Panne du 9 août** : entre ~22 h et le 10 août au matin, le serveur était
   totalement injoignable depuis le Bénin (TCP + ICMP), réseau local sain. Si ce n'était
   pas une opération planifiée, cela mérite un coup d'œil aux logs de cette fenêtre.
