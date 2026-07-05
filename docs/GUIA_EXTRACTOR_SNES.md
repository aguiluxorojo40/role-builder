# Guía del extractor de assets de SNES

Esta guía explica, **sin dar por hecho ningún conocimiento previo**, cómo sacar
gráficos de una ROM de Super Nintendo y convertirlos en un *tileset* de tu juego
dentro de Role Builder. Léela una vez de principio a fin; luego el proceso es de
un par de minutos.

---

## 1. ¿Qué hace y qué NO hace?

**Qué hace:** lee el archivo de una ROM (`.smc`/`.sfc`), interpreta sus bytes
como gráficos de 8×8 píxeles (tiles), les aplica colores y guarda el resultado
como una imagen PNG + un tileset que ya puedes pintar en tus mapas. Todo ocurre
**en tu teléfono**; la ROM no se copia al proyecto, solo los tiles que extraigas.

**Qué NO hace (importante):** no descomprime gráficos. Muchos juegos de SNES
guardan sus dibujos **comprimidos** (con algoritmos tipo LZ77/LZSS) para ahorrar
espacio. Un extractor de datos crudos como este solo puede mostrar los gráficos
que están **sin comprimir**. Esto es normal y le pasa a todas las herramientas de
este tipo: verás partes del juego a la primera y otras aparecerán como "nieve" o
ruido porque están comprimidas.

> Regla mental: si al recorrer la ROM ves basura por todos lados, no es un fallo
> de la app: es que ese juego comprime sus gráficos. Prueba otro juego o busca
> las zonas sin comprimir (fuentes de texto, iconos del HUD y logos suelen estarlo).

> **Ejemplo real (Secret of Mana):** es un juego que comprime casi todo su arte
> (hasta la intro se diseñó pensando en lo rápido que podían descomprimir). Al
> recorrerlo verás sobre todo ruido; el **Auto-buscar** encontrará solo unas
> pocas zonas con estructura. Es un mal candidato para practicar: elige un juego
> que no comprima sus gráficos y tendrás éxito a la primera.

---

## 2. Vocabulario mínimo

- **Tile**: el ladrillo básico del SNES, un cuadradito de **8×8 píxeles**. Todo
  se dibuja juntando tiles.
- **bpp** (*bits per pixel*): cuántos colores puede tener cada tile.
  - **2bpp** → 4 colores (fondos simples, texto).
  - **4bpp** → 16 colores (**el más común**: personajes y escenarios).
  - **8bpp** → 256 colores (poco habitual; Modo 7, gráficos de alto color).
  - Empieza **siempre probando 4bpp**.
- **Offset**: la posición (en bytes) dentro del archivo donde empiezan unos
  gráficos. Se escribe en decimal (`8192`) o en hexadecimal (`0x2000`). No hace
  falta que sepas hex: el botón **Auto-buscar** los encuentra por ti.
- **Paleta**: el conjunto de colores. El SNES guarda colores de 15 bits (formato
  CGRAM). La app **detecta paletas** dentro de la ROM automáticamente; si ninguna
  encaja, tienes una en escala de grises para ver la forma.
- **Índice 0 = transparente**: en los sprites del SNES el primer color es el
  fondo. La app lo guarda transparente para que el tile se integre en tus mapas.

---

## 3. Instalar la app sin Android Studio

No necesitas ordenador ni Android Studio: cada cambio se compila solo en la nube
(GitHub Actions) y genera un **APK instalable**.

1. Entra en la sección **Releases** del repositorio y descarga el APK de la
   release **`apk-snes-latest`** (archivo `role-builder-debug.apk`).
   - Alternativa: en la pestaña **Actions**, abre la última ejecución verde de tu
     rama y descarga el artefacto `role-builder-debug-apk` (viene en un .zip).
2. Pásalo a tu móvil Android e instálalo. La primera vez te pedirá permitir
   "instalar apps de orígenes desconocidos": acéptalo solo para esta instalación.
3. Ábrela. Es una *debug build*, perfecta para probar.

---

## 4. Consigue una ROM (legalmente)

