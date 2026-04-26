# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SkillMatch is a professional networking platform connecting job seekers with local companies in Cartagena, Colombia. It is a full-stack app with a Spring Boot REST API backend and a vanilla JS multi-page frontend served via Live Server.

---

## Commands

### Backend

```powershell
# Start backend (sets required env vars, then runs Maven wrapper)
cd backend
.\start.ps1

# Or manually with env vars
$env:JWT_SECRET = "skillmatch-dev-secret-key-for-local-development-only-do-not-use-in-production-2024"
$env:DB_PASSWORD = ""
.\mvnw.cmd spring-boot:run

# Build only (no run)
.\mvnw.cmd clean package -DskipTests

# Run tests
.\mvnw.cmd test

# Run a single test class
.\mvnw.cmd test -Dtest=BackendApplicationTests
```

**Prerequisites:** MySQL running on port 3306 (XAMPP recommended). The schema `skillmatch` is auto-created on first run. `JWT_SECRET` env var is required at startup — the app will fail without it.

### Frontend

Open any `.html` file in `SkillMatch/src/pages/` using **VS Code Live Server** (default port 5501). CORS is configured on the backend only for `localhost:5501` and `127.0.0.1:5501`.

---

## Architecture

### Backend — `backend/src/main/java/com/skillmatch/backend/`

Standard Spring Boot layered architecture:

| Package | Role |
|---|---|
| `controller/` | 13 REST controllers — thin, delegate to service layer |
| `service/` | 14 service classes — all business logic lives here |
| `repository/` | 13 Spring Data JPA repositories — custom JPQL where needed |
| `model/` | 13 JPA entities — `User` implements `UserDetails` |
| `dto/` | 28 request/response objects — validated with Jakarta Bean Validation |
| `config/` | Spring beans: `SecurityConfig`, `JwtProperties`, seeder |
| `security/` | `JwtTokenProvider`, `JwtAuthenticationFilter`, entry point |
| `exception/` | Custom exceptions (`ResourceNotFoundException`, `DuplicateResourceException`) |

**Auth flow:** Login → `AuthService` → `JwtTokenProvider.generateToken()` (embeds `userId` claim) → client stores JWT in `localStorage` → all subsequent requests go through `JwtAuthenticationFilter` → `@AuthenticationPrincipal User` is available in every controller method.

**Public routes:** `GET /api/jobs/**`, `GET /api/companies/**`, `/api/auth/**`, `/api/public/**`. Everything else requires a valid JWT.

**Pagination:** `GET /api/jobs` returns `Page<JobResponse>` (Spring Data Page with `content`, `totalElements`, etc.). Other list endpoints return plain `List<>`.

### Frontend — `SkillMatch/src/assets/js/`

Vanilla JS, no framework. Each HTML page loads its own JS file. **`api-config.js` must be the first script on every page** — it defines `API_BASE_URL`, `fetchWithAuth()`, `logout()`, `saveToken()`, `saveUserData()`, and `isAuthenticated()`.

```
api-config.js   ← always load first; defines shared globals
notifications.js ← load second on authenticated pages
auth.js         ← login pages only
profile.js      ← perfil-usuario.html
perfil-empresa.js ← perfil-empresa.html
oportunidades.js ← oportunidades.html
conexiones.js   ← conexiones.html
mensajes.js     ← mensajes.html
search-handler.js ← index.html
```

**Authentication on the frontend:** JWT stored in `localStorage` under key `token`. `userData` (id, email, role, companyId, etc.) stored under key `userData`. `isAuthenticated()` decodes the JWT payload and checks `exp` against `Date.now()`. `fetchWithAuth()` automatically attaches `Authorization: Bearer <token>` and calls `logout()` on 401/403.

**User ID extraction pattern** (stale localStorage guard):
```js
const parsed = JSON.parse(localStorage.getItem('userData'));
let id = parsed?.id || parsed?.userId;
if (!id) {
    const payload = JSON.parse(atob(localStorage.getItem('token').split('.')[1]));
    id = payload.userId;
}
```

---

## Key Conventions

### Backend
- All controllers use `@CrossOrigin(origins = "*")` and delegate immediately to the service layer — no logic in controllers.
- Services are annotated `@Transactional` (read-only where applicable). Logging uses `@Slf4j` from Lombok.
- DTOs use Lombok `@Data` + Jakarta validation annotations (`@NotBlank`, `@Size`, `@Email`).
- `ddl-auto=validate` — schema changes must be applied to MySQL manually; Hibernate will refuse to start if the schema doesn't match entities.
- `User` entity implements Spring Security's `UserDetails`; `getUsername()` returns the email address.
- The JWT token embeds a `userId` claim (Long) alongside the standard `sub` (email) claim.

### Frontend
- All authenticated API calls go through `fetchWithAuth()` — never use raw `fetch()` for authenticated routes.
- `API_BASE_URL` is defined once in `api-config.js`; do not redeclare it in individual JS files.
- `notifications.js` injects the bell icon into `nav > ul.nav-links` (the right-side nav list, direct child of `nav`). All pages must use `<nav class="container">` with `<ul class="nav-links">` for the nav structure.
- Paginated backend responses (`Page<T>`) must be unwrapped: `Array.isArray(data) ? data : (data.content || [])`.

---

## Domain Model Summary

13 entities: `User`, `Company`, `Job`, `Application`, `Skill`, `Experience`, `Education`, `Certification`, `Message`, `Notification`, `Connection`, `SavedJob`, `Role`.

- A `User` may have one associated `Company` (role `EMPRESA`) or be a job seeker (role `USER`).
- `Connection` is bidirectional: `userId` (requester) ↔ `connectedUserId` (receiver), status: `PENDING` / `ACCEPTED` / `REJECTED`.
- `Job` belongs to `Company`; `Application` links `User` → `Job`.
