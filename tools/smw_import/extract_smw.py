#!/usr/bin/env python3
"""
extract_smw.py — Ingiere assets gráficos desde una ROM de Super Mario World (USA).

Lee TU copia de la ROM y vuelca, en una carpeta local `out/`:
  - gfx/gfx_XX.png   hojas de tiles 8x8 reales (descomprimidos y decodificados)
  - palettes.json    todas las paletas globales como RGB
  - palettes.png     tira visual de las paletas
  - manifest.json    describe las hojas (ruta, columnas/filas, tileSize, paleta)

IMPORTANTE (copyright): la ROM y todo lo que sale de ella son de Nintendo. Este script
es código propio; su SALIDA se queda en local (out/ está en .gitignore) y NO se
redistribuye. Es el mismo principio del proyecto `smw`: extraer de tu propia ROM.

Los offsets del ROM y el descompresor LC_LZ2 están adaptados del proyecto de
ingeniería inversa `smw` (snesrev/smw, assets/util.py y assets/compile_resources.py).

Uso:
  python3 extract_smw.py /ruta/a/"Super Mario World (USA).sfc"
  python3 extract_smw.py ROM.sfc --bpp 3 --out out
  python3 extract_smw.py ROM.sfc --probe 0x32     # diagnóstico: prueba un banco a 2/3/4 bpp
"""
import argparse, hashlib, json, os, struct, sys, zlib

SMW_SHA1_US = "6b47bb75d16514b6a476aa0c73a683a2a4c18765"

# --------------------------------------------------------------------------------------
# Acceso al ROM (LoROM) — adaptado de smw/assets/util.py (LoadedRom)
# --------------------------------------------------------------------------------------
class Rom:
    def __init__(self, path):
        data = open(path, "rb").read()
        # quita cabecera SMC de 512 bytes si la hubiera
        if (len(data) & 0xFFFFF) == 0x200:
            data = data[0x200:]
        self.rom = data
        self.sha1 = hashlib.sha1(data).hexdigest()

    def byte(self, ea):
        # ea es una dirección SNES LoROM con el bit 0x8000 puesto
        assert ea & 0x8000, "dirección LoROM inválida: %#x" % ea
        off = ((ea >> 16) & 0x7F) * 0x8000 + (ea & 0x7FFF)
        return self.rom[off]

    def word(self, ea):
        return self.byte(ea) + self.byte(ea + 1) * 256

    def bytes(self, addr, n):
        r = bytearray()
        for _ in range(n):
            r.append(self.byte(addr))
            addr += 1
            if (addr & 0x8000) == 0:
                addr += 0x8000
        return r

    def words(self, addr, n):
        r = []
        for _ in range(n):
            r.append(self.word(addr))
            addr += 2
            if (addr & 0x8000) == 0:
                addr += 0x8000
        return r


# --------------------------------------------------------------------------------------
# Descompresor LC_LZ2 — adaptado de smw/assets/util.py (decomp)
# --------------------------------------------------------------------------------------
def decomp(rom, ea):
    result = bytearray()
    def nxt():
        nonlocal ea
        b = rom.byte(ea)
        ea += 1
        if (ea & 0xFFFF) == 0:
            ea += 0x8000
        return b
    while True:
        b = nxt()
        if b == 0xFF:
            return result
        if (b & 0xE0) != 0xE0:
            lx = b & 0x1F
            cmd = b & 0xE0
        else:
            cmd = (b << 3) & 0xE0
            lx = ((b & 3) << 8) | nxt()
        lx += 1
        if cmd == 0x00:  # literal
            for _ in range(lx):
                result.append(nxt())
        elif cmd & 0x80:  # copy
            offs = (nxt() << 8) | nxt()
            for _ in range(lx):
                result.append(result[offs]); offs += 1
        elif (cmd & 0x40) == 0:  # memset
            v = nxt()
            for _ in range(lx):
                result.append(v)
        elif (cmd & 0x20) == 0:  # memset16
            b1, b2 = nxt(), nxt()
            i = 0
            while i < lx:
                result.append(b1); i += 1
                if i < lx:
                    result.append(b2); i += 1
        else:  # increment
            v = nxt()
            for _ in range(lx):
                result.append(v); v = (v + 1) & 0xFF
    return result


