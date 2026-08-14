# design/

La imagen de **social preview** del repositorio y sus fuentes. Es la que GitHub
enseña cuando alguien comparte el enlace en WhatsApp, Telegram, Discord o Twitter.

| Fichero | Qué es |
|---|---|
| `social-preview.png` | La imagen, 1280×640. Se sube a mano en **Settings → General → Social preview** |
| `render_social.py` | El programa que la genera. Única fuente de verdad: la imagen no se retoca a mano |
| `ESTRATOS_LATENTES.md` | El manifiesto de diseño del que sale la composición |

## Regenerar

```bash
cd design
pip install Pillow
python3 render_social.py      # escribe social-preview.png en esta carpeta
```

Las tipografías (Jura Light para el título, Geist Mono para las anotaciones) se
leen de una ruta local declarada en la constante `FONTS` del script; si no están,
ajústala antes de ejecutar.

## Qué representa

El cuadro grande es una **tesela de 16×16 a 4 bits por pixel**, y los cuatro
cuadrados monocromos son **su descomposición binaria real**, con sus pesos ×1, ×2,
×4 y ×8. No es decoración con aire técnico: es la misma aritmética que hace el
decodificador planar de `core/snes` al leer la ROM — el color de cada celda es la
suma de los cuatro bits que tiene debajo, y la tira de abajo a la izquierda es la
paleta de 16 entradas que los indexa.

El motivo se calcula con una fórmula (una onda triangular sobre una distancia
octogonal), así que **todo el dibujo es original**: ni un pixel procede de la ROM,
en coherencia con el resto del repositorio.
