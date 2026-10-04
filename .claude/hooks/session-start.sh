#!/bin/bash
# SessionStart hook — prepara las sesiones de Claude Code en la nube para SkillMatch:
#   1. Descarga las dependencias Maven del backend (quedan en ~/.m2).
#   2. Arranca el daemon de Docker.
#   3. Levanta MongoDB 8.0 en localhost:27017, requerido por la app y por AuthIntegrationTest.
# Idempotente: se puede ejecutar varias veces. En local (fuera de la nube) no hace nada.
set -euo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "$0")/../.." && pwd)}"
LOG_DIR="${TMPDIR:-/tmp}/skillmatch-session"
MONGO_IMAGE="mongo:8.0"
MONGO_CONTAINER="skillmatch-mongo"
mkdir -p "$LOG_DIR"

port_in_use() {
  (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null
}

# Borra un PID file si el proceso al que apunta no es el esperado. Al reanudar una sesión, el snapshot del
# contenedor conserva los PID files de la sesión anterior; si ese PID lo reutiliza otro proceso, dockerd cree
# que containerd sigue vivo, espera a que responda y se apaga a los ~15 s.
remove_stale_pidfile() {
  local pidfile=$1 expected=$2 pid
  [ -f "$pidfile" ] || return 0
  pid=$(cat "$pidfile" 2>/dev/null)
  if [ -z "$pid" ] || [ "$(cat "/proc/$pid/comm" 2>/dev/null)" != "$expected" ]; then
    rm -f "$pidfile"
  fi
}

start_dockerd() {
  docker info >/dev/null 2>&1 && return 0
  command -v dockerd >/dev/null 2>&1 || return 1
  for _attempt in 1 2; do
    remove_stale_pidfile /var/run/docker.pid dockerd
    remove_stale_pidfile /var/run/docker/containerd/containerd.pid containerd
    setsid nohup dockerd >>"$LOG_DIR/dockerd.log" 2>&1 </dev/null &
    for _ in $(seq 1 30); do
      docker info >/dev/null 2>&1 && return 0
      sleep 1
    done
    pkill -x dockerd 2>/dev/null
    sleep 2
  done
  return 1
}

mongo_ready() {
  docker exec "$MONGO_CONTAINER" mongosh --quiet --eval 'db.runCommand({ ping: 1 }).ok' 2>/dev/null | grep -q 1
}

start_mongo() {
  if docker ps --format '{{.Names}}' | grep -qx "$MONGO_CONTAINER"; then
    :
  elif docker ps -a --format '{{.Names}}' | grep -qx "$MONGO_CONTAINER"; then
    docker start "$MONGO_CONTAINER" >/dev/null
  else
    docker run -d --name "$MONGO_CONTAINER" --network host "$MONGO_IMAGE" --bind_ip 127.0.0.1 >/dev/null
  fi
  for _ in $(seq 1 30); do
    mongo_ready && return 0
    sleep 1
  done
  return 1
}

# 1. Dependencias Maven
if (cd "$PROJECT_DIR/backend" && mvn -B -ntp -q dependency:go-offline) >"$LOG_DIR/maven.log" 2>&1; then
  maven_status="dependencias en caché"
else
  maven_status="FALLÓ la descarga de dependencias (ver $LOG_DIR/maven.log)"
fi

# 2 y 3. Docker + MongoDB
if docker ps --format '{{.Names}}' 2>/dev/null | grep -qx "$MONGO_CONTAINER" && mongo_ready; then
  mongo_status="corriendo en localhost:27017 (contenedor $MONGO_CONTAINER)"
elif port_in_use 27017; then
  mongo_status="el puerto 27017 ya está ocupado por otro proceso; se usa ese servidor"
elif ! start_dockerd; then
  mongo_status="NO disponible: no se pudo iniciar dockerd (ver $LOG_DIR/dockerd.log)"
elif start_mongo >"$LOG_DIR/mongo.log" 2>&1; then
  mongo_status="corriendo en localhost:27017 (contenedor $MONGO_CONTAINER)"
else
  mongo_status="NO disponible: falló el contenedor $MONGO_IMAGE (ver $LOG_DIR/mongo.log)"
fi

# Resumen breve: la salida de este hook se añade al contexto de la sesión.
echo "Entorno SkillMatch: Maven -> $maven_status; MongoDB 8.0 -> $mongo_status."
