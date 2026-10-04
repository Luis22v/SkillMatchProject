
# SkillMatch — Guía de instalación y ejecución

Plataforma de conexión entre usuarios y empresas, estilo LinkedIn. Backend con Spring Boot + MongoDB, frontend con HTML/CSS/JS vanilla.

---

## Requisitos previos

Antes de correr el proyecto necesitas tener instalado:

| Herramienta | Versión mínima | Descarga |
|---|---|---|
| Java JDK | 17 o superior | https://adoptium.net |
| MongoDB Community Server | 7.x o 8.x | https://www.mongodb.com/try/download/community |

> El proyecto fue probado con Java 17 y MongoDB 8.3.2 en Windows 11.

Maven **no necesitas instalarlo** — el proyecto incluye `mvnw.cmd` que lo descarga automáticamente.

---

## Paso 1 — Instalar MongoDB

1. Descarga el instalador `.msi` desde https://www.mongodb.com/try/download/community
2. Ejecuta el instalador y selecciona **Complete**
3. Deja marcada la opción **"Install MongoDB as a Service"** — esto hace que MongoDB arranque automáticamente con Windows

Para verificar que está corriendo, abre CMD y escribe:

```
mongosh
```

Si aparece el prompt `test>` todo está bien. Escribe `exit` para salir.

---

## Paso 2 — Correr el backend

Abre una terminal (CMD o PowerShell) y navega a la carpeta `backend`:

```
cd backend
```

Luego ejecuta el script de arranque, que define un `JWT_SECRET` local y levanta la API:

```
.\start.ps1
```

> Si prefieres lanzarlo a mano, define antes el secreto en PowerShell (mínimo 32 caracteres) y ejecuta Maven:
>
> ```
> $env:JWT_SECRET = "cualquier-cadena-local-de-al-menos-32-caracteres"
> .\mvnw.cmd spring-boot:run -DskipTests
> ```
>
> Sin `JWT_SECRET` la API también arranca, pero usa una clave aleatoria temporal: cada reinicio (incluidos los
> automáticos de DevTools al recompilar) cierra todas las sesiones y tendrás que volver a iniciar sesión.

La primera vez Maven descarga las dependencias, puede tardar unos minutos. Cuando veas esta línea, el backend está listo:

```
Started BackendApplication in X seconds
```

El servidor queda corriendo en **http://localhost:8080**

Al iniciar con la base de datos vacía, el sistema crea automáticamente datos de prueba en MongoDB (tarda unos segundos;
espera a ver `Seed completado exitosamente!` en la consola antes de iniciar sesión):
- 3000 usuarios (`usuario0@skillmatch.com` … `usuario2999@skillmatch.com`)
- 1000 empresas (`empresa0@skillmatch.com` … `empresa999@skillmatch.com`)
- 2000 ofertas de trabajo
- 9000 aplicaciones

Todas las cuentas de prueba usan la contraseña `password123`.

---

## Paso 3 — Correr el frontend

1. Abre la carpeta `SkillMatch/src/pages/` en VS Code
2. Click derecho sobre `index.html`
3. Selecciona **"Open with Live Server"**

El frontend se abre en el navegador en `http://localhost:5500`.

> **Importante:** el frontend debe abrirse con Live Server en el puerto 5500 o 5501. Otros puertos o métodos de apertura causarán errores de CORS con el backend.

---

## Estructura del proyecto

```
SkillMatchProject/
├── backend/                  # API REST — Spring Boot + MongoDB
│   ├── src/main/java/        # Código fuente Java
│   ├── src/main/resources/   # application.properties y migraciones
│   └── mvnw.cmd              # Maven wrapper para Windows
└── SkillMatch/
    └── src/
        ├── pages/            # HTML de cada pantalla
        └── assets/
            ├── js/           # Lógica del frontend
            └── css/          # Estilos
```

---

## Despliegue en producción (Railway)

Variables de entorno necesarias:

| Variable | Obligatoria | Descripción |
|---|---|---|
| `SPRING_DATA_MONGODB_URI` | Sí | Cadena de conexión de MongoDB |
| `JWT_SECRET` | Sí | Secreto para firmar los tokens; aleatorio y de al menos 32 caracteres. Si falta, la API arranca con una clave temporal y cada redespliegue cierra todas las sesiones |
| `SKILLMATCH_SEED_ENABLED` | Recomendada: `false` | Si no se define y la base de datos está vacía, se crean las cuentas de prueba con la contraseña pública `password123` |

---

## Documentación adicional

Dentro de `SkillMatch/docs/` encontrarás:

- `ENDPOINTS-API.md` — lista completa de endpoints REST disponibles
- `GUIA-PRUEBAS.md` — cómo probar la API con Postman o Thunder Client

---

## Tecnologías usadas

**Backend**
- Java 17 
- Spring Boot 3.5.14
- Spring Security + JWT
- MongoDB
- Maven

**Frontend**
- HTML5 / CSS3
- JavaScript
- Live Server
