#!/usr/bin/env python3
"""Captura de los JEFES de MODO 7 de SMW (Bowser, Koopalings) con un emulador real.

Los jefes de Modo 7 no se dibujan por la tabla OAM 4bpp como el resto de enemigos
(los reconstruye `SmwEnemyGraphics`), sino como bloque del tilemap de Modo 7. En vez
de decodificar Modo 7 a mano, este script arranca un emulador de SNES headless
(stable-retro / núcleo snes9x), warpea a cada sala de jefe manipulando la RAM,
captura el framebuffer y recorta el jefe a `big_<id>.png` (los que la app auto-carga
desde `assets/sprites/big/`, igual que Reznor/Big Boo).

Requisitos:
  pip install stable-retro pillow numpy scipy
  python -m stable_retro.import <dir-con-la-rom>   # importa la ROM US de SMW

Warp (leído del port snesrev/smw):
  - $7E:0100 = game mode; 0x11 = GameMode11_LoadSublevel, que carga el nivel de $0109.
  - $7E:0109 = nº de nivel a cargar cuando counter_sublevels_entered ($141A) == 0.
    LoadLevel hace `if (v2 >= 0x25) v2 -= 0x24`, así que para el nivel interno L
    (L <= 0xDB) se pone $0109 = L + 0x24, con ow_players_map[0] ($1F11) = 0.
  - Salas Modo 7 (modo de nivel 9/11/16) con jefe: Bowser 0x9B, Koopalings 0x96-0x9a/
    0xcc/0xd9, Reznor 0x95/0xd5.

Uso:
  python scripts/mode7_boss_capture.py --out app/src/main/assets/sprites/big
"""
import argparse, gzip, os, sys
import numpy as np
from PIL import Image
try:
    from scipy import ndimage
except ImportError:
    ndimage = None

GM, LVL, SUB, OWM, SPS = 0x7E0100, 0x7E0109, 0x7E141A, 0x7E1F11, 0x7E141D

# id de sprite -> (nivel interno, nº de frame tras la carga, recorte)
# 'black' = fondo negro (Bowser): key por luminancia. 'color' = fondo con ladrillos
# (Koopaling): key naranja|verde|blanco-brillante para descartar el ladrillo azul-gris.
BOSSES = {
    0xA0: dict(level=0x9B, frame=1200, mode="black", name="Bowser"),
    0x29: dict(level=0x9A, frame=160,  mode="color", name="Koopaling (Morton)"),
}

# Los 7 Koopalings son el MISMO sprite 0x29 (uno por castillo). El asset `big_29.png` usa uno
# (Morton, sala 0x9a); estas son las 7 salas de Modo 7 (modo de nivel 9/11/16) para el montaje
# de referencia. Trepamuros de fondo negro (recorte 'color' limpio): 0x96/0x97/0x99/0x9a/0xcc.
# Lanzafuegos (0x98/0xd9): la bola de fuego se pega al recorte; requieren un frame entre ataques.
KOOPALING_ROOMS = {
    0x96: "Iggy", 0x97: "Larry", 0x99: "Roy", 0x9A: "Morton",
    0xCC: "Ludwig", 0x98: "(lanzafuegos)", 0xD9: "(lanzafuegos)",
}


def make_env():
    import stable_retro as retro
    env = retro.make(game="SuperMarioWorld-Snes-v0", render_mode=None)
    env.reset()
    for n, a in (("gm", GM), ("lvl", LVL), ("sub", SUB), ("owm", OWM), ("sps", SPS)):
        env.data.set_variable(n, {"address": a, "type": "|u1"})
    statedir = os.path.join(os.path.dirname(retro.__file__), "data", "stable",
                            "SuperMarioWorld-Snes-v0")
    base = gzip.decompress(open(os.path.join(statedir, "Forest1.state"), "rb").read())
    return env, base


def warp_capture(env, base, level, upto):
    """Warpea al nivel interno `level` y devuelve la lista de framebuffers hasta `upto`."""
    env.em.set_state(base)
    for n in ("sub", "owm", "sps"):
        env.data.set_value(n, 0)
    env.data.set_value("lvl", (level + 0x24) & 0xFF)
    env.data.set_value("gm", 0x11)
    frames = []
    for _ in range(upto):
        env.em.set_button_mask(np.zeros(12, dtype=np.uint8), 0)
        env.em.step()
        frames.append(np.array(env.em.get_screen()))
    return frames


def cut_black(frame, ymax=150, lum=28):
    """Recorta el jefe sobre fondo negro (Bowser): alpha = luminancia > lum."""
    reg = frame[:ymax]
    m = reg.max(axis=2) > lum
    ys, xs = np.where(m)
    x0, x1, y0, y1 = xs.min(), xs.max(), ys.min(), ys.max()
    crop = frame[y0:y1 + 1, x0:x1 + 1]
    a = (crop.max(axis=2) > lum).astype(np.uint8) * 255
    return np.dstack([crop, a])


def cut_color(frames):
    """Recorta un Koopaling: key naranja|verde|blanco, la mancha grande sobre fondo negro."""
    def key(f):
        f = f.astype(np.int16); R, G, B = f[..., 0], f[..., 1], f[..., 2]; lum = f.max(2)
        return ((R > B + 30) & (R > 90)) | ((G > B + 25) & (G > 90)) | (lum > 150)
    best = None
    for k, f in enumerate(frames):
        m = key(f)
        m[:26], m[168:], m[:, :50], m[:, 208:] = False, False, False, False
        lbl, n = ndimage.label(m)
        for i in range(1, n + 1):
            ys, xs = np.where(lbl == i)
            if len(xs) < 260:
                continue
            w, h = xs.max() - xs.min(), ys.max() - ys.min()
            if 22 <= w <= 58 and 20 <= h <= 44 and (best is None or len(xs) > best[0]):
                best = (len(xs), k, xs.min() - 2, xs.max() + 2, ys.min() - 2, ys.max() + 2)
    _, k, x0, x1, y0, y1 = best
    m = key(frames[k]); m[168:] = False
    am = np.zeros(frames[k].shape[:2], bool); am[y0:y1 + 1, x0:x1 + 1] = m[y0:y1 + 1, x0:x1 + 1]
    am = ndimage.binary_fill_holes(ndimage.binary_closing(am, iterations=1))
    crop = frames[k][y0:y1 + 1, x0:x1 + 1]
    return np.dstack([crop, (am[y0:y1 + 1, x0:x1 + 1] * 255).astype(np.uint8)])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True, help="dir de salida (assets/sprites/big)")
    ap.add_argument("--ids", nargs="*", help="ids hex a capturar (por defecto todos)")
    args = ap.parse_args()
    env, base = make_env()
    ids = [int(x, 16) for x in args.ids] if args.ids else list(BOSSES)
    for sid in ids:
        b = BOSSES[sid]
        frames = warp_capture(env, base, b["level"], b["frame"] + (240 if b["mode"] == "color" else 1))
        if b["mode"] == "black":
            rgba = cut_black(frames[b["frame"]])
        else:
            rgba = cut_color(frames[b["frame"]:])
        out = os.path.join(args.out, f"big_{sid:x}.png")
        Image.fromarray(rgba, "RGBA").save(out)
        print(f"{b['name']} (0x{sid:x}) nivel 0x{b['level']:x}: {rgba.shape[1]}x{rgba.shape[0]} -> {out}")
    env.close()


if __name__ == "__main__":
    main()
