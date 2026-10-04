# CLAUDE.md — SkillMatch

Plataforma de intermediación laboral entre estudiantes/usuarios y empresas (estilo LinkedIn).
Backend REST en Spring Boot + MongoDB; frontend en HTML/CSS/JS vanilla. Despliegue en Railway.

## Estructura

```
backend/                       API REST (Spring Boot 3.5, Java 17, Maven)
  src/main/java/com/skillmatch/backend/
    controller/   REST controllers (/api/**), validan con @Valid y autorizan con @PreAuthorize
    service/      Lógica de negocio; único lugar con reglas de dominio
    repository/   Interfaces MongoRepository (queries derivadas)
    model/        Documentos MongoDB (@Document) + enums de estado
    dto/          *Request (entrada validada) y *Response (salida); nunca exponer model/
    exception/    Excepciones de dominio + GlobalExceptionHandler
    security/     JWT (filtro, provider, blacklist), rate limiting (Bucket4j)
    config/       SecurityConfig (CORS, CSP, rutas públicas), DataSeeder, índices Mongo
  src/main/resources/messages.properties   Mensajes de validación (claves {validation.*})
  src/test/java/...                        Tests JUnit 5 + Mockito
SkillMatch/                    Frontend estático
  src/pages/*.html             Una página por pantalla
  src/assets/js/*.js           Un script por página + compartidos (api-config.js, utils.js, notifications.js, auth.js)
  src/assets/css/app.css       Hoja de estilos única
  docs/ENDPOINTS-API.md        Referencia de endpoints; actualizarla al cambiar la API
```

## Comandos

Ejecutar desde `backend/`. En la nube usar `mvn`; en Windows, `.\mvnw.cmd`.

| Tarea | Comando |
|---|---|
| Compilar | `mvn -B -ntp compile` |
| Todos los tests | `mvn -B -ntp test` |
| Un test / un método | `mvn -B -ntp test -Dtest=JobServiceTest` · `-Dtest=JobControllerTest#updateJob_wrongOwner_returnsForbidden` |
| Tests + jar + cobertura | `mvn -B -ntp verify` → `target/site/jacoco/index.html` (y `jacoco.csv` para leerlo por script) |
| Levantar la API | `mvn -B -ntp spring-boot:run` → http://localhost:8080 (Swagger: `/swagger-ui.html`) |
| Escaneo CVE (OWASP) | `mvn dependency-check:check` (falla con CVSS ≥ 7; lento la primera vez) |

- `AuthIntegrationTest` (`@SpringBootTest`) necesita MongoDB en `localhost:27017`. El resto son unitarios y no lo necesitan.
- En sesiones de Claude Code en la nube, `.claude/hooks/session-start.sh` arranca MongoDB 8.0 (contenedor Docker
  `skillmatch-mongo`) y precarga Maven. Si Mongo no responde: `docker ps`, y relanzar el hook.
- Frontend: servir `SkillMatch/src/pages/` en el puerto **5500 o 5501** (Live Server). Cualquier otro origen falla por CORS
  o porque `api-config.js` solo apunta a `localhost:8080` cuando el host es `localhost`/`127.0.0.1`.
- Si la BD está vacía, `DataSeeder` siembra 3000 usuarios, 1000 empresas, 2000 ofertas y 9000 postulaciones (~2 s).
  Corre **después** de que Tomcat ya responde: hasta que el log muestra `Seed completado` no hay usuarios para hacer login.
  El hash BCrypt de la contraseña de prueba se calcula una sola vez a propósito (ver `DataSeederTest`); no moverlo al bucle.
  Login de prueba: `usuario1@skillmatch.com` / `empresa1@skillmatch.com`, contraseña `password123`.
  Los tests desactivan el seed (`skillmatch.seed.enabled=false`) y usan la BD `skillmatch-test`.

## Arquitectura y convenciones del backend

- **Capas estrictas**: Controller → Service → Repository. El controller no toca repositorios ni contiene reglas de negocio.
  Entrada siempre por `*Request` con Bean Validation; salida siempre por `*Response` (mapeo en el service).
- **Inyección por constructor** con `@RequiredArgsConstructor` + campos `private final`. Lombok `@Data` en DTOs y modelos.
- **IDs**: `String` (ObjectId de MongoDB). Consultas dinámicas/filtros con `MongoTemplate` + `Criteria`; las simples, como
  métodos derivados en el repositorio.
