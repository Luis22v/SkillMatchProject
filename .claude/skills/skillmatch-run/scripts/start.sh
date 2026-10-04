#!/bin/bash
# Levanta SkillMatch completo para pruebas end-to-end:
#   MongoDB (localhost:27017) -> API Spring Boot (localhost:8080) -> frontend estático (localhost:5500)
# Idempotente: si un servicio ya está escuchando, lo reutiliza.
set -euo pipefail

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(git -C "$(dirname "$0")" rev-parse --show-toplevel)}"
RUN_DIR="${TMPDIR:-/tmp}/skillmatch-run"
API_PORT=8080
WEB_PORT=5500
mkdir -p "$RUN_DIR"

port_in_use() {
  (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null
}

wait_for_http() {
  local url=$1 timeout=$2
  for _ in $(seq 1 "$timeout"); do
    [ "$(curl -s -o /dev/null -w '%{http_code}' --max-time 2 "$url")" = "200" ] && return 0
    sleep 1
  done
  return 1
}

# 1. MongoDB: lo gestiona el hook de sesión (Docker en la nube); en local debe estar instalado como servicio.
if ! port_in_use 27017; then
  echo "MongoDB no responde en :27017; ejecutando el hook de sesión para levantarlo..."
  CLAUDE_CODE_REMOTE=true CLAUDE_PROJECT_DIR="$PROJECT_DIR" "$PROJECT_DIR/.claude/hooks/session-start.sh"
  port_in_use 27017 || { echo "ERROR: MongoDB sigue sin responder en :27017" >&2; exit 1; }
fi
echo "MongoDB   -> localhost:27017"

# 2. API Spring Boot
if port_in_use "$API_PORT"; then
  echo "API       -> ya estaba corriendo en http://localhost:$API_PORT"
else
  (
    cd "$PROJECT_DIR/backend"
    # Secreto aleatorio por ejecución (solo local): evita la clave efímera, que cambiaría en cada reinicio
    # de DevTools al recompilar e invalidaría los tokens del frontend.
    export JWT_SECRET="${JWT_SECRET:-$(head -c 48 /dev/urandom | base64 | tr -d '\n')}"
    setsid nohup mvn -B -ntp -q spring-boot:run >"$RUN_DIR/backend.log" 2>&1 </dev/null &
    echo $! >"$RUN_DIR/backend.pid"
  )
  echo "API       -> arrancando (log: $RUN_DIR/backend.log)..."
  if ! wait_for_http "http://localhost:$API_PORT/api/jobs/recent" 180; then
    echo "ERROR: la API no respondió en 180 s. Últimas líneas del log:" >&2
    tail -30 "$RUN_DIR/backend.log" >&2
    exit 1
  fi
  # DataSeeder (CommandLineRunner) corre DESPUÉS de que Tomcat ya responde: hasta que termina no hay
  # usuarios para hacer login. Con la BD vacía tarda unos segundos; si ya hay datos, se salta al instante.
  echo "API       -> respondiendo; esperando a que DataSeeder termine..."
  seed_done=false
  for _ in $(seq 1 120); do
    if grep -qE "Seed completado|Saltando seed|Error durante el seed" "$RUN_DIR/backend.log"; then
      seed_done=true
      break
    fi
    sleep 1
  done
  $seed_done || { echo "ERROR: DataSeeder no terminó en 2 min (ver $RUN_DIR/backend.log)" >&2; exit 1; }
  grep -q "Error durante el seed" "$RUN_DIR/backend.log" && echo "AVISO: DataSeeder falló (ver $RUN_DIR/backend.log)" >&2
  echo "API       -> lista en http://localhost:$API_PORT (Swagger: /swagger-ui.html)"
fi

# 3. Frontend estático. Se sirve SkillMatch/src para que ../assets/ resuelva igual que con Live Server.
if port_in_use "$WEB_PORT"; then
  echo "Frontend  -> ya estaba corriendo en http://localhost:$WEB_PORT/pages/index.html"
else
  setsid nohup python3 -m http.server "$WEB_PORT" --bind 127.0.0.1 --directory "$PROJECT_DIR/SkillMatch/src" \
    >"$RUN_DIR/frontend.log" 2>&1 </dev/null &
  echo $! >"$RUN_DIR/frontend.pid"
  wait_for_http "http://localhost:$WEB_PORT/pages/index.html" 15 || { echo "ERROR: el frontend no arrancó" >&2; exit 1; }
  echo "Frontend  -> listo en http://localhost:$WEB_PORT/pages/index.html"
fi

echo "Login de prueba: usuario1@skillmatch.com / empresa1@skillmatch.com — contraseña: password123"
