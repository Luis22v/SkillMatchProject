---
name: skillmatch-run
description: Levanta SkillMatch completo (MongoDB + API Spring Boot en :8080 + frontend en :5500), toma capturas de pantalla con Playwright (con login real por la UI) y reporta errores de consola y peticiones HTTP fallidas. Usar para verificar de punta a punta un cambio de API o de frontend, ver cómo queda una página, hacer smoke tests de endpoints o depurar un fallo que solo aparece con la app corriendo.
---

# SkillMatch: ejecutar y verificar la app

Los scripts viven en `${CLAUDE_SKILL_DIR}/scripts/` y funcionan desde cualquier directorio.

## 1. Arrancar

```bash
bash "${CLAUDE_SKILL_DIR}/scripts/start.sh"
```

- Idempotente: reutiliza lo que ya esté escuchando en :27017, :8080 y :5500.
- Si MongoDB no responde, ejecuta el hook de sesión (`.claude/hooks/session-start.sh`) para levantarlo en Docker.
- Espera a que **termine `DataSeeder`**, no solo a que Tomcat responda: el seed corre después del arranque (unos
  segundos con la BD vacía; se salta si ya hay datos). Arranque completo típico: ~15 s con la BD vacía, ~6 s con datos.
- Logs: `${TMPDIR:-/tmp}/skillmatch-run/backend.log` y `frontend.log`.

URLs: API `http://localhost:8080/api`, Swagger `http://localhost:8080/swagger-ui.html`,
frontend `http://localhost:5500/pages/index.html`.

## 2. Verificar

**API** (smoke test con un token real):

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"usuario1@skillmatch.com","password":"password123"}' | python3 -c 'import sys,json; print(json.load(sys.stdin)["token"])')
curl -s localhost:8080/api/users/me -H "Authorization: Bearer $TOKEN"
curl -s localhost:8080/api/applications/my-applications -H "Authorization: Bearer $TOKEN"
```

Cuentas sembradas: `usuarioN@skillmatch.com` (rol USER) y `empresaN@skillmatch.com` (rol EMPRESA), contraseña `password123`.

**Frontend** (captura + errores):

```bash
node "${CLAUDE_SKILL_DIR}/scripts/screenshot.cjs" <pagina.html> [--login usuario|empresa] [--out ruta.png] [--mobile]
```

- `--login` inicia sesión por `login.html` como `usuario1` o `empresa1` antes de navegar (las páginas privadas lo necesitan).
- `--mobile` usa un viewport de 390×844 para revisar el responsive.
- La salida lista los problemas **de la app**: errores de consola, excepciones JS, respuestas HTTP ≥ 400, peticiones a
  localhost fallidas y **desbordamiento horizontal** (contenido más ancho que el viewport, típico en `--mobile`).
  Trata cualquiera de ellos como un fallo que hay que explicar o corregir, no como ruido.
- Los recursos externos que no cargan (avatares, imágenes de CDN) se listan aparte: en la nube la política de red puede
  bloquearlos, así que no son por sí solos un bug de SkillMatch.
- Guarda las capturas en el scratchpad y muéstralas al usuario con `SendUserFile`. Mira la imagen (Read) antes de afirmar
  que algo se ve bien.

Para cambios visuales, combina esto con la skill `ui-ux-pro-max`: captura antes y después del cambio, en escritorio y en `--mobile`.

## 3. Detener

```bash
bash "${CLAUDE_SKILL_DIR}/scripts/stop.sh"
```

Detiene la API y el frontend. MongoDB se deja corriendo porque lo usa `AuthIntegrationTest`.
Tras cambiar código Java, reinicia (stop + start) para que la API cargue los cambios.

## Problemas comunes

| Síntoma | Causa / solución |
|---|---|
| Login devuelve 401 justo después de arrancar | El seed no había terminado. `start.sh` ya lo espera; si arrancaste la API a mano, revisa el log. |
| Error de CORS o "Error de conexión" en el frontend | El frontend debe abrirse desde `localhost`/`127.0.0.1`; `api-config.js` solo usa `localhost:8080` en ese caso. |
| `Port 8080 already in use` | Otra instancia sigue viva: `stop.sh`, o localizarla con `lsof -i :8080`. |
| MongoDB no arranca | `docker ps -a`, `docker logs skillmatch-mongo`, y `${TMPDIR:-/tmp}/skillmatch-session/*.log`. |
