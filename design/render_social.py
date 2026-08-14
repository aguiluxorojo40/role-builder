#!/usr/bin/env python3
"""Estratos Latentes — lámina social 1280x640 para Role Builder.

El motivo central es una tesela 16x16 de 4 bits por pixel. Los cuatro planos
que la acompañan NO son decoración: son su descomposición binaria real, la
misma que hace el decodificador planar del motor. El color de cada celda es
la consecuencia aritmética de los cuatro bits que tiene debajo.
"""
from PIL import Image, ImageDraw, ImageFont

S = 4                                   # supermuestreo
W, H = 1280, 640
FONTS = "/root/.claude/skills/synced/canvas-design/canvas-fonts"

INK        = (11, 14, 19)
BONE       = (232, 226, 214)
MUTED      = (106, 115, 133)
FAINT      = (30, 36, 47)
RULE       = (44, 52, 66)

def f(name, size):
    return ImageFont.truetype(f"{FONTS}/{name}", size * S)

# ── paleta indexada: rampa discreta de 16 entradas ───────────────────────────
ANCHORS = [(0,  (255, 243, 226)), (3,  (242, 195, 107)), (6,  (226, 114,  91)),
           (9,  (155,  78, 123)), (12, (62,  58, 117)),  (15, (20,  26,  42))]

def build_palette():
    pal = []
    for i in range(16):
        lo = max([a for a in ANCHORS if a[0] <= i], key=lambda a: a[0])
        hi = min([a for a in ANCHORS if a[0] >= i], key=lambda a: a[0])
        if lo[0] == hi[0]:
            pal.append(lo[1]); continue
        t = (i - lo[0]) / (hi[0] - lo[0])
        pal.append(tuple(round(lo[1][c] + (hi[1][c] - lo[1][c]) * t) for c in range(3)))
    return pal

PAL = build_palette()

# ── el motivo: índices 0..15 sobre una retícula 16x16 ────────────────────────
def motif():
    grid = []
    for y in range(16):
        row = []
        for x in range(16):
            dx, dy = abs(x - 7.5), abs(y - 7.5)
            manhattan = dx + dy                       # rombo
            chebyshev = max(dx, dy) * 2               # cuadrado
            d = 0.60 * manhattan + 0.40 * chebyshev   # octógono
            phase = (d / 15.0) * 1.5                   # ciclo y medio
            u = phase % 1.0
            tri = 2 * u if u < 0.5 else 2 * (1 - u)    # va y vuelve
            row.append(max(0, min(15, round(15 * (tri ** 0.68)))))
        grid.append(row)
    return grid

MOTIF = motif()
PLANES = [[[(MOTIF[y][x] >> b) & 1 for x in range(16)] for y in range(16)]
          for b in range(4)]

# ── lienzo ───────────────────────────────────────────────────────────────────
img = Image.new("RGB", (W * S, H * S), INK)
d = ImageDraw.Draw(img)

# retícula de fondo: acumulación densa, casi imperceptible
for x in range(0, W + 1, 16):
    d.line([(x * S, 0), (x * S, H * S)], fill=FAINT, width=1)
for y in range(0, H + 1, 16):
    d.line([(0, y * S), (W * S, y * S)], fill=FAINT, width=1)

def track(draw, xy, text, font, fill, tracking=0.0, anchor_y="la"):
    """Texto con rastreo manual, letra a letra."""
    x, y = xy
    for ch in text:
        draw.text((x, y), ch, font=font, fill=fill, anchor=anchor_y)
        x += draw.textlength(ch, font=font) + tracking * S
    return x

def track_width(draw, text, font, tracking=0.0):
    w = sum(draw.textlength(c, font=font) for c in text)
    return w + tracking * S * max(0, len(text) - 1)

M = 76                                  # margen maestro

# ── COLUMNA IZQUIERDA ────────────────────────────────────────────────────────
mono_xs = f("GeistMono-Regular.ttf", 9)
mono_sm = f("GeistMono-Regular.ttf", 11)
jura    = f("Jura-Light.ttf", 82)

track(d, (M * S, 104 * S), "MOTOR  ·  EDITOR  ·  ANDROID", mono_xs, MUTED, 3.4)

track(d, (M * S, 138 * S), "ROLE",    jura, BONE, 9.0)
track(d, (M * S, 218 * S), "BUILDER", jura, BONE, 9.0)

d.line([(M * S, 322 * S), ((M + 384) * S, 322 * S)], fill=RULE, width=S)

