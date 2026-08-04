#!/usr/bin/env python3
"""
Compara la traza REAL de un enemigo (enemy_trace.py, leída del emulador) con la que
produce NUESTRO motor, y da la diferencia como NÚMERO.

Esto es lo que convierte "pasa mis tests" en "coincide con el juego". Un test escrito
por quien escribió el motor solo prueba consistencia interna; esto prueba fidelidad,
que es lo que se estaba afirmando sin base.

Uso:
    # 1) traza real desde el emulador
    python3 scripts/enemy_trace.py --rom SMW.sfc --state casa.state --expect-id 0x37 \\
        --out real_boo.csv
    # 2) traza de nuestro motor (CSV con el mismo formato: frame,x,y[,vx,vy])
    ./gradlew :core:traceEnemy --args="--id 0x37 --frames 600 --out ours_boo.csv"
    # 3) comparar
    python3 scripts/enemy_compare.py --real real_boo.csv --ours ours_boo.csv --id 0x37

Qué informa:
  * Velocidad MEDIA real vs nuestra (px/frame) — detecta constantes mal puestas.
  * Error de posición medio y máximo (px) — detecta conducta distinta.
  * Veredicto explícito: COINCIDE / DIFIERE, con el umbral usado.

Nota honesta: no se pretende igualdad exacta frame a frame salvo que el port sea 1:1
de la rutina. El valor de este script es CUANTIFICAR la distancia, para no volver a
decir "está bien" sin evidencia.
"""

import argparse
import csv
import sys


def load(path: str, sprite_id: int | None) -> list[dict]:
    """Carga un CSV de traza y se queda con el sprite pedido (el primero si no se dice)."""
    with open(path) as f:
        rows = list(csv.DictReader(f))
    if not rows:
        print(f"{path}: vacío", file=sys.stderr)
        return []
    if sprite_id is not None and "id" in rows[0]:
        rows = [r for r in rows if int(r["id"], 0) == sprite_id]
    # Un solo sprite: si hay varias ranuras, quédate con la más frecuente.
    if rows and "slot" in rows[0]:
        slots = [r["slot"] for r in rows]
        best = max(set(slots), key=slots.count)
        rows = [r for r in rows if r["slot"] == best]
    return sorted(rows, key=lambda r: int(r["frame"]))


def avg_speed(rows: list[dict]) -> tuple[float, float]:
    """Velocidad media (px/frame) por diferencias de posición: no depende de unidades."""
    if len(rows) < 2:
        return (0.0, 0.0)
    dx = dy = 0.0
    for a, b in zip(rows, rows[1:]):
        dx += abs(float(b["x"]) - float(a["x"]))
        dy += abs(float(b["y"]) - float(a["y"]))
    n = len(rows) - 1
    return (dx / n, dy / n)


def main() -> int:
    ap = argparse.ArgumentParser(description="Compara traza real vs motor propio.")
    ap.add_argument("--real", required=True, help="CSV del emulador (enemy_trace.py).")
    ap.add_argument("--ours", required=True, help="CSV de nuestro motor.")
    ap.add_argument("--id", type=lambda s: int(s, 0), help="Id de sprite a comparar.")
    ap.add_argument("--tol", type=float, default=0.15,
                    help="Tolerancia de velocidad en px/frame (por defecto 0.15).")
    args = ap.parse_args()

    real = load(args.real, args.id)
    ours = load(args.ours, args.id)
    if not real or not ours:
        print("Sin datos que comparar.", file=sys.stderr)
        return 2

    rvx, rvy = avg_speed(real)
    ovx, ovy = avg_speed(ours)

    print(f"Frames: real {len(real)} · motor {len(ours)}")
    print(f"Velocidad media X: real {rvx:.3f} px/f · motor {ovx:.3f} px/f · dif {abs(rvx-ovx):.3f}")
    print(f"Velocidad media Y: real {rvy:.3f} px/f · motor {ovy:.3f} px/f · dif {abs(rvy-ovy):.3f}")

    # Error de posición sobre los frames comunes, normalizado al origen de cada traza
    # (lo que importa es la FORMA del movimiento, no dónde estaba colocado el enemigo).
    n = min(len(real), len(ours))
    if n >= 2:
        rx0, ry0 = float(real[0]["x"]), float(real[0]["y"])
        ox0, oy0 = float(ours[0]["x"]), float(ours[0]["y"])
        errs = [
            (((float(real[i]["x"]) - rx0) - (float(ours[i]["x"]) - ox0)) ** 2 +
             ((float(real[i]["y"]) - ry0) - (float(ours[i]["y"]) - oy0)) ** 2) ** 0.5
            for i in range(n)
        ]
        print(f"Error de posición: medio {sum(errs)/len(errs):.2f} px · máx {max(errs):.2f} px")

    ok = abs(rvx - ovx) <= args.tol and abs(rvy - ovy) <= args.tol
    print(f"\nVEREDICTO: {'COINCIDE' if ok else 'DIFIERE'} (tolerancia {args.tol} px/f)")
    if not ok:
        print("→ Ajusta las constantes del motor a la velocidad real, o porta la rutina.")
    return 0 if ok else 1


if __name__ == "__main__":
    raise SystemExit(main())
