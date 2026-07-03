# Historial de versiones de Role Builder

Esquema de versionado: `0.<fase>.<parche>` — cada fase del motor es una
versión menor, con su nombre en clave. `versionCode = fase*100 + parche`.
Cada versión etiquetada (`v0.X.Y`) tiene release propia en GitHub con su
APK; la release rodante `apk-latest` siempre apunta a la última build de
la rama de desarrollo.

## v0.9.0 — «Diorama» (Fase 9)
Proyección 2.5D de las entrañas de Square Enix: el suelo se tumba hacia
atrás con foreshortening real y los personajes, enemigos y tiles «De pie»
(árboles, puertas...) se levantan como billboards anclados por los pies,
con sombra en la base y profundidad por orden Y. Inclinación configurable
por proyecto (0 = 2D clásico) y modo «De pie (2.5D)» en el editor de
tilesets.

## v0.8.0 — «Nube» (Fase 8)
Capas de parallax por mapa (fondo y niebla) con anclaje cámara↔mapa,
deriva automática y opacidad; nubes procedurales en la plantilla y editor
de capas en los ajustes del mapa.

## v0.7.0 — «Farol» (Fase 7)
Estilo HD-2D lite: post-procesado con tilt-shift (efecto maqueta), bloom,
viñeta y etalonaje cálido; luces 2D aditivas por evento con parpadeo,
sombras suaves y motas de luz ambientales. Interruptor por proyecto.

## v0.6.0 — «Juglar» (Fase 6)
Música chiptune 100% sintetizada (4 pistas), tinte/destello/clima/temblor
como comandos de evento, música y clima por mapa, pantalla de título,
guardado en 3 ranuras con metadatos y migración, y exportar/importar
proyectos como zip con validación.

## v0.5.0 — «Oro» (Fase 5)
Progresión y economía: experiencia y niveles con crecimiento de stats,
oro, equipo (arma/armadura con bonus), tiendas con compra/venta, nuevos
comandos y condición de oro, y sus editores y pantallas.

## v0.4.0 — «Pluma» (Fase 4)
Editor completo en el dispositivo: mapas táctiles con capas, eventos con
páginas/condiciones/comandos anidados, base de datos, passability del
tileset, ajustes e importación de imágenes.

## v0.3.0 — «Antorcha» (Fase 3)
Runtime Android: renderer OpenGL ES 3.0 con culling y orden por Y,
joystick y botones táctiles, HUD, menú de pausa y efectos de sonido
sintetizados.

## v0.2.0 — «Espada» (Fase 2)
Motor de juego puro en `:core`: movimiento con colisión, intérprete de
eventos, triggers, combate ARPG (melee, proyectiles, IA, drops) y
guardado serializable, con batería de tests JVM.

## v0.1.0 — «Pergamino» (Fase 1)
Esqueleto Gradle multi-módulo, modelo de datos serializable del proyecto
RPG y generador de assets de plantilla.
