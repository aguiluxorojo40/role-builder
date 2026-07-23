#!/usr/bin/env python3
"""
smw_sprites.py — Ensambla sprites de SMW 1:1 (tiles OBJ + paleta reales).

Cada sprite se define como una lista de tiles OBJ 16x16 sobre un banco GFX y una fila
de la CGRAM real (build_cgram). Una tile OBJ 16x16 = tiles 8x8: N, N+1, N+16, N+17
(la VRAM es de 16 tiles de ancho). Las tablas de tiles vienen del motor `smw`
(snesrev/smw, p. ej. kPlayerGFXRt_Tiles en src/smw_00.c para Mario).

Salida (todo local, out/ está en .gitignore):
  out/sprites/<nombre>.png   cada sprite ensamblado y coloreado 1:1
  out/smw_sprites.js         window.SMW_SPRITES = [...] para el visor (preview.html)

Uso:
  python3 smw_sprites.py "/ruta/Super Mario World (USA).sfc"
"""
import base64, json, os, sys
import extract_smw as E

# banco -> fila CGRAM: la paleta real de cada sprite (8-15 = sprites; Mario en la 8).
SPRITE_SPECS = [
    {"name": "Mario",            "cat": "Mario", "bank": 0x32, "row": 8, "w": 16, "h": 32,
     "tiles": [(0x00, 0, 0), (0x02, 0, 16)]},
    {"name": "Mario (retrato)",  "cat": "Mario", "bank": 0x32, "row": 8, "w": 16, "h": 16,
     "tiles": [(0x2E, 0, 0)]},
    {"name": "Mario (agachado)", "cat": "Mario", "bank": 0x32, "row": 8, "w": 16, "h": 16,
     "tiles": [(0x08, 0, 0)]},
]


def obj16_indices(ix, W, H, objtile):
    """Devuelve 16x16 índices de una tile OBJ (N, N+1, N+16, N+17)."""
    quad = [objtile, objtile + 1, objtile + 16, objtile + 17]
    out = [0] * 256
    for qi, t in enumerate(quad):
        cx, cy = (t % 16) * 8, (t // 16) * 8
        ox, oy = (qi % 2) * 8, (qi // 2) * 8
        for y in range(8):
            for x in range(8):
                sx, sy = cx + x, cy + y
                out[(oy + y) * 16 + (ox + x)] = ix[sy * W + sx] if (sy < H and sx < W) else 0
    return out


def assemble(rom, spec):
    """Ensambla un sprite -> (w, h, bytearray de índices)."""
    gfx = E.load_gfx(rom, spec["bank"])
    W, H, ix = E.index_sheet(gfx, E.bpp_for(spec["bank"]), 16)
    w, h = spec["w"], spec["h"]
    grid = bytearray(w * h)
    for (obj, dx, dy) in spec["tiles"]:
        cell = obj16_indices(ix, W, H, obj)
        for y in range(16):
            for x in range(16):
                px, py = dx + x, dy + y
                if px < w and py < h:
                    grid[py * w + px] = cell[y * 16 + x]
    return w, h, grid


def render_png(w, h, grid, pal, path, scale=6):
    rgba = bytearray(w * h * 4)
    for i in range(w * h):
        v = grid[i]
        if v:
            c = pal[v] if v < len(pal) else (255, 0, 255)
            rgba[i * 4:i * 4 + 4] = bytes((*c, 255))
    W, H, up = E.upscale(w, h, rgba, scale)
    E.write_png(path, W, H, up)


def main():
    if len(sys.argv) < 2:
        print(__doc__); sys.exit(1)
    out = os.path.join(os.path.dirname(__file__), "out")
    spr_dir = os.path.join(out, "sprites")
    os.makedirs(spr_dir, exist_ok=True)
    rom = E.Rom(sys.argv[1])
    print("ROM sha1:", rom.sha1)
    if rom.sha1 != E.SMW_SHA1_US and "--force" not in sys.argv:
        print("  ROM no es SMW (USA); usa --force.", file=sys.stderr); sys.exit(2)
    cg = E.build_cgram(rom)

    data = []
    for spec in SPRITE_SPECS:
        w, h, grid = assemble(rom, spec)
        pal = E.cgram_row(cg, spec["row"])
        safe = spec["name"].lower().replace(" ", "_").replace("(", "").replace(")", "")
        render_png(w, h, grid, pal, os.path.join(spr_dir, safe + ".png"))
        data.append({"name": spec["name"], "cat": spec["cat"], "w": w, "h": h,
                     "row": spec["row"], "px": base64.b64encode(bytes(grid)).decode("ascii")})
        print("  %-16s %dx%d  paleta %d -> sprites/%s.png" % (spec["name"], w, h, spec["row"], safe))

    with open(os.path.join(out, "smw_sprites.js"), "w") as f:
        f.write("window.SMW_SPRITES = " + json.dumps({"cgram": cg, "sprites": data}) + ";\n")
    print("Listo. %d sprites -> out/smw_sprites.js" % len(data))


if __name__ == "__main__":
    main()