Usa **volcados de tus propios cartuchos**. La app acepta archivos `.smc`, `.sfc`
o `.bin`. Si el archivo tiene una "cabecera de copiador" de 512 bytes, la app la
detecta sola; no tienes que hacer nada.

---

## 5. Extraer gráficos paso a paso

1. En Role Builder, abre tu proyecto y ve a **Ajustes** (la pestaña de la rueda).
2. Baja hasta **"Assets de ROM de SNES"** y pulsa **"Importar desde ROM de SNES"**.
3. Pulsa **"Elegir ROM"** y selecciona tu archivo. Verás la **cabecera** del
   juego (título, región, tamaño): si sale un título con sentido, la ROM se leyó
   bien.
4. Deja el **formato en 4bpp** para empezar.
5. Pulsa **"Auto-buscar gráficos"**. La app salta a la primera zona que parece
   contener dibujos sin comprimir. Usa **◀ ▶** para recorrer las demás zonas
   candidatas mirando la **vista previa** en vivo.
6. Cuando veas algo reconocible (un personaje, tiles de suelo, una fuente…):
   - Ajusta la **paleta** en el desplegable hasta que los colores cuadren (o deja
     escala de grises si solo quieres la forma).
   - Cambia **Columnas** si quieres reorganizar la rejilla, y **Nº de tiles**
     para recortar cuántos coges.
   - Afina el **offset** con pequeños desplazamientos si la imagen sale "cortada"
     (medio tile desplazada): a veces basta con sumar/restar unos bytes.
7. Escribe un **nombre** (p. ej. `heroe_snes`) y pulsa **"Guardar tileset"**.
8. El PNG queda en tu proyecto y el tileset aparece en **Base de datos → Tilesets**,
   donde puedes marcar qué tiles bloquean el paso o van "de pie" (2.5D). Ya puedes
   pintarlos en el **editor de mapas**.

---

## 6. Consejos para encontrar buenos gráficos

- **Prueba varios formatos.** Si a 4bpp se ve raro, prueba 2bpp: muchas fuentes e
  iconos son 2bpp.
- **Las fuentes y el HUD casi siempre están sin comprimir.** Son un buen primer
  objetivo para practicar.
- **La imagen "cortada" se arregla con el offset.** Si cada tile parece partido
  por la mitad, mueve el offset unos pocos bytes hasta que se alinee.
- **¿Ruido de colores o gráficos de verdad? Usa "Escala de grises".** Con la
  paleta equivocada, unos gráficos perfectamente decodificados **parecen ruido de
  colores** (cada índice recibe un color al azar). Pon la paleta en **"Escala de
  grises (ver formas)"**: traduce cada índice a un nivel de brillo ordenado, así
  que **verás la forma** aunque no sepas el color real. Si en gris distingues
  dibujos, has dado con gráficos auténticos: solo te falta elegir la paleta CGRAM
  para el color. Si en gris sigue siendo ruido aleatorio, ahí no hay gráficos (o
  están comprimidos).
- **La paleta correcta suele estar en la ROM.** Una vez confirmes la forma en
  gris, ve probando las entradas "CGRAM @ 0x…" del desplegable de paletas hasta
  que los colores cuadren.
- **Si ni en gris se ve nada**, ese juego comprime sus gráficos: prueba la
  descompresión (sección 6.5) o cambia de juego. Los homebrew y las demos suelen
  tener gráficos sin comprimir.

---

## 6.5 Gráficos comprimidos (descompresión)

Aquí está la clave de por qué antes solo se veía ruido en muchos juegos. La app
incluye un **framework de descompresión**: un motor común más un códec por
formato. Ahora mismo soporta **LC_LZ2** (el formato de Lunar Compress que usa
**Super Mario World** y muchos otros juegos para sus gráficos).

Cómo funciona en la pantalla de importación:

1. En **Descompresión** elige **Auto-detectar** (o **LC_LZ2** directamente).
2. En **Offset** pon dónde **empieza** el bloque comprimido. El truco: los bloques
   no están alineados a números redondos, así que hay que dar con el byte exacto.
   Desde el modo avanzado (abajo) puedes listar los offsets de bloques comprimidos.
