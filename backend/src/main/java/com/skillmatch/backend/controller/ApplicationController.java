package com.skillmatch.backend.controller;

import com.skillmatch.backend.dto.ApplicationRequest;
import com.skillmatch.backend.dto.ApplicationResponse;
import com.skillmatch.backend.dto.MessageResponse;
import com.skillmatch.backend.security.UserDetailsImpl;
import com.skillmatch.backend.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.lang.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@Tag(name = "Postulaciones", description = "Gestión de postulaciones a ofertas laborales")
@RestController
@RequestMapping("/api/applications")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @Operation(summary = "Crear postulación", description = "El candidato se postula a una oferta laboral")
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> createApplication(
        @Valid @RequestBody ApplicationRequest request,
        @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        try {
            Long userId = requireAuthenticatedUser(currentUser);
            ApplicationResponse response = applicationService.createApplication(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse(ex.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse("Error al crear postulación: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("Error inesperado al crear postulación"));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('EMPRESA') or hasRole('ADMIN')")
    public ResponseEntity<?> getApplicationById(@PathVariable @NonNull Long id) {
        try {
            ApplicationResponse response = applicationService.getApplicationById(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new MessageResponse("Postulación no encontrada: " + e.getMessage()));
        }
    }

    @Operation(summary = "Mis postulaciones", description = "Devuelve todas las postulaciones del usuario autenticado")
    @GetMapping("/my-applications")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getMyApplications(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        try {
            Long userId = requireAuthenticatedUser(currentUser);
            List<ApplicationResponse> applications = applicationService.getApplicationsByUser(userId);
            return ResponseEntity.ok(applications);
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse(ex.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse("Error al obtener postulaciones: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("Error inesperado al obtener postulaciones"));
        }
    }

    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasRole('EMPRESA') or hasRole('ADMIN')")
    public ResponseEntity<?> getApplicationsByJob(
            @PathVariable @NonNull Long jobId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        try {
            Long requesterId = requireAuthenticatedUser(currentUser);
            List<ApplicationResponse> applications = applicationService.getApplicationsByJob(jobId, requesterId);
            return ResponseEntity.ok(applications);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse("Error al obtener postulaciones: " + e.getMessage()));
        }
    }

    @Operation(summary = "Postulaciones por empresa", description = "Devuelve las postulaciones recibidas por la empresa (paginado, solo dueño)")
    @GetMapping("/company/{companyId}")
    @PreAuthorize("hasRole('EMPRESA') or hasRole('ADMIN')")
    public ResponseEntity<?> getApplicationsByCompany(
            @PathVariable @NonNull Long companyId,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        try {
            Long requesterId = requireAuthenticatedUser(currentUser);
            Page<ApplicationResponse> applications = applicationService.getApplicationsByCompany(companyId, requesterId, pageable);
            return ResponseEntity.ok(applications);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse("Error al obtener postulaciones: " + e.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('EMPRESA') or hasRole('ADMIN')")
    public ResponseEntity<List<ApplicationResponse>> getApplicationsByStatus(@PathVariable String status) {
        List<ApplicationResponse> applications = applicationService.getApplicationsByStatus(status);
        return ResponseEntity.ok(applications);
    }

    @Operation(summary = "Actualizar estado", description = "La empresa cambia el estado de una postulación (pendiente/revisada/aceptada/rechazada)")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('EMPRESA') or hasRole('ADMIN')")
    public ResponseEntity<?> updateStatus(
        @PathVariable @NonNull Long id,
        @RequestParam String status,
        @RequestParam(required = false) String notes,
        @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        try {
            Long requesterId = requireAuthenticatedUser(currentUser);
            ApplicationResponse response = applicationService.updateStatus(id, status, notes, requesterId);
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse("Error al actualizar estado: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteApplication(
        @PathVariable @NonNull Long id,
        @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        try {
            Long requesterId = requireAuthenticatedUser(currentUser);
            boolean hasManagementPrivileges = hasRole(currentUser, "ADMIN") || hasRole(currentUser, "EMPRESA");
            applicationService.deleteApplication(id, requesterId, hasManagementPrivileges);
            return ResponseEntity.ok(new MessageResponse("Postulación eliminada exitosamente"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new MessageResponse("Error al eliminar postulación: " + e.getMessage()));
        }
    }

    @GetMapping("/job/{jobId}/count")
    @PreAuthorize("hasRole('EMPRESA') or hasRole('ADMIN')")
    public ResponseEntity<Long> countApplicationsByJob(@PathVariable @NonNull Long jobId) {
        Long count = applicationService.countApplicationsByJob(jobId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/check")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Boolean> hasUserApplied(
        @RequestParam @NonNull Long jobId,
        @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        try {
            Long userId = requireAuthenticatedUser(currentUser);
            boolean hasApplied = applicationService.hasUserApplied(userId, jobId);
            return ResponseEntity.ok(hasApplied);
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);
        }
    }

    private @NonNull Long requireAuthenticatedUser(UserDetailsImpl currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Usuario no autenticado");
        }
        return Objects.requireNonNull(currentUser.getId());
    }

    private boolean hasRole(UserDetailsImpl currentUser, String roleName) {
        if (currentUser == null) return false;
        String expectedAuthority = "ROLE_" + roleName.toUpperCase();
        return currentUser.getAuthorities().stream()
            .anyMatch(authority -> expectedAuthority.equals(authority.getAuthority()));
    }
}
