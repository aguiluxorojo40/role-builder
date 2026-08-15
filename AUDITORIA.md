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
| "`:app` no tiene ni un test" | ⚠️ ACTUALIZADO (dos veces): ya no es cierto del todo. Hoy hay **28 tests** en `app/src/test` (sesión de ROM, almacén de assets SMW, deshacer del editor y el contrato de sonido con `:core`), y CI ejecuta `:app:testDebugUnitTest` antes de compilar. El desequilibrio sigue siendo real —461 líneas de test para 16.821 de código, frente a 13.091 / 27.432 de `core`—, pero **el ratio es la métrica equivocada** y perseguirlo lleva a escribir tests de Compose de bajo valor. El problema de verdad era que había lógica de dominio dentro de ficheros de UI, donde ninguna suite podía mirarla: `SnesImportDialog.kt` tenía 1.788 líneas y **2** `@Composable`. Lo puro se ha bajado a `:core` y ahí sí se prueba. El objetivo no es subir el ratio de `:app`: es que `:app` adelgace hasta que su ratio no signifique nada. |

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

  ✅ CORREGIDO el de los sonidos, y conviene decir **cómo**, porque el arreglo obvio no
  servía: un test que compare dos listas se desincroniza igual que las listas. Ahora hay
  una sola lista, `SoundEffects` en `:core`; el motor y el proyecto por defecto usan sus
  constantes en vez de literales, y la tabla de `:app` **es** esa lista. Desincronizarse
  dejó de ser detectable porque dejó de ser posible. Lo que sí queda como test
  (`AudioContractTest`) es el hueco que una lista compartida no cierra: que cada nombre
  tenga su propia forma de onda y no caiga en el pitido genérico, que un nombre libre
  desconocido suene a algo en vez de enmudecer, y que la cabecera del WAV cuadre —un WAV
  desalineado no carga, y enmudece el efecto igual que el bug original—.
- **Aserciones blandas** en la parte de RPG: cotas del tipo `hp < hp0` detectan
  catástrofes, no regresiones finas.

### El punto ciego propio de esta rama: la ROM

Aquí hay una limitación estructural que el informe original no podía ver, porque esta
rama no existía:

**CI no puede comprobar nada de la extracción desde la ROM.** En el repositorio no hay
ni un byte de Nintendo —y así debe seguir—, así que todo lo que mide contra ella se
salta solo. En números: **37 ficheros de test consultan `SMW_ROM`**, y los **25 tests
de las 24 sondas `Zz*`** no ejercitaban absolutamente nada sin ROM (los demás sí
corren, pero se saltan la parte que la necesita). El punto ciego no ha dejado de
crecer: cuando se auditó eran 19 ficheros y 6 sondas.

⚠️ ACTUALIZADO: lo de las sondas era peor de lo que decía este párrafo. No es solo que
no ejercitaran nada sin ROM: **no tenían un solo `assert`**. 2.862 líneas —el 18% de
todas las líneas de test de `:core`— con 25 `@Test` y 211 `println`, corriendo en cada
`:core:test` y saliendo en verde pasara lo que pasara. Como herramienta valen, y por eso
no se han tirado: son para MIRAR, que es justo lo que este documento defiende dos
párrafos más abajo. Pero estaban disfrazadas de suite. ✅ CORREGIDO: viven en un source
set propio (`core/src/sondas`) y se lanzan a mano con
`SMW_ROM=... ./gradlew :core:sondas`. Se siguen compilando en cada `:core:test` —fuera
de la compilación se pudrirían en silencio con el primer refactor, que es peor que el
problema de partida— pero ya no se ejecutan ni cuentan.

La suite de `:core` baja así de 714 a **689 tests**, y su tamaño honesto es de **13.091
líneas de test** para 27.432 de código, no las ~15.500 que este documento venía citando.

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

## 4. Deuda conocida

⚠️ ACTUALIZADO: esta sección se llamaba "(no corregida)" y ya no le pega. De los seis
puntos que quedaban vivos se han cerrado cuatro —el modelo de hilos, el estado
observable, el tope del zip y el `AudioTrack`—; siguen abiertos el soft-lock de AUTORUN
y el botón atrás. Lo corregido se deja tachado y explicado, no borrado: qué falló y por
qué es la parte que sirve para la próxima vez.

- ~~**Modelo de hilos del motor**~~ ✅ CORREGIDO: era el más grave de los que quedaban.
  `@Volatile` arreglaba visibilidad, no atomicidad, y las compras, `equip()` y
  `useItem()` mutaban `state.items` desde el hilo de UI mientras el hilo GL iteraba lo
  mismo. Está montada la cola de comandos UI→motor que este documento proponía
  (`EngineCommand`, drenada al principio de `tick()`): las siete entradas públicas
  encolan, y **solo el hilo del motor escribe el estado**. Validar y aplicar quedan
  separados a propósito —validar es una lectura pura que responde en el acto a quien
  pide; aplicar es del motor, que re-valida porque entre encolar y aplicar el mundo
  cambia—, así que el `Boolean` que devuelven pasa a significar "aceptada", no "hecha",
  y así está escrito en su KDoc.

  Faltaba la otra mitad, y se hizo después: **la UI seguía recorriendo `state.items`**
  a pelo en el menú de inventario y en la tienda. Encolar las escrituras no sirve de
  nada si las lecturas entran directas, así que el motor publica una foto inmutable
  (`RpgEngine.inventory`) y la UI lee de ahí.

  Lo vigilan 7 tests en `EngineCommandQueueTest`, y —siguiendo lo que pide el §3— se
  comprobó que **no son verdes de adorno**: mutando `submit()` para que aplicara en el
  acto, como el código de antes, los dos que fijan el aplazamiento fallan.
- ~~**`PlayerActivity.data` sin estado observable**~~ ✅ CORREGIDO: era un campo normal
  de la actividad, así que el `copy()` tras "Guardar estilo visual" no recomponía. Pasa
  a ser estado observable dentro de la composición. Lo difícil de este fallo era verlo:
  los valores en vivo del renderer lo disimulaban.
- **Autorun sin condiciones = soft-lock**: una página AUTORUN que no cambia sus
  propias condiciones se relanza cada tick para siempre (fiel a RPG Maker, pero el
  editor no avisa).
- ~~**Zip sin límite de tamaño**~~ ✅ CORREGIDO: hay tope de bytes descomprimidos y de
  entradas, contados **según se leen** y no sobre el `size` de la cabecera, que lo
  escribe quien manda el zip. Ante cualquier fallo se borra el destino, porque un
  import a medias deja una carpeta que la app enseña como proyecto sin serlo. Los topes
  son parámetros para que los tests los ejerciten sin fabricar medio giga: el central
  monta una zip-bomba de verdad y comprueba primero que el archivo comprimido **cabe**
  bajo el tope, que es lo que hace que la prueba demuestre algo.
- **Botón atrás en el juego**: ⚠️ A MEDIAS, y conviene decir qué mitad. Ya **no sale sin
  avisar**: pide confirmación, y el botón destacado es "Seguir jugando", porque quien
  llega a ese diálogo casi siempre ha rozado el atrás sin querer y lo que no puede estar
  a un toque es perder la partida.

  Lo que sigue sin haber es **autoguardado**, y no por olvido: guardar solo tiene
  sentido sobre una ranura concreta, y no existe la noción de "ranura actual". Elegir
  una por su cuenta —la 1, la última cargada— arriesga pisar una partida del jugador,
  que es exactamente la pérdida de datos que este punto viene a evitar. Hace falta antes
  decidir el modelo (ranura de autoguardado aparte, o recordar la ranura en uso); es una
  decisión de diseño, no una línea de código.
- ~~**`MusicPlayer.stopInternal`**~~ ✅ CORREGIDO: el `join(300)` expiraba y se liberaba
  el `AudioTrack` con el hilo aún escribiendo —un uso-después-de-liberar sobre un
  recurso nativo, que no lanza excepción: corrompe memoria fuera de la JVM—. Ahora cada
  hilo posee su `AudioTrack` y lo suelta él en su `finally`, avisando por un latch; el
  que para espera esa señal en vez de un plazo a ojo. Subir el timeout no habría
  arreglado nada, solo estrechado la ventana.
- ~~**Sin *mutation testing***~~ ✅ RESUELTO: el job de pitest que solo existía en la
  rama original ya está montado aquí (`:core:pitest`, informe publicado por CI). Es
  la forma honesta de poner número a lo del §3 en vez de opinar. Corre sin umbral a
  propósito: primero medir, y decidir el listón con datos reales. La primera medida
  está en el §6.

## 5. Verificación

Lo que corre en GitHub Actions: tests de `:core` con cobertura (kover), **mutación
de `:core` (pitest)**, análisis estático (detekt, con SARIF a Code Scanning), tests
JVM de `:app`, CodeQL, un informe de deuda técnica y la compilación del APK. Lo que
**no** corre ahí, y hay que hacer con la ROM delante, está en el §3.

## 6. Primera medida de mutación

Ejecución completa de `:core:pitest` **sin ROM** (las mismas condiciones que CI),
11 min 38 s, 16.840 mutantes generados contra 543 clases de test:

| Paquete | Mutantes | Cazados | % |
|---|---:|---:|---:|
| `core.snes` | 11.386 | 3.206 | **28,2 %** |
| `core.engine` | 3.665 | 1.900 | 51,8 % |
| `core.model` | 1.753 | 1.116 | 63,7 % |
| `core.io` | 36 | 21 | 58,3 % |
| **TOTAL** | **16.840** | **6.243** | **37,1 %** |

**El 37 % global no se lee como "la suite es mala": se lee como el §3, medido.** El
grueso de `core` es lectura de la ROM, y sus tests se saltan solos sin ella, así que
esos mutantes son incazables por construcción en este entorno. Se ve clase a clase:
`SmwTitleScreen`, `SmwOverworldAnim`, `SmwMode7Boss` y `SmwBakedAssets` marcan **0 %**;
`SmwMusicRenderer` 3,3 %; `SmwDsp` 7,4 %; `SmwEnemyGraphics` 9,5 %. Ese es el punto
ciego del §3 con un número encima, no una sorpresa.

Donde la lógica es pura y los tests pueden morder, la suite aguanta bien:
`Interpreter` 86,8 %, `SmwBlockBehavior` 91,7 %, `SmwBlockCollision` 91,4 %,
`SmwLevelStart` 89,2 %, `GameState` 89,3 %, `MapReachability` 85,7 %,
`snes.compression` 68,5 %.

Lo accionable, entonces, no es "subir el 37 %" —que premiaría escribir tests de ROM
imposibles de ejecutar en CI— sino mirar los supervivientes **dentro de los paquetes
que sí se ejercitan**: `core.engine` al 51,8 % es el objetivo honesto, y encaja con
lo que ya decía el §3 sobre las aserciones blandas del RPG.
