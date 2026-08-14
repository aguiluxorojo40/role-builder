# Auditoría de Role Builder

Auditoría con una premisa explícita: **no dar por hecho nada de lo que la app, el
README o los tests dicen que hacen**. Cada afirmación se contrasta con el código o
con evidencia ejecutable.

## Procedencia de este documento

El informe original se hizo en una **línea de historia distinta** (la del diorama,
sin base común con la actual), en una rama ya retirada. Al traerlo aquí no se copió
tal cual: **cada afirmación se volvió a comprobar contra este código**, y varias
habían dejado de ser ciertas. Lo que cambió respecto al informe original está
marcado con ⚠️ ACTUALIZADO.

## 1. Afirmaciones vs realidad

| Afirmación | Veredicto |
|---|---|
| README: "124 tests" | ❌ Falso: eran 535 en `core` cuando se auditó, y hoy son **690**. Cifra sin mantener, igual que el "34 tests" que decía el README original. ✅ CORREGIDO, y de paso el README ahora dice cómo sacar la cifra de verdad en vez de fiarse del número. |
| "Rechaza rutas maliciosas (zip-slip)" en `ZipIo` | ✔️ Cierto: comprobación canónica real (`target.canonicalFile.toPath().startsWith(canonicalDest.toPath())`) y test que la verifica. |
| "Validación: debe poder cargarse como proyecto completo" (importar zip) | ⚠️ Engañoso: solo validaba que el JSON deserializara. Un mapa con capas del tamaño equivocado importaba "bien" y reventaba después al dibujar. ✅ CORREGIDO: `GameMap` valida dimensiones y capas en `init`. |
| Aviso de "mapas huérfanos" | ❌ Con bug: `MapReachability` seguía solo `TransferPlayer` e ignoraba las conexiones por bordes. ✅ CORREGIDO + tests. |
| Efectos de sonido del juego | ❌ Incompleto: el motor encolaba `"coin"` y `"levelup"` y `SoundFx` no los registraba, así que la app los descartaba **en silencio**. Los tests del core afirmaban `soundQueue.contains("coin")` y pasaban: verificaban la cola, no que sonara. ✅ CORREGIDO. |
| Mapas "hasta 200×200" | ✔️ Cierto (`coerceIn(5, 200)` en el editor de mapas). |
| "3 ranuras de guardado" | ✔️ Cierto (`SLOT_COUNT = 3`). |
| "5 pistas de música" | ✔️ Cierto (`MusicTracks.ALL` tiene 5). |
| "`:app` no tiene ni un test" | ⚠️ ACTUALIZADO: ya no es cierto del todo. Hoy hay **23 tests** en `app/src/test` (sesión de ROM, almacén de assets SMW y deshacer del editor), y CI ejecuta `:app:testDebugUnitTest` antes de compilar. Pero el desequilibrio sigue siendo real: ~380 líneas de test para ~16.700 de código, frente a las ~15.500 / ~26.700 de `core`. |

## 2. Los siete bugs, y dónde están arreglados aquí

Los siete venían del informe original. **Se comprobó uno por uno que seguían vivos en
esta rama** antes de portarlos; ninguno se dio por bueno por estar arreglado allí.

1. **Sonidos mudos** (`coin`, `levelup`) — desalineación core↔app que ningún test de
   `:core` podía cazar. `SoundFx` los registra, con nota de mantenimiento: `NAMES`
   debe cubrir todo lo que encola el motor.
2. **Falsos huérfanos** — `MapReachability` recorre también los bordes conectados.
3. **Guardado no atómico** — `SaveIo`/`ProjectIo` escribían directo sobre el fichero
   final: un corte a media escritura corrompía la partida o el proyecto. Ahora
   temporal + rename. Es el de mayor gravedad de los siete: se pierde el trabajo del
   usuario.
4. **Carreras de datos UI↔GL** — `RpgEngine.tick()` corre en el hilo GL y Compose
   escribe input/pausa desde el de UI. Mitigado con `@Volatile`. Ver deuda §4.
5. **Path traversal al importar imágenes** — `SettingsTab` usaba el `DISPLAY_NAME`
   del content provider (que controla **otra app**) como ruta sin sanitizar: un
   nombre con `../` escribía fuera de `images/`.
6. **Proyectos corruptos que importaban "bien"** — `GameMap` valida en `init`;
   `tileAt` tolera índice de capa fuera de rango.
7. **Test debilitado** — el de invulnerabilidad aceptaba hasta 4 puntos de daño
   cuando el máximo real es 2.

Al portar el 6 hubo que hacer un trabajo que no venía en el informe: esta rama
construye mapas en cinco sitios (incluida la importación de niveles de SMW, que la
rama de auditoría no tenía), y un invariante nuevo puede romper lo que ya funciona.
Se comprobaron los cinco antes de meter el `require()`.

## 3. Los tests, puestos en duda