- **Errores**: en código nuevo, lanzar excepciones de dominio (`ResourceNotFoundException` → 404,
  `DuplicateResourceException` → 409, `UnauthorizedException` / `AccessDeniedException` → 403) y dejar que
  `GlobalExceptionHandler` construya la respuesta. No añadir más `try/catch (Exception)` en controllers ni lanzar
  `RuntimeException` genérica: varios controllers antiguos lo hacen (p. ej. `JobController`), y es deuda técnica, no el patrón a seguir.
  Spring elige el handler cuyo tipo de excepción está más cerca de la lanzada (no el orden en el archivo; ver
  `GlobalExceptionHandlerTest#exceptionResolution_mostSpecificHandlerWins`). Los 500 (incluidos los fallos de BD) y las
  violaciones de índice único (409) responden con mensajes genéricos y dejan el detalle en el log: nunca devolver `ex.getMessage()` de
  errores no controlados ni de infraestructura.
- **Validación**: mensajes en `messages.properties` y referenciados como `message = "{validation.<entidad>.<campo>.<regla>}"`.
  Textos visibles al usuario en español.
- **Seguridad**:
  - JWT stateless. Roles: `USER`, `EMPRESA`, `ADMIN`; se autorizan con `@PreAuthorize("hasRole('...')")`.
  - Comprobar la **propiedad del recurso** en el service (ver `verifyOwnership` en `JobService`), no solo el rol.
  - Las rutas públicas, CORS y CSP se declaran únicamente en `SecurityConfig`. Ningún controller usa `@CrossOrigin`
    (el filtro global decide; `CorsIntegrationTest` lo fija): no añadirlos.
  - Nunca registrar ni devolver contraseñas o tokens. Los secretos van por variables de entorno (`JWT_SECRET`,
    `SPRING_DATA_MONGODB_URI`) y nunca se commitean ni llevan valor por defecto en `application.properties`.
    Sin `JWT_SECRET`, `JwtTokenProvider` usa una clave aleatoria efímera (avisa en el log); nunca reintroducir un
    secreto por defecto, porque permitiría forjar tokens.
- **Java**: el target es 17 (`<java.version>`). La nube tiene JDK 21, así que no usar APIs posteriores a Java 17.
- Tareas programadas con `@Scheduled` (habilitado en `BackendApplication`); p. ej. el cierre diario de ofertas vencidas.

## Tests

- Todo cambio en un service o controller debe venir con su test. Seguir el estilo existente:
  - **Service** → `@ExtendWith(MockitoExtension.class)`, `@Mock` de repositorios/`MongoTemplate`, `@InjectMocks`, AssertJ.
  - **Controller** → `@WebMvcTest(controllers = X.class)` + `@AutoConfigureMockMvc(addFilters = false)`, `@MockitoBean`
    para el service, `UserDetailsService` y `JwtTokenProvider`; el principal se fija en el `SecurityContext`.
  - **Integración** → `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate` (requiere MongoDB).
- Nombres: `metodo_condicion_resultadoEsperado` (p. ej. `login_invalidCredentials_returnsUnauthorized`).
- Antes de dar algo por terminado: `mvn -B -ntp test` en verde.
- Cobertura de líneas actual (JaCoCo): ~34 % total, controllers ~7 %. Al tocar una clase sin tests, añadir los suyos;
  la cobertura no debe bajar. El CI (`.github/workflows/backend-ci.yml`) ejecuta `mvn verify` en cada push y PR.

## Frontend

- JS vanilla sin bundler ni framework: cada página carga sus scripts con `<script>`; `api-config.js` siempre primero,
  luego los compartidos que use (`notifications.js`, `utils.js`) y al final el script propio de la página.
  Las funciones compartidas son globales.
- Llamadas a la API siempre con `API_BASE_URL` + `fetchWithAuth()` de `api-config.js` (añade el Bearer token y `credentials`).
  No hardcodear URLs.
- El token y el usuario viven en `localStorage` (`token`, `userData`). Al pintar datos del servidor en el DOM, usar
  `textContent` o escapar (XSS), nunca interpolar HTML sin sanear.
- Para trabajo visual (páginas, componentes, colores, accesibilidad), usar la skill `ui-ux-pro-max` con `--stack html-tailwind`
  solo como referencia de buenas prácticas: el proyecto usa CSS propio en `app.css`, no Tailwind.

## Flujo de trabajo

- Commits en formato Conventional Commits, como en el historial: `feat:`, `fix:`, `refactor:`, `chore:`, `docs:`, `test:`.
- Cambios de API → actualizar `SkillMatch/docs/ENDPOINTS-API.md` y, si aplica, el JS del frontend que la consume.
- Para levantar backend y frontend juntos y verificar un cambio de punta a punta, usar la skill `skillmatch-run`.
