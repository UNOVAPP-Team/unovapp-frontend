# Demande backend — Livraison vidéo (fluidité / démarrage lent)

**Contexte.** Diagnostic mesuré le 2026-07-07 depuis l'app Android : la première vidéo mettait
plusieurs secondes à démarrer et la lecture saccadait. Les mesures (curl + logs ExoPlayer)
montrent que le problème est **côté livraison des fichiers**, pas côté app :

| Mesure | Valeur | Conséquence |
|---|---|---|
| Segment 480p (720 Ko) depuis Supabase | **6,9 s** (~100 Ko/s) | l'origine Supabase bride le débit |
| Re-téléchargement du même segment | 5,7 s, `CF-Cache-Status: MISS` | **Cloudflare ne met JAMAIS en cache** |
| En-tête servi sur les segments | `Cache-Control: no-cache` | c'est la cause du MISS permanent |
| Playlist `.m3u8` | 0,8–1,2 s TTFB | repaye l'origine à chaque vue |

L'app fait maintenant de l'ABR (elle synthétise une master playlist à partir de `renditions`
et démarre sur une rendition légère), donc la lecture est fluide *malgré* l'origine lente —
mais chaque vue repaye Supabase, et la montée en 480p reste bridée à ~100 Ko/s.

---

## 1. 🔴 CRITIQUE — Cache-Control sur les objets vidéo (1 ligne dans le pipeline)

Les vidéos sont **immuables** (un dossier par `video_id`, jamais réécrit) : elles doivent être
cacheables un an. À l'upload des **segments `.ts`**, **playlists `.m3u8`** et **miniatures**
vers Supabase Storage, passer l'option `cacheControl` :

```go
// storage-go
client.UploadFile(bucket, path, file, storage.FileOptions{
    CacheControl: "31536000",          // → Cache-Control: public, max-age=31536000
    ContentType:  "video/mp2t",        // .ts (application/vnd.apple.mpegurl pour .m3u8)
})
```

(Équivalent supabase-js : `upload(path, file, { cacheControl: '31536000' })`.)

**Backfill des vidéos existantes** : re-upload avec `upsert` + `cacheControl`, ou copie S3
« sur place » avec nouvelles métadonnées via l'API S3-compatible de Supabase.

**Vérification** (à faire après déploiement) — le 2ᵉ appel doit passer de MISS à HIT :
```bash
curl -sI "https://plftrvjhqtvdohvuyvye.supabase.co/storage/v1/object/public/unovapp-videos/videos/<id>/480p/seg_000.ts" | grep -iE "cf-cache-status|cache-control"
```

Gain attendu : segments servis par le edge Cloudflare le plus proche de l'utilisateur
(≈ 30–80 ms au lieu de 600 ms de TTFB, débit CDN au lieu de 100 Ko/s). **C'est le plus gros
levier de fluidité de toute la plateforme, zéro changement app.**

## 2. 🟠 Master playlist HLS générée au transcodage

Le feed expose des renditions séparées (`renditions.144p/240p/480p`) mais pas de master
playlist → l'app doit en synthétiser une avec des bitrates *estimés*. Générer une vraie
`master.m3u8` au transcodage avec les bitrates *réels* :

```
#EXTM3U
#EXT-X-INDEPENDENT-SEGMENTS
#EXT-X-STREAM-INF:BANDWIDTH=263000,RESOLUTION=256x144,CODECS="avc1.64000c,mp4a.40.2"
144p/playlist.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=520000,RESOLUTION=426x240,CODECS="avc1.640015,mp4a.40.2"
240p/playlist.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=1150000,RESOLUTION=854x480,CODECS="avc1.64001f,mp4a.40.2"
480p/playlist.m3u8
```

et renseigner `hls_manifest_url` dans la réponse du feed (le champ existe déjà côté app).
Garder `renditions` dans la réponse (utilisé pour le téléchargement/partage).

- `BANDWIDTH` = bitrate crête réel mesuré (ffprobe), `CODECS` = profil réel.
- L'attribut `CODECS` permet la préparation « chunkless » d'ExoPlayer (zéro segment
  téléchargé pour préparer la lecture).

## 3. 🟠 Segments de 2 s (au lieu de ~6 s)

Premier segment 480p actuel : 6,4 s / 720 Ko → premier téléchargement énorme. Passer à :

```
-hls_time 2 -hls_flags independent_segments \
-force_key_frames "expr:gte(t,n_forced*2)"
```

Segments ~3× plus petits → première frame plus tôt, ABR plus réactif (il peut changer de
qualité toutes les 2 s au lieu de 6).

## 4. 🟠 QUALITÉ — étendre le ladder d'encodage (720p, puis 1080p)

Le transcodage actuel plafonne à **480p ≈ 900 kbps** : c'est le plafond de qualité de toute la
plateforme, quel que soit le réseau de l'utilisateur (à titre de comparaison, TikTok sert du
720p/1080p à 1–2,5 Mbps). L'app enregistre et uploade désormais des sources **1080p** — le
transcodeur peut donc produire :

| Rendition | Résolution (9:16) | Bitrate vidéo cible | Audio |
|---|---|---|---|
| 144p | 144×256 | ~250 kbps | AAC 48 k |
| 240p | 240×426 | ~500 kbps | AAC 64 k |
| 480p | 480×854 | ~1,1 Mbps | AAC 96 k |
| **720p** (nouveau) | 720×1280 | **~2,2 Mbps** | AAC 128 k |
| **1080p** (nouveau, si source ≥ 1080p) | 1080×1920 | **~4 Mbps** | AAC 128 k |

Recommandations d'encodage (ffmpeg, qualité constante plutôt que bitrate fixe) :

```
-c:v libx264 -preset slow -crf 23 -maxrate <cible> -bufsize <2×cible> \
-profile:v high -pix_fmt yuv420p -c:a aac
```

- **Ne jamais upscaler** : si la source est 720p, générer le ladder jusqu'à 720p seulement.
- Ajouter les nouvelles renditions dans `renditions` (l'app les prendra automatiquement,
  l'ABR choisira selon le débit réel de l'utilisateur).
- ⚠️ Le point 1 (Cache-Control/CDN) est un prérequis : sans CDN, personne n'a le débit
  pour le 720p. Avec CDN, la majorité des utilisateurs l'aura.

## 5. 🟡 Optionnel — plus tard

- **MP4 progressif `faststart`** par rendition pour les clips < 60 s (`mp4_url` dans le feed) :
  démarrage en 1 aller-retour au lieu de 3-4 (c'est ce que fait TikTok).
- **TTFB de l'API** : `GET /api/v1/feed` répond en ~0,9–1,3 s depuis l'Europe — vérifier
  index DB/N+1 ; viser < 300 ms (le feed bloque l'affichage de la première vidéo au démarrage).
- **Domaine + TLS sur la passerelle** : débloque HTTP/2-3 côté app (Cronet) et supprime le
  cleartext.

> Priorité : livrer le **point 1 seul** débloque déjà l'essentiel. Les points 2-3 s'appliquent
> aux nouvelles vidéos au fil de l'eau (pas besoin de re-transcoder l'existant).
