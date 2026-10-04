#!/bin/bash
# Detiene la API y el frontend iniciados por start.sh. MongoDB se deja corriendo (lo usan los tests).
set -uo pipefail

RUN_DIR="${TMPDIR:-/tmp}/skillmatch-run"

stop_service() {
  local name=$1 pid_file="$RUN_DIR/$1.pid"
  if [ -f "$pid_file" ]; then
    # start.sh lanza cada servicio con setsid: matar el grupo de procesos detiene también el JVM hijo de Maven.
    kill -- "-$(cat "$pid_file")" 2>/dev/null && echo "$name detenido" || echo "$name no estaba corriendo"
    rm -f "$pid_file"
  else
    echo "$name: sin PID registrado (no fue iniciado por start.sh)"
  fi
}

stop_service backend
stop_service frontend