Lo bueno: la suite de `core` es real —ejercita el motor tick a tick, sin mocks— y
cubre serialización antigua, el proyecto demo y, en esta rama, la reconstrucción de
niveles de SMW con posiciones exactas y no solo recuentos.

Lo malo, y sigue siéndolo:

- **El desequilibrio `:app`**: ~16.700 líneas (renderer GL, activities, editor
  completo, audio) contra ~380 de test. Tres de los siete bugs de arriba —los dos de
  sonido y el path traversal— vivían exactamente ahí, donde casi ningún test mira.
  El módulo ha crecido más deprisa que su suite: cuando se auditó eran ~13.900
  líneas contra ~160.
- **Tests que pasan sin probar lo que prometen**: el contrato core↔app de nombres de
  sonido no lo verifica nadie (el core prueba su cola; la app no prueba su tabla).
  El mismo punto ciego se repitió en reachability: 7 tests, todos de transfers,
  ninguno de bordes — la suite había heredado la ceguera del código.
- **Aserciones blandas** en la parte de RPG: cotas del tipo `hp < hp0` detectan
  catástrofes, no regresiones finas.

### El punto ciego propio de esta rama: la ROM

Aquí hay una limitación estructural que el informe original no podía ver, porque esta
rama no existía:

**CI no puede comprobar nada de la extracción desde la ROM.** En el repositorio no hay
ni un byte de Nintendo —y así debe seguir—, así que todo lo que mide contra ella se
salta solo. En números: **37 ficheros de test consultan `SMW_ROM`**, y los **25 tests
de las 24 sondas `Zz*`** no ejercitan absolutamente nada sin ROM (los demás sí
corren, pero se saltan la parte que la necesita). El punto ciego no ha dejado de
crecer: cuando se auditó eran 19 ficheros y 6 sondas.

Lo que CI sí verifica es lo **sintético**: los tests de objetos de Layer 1 y Layer 2
plantan un nivel mínimo en una ROM vacía y comprueban posiciones exactas, sin un solo
dato con copyright. Eso cubre las rutinas de dibujo; no cubre que los punteros y las
tablas de la ROM real se estén leyendo donde toca.

Eso deja una parte de la verificación **fuera de CI y en manos de quien tenga la ROM**:
medir la cobertura de niveles, renderizar y mirar. Es una limitación asumida, no un
descuido, pero conviene tenerla escrita: un cambio que rompa la extracción puede pasar
CI en verde.

Mitigaciones que sí funcionan y conviene mantener:

- **Mirar, no contar.** "0 ids desconocidos" solo dice que cada objeto encontró una
  rutina, no que dibuje bien. Varias veces el recuento estaba en verde y el render
  enseñaba el fallo: las alas del bloque volador tapando el bloque, el fondo cortado a
  27 filas en los niveles verticales.
- **Comprobar que el test no es vacío.** Al menos una vez por rutina delicada, mutar
  la constante y ver el test fallar.
- **Sospechar del recuento redondo.** Un test escrito a partir de lo que uno *cree*
  que hace el juego falla contra el código correcto; pasó con los travesaños del
  lienzo grande, que los tapices ocultan casi enteros.

## 4. Deuda conocida (no corregida)

- **Modelo de hilos del motor**: `@Volatile` arregla visibilidad, no atomicidad. Las
  compras en tienda, `equip()` y `useItem()` mutan `state.items` desde el hilo de UI
  mientras el hilo GL puede iterar lo mismo. Lo correcto sería una cola de comandos
  UI→motor drenada al principio de `tick()`. Es un cambio de diseño y merece rama
  propia.
- **`PlayerActivity.data` sin estado observable**: tras "Guardar estilo visual" el
  `copy()` no recompone; los valores en vivo del renderer lo disimulan.
- **Autorun sin condiciones = soft-lock**: una página AUTORUN que no cambia sus
  propias condiciones se relanza cada tick para siempre (fiel a RPG Maker, pero el
  editor no avisa).
- **Zip sin límite de tamaño**: un zip-bomba compartido puede llenar el
  almacenamiento al importar. Falta tope de bytes descomprimidos.
- **Botón atrás en el juego**: sale de la actividad sin confirmación ni autoguardado.
- **`MusicPlayer.stopInternal`**: el `join(300)` puede expirar con el hilo aún
  escribiendo en el `AudioTrack`.
- ~~**Sin *mutation testing***~~ ✅ RESUELTO: el job de pitest que solo existía en la
  rama original ya está montado aquí (`:core:pitest`, informe publicado por CI). Es
  la forma honesta de poner número a lo del §3 en vez de opinar. Corre sin umbral a
  propósito: primero medir, y decidir el listón con datos reales.

## 5. Verificación

Lo que corre en GitHub Actions: tests de `:core` con cobertura (kover), **mutación
de `:core` (pitest)**, análisis estático (detekt, con SARIF a Code Scanning), tests
JVM de `:app`, CodeQL, un informe de deuda técnica y la compilación del APK. Lo que
**no** corre ahí, y hay que hacer con la ROM delante, está en el §3.