# --------------------------------------------------------------------------------------
# Tabla de punteros a GFX (offsets de smw/assets/compile_resources.py)
# --------------------------------------------------------------------------------------
GFX_PTR_LO, GFX_PTR_HI, GFX_PTR_BANK = 0xB992, 0xB9C4, 0xB9F6
GFX_COUNT = 50  # GFX00..GFX31
GFX32_PTR = 0xB8D8  # word -> 0x80000 | word
GFX33_PTR = 0xB88B

def gfx_addr(rom, i):
    lo = rom.bytes(GFX_PTR_LO, GFX_COUNT)
    hi = rom.bytes(GFX_PTR_HI, GFX_COUNT)
    bk = rom.bytes(GFX_PTR_BANK, GFX_COUNT)
    return (bk[i] << 16) | (hi[i] << 8) | lo[i]

def load_gfx(rom, i):
    if i == 0x32:
        return decomp(rom, 0x80000 | rom.word(GFX32_PTR))
    if i == 0x33:
        return decomp(rom, 0x80000 | rom.word(GFX33_PTR))
    return decomp(rom, gfx_addr(rom, i))


# --------------------------------------------------------------------------------------
# Paletas globales (offsets de compile_resources.py). BGR555 -> RGB.
# --------------------------------------------------------------------------------------
PALETTE_SOURCES = {
    "sky":        (0x00B0A0, 16),
    "background": (0x00B0B0, 96),
    "layer3":     (0x00B170, 16),
    "foreground": (0x00B190, 96),
    "objects":    (0x00B250, 60),
    "player":     (0x00B2C8, 40),
    "sprites":    (0x00B318, 84),
    "berry":      (0x00B674, 21),
}

