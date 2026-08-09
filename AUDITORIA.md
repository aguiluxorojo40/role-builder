# Auditoría de Role Builder — agosto 2026

Auditoría con premisa explícita: **no dar por hecho nada de lo que la app o sus
tests dicen que hacen**. Cada afirmación se contrastó con el código o con
evidencia ejecutable. Los arreglos aplicados en esta rama se marcan como
✅ CORREGIDO; lo demás queda como deuda documentada.

## 1. Afirmaciones vs realidad

| Afirmación | Veredicto |
|---|---|
| README: "34 tests" | ❌ Falso: había 97 (`grep -c @Test`). Cifra sin mantener. ✅ CORREGIDO en el README. |
| "Rechaza rutas maliciosas (zip-slip)" en ZipIo | ✔️ Cierto: comprobación canónica + test que lo verifica. |
| "Validación: debe poder cargarse como proyecto completo" (importar zip) | ⚠️ Engañoso: solo validaba que el JSON deserializara. Un mapa con capas de tamaño incorrecto importaba "bien" y reventaba después al dibujar (`layers[layer][y*width+x]`). ✅ CORREGIDO: `GameMap` valida dimensiones y capas en `init`; el import corrupto ahora falla y limpia. |
| Aviso de "mapas huérfanos" al pulsar ▶ | ❌ Con bug: `MapReachability` solo seguía `TransferPlayer` e **ignoraba las conexiones por bordes** (`edgeEast`…), la funcionalidad del último commit. Dos zonas conectadas solo por bordes daban un falso aviso de huérfano. ✅ CORREGIDO + tests. |
| Efectos de sonido del juego | ❌ Incompleto: el motor encola `"coin"` (comprar/vender) y `"levelup"`, pero `SoundFx` no los registraba → la app los descartaba en silencio. Los tests del core afirman `soundQueue.contains("coin")` y pasan: **verifican la cola, no que suene**. ✅ CORREGIDO: ambos sonidos sintetizados y registrados. |
| Mapas "hasta 200×200" | ✔️ Cierto (`coerceIn(5, 200)` en el editor). |
| "3 ranuras de guardado" / "5 pistas" / plantilla jugable | ✔️ Ciertos (SLOT_COUNT=3; TRACKS y MusicTracks coinciden clave a clave; DemoProjectTest recorre la plantilla de verdad). |

## 2. Bugs corregidos en esta rama

1. **Sonidos mudos** (`coin`, `levelup`) — desalineación core↔app sin test posible
   desde `:core`. Arreglado en `SoundFx` con nota de mantenimiento: `NAMES` debe
   cubrir todo lo que encola el motor.
2. **Falsos huérfanos** — `MapReachability` ahora recorre también los bordes
   conectados (+2 tests, incluido borde hacia mapa inexistente).
3. **Path traversal al importar imágenes** — `SettingsTab` usaba el
   `DISPLAY_NAME` del content provider (controlado por otra app) como ruta sin
   sanitizar: un nombre con `../` escribía fuera de `images/`. Ahora se toma el
   nombre base y se filtran caracteres.
4. **Proyectos corruptos que importaban "bien"** — `GameMap` valida en `init`
   (dimensiones > 0, capas del tamaño exacto); `tileAt` tolera índice de capa
   fuera de rango (el renderer lee la capa 1 sin comprobarla). +3 tests y un
   test de import extremo a extremo que verifica la limpieza del destino.
5. **Guardado no atómico** — `SaveIo`/`ProjectIo` escribían directo sobre el
   archivo final: un cierre a mitad de escritura corrompía la partida o el
   proyecto. Ahora: temporal + rename.
6. **Carreras de datos UI↔GL** — `RpgEngine.tick()` corre en el hilo GL y
   Compose escribe input/pausa desde el hilo de UI sin sincronización; solo
   `soundQueue` era concurrente. Mitigado: `@Volatile` en `inputX/Y`,
   `actionPressed`, `secondaryPressed`, `paused`, `notice` (visibilidad de
   pulsaciones). **Ver deuda pendiente §4.**
