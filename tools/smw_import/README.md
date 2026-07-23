# smw_import — ingesta de assets de Super Mario World

Herramienta local para **extraer los gráficos y paletas de tu propia ROM de SMW (USA)**
y convertirlos a PNG + JSON que el editor puede usar. Es el primer paso del motor
"Lunar Magic × Mario Maker 2": tomar los assets reales de SMW.

## ⚠️ Copyright

La ROM y todo lo que sale de ella (gráficos, paletas, niveles) son propiedad de Nintendo.

- **Este script (`extract_smw.py`) es código propio** y se versiona en el repo.
- **La ROM y la salida (`out/`) NO se suben** — están en `.gitignore`. No se redistribuyen.
- Es el mismo principio que el proyecto `smw` (snesrev/smw): extraes de *tu* copia, para
  *tu* uso; nunca se comparte el material con copyright.

## Uso

```bash
# extrae los bancos clave a out/  (PNGs a 3×, palettes.json, montage.png)
python3 extract_smw.py "/ruta/a/Super Mario World (USA).sfc"

# todos los bancos + datos para el visor interactivo (out/smw_data.js)
python3 extract_smw.py ROM.sfc --all --emit-data

# diagnóstico de un banco a 2/3/4 bpp
python3 extract_smw.py ROM.sfc --probe 0x14
```

Requisitos: **solo Python 3** (sin dependencias — trae su propio escritor de PNG).

La ROM esperada es SMW (USA), sha1 `6b47bb75d16514b6a476aa0c73a683a2a4c18765`.
Usa `--force` para otras (p. ej. hacks de Lunar Magic), bajo tu responsabilidad.

## Qué produce (en `out/`, ignorado por git)

| Archivo            | Contenido |
|--------------------|-----------|
| `gfx/gfx_XX_*.png` | Hojas de tiles 8×8 reales, decodificadas y coloreadas |
| `palettes.json`    | Todas las paletas globales como RGB |
| `palettes.png`     | Tira visual de las paletas |
| `montage.png`      | Montaje de los bancos clave |
| `smw_data.js`      | Índices + paletas para `preview.html` (con `--emit-data`) |
| `manifest.json`    | Describe cada hoja (banco, bpp, columnas/filas, paleta) |

## Visor interactivo

`preview.html` es un visor tipo YY-CHR: elige banco, paleta (con *palette-swap* en vivo)
y zoom. Ábrelo tras generar `out/smw_data.js`:

```bash
python3 extract_smw.py ROM.sfc --all --emit-data
# abre preview.html en un navegador (necesita out/smw_data.js al lado)
```

El visor **no** contiene arte de Nintendo: lo lee en tiempo real de `out/smw_data.js`
(que sí es local). Por eso el visor se puede versionar, pero los datos no.

## Cómo funciona (formato SMW)

- **Direcciones y descompresor** adaptados de `smw/assets/{util,compile_resources}.py`
  (proyecto snesrev/smw). Los GFX están comprimidos con LC_LZ2 (`decomp`).
- **GFX 8×8**: tablas de punteros en `0xB992/0xB9C4/0xB9F6` (+ GFX32/33). La mayoría de
  bancos son **3bpp** (24 bytes/tile, 128 tiles = 3072 bytes); el overworld es 4bpp y
  el texto/Layer-3 es 2bpp.
- **Paletas**: BGR555 en offsets conocidos (Sky `0xB0A0`, Fondo `0xB0B0`, Primer plano
  `0xB190`, Sprites `0xB318`, Jugador `0xB2C8`…).
- **Map16** (bloques 16×16) en `0xD8000` — siguiente paso (ver `INTEGRATION.md`).