def bgr555(w):
    r, g, b = w & 0x1F, (w >> 5) & 0x1F, (w >> 10) & 0x1F
    return (r * 255 // 31, g * 255 // 31, b * 255 // 31)

def read_palettes(rom):
    out = {}
    for name, (addr, n) in PALETTE_SOURCES.items():
        out[name] = [bgr555(w) for w in rom.words(addr, n)]
    return out


# --------------------------------------------------------------------------------------
# CGRAM real de un nivel — replica BufferPalettesRoutines_Levels de smw/src/smw_00.c
# Devuelve 256 colores RGB = 16 paletas x 16. Es la asignación AUTÉNTICA de SMW:
#   paletas 0-7 = fondo/primer plano, 8-15 = sprites, y Mario en la paleta 8.
# --------------------------------------------------------------------------------------
_RAW = {  # (addr, n_words)
    "sky": (0x00B0A0, 16), "background": (0x00B0B0, 96), "layer3": (0x00B170, 16),
    "foreground": (0x00B190, 96), "objects": (0x00B250, 60), "player": (0x00B2C8, 40),
    "sprites": (0x00B318, 84), "yoshiberry": (0x00B674, 21),
}
# offsets por "palette setting" de la cabecera de nivel (kBufferPalettesRoutines_DATA_00ABD3)
_PSET = [0x0, 0x18, 0x30, 0x48, 0x60, 0x78, 0x90, 0xA8, 0x0, 0x14, 0x28, 0x3C]

def build_cgram(rom, fg=0, spr=0, bg=0):
    W = {name: rom.words(addr, n) for name, (addr, n) in _RAW.items()}
    cg = [0] * 256

    def strip(k, val):
        for _ in range(8):
            cg[k >> 1] = val; k += 32

    def load(src, dstbyte, count, rows):
        idx, r8 = 0, rows & 0xFFFF
        while True:
            v0, v1 = dstbyte, count
            while True:
                if idx < len(src):
                    cg[v0 >> 1] = src[idx]
                idx += 1; v0 += 2; v1 -= 1
                if v1 < 0:
                    break
            dstbyte += 32
            r8 = (r8 - 1) & 0xFFFF
            if r8 & 0x8000:
                break

    strip(2, 0x7FDD)
    strip(0x102, 0x7FFF)
    load(W["layer3"], 16, 7, 1)
    load(W["objects"], 132, 5, 9)
    load(W["foreground"][_PSET[fg] >> 1:], 68, 5, 1)
    load(W["sprites"][_PSET[spr] >> 1:], 452, 5, 1)
    load(W["background"][_PSET[bg] >> 1:], 4, 5, 1)
    load(W["yoshiberry"], 82, 6, 2)
    load(W["yoshiberry"], 306, 6, 2)
    # Mario (small): RtlUpdatePalette(GetPlayerPalette(), 0x86, 10) -> paleta 8, colores 6-15
    for i in range(10):
        cg[0x86 + i] = W["player"][i]
    return [bgr555(w) for w in cg]

def cgram_row(cg, row):
    return cg[row * 16:(row + 1) * 16]


# --------------------------------------------------------------------------------------
# Decodificación de tiles SNES planar (2/3/4 bpp), 8x8
# --------------------------------------------------------------------------------------
def tile_bytes(bpp):
    return {2: 16, 3: 24, 4: 32}[bpp]

def decode_tile(data, off, bpp):
    px = [[0] * 8 for _ in range(8)]
    for y in range(8):
        if off + y * 2 + 1 >= len(data):
            return px
        b0, b1 = data[off + y * 2], data[off + y * 2 + 1]
        for x in range(8):
            bit = 7 - x
            px[y][x] = ((b0 >> bit) & 1) | (((b1 >> bit) & 1) << 1)
    if bpp == 3:
        base = off + 16
        for y in range(8):
            if base + y >= len(data): break
            b2 = data[base + y]
            for x in range(8):
                px[y][x] |= ((b2 >> (7 - x)) & 1) << 2
    elif bpp == 4:
        base = off + 16
        for y in range(8):
            if base + y * 2 + 1 >= len(data): break
            b2, b3 = data[base + y * 2], data[base + y * 2 + 1]
            for x in range(8):
                bit = 7 - x
                px[y][x] |= (((b2 >> bit) & 1) << 2) | (((b3 >> bit) & 1) << 3)
    return px


# --------------------------------------------------------------------------------------
# Escritor PNG (RGBA, sin dependencias)
# --------------------------------------------------------------------------------------
def write_png(path, w, h, rgba):
    def chunk(typ, data):
        return (struct.pack(">I", len(data)) + typ + data +
                struct.pack(">I", zlib.crc32(typ + data) & 0xFFFFFFFF))
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        raw += rgba[y * w * 4:(y + 1) * w * 4]
    ihdr = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        f.write(chunk(b"IHDR", ihdr))
        f.write(chunk(b"IDAT", zlib.compress(bytes(raw), 9)))
        f.write(chunk(b"IEND", b""))


def index_sheet(data, bpp, cols=16):
    """Devuelve (w, h, bytearray) con el índice de color (0-15) por píxel."""
    tb = tile_bytes(bpp)
    ntiles = max(1, len(data) // tb)
    rows = (ntiles + cols - 1) // cols
    W, H = cols * 8, rows * 8
    idx = bytearray(W * H)
    for t in range(ntiles):
        px = decode_tile(data, t * tb, bpp)
        tx, ty = (t % cols) * 8, (t // cols) * 8
        for yy in range(8):
            row = (ty + yy) * W + tx
            for xx in range(8):
                idx[row + xx] = px[yy][xx]
    return W, H, idx


def render_tilesheet(data, bpp, palette, cols=16, bg=None):
    """palette: lista de (r,g,b); índice 0 = transparente. Devuelve (w,h,rgba)."""
    tb = tile_bytes(bpp)
    ntiles = max(1, len(data) // tb)
    rows = (ntiles + cols - 1) // cols
    W, H = cols * 8, rows * 8
    rgba = bytearray(W * H * 4)
    # fondo tipo tablero de ajedrez para ver la transparencia
    for y in range(H):
        for x in range(W):
            if bg is None:
                c = 40 if ((x // 4 + y // 4) & 1) else 60
                base = (y * W + x) * 4
                rgba[base:base+4] = bytes((c, c, c, 255))
            else:
                base = (y * W + x) * 4
                rgba[base:base+4] = bytes((*bg, 255))
    for t in range(ntiles):
        px = decode_tile(data, t * tb, bpp)
        tx, ty = (t % cols) * 8, (t // cols) * 8
        for yy in range(8):
            for xx in range(8):
                v = px[yy][xx]
                if v == 0:
                    continue  # transparente
                col = palette[v] if v < len(palette) else (255, 0, 255)
                base = ((ty + yy) * W + (tx + xx)) * 4
                rgba[base:base+4] = bytes((*col, 255))
    return W, H, rgba


# --------------------------------------------------------------------------------------
# Bancos clave y su paleta sugerida (para un render reconocible)
# --------------------------------------------------------------------------------------
def palette_row(pals, name, row):
    src = pals.get(name, [])
    chunk = src[row * 16:(row + 1) * 16]
    if len(chunk) < 16:
        chunk = (chunk + [(0, 0, 0)] * 16)[:16]
    return chunk

# banco -> (nombre legible, fuente de paleta, fila)
KEY_BANKS = {
    0x00: ("sprites_00", "sprites", 0),
    0x02: ("sprites_02", "sprites", 0),
    0x10: ("fg_10",      "foreground", 0),
    0x14: ("fg_14",      "foreground", 0),
    0x18: ("bg_18",      "background", 0),
    0x1C: ("fg_1c",      "foreground", 1),
    0x28: ("misc_28",    "objects", 0),
    0x32: ("mario",      "player", 0),
    0x33: ("mario_ow",   "player", 0),
}

# Profundidad de bits por banco: 3bpp por defecto; los bancos de overworld son 4bpp
# y los de texto/Layer-3 son 2bpp (deducido de los tamaños descomprimidos).
BANK_BPP = {0x28: 4, 0x29: 4, 0x2A: 4, 0x2B: 4, 0x2F: 2, 0x32: 4, 0x33: 4}
def bpp_for(idx):
    return BANK_BPP.get(idx, 3)

# Fila de CGRAM por defecto para cada banco (paletas reales 0-15).
# FG/BG usan 0-7; sprites 8-15; Mario la 8.
DEFAULT_ROW = {0x00: 9, 0x02: 9, 0x10: 8, 0x14: 2, 0x18: 1, 0x1C: 2, 0x28: 2, 0x32: 8, 0x33: 8}
def row_for(idx):
    return DEFAULT_ROW.get(idx, 2)

def upscale(w, h, rgba, s):
    if s <= 1:
        return w, h, rgba
    W, H = w * s, h * s
    out = bytearray(W * H * 4)
    for y in range(h):
        for x in range(w):
            px = rgba[(y * w + x) * 4:(y * w + x) * 4 + 4]
            for dy in range(s):
                row = (y * s + dy) * W + x * s
                for dx in range(s):
                    out[(row + dx) * 4:(row + dx) * 4 + 4] = px
    return W, H, out

def montage(sheets, gap=6, bg=(24, 20, 33)):
    """sheets: lista de (w,h,rgba) ya escaladas. Apila en columna centrada."""
    W = max(s[0] for s in sheets) + gap * 2
    H = sum(s[1] for s in sheets) + gap * (len(sheets) + 1)
    out = bytearray(W * H * 4)
    for i in range(0, len(out), 4):
        out[i:i+4] = bytes((*bg, 255))
    y = gap
    for (w, h, rgba) in sheets:
        x0 = (W - w) // 2
        for yy in range(h):
            dst = ((y + yy) * W + x0) * 4
            src = yy * w * 4
            out[dst:dst + w * 4] = rgba[src:src + w * 4]
        y += h + gap
    return W, H, out


def main():
    ap = argparse.ArgumentParser(description="Extrae GFX/paletas de una ROM de SMW (USA).")
    ap.add_argument("rom", help="ruta a la ROM .sfc/.smc")
    ap.add_argument("--out", default=os.path.join(os.path.dirname(__file__), "out"))
    ap.add_argument("--bpp", type=int, default=0, choices=(0, 2, 3, 4),
                    help="0 = auto por banco (recomendado)")
    ap.add_argument("--scale", type=int, default=3, help="escala de los PNG (nearest)")
    ap.add_argument("--all", action="store_true", help="volcar TODOS los bancos GFX00..GFX33")
    ap.add_argument("--emit-data", action="store_true",
                    help="además escribe out/smw_data.js (índices + paletas) para el visor interactivo")
    ap.add_argument("--probe", help="diagnóstico: banco (hex) renderizado a 2/3/4 bpp")
    ap.add_argument("--force", action="store_true", help="ignora el chequeo de hash")
    args = ap.parse_args()

    rom = Rom(args.rom)
    print("ROM sha1:", rom.sha1)
    if rom.sha1 != SMW_SHA1_US:
        msg = "  ¡AVISO! No coincide con SMW (USA) %s" % SMW_SHA1_US
        if not args.force:
            print(msg + "\n  Usa --force para continuar de todos modos.", file=sys.stderr)
            sys.exit(2)
        print(msg + " (continuando por --force)", file=sys.stderr)

    os.makedirs(args.out, exist_ok=True)
    gfx_dir = os.path.join(args.out, "gfx")
    os.makedirs(gfx_dir, exist_ok=True)
    pals = read_palettes(rom)
    cg = build_cgram(rom)  # 256 colores RGB = CGRAM real de un nivel estándar

    if args.probe is not None:
        idx = int(args.probe, 0)
        data = load_gfx(rom, idx)
        print("banco %#04x: %d bytes descomprimidos" % (idx, len(data)))
        name, psrc, prow = KEY_BANKS.get(idx, ("bank", "player", 0))
        pal = [(0, 0, 0)] + palette_row(pals, psrc, prow)[1:]
        for bpp in (2, 3, 4):
            W, H, rgba = render_tilesheet(data, bpp, pal)
            p = os.path.join(gfx_dir, "probe_%02x_%dbpp.png" % (idx, bpp))
            write_png(p, W, H, rgba)
            print("  ->", p, "(%dx%d, %d tiles)" % (W, H, len(data) // tile_bytes(bpp)))
        return

    # paletas
    with open(os.path.join(args.out, "palettes.json"), "w") as f:
        json.dump(pals, f, indent=1)
    # tira de paletas
    render_palette_strip(pals, os.path.join(args.out, "palettes.png"))

    banks = range(0x34) if args.all else sorted(KEY_BANKS)
    manifest = {"rom_sha1": rom.sha1, "scale": args.scale, "sheets": []}
    montage_sheets = []
    for idx in banks:
        try:
            data = load_gfx(rom, idx)
        except Exception as e:
            print("  banco %#04x: error %s" % (idx, e), file=sys.stderr); continue
        bpp = args.bpp or bpp_for(idx)
        name = KEY_BANKS.get(idx, ("gfx_%02x" % idx,))[0]
        prow = row_for(idx)
        pal = cgram_row(cg, prow)
        W, H, rgba = render_tilesheet(data, bpp, pal)
        W, H, rgba = upscale(W, H, rgba, args.scale)
        fn = "gfx_%02x_%s.png" % (idx, name)
        write_png(os.path.join(gfx_dir, fn), W, H, rgba)
        manifest["sheets"].append({
            "bank": idx, "name": name, "file": "gfx/" + fn, "bpp": bpp,
            "tileSize": 8, "cols": 16, "rows": (H // args.scale) // 8,
            "tiles": len(data) // tile_bytes(bpp),
            "cgramRow": prow,
        })
        montage_sheets.append((W, H, rgba))
        print("  banco %#04x %dbpp -> gfx/%s (%dx%d)" % (idx, bpp, fn, W, H))

    if montage_sheets:
        mw, mh, mrgba = montage(montage_sheets)
        write_png(os.path.join(args.out, "montage.png"), mw, mh, mrgba)
        print("  montaje -> montage.png (%dx%d)" % (mw, mh))

    if args.emit_data:
        import base64
        data_banks = []
        for idx in banks:
            try:
                data = load_gfx(rom, idx)
            except Exception:
                continue
            bpp = args.bpp or bpp_for(idx)
            name = KEY_BANKS.get(idx, ("gfx_%02x" % idx,))[0]
            W, H, ix = index_sheet(data, bpp)
            data_banks.append({"bank": idx, "name": name, "bpp": bpp,
                               "w": W, "h": H, "cols": 16, "rows": H // 8,
                               "tiles": len(data) // tile_bytes(bpp), "defRow": row_for(idx),
                               "px": base64.b64encode(bytes(ix)).decode("ascii")})
        payload = {"rom_sha1": rom.sha1, "cgram": cg, "palettes": pals, "banks": data_banks}
        with open(os.path.join(args.out, "smw_data.js"), "w") as f:
            f.write("window.SMW = " + json.dumps(payload) + ";\n")
        print("  datos -> smw_data.js (%d bancos)" % len(data_banks))

    with open(os.path.join(args.out, "manifest.json"), "w") as f:
        json.dump(manifest, f, indent=1)
    print("Listo. Salida en", args.out)


def render_palette_strip(pals, path, sw=12):
    order = ["sky", "background", "layer3", "foreground", "objects", "player", "sprites", "berry"]
    rows = []
    for name in order:
        cols = pals[name]
        for r in range((len(cols) + 15) // 16):
            rows.append(cols[r * 16:(r + 1) * 16])
    H = len(rows) * sw
    W = 16 * sw
    rgba = bytearray(W * H * 4)
    for ri, row in enumerate(rows):
        for ci in range(16):
            col = row[ci] if ci < len(row) else (0, 0, 0)
            for y in range(sw):
                for x in range(sw):
                    base = ((ri * sw + y) * W + (ci * sw + x)) * 4
                    rgba[base:base+4] = bytes((*col, 255))
    write_png(path, W, H, rgba)


if __name__ == "__main__":
    main()