3. Mira la vista previa: si aparece un dibujo, ¡acertaste! Guarda el tileset.

**Cómo se valida que un códec es el correcto:** la app decodifica la salida y mide
su *coherencia* (si "parece un dibujo"). El modo **Auto-detectar** prueba cada
códec y se queda con el que produce gráficos reales. Es el mismo juez que usa el
buscador de zonas, reutilizado para validar una descompresión.

> **Verificado con Super Mario World:** su fichero de gráficos GFX00 (0x1000 bytes
> descomprimidos) se extrae correctamente con LC_LZ2 desde el offset `0x4DDC6`;
> la fuente/HUD aparece hacia `0x5C6A3`. Prueba estos con `--decompress` para ver
> gráficos reales de Mario.

**Límite honesto:** cada juego puede usar un formato distinto. Si el tuyo no es
LC_LZ2 (p. ej. Secret of Mana usa un formato propio de Square), la descompresión
dará ruido hasta que se añada ese códec al framework. La arquitectura está
preparada para incorporar códecs nuevos uno a uno.

## 7. Para quien quiera el modo avanzado (línea de comandos)

Si algún día usas un ordenador, el mismo motor de extracción está disponible como
herramienta de escritorio (no necesita el editor):

```bash
# Autodetectar offsets con gráficos:
./gradlew :core:extractSnesTileset --args="--rom juego.sfc --format 4bpp --scan"

# Extraer (si omites --offset, se autodetecta el mejor):
./gradlew :core:extractSnesTileset --args="--rom juego.sfc --out salida --format 4bpp --palette-offset 0x100 --name terreno"

# Probar sin ninguna ROM (genera una de ejemplo):
./gradlew :core:extractSnesTileset --args="--demo salida"

# Localizar bloques COMPRIMIDOS (LC_LZ2) dentro de una ROM:
./gradlew :core:extractSnesTileset --args="--rom smw.sfc --format 4bpp --scan --decompress lc_lz2"

# Descomprimir y extraer un bloque (auto-detecta el códec):
./gradlew :core:extractSnesTileset --args="--rom smw.sfc --out out --offset 0x4DDC6 --decompress auto --format 4bpp"

# Demo de descompresión sin ROMs con copyright:
./gradlew :core:extractSnesTileset --args="--demo-compressed out"
```

Genera un PNG en `salida/images/` y un `*.tileset.json` que describe la rejilla.

---

## 8. Cómo funciona por dentro (resumen técnico)

- El SNES almacena cada tile 8×8 en **planos de bits entrelazados**: para 2bpp,
  dos bytes por fila (plano bajo primero); para 4bpp, dos pares de planos (bytes
  0–15 y 16–31); para 8bpp, cuatro pares (64 bytes). El decodificador reconstruye
  el índice de color de cada píxel combinando los bits de cada plano.
- Los **colores** se guardan en 15 bits `0bbbbbgg gggrrrrr` (BGR); se escalan a
  RGB de 8 bits para pintarlos.
- El **autodetector** recorre la ROM en ventanas, **decodifica** cada una como
  tiles y mide su **coherencia espacial**: qué fracción de píxeles contiguos
  comparten color. Un dibujo real tiene grandes zonas planas (coherencia alta);
  el ruido de datos comprimidos o de código casi no la tiene. Así, en un juego
  que comprime su arte, devuelve pocos candidatos —la respuesta honesta— en vez
  de mandarte a ruido. No descomprime nada: solo localiza lo que ya está claro.
- Toda esta lógica vive en `core/snes` (Kotlin puro) y está cubierta por tests
  JVM, así que se ejecuta igual en el móvil que en el escritorio.

### Fuentes

- [SnesLab — Graphics Format](https://sneslab.net/wiki/Graphics_Format)
- [SNESdev Wiki — Tiles](https://snes.nesdev.org/wiki/Tiles)
- [NESdev Wiki — Tile compression](https://www.nesdev.org/wiki/Tile_compression)
- [The Cutting Room Floor — Finding graphics](https://tcrf.net/Help:Contents/Finding_Content/Finding_graphics)