track(d, (M * S, 344 * S), "Construye plataformas leyendo tu propia ROM de", mono_sm, MUTED, 0.3)
track(d, (M * S, 364 * S), "Super Mario World. En el móvil, sin PC.",       mono_sm, MUTED, 0.3)

# tira de paleta: el inventario cromático, declarado
sw_w, sw_h, sw_y = 24, 11, 432
track(d, (M * S, (sw_y - 18) * S), "PALETA  ·  16 ENTRADAS", mono_xs, MUTED, 3.0)
for i, c in enumerate(PAL):
    x0 = (M + i * sw_w) * S
    d.rectangle([x0, sw_y * S, x0 + sw_w * S - 1, (sw_y + sw_h) * S], fill=c)
d.rectangle([M * S, sw_y * S, (M + 16 * sw_w) * S - 1, (sw_y + sw_h) * S],
            outline=RULE, width=1)
for i in (0, 8, 16):
    x0 = (M + i * sw_w) * S
    d.line([(x0, (sw_y + sw_h + 4) * S), (x0, (sw_y + sw_h + 9) * S)], fill=RULE, width=S)
track(d, (M * S, (sw_y + sw_h + 14) * S), "0", mono_xs, MUTED, 0)
track(d, ((M + 16 * sw_w - 8) * S, (sw_y + sw_h + 14) * S), "15", mono_xs, MUTED, 0)

track(d, (M * S, 510 * S), "github.com/aguiluxorojo40/role-builder", mono_xs, (78, 86, 102), 1.4)

# ── LÁMINA DERECHA ───────────────────────────────────────────────────────────
C, CX, CY = 336, 868, 140               # compuesto: 16 celdas de 21 px
cell = C // 16
for y in range(16):
    for x in range(16):
        px, py = (CX + x * cell) * S, (CY + y * cell) * S
        d.rectangle([px, py, px + cell * S - 1, py + cell * S - 1], fill=PAL[MOTIF[y][x]])
d.rectangle([CX * S, CY * S, (CX + C) * S - 1, (CY + C) * S - 1], outline=RULE, width=1)

# marcas de registro cada 4 celdas
for i in range(0, 17, 4):
    x = (CX + i * cell) * S
    d.line([(x, (CY + C + 5) * S), (x, (CY + C + 11) * S)], fill=RULE, width=S)
    y = (CY + i * cell) * S
    d.line([((CX + C + 5) * S, y), ((CX + C + 11) * S, y)], fill=RULE, width=S)

track(d, (CX * S, (CY - 20) * S), "COMPUESTO  ·  ÍNDICE 0–15", mono_xs, MUTED, 3.0)
track(d, (CX * S, (CY + C + 20) * S), "16 × 16 PX   ·   4 BPP", mono_xs, MUTED, 3.0)

# los cuatro planos de bits, en serie y ordenados por peso
P, PX, PY, GAP = 64, 700, 141, 26
for b in range(4):
    top = PY + b * (P + GAP)
    pc = P // 16
    for y in range(16):
        for x in range(16):
            on = PLANES[b][y][x]
            px, py = (PX + x * pc) * S, (top + y * pc) * S
            d.rectangle([px, py, px + pc * S - 1, py + pc * S - 1],
                        fill=(226, 218, 202) if on else (22, 27, 36))
    d.rectangle([PX * S, top * S, (PX + P) * S - 1, (top + P) * S - 1],
                outline=RULE, width=1)
    track(d, ((PX - 62) * S, (top + 2) * S), f"PLANO {b}", mono_xs, MUTED, 1.2)
    track(d, ((PX - 62) * S, (top + 16) * S), f"× {2 ** b}", mono_xs, RULE, 1.2)

# corchete: los cuatro planos suman el compuesto
bx = PX + P + 22
bot = PY + 3 * (P + GAP) + P
d.line([(bx * S, PY * S), (bx * S, bot * S)], fill=RULE, width=S)
for cap in (PY, bot):                       # topes del corchete
    d.line([(bx * S, cap * S), ((bx - 7) * S, cap * S)], fill=RULE, width=S)
mid = PY + (bot - PY) / 2
d.line([(bx * S, mid * S), ((CX - 20) * S, mid * S)], fill=RULE, width=S)
d.line([((CX - 20) * S, (mid - 4) * S), ((CX - 20) * S, (mid + 4) * S)], fill=MUTED, width=S)

track(d, (1204 * S - track_width(d, "LÁMINA I", mono_xs, 3.0), 104 * S),
      "LÁMINA I", mono_xs, MUTED, 3.0)

img.resize((W, H), Image.LANCZOS).save("social-preview.png")
print("ok")
