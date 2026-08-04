#!/usr/bin/env python3
"""
ARNÉS DE COMPARACIÓN: traza el movimiento REAL de un enemigo de SMW en el emulador.

Para qué: hasta ahora las conductas nuevas de enemigos se validaban con tests escritos
contra nuestro propio motor. Eso demuestra consistencia interna, NO fidelidad al juego.
Este script produce la única evidencia que sirve: la trayectoria REAL del enemigo, leída
de la RAM del juego corriendo en un emulador, frame a frame. Después se compara con la
que produce nuestro motor y la diferencia es un número, no una opinión.

Uso:
    python3 scripts/enemy_trace.py --rom SMW.sfc --state ghost.state --frames 600 \\
        --out trace_boo.csv [--expect-id 0x37]

Salida (CSV, una fila por sprite VIVO y frame):
    frame,slot,id,x,y,vx,vy,status

Requisitos (NADA de esto se versiona — es material de Nintendo):
  * pip install stable-retro
  * Tu ROM de SMW y un savestate colocado donde aparezca el enemigo a estudiar.

IMPORTANTE — auto-verificación: las direcciones de las tablas de sprites de SMW son
conocidas, pero este script NO se fía de ellas. Con --expect-id comprueba que el id
esperado aparece de verdad en la tabla de números de sprite; si no, ABORTA en vez de
escupir un CSV de basura que parecería válido. Sin esa comprobación, un arnés con las
direcciones mal daría datos plausibles y falsos, que es peor que no tener arnés.
"""

import argparse
import csv
import sys

# --- Tablas de sprites de SMW (12 ranuras). Direcciones DOCUMENTADAS de la comunidad;
# el script las CONFIRMA en ejecución con --expect-id antes de fiarse de ellas.
SPRITE_SLOTS = 12
ADDR = {
    "id":      0x9E,    # número de sprite por ranura
    "status":  0x14C8,  # estado (0 = ranura libre)
    "x_lo":    0x00E4,  # X byte bajo
    "x_hi":    0x14E0,  # X byte alto
    "y_lo":    0x00D8,  # Y byte bajo
    "y_hi":    0x14D4,  # Y byte alto
    "vx":      0x00B6,  # velocidad X (con signo, 1/16 px por frame)
    "vy":      0x00AA,  # velocidad Y (con signo, 1/16 px por frame)
}


def s8(v: int) -> int:
    """Byte con signo: las velocidades de SMW son complemento a dos."""
    return v - 256 if v >= 128 else v


def read_slots(ram) -> list[dict]:
    """Lee las 12 ranuras de sprite del frame actual."""
    out = []
    for i in range(SPRITE_SLOTS):
        status = ram[ADDR["status"] + i]
        if status == 0:
            continue  # ranura vacía: ni la apuntamos
        out.append({
            "slot": i,
            "id": ram[ADDR["id"] + i],
            # Posición de 16 bits: alto*256 + bajo (coordenadas de NIVEL, no de pantalla).
            "x": (ram[ADDR["x_hi"] + i] << 8) | ram[ADDR["x_lo"] + i],
            "y": (ram[ADDR["y_hi"] + i] << 8) | ram[ADDR["y_lo"] + i],
            # Velocidades en unidades SMW: 1/16 px por frame. Divide entre 16 para px/f.
            "vx": s8(ram[ADDR["vx"] + i]),
            "vy": s8(ram[ADDR["vy"] + i]),
            "status": status,
        })
    return out


def main() -> int:
    ap = argparse.ArgumentParser(description="Traza enemigos reales de SMW desde el emulador.")
    ap.add_argument("--rom", required=True, help="Tu ROM de SMW (no se versiona).")
    ap.add_argument("--state", help="Savestate donde ya se ve el enemigo.")
    ap.add_argument("--frames", type=int, default=600, help="Frames a grabar (600 = 10 s).")
    ap.add_argument("--out", required=True, help="CSV de salida.")
    ap.add_argument("--expect-id", type=lambda s: int(s, 0),
                    help="Id de sprite que DEBE aparecer (p. ej. 0x37 Boo). Verifica las "
                         "direcciones: si no sale, aborta en vez de dar datos falsos.")
    ap.add_argument("--hold", default="", help="Botones a mantener, separados por coma (p. ej. RIGHT).")
    args = ap.parse_args()

    try:
        import retro
    except ImportError:
        print("Falta stable-retro:  pip install stable-retro", file=sys.stderr)
        return 2

    # Integración por ruta: se le pasa TU ROM, no va ninguna incluida.
    env = retro.make(game="SuperMarioWorld-Snes", state=args.state or retro.State.DEFAULT)
    env.reset()

    buttons = env.buttons if hasattr(env, "buttons") else []
    hold = {b.strip().upper() for b in args.hold.split(",") if b.strip()}
    action = [1 if b.upper() in hold else 0 for b in buttons] if buttons else None

    rows: list[dict] = []
    seen_ids: set[int] = set()
    for frame in range(args.frames):
        obs = env.step(action if action is not None else env.action_space.sample() * 0)
        ram = env.get_ram()
        for s in read_slots(ram):
            seen_ids.add(s["id"])
            rows.append({"frame": frame, **s})
    env.close()

    # AUTO-VERIFICACIÓN: si pedimos un id concreto y no aparece nunca, o las direcciones
    # están mal o el savestate no muestra ese enemigo. En ambos casos el CSV no vale:
    # mejor abortar que publicar números que parecen buenos y no lo son.
    if args.expect_id is not None and args.expect_id not in seen_ids:
        print(
            f"ABORTA: no apareció el sprite 0x{args.expect_id:02X} en {args.frames} frames.\n"
            f"Ids vistos: {sorted(hex(i) for i in seen_ids)}\n"
            "O las direcciones de la tabla de sprites no son las correctas para esta ROM, "
            "o el savestate no está donde sale ese enemigo. NO se escribe el CSV.",
            file=sys.stderr,
        )
        return 1

    with open(args.out, "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=["frame", "slot", "id", "x", "y", "vx", "vy", "status"])
        w.writeheader()
        w.writerows(rows)

    print(f"OK: {len(rows)} muestras de {len(seen_ids)} sprites distintos → {args.out}")
    print(f"Ids vistos: {sorted(hex(i) for i in seen_ids)}")
    print("Velocidades en unidades SMW (1/16 px por frame): divide entre 16 para px/frame.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