7. **Test debilitado** — el test de invulnerabilidad aceptaba hasta 4 puntos de
   daño cuando el máximo real es 2; ahora la cota es exacta.

## 3. Los tests, puestos en duda

Lo bueno: la suite del core es real (ejercita el motor tick a tick, no mocks),
cubre serialización legacy y el proyecto demo. Lo malo:

- **`:app` tiene 0 tests** sobre ~5.900 líneas: renderer GL, activities,
  editor completo, audio. Los dos bugs de sonido y el path traversal vivían
  exactamente ahí — donde ningún test mira.
- **Tests que "pasan" sin probar lo que prometen**: el contrato core↔app de
  nombres de sonido no lo verifica nadie (el core testea su cola; la app no
  testea su tabla). Mismo patrón de punto ciego en reachability: 7 tests, todos
  de transfers, ninguno de bordes — la suite heredó la ceguera del código.
- **Aserciones blandas**: cotas como `1..2*attack`, `>= 4` iteraciones del
  paralelo, `hp < hp0`. Detectan catástrofes, no regresiones finas.
- **Determinismo a medias**: los tests inyectan `Random(seed)`, bien; pero
  `SaveIo.save` usa `System.currentTimeMillis()` y el motor depende de dt fijo
  de los helpers — nada testea dt variables (el clamp de 0.05 s del renderer
  no tiene test porque vive en `:app`).

### ¿Hace falta redundancia para "testear los tests"? Sí — y ya está montada

La respuesta honesta no es una opinión sino una medición: esta rama añade
**mutation testing con pitest** al CI (job "Mutación de :core"). Pitest muta el
bytecode (invierte condiciones, elimina llamadas, cambia constantes) y
re-ejecuta la suite; **cada mutante superviviente es un punto donde los tests
pasan con el código roto**. El job publica el informe HTML como artefacto y un
resumen (generados/cazados/supervivientes) en el summary del workflow.

Política propuesta: medir 2-3 ejecuciones, fijar un umbral realista
(`mutationThreshold`) y a partir de ahí bloquear el build si la calidad de la
suite retrocede. Hoy el job **mide sin bloquear**, a propósito: primero datos,
luego el listón.

## 4. Deuda conocida (no corregida aquí)

- **Modelo de hilos del motor**: `@Volatile` arregla visibilidad, no
  atomicidad. Compras en tienda, `equip()` y `useItem()` mutan `state.items`
  desde el hilo de UI mientras el hilo GL puede iterar el mismo mapa
  (`updateDrops`, guardado). Lo correcto: una cola de comandos de UI→motor
  drenada al inicio de `tick()`, o ejecutar el tick y las mutaciones en un
  único hilo. Cambio de diseño; merece rama propia.
- **`PlayerActivity.data` sin estado observable**: tras "Guardar estilo
  visual", el `copy()` no recompone (los valores en vivo del renderer lo
  disimulan). Reinciar partida usa el objeto nuevo por casualidad de captura.
- **Autorun sin condiciones = soft-lock**: una página AUTORUN que no cambia
  sus propias condiciones se relanza cada tick para siempre (fiel a RPG Maker,
  pero el editor no avisa).
- **Zip sin límite de tamaño**: un zip-bomba compartido puede llenar el
  almacenamiento del dispositivo al importar (DoS local). Poner tope de bytes
  descomprimidos.
- **Botón atrás en el juego**: sale de la actividad sin confirmación ni
  autoguardado.
- **`MusicPlayer.stopInternal`**: `join(300)` puede expirar con el hilo aún
  escribiendo en el `AudioTrack` (~0,5 s de solape posible al cambiar pista).

## 5. Verificación

Todo lo anterior se valida en GitHub Actions (el workflow ahora también admite
`workflow_dispatch`): tests de `:core` (los 97 previos + los nuevos),
compilación del APK y el nuevo job de mutación con su informe descargable.
