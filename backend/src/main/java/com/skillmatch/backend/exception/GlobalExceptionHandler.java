package com.skillmatch.backend.exception;

import com.skillmatch.backend.dto.MessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones.
 *
 * Spring elige el handler cuyo tipo declarado está más cerca de la excepción lanzada, sin importar el orden
 * de los métodos en este archivo (ver GlobalExceptionHandlerTest#exceptionResolution_mostSpecificHandlerWins).
 * Se mantienen ordenados de más específico a más general solo por legibilidad.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String INTERNAL_ERROR_MESSAGE = "Error interno del servidor. Inténtalo de nuevo más tarde.";
    static final String DUPLICATE_KEY_MESSAGE = "El recurso ya existe.";

    // 1. Autenticación — usuario no encontrado
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<?> handleUsernameNotFound(UsernameNotFoundException ex,
                                                     WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new MessageResponse(ex.getMessage()));
    }

    // 2. Autenticación — credenciales incorrectas
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentials(BadCredentialsException ex,
                                                   WebRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("Email o contraseña incorrectos"));
    }

    // 3. ✅ FIX: Autorización — acceso denegado (faltaba completamente)
    // Sin este handler, Spring Security retorna 403 sin cuerpo JSON,
    // lo que rompe el parsing en el frontend.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex,
                                                 WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse("Acceso denegado: " + ex.getMessage()));
    }

    // 4. Validación de campos (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field   = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });
        return ResponseEntity.badRequest().body(errors);
    }

    // 5. Tipo incorrecto en parámetros de URL (ej: /api/users/undefined)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                 WebRequest request) {
        Object rejected = ex.getValue();
        if ("undefined".equals(String.valueOf(rejected))) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(
                            "Parámetro '" + ex.getName() + "' inválido. "
                            + "Verifica que el ID sea válido antes de llamar este endpoint."));
        }
        Class<?> requiredType = ex.getRequiredType();
        String typeName = requiredType != null ? requiredType.getSimpleName() : "tipo desconocido";
        return ResponseEntity.badRequest()
                .body(new MessageResponse(
                        "El parámetro '" + ex.getName() + "' debe ser " + typeName));
    }

    // 6. Recurso no encontrado (nuestro tipo custom)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleResourceNotFound(ResourceNotFoundException ex,
                                                      WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new MessageResponse(ex.getMessage()));
    }

    // 7. Recurso duplicado (409)
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<?> handleDuplicateResource(DuplicateResourceException ex,
                                                      WebRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new MessageResponse(ex.getMessage()));
    }

    // 8. Acceso no autorizado — dominio (403)
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<?> handleUnauthorized(UnauthorizedException ex,
                                                 WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse(ex.getMessage()));
    }

    // 9. Violación de un índice único (p. ej. doble envío simultáneo de una postulación). Es una DataAccessException,
    // pero semánticamente un conflicto; el mensaje de MongoDB (colección, índice, valores) no se expone.
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<?> handleDuplicateKey(DuplicateKeyException ex, WebRequest request) {
        log.warn("Clave duplicada en {}: {}", request.getDescription(false), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new MessageResponse(DUPLICATE_KEY_MESSAGE));
    }

    // 10. Fallos de base de datos: son RuntimeException, pero no errores de negocio. Sin este handler caerían en
    // handleRuntime y devolverían 400 con detalles de infraestructura (hosts, timeouts) en el mensaje.
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<?> handleDataAccess(DataAccessException ex, WebRequest request) {
        log.error("Error de acceso a datos en {}", request.getDescription(false), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse(INTERNAL_ERROR_MESSAGE));
    }

    // 11. Errores de negocio genéricos (patrón heredado: los services lanzan RuntimeException con mensajes
    // pensados para el usuario). En código nuevo, usar excepciones de dominio.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex, WebRequest request) {
        return ResponseEntity.badRequest()
                .body(new MessageResponse(ex.getMessage()));
    }

    // 12. Catch-all — último recurso. El detalle va al log, nunca al cliente (OWASP A05).
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobal(Exception ex, WebRequest request) {
        log.error("Error no controlado en {}", request.getDescription(false), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse(INTERNAL_ERROR_MESSAGE));
    }
}