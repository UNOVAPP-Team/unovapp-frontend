#!/usr/bin/env python3
"""Génère les icônes de lancement UNOVAPP à partir de app/branding/unovapp.png.

- Extrait uniquement l'emblème (haut du logo), en excluant le mot « UNOVAPP ».
- Fond sombre #0A0A0A (couleur du splash), emblème doré conservé.
- Sort : adaptive icon (foreground PNG + background couleur), icônes legacy
  (carré + rond) toutes densités, et l'icône 512x512 pour la fiche Play Store.
"""
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.abspath(__file__))
RES = os.path.abspath(os.path.join(ROOT, "..", "src", "main", "res"))
SRC = os.path.join(ROOT, "unovapp.png")
BG = (10, 10, 10, 255)  # #0A0A0A

raw = Image.open(SRC).convert("RGBA")
W, H = raw.size
src = raw.load()

# Le logo a un fond en damier gris (R≈G≈B, opaque) aplati dans l'image.
# On reconstruit l'alpha par saturation : on ne garde que l'or (forte saturation).
img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
out = img.load()
for y in range(H):
    for x in range(W):
        r, g, b, _ = src[x, y]
        sat = max(r, g, b) - min(r, g, b)
        if sat <= 20:          # gris du damier -> transparent
            continue
        a = 255 if sat >= 50 else int((sat - 20) / 30 * 255)  # bords anti-aliasés
        out[x, y] = (r, g, b, a)

px = img.split()[3].load()

# --- Détecter les bandes de contenu (lignes non transparentes) ---
THRESH = 16
row_has = []
for y in range(H):
    has = False
    for x in range(0, W, 2):  # échantillonnage 1px/2 pour la vitesse
        if px[x, y] > THRESH:
            has = True
            break
    row_has.append(has)

# Trouver les runs True séparés par des gaps -> 1er run = emblème
bands = []
start = None
for y, h in enumerate(row_has):
    if h and start is None:
        start = y
    elif not h and start is not None:
        bands.append((start, y - 1))
        start = None
if start is not None:
    bands.append((start, H - 1))

top, bot = bands[0]  # bande supérieure = emblème (texte = bande suivante)

# bbox colonnes dans cette bande
left, right = W, 0
for y in range(top, bot + 1):
    for x in range(W):
        if px[x, y] > THRESH:
            if x < left:
                left = x
            if x > right:
                right = x

emblem = img.crop((left, top, right + 1, bot + 1))
print(f"Emblème extrait: bande y[{top}:{bot}] x[{left}:{right}] -> {emblem.size}")


def fit(im, target, frac):
    """Redimensionne im pour que sa plus grande dimension = frac*target, garde le ratio."""
    w, h = im.size
    scale = (target * frac) / max(w, h)
    return im.resize((max(1, round(w * scale)), max(1, round(h * scale))), Image.LANCZOS)


def centered(emb_scaled, canvas, bg=None):
    base = Image.new("RGBA", (canvas, canvas), bg if bg else (0, 0, 0, 0))
    ew, eh = emb_scaled.size
    base.alpha_composite(emb_scaled, ((canvas - ew) // 2, (canvas - eh) // 2))
    return base


def save(im, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)
    print("écrit:", os.path.relpath(path, os.path.join(ROOT, "..", "..")))


# --- 1. Adaptive foreground (emblème centré, transparent, zone de sécurité 58%) ---
fg_sizes = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
for dpi, s in fg_sizes.items():
    fg = centered(fit(emblem, s, 0.58), s)
    save(fg, os.path.join(RES, f"mipmap-{dpi}", "ic_launcher_foreground.png"))

# --- 2. Icônes legacy carrées + rondes ---
legacy = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
for dpi, s in legacy.items():
    # carré arrondi
    sq = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    mask = Image.new("L", (s, s), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, s - 1, s - 1], radius=round(s * 0.18), fill=255)
    bgimg = Image.new("RGBA", (s, s), BG)
    sq.paste(bgimg, (0, 0), mask)
    emb = fit(emblem, s, 0.62)
    sq.alpha_composite(emb, ((s - emb.size[0]) // 2, (s - emb.size[1]) // 2))
    save(sq, os.path.join(RES, f"mipmap-{dpi}", "ic_launcher.png"))

    # rond
    rd = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    cmask = Image.new("L", (s, s), 0)
    ImageDraw.Draw(cmask).ellipse([0, 0, s - 1, s - 1], fill=255)
    rd.paste(bgimg, (0, 0), cmask)
    emb2 = fit(emblem, s, 0.58)
    rd.alpha_composite(emb2, ((s - emb2.size[0]) // 2, (s - emb2.size[1]) // 2))
    save(rd, os.path.join(RES, f"mipmap-{dpi}", "ic_launcher_round.png"))

# --- 3. Icône fiche Play Store 512x512 (plein cadre, sans rognage) ---
ps = Image.new("RGBA", (512, 512), BG)
emb = fit(emblem, 512, 0.62)
ps.alpha_composite(emb, ((512 - emb.size[0]) // 2, (512 - emb.size[1]) // 2))
save(ps, os.path.join(ROOT, "ic_launcher-playstore.png"))

print("OK")
