package com.skillmatch.backend.controller;

import com.skillmatch.backend.dto.JobRequest;
import com.skillmatch.backend.dto.JobResponse;
import com.skillmatch.backend.dto.MessageResponse;
import com.skillmatch.backend.security.UserDetailsImpl;
import com.skillmatch.backend.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;

@Tag(name = "Ofertas de trabajo", description = "Creación, consulta y gestión de ofertas laborales")
@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    // ─── Escritura (empresa/admin) ────────────────────────────────────────────

    @Operation(summary = "Crear oferta", description = "Crea una nueva oferta laboral (solo empresas)")
    @PostMapping
    @PreAuthorize("hasRole('EMPRESA') or hasRole('ADMIN')")
    public ResponseEntity<?> createJob(@Valid @RequestBody JobRequest request) {
        try {
            JobResponse response = jobService.createJob(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Error al crear oferta: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EMPRESA') or hasRole('ADMIN')")
    public ResponseEntity<?> updateJob(@PathVariable @NonNull Long id,
                                       @Valid @RequestBody JobRequest request,
                                       @AuthenticationPrincipal UserDetailsImpl currentUser) {
        try {
            JobResponse response = jobService.updateJob(id, request, Objects.requireNonNull(currentUser.getId()));
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Error al actualizar oferta: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('EMPRESA') or hasRole('ADMIN')")
    public ResponseEntity<?> changeStatus(@PathVariable @NonNull Long id,
                                          @RequestParam String status,
                                          @AuthenticationPrincipal UserDetailsImpl currentUser) {
        try {
            JobResponse response = jobService.changeStatus(id, status, Objects.requireNonNull(currentUser.getId()));
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Error al cambiar estado: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EMPRESA') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteJob(@PathVariable @NonNull Long id,
                                       @AuthenticationPrincipal UserDetailsImpl currentUser) {
        try {
            jobService.deleteJob(id, Objects.requireNonNull(currentUser.getId()));
            return ResponseEntity.ok(new MessageResponse("Oferta eliminada exitosamente"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Error al eliminar oferta: " + e.getMessage()));
        }
    }

    // ─── Lectura pública con paginación ──────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<?> getJobById(@PathVariable @NonNull Long id) {
        try {
            return ResponseEntity.ok(jobService.getJobById(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Oferta no encontrada: " + e.getMessage()));
        }
    }

    /**
     * ✅ FIX #4: Lista paginada de ofertas activas.
     *
     * Query params:
     *   page     (default 0)          — número de página, base 0
     *   size     (default 20, max 50) — resultados por página
     *   sortBy   (default postedDate) — campo de ordenamiento
     *   direction(default desc)       — asc | desc
     *
     * Respuesta: Page<JobResponse> con metadata (totalElements, totalPages, etc.)
     */
    @Operation(summary = "Listar ofertas activas", description = "Devuelve página de ofertas activas, ordenadas por fecha (público)")
    @GetMapping
    public ResponseEntity<Page<JobResponse>> getAllActiveJobs(
            @RequestParam(defaultValue = "0")          int page,
            @RequestParam(defaultValue = "20")         int size,
            @RequestParam(defaultValue = "postedDate") String sortBy,
            @RequestParam(defaultValue = "desc")       String direction) {

        size = Math.min(size, 50); // límite máximo
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        return ResponseEntity.ok(jobService.getAllActiveJobs(pageable));
    }

    /**
     * Últimas 10 ofertas para landing/home.
     * Sin paginación — siempre retorna los 10 más recientes.
     */
    @Operation(summary = "Ofertas recientes", description = "Devuelve las 10 ofertas más recientes para la landing page")
    @GetMapping("/recent")
    public ResponseEntity<List<JobResponse>> getRecentJobs() {
        return ResponseEntity.ok(jobService.getRecentJobs());
    }

    /**
     * Búsqueda por palabra clave con paginación.
     */
    @Operation(summary = "Buscar ofertas", description = "Búsqueda por palabra clave en título, descripción y empresa")
    @GetMapping("/search")
    public ResponseEntity<Page<JobResponse>> searchJobs(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0")          int page,
            @RequestParam(defaultValue = "20")         int size,
            @RequestParam(defaultValue = "postedDate") String sortBy,
            @RequestParam(defaultValue = "desc")       String direction) {

        size = Math.min(size, 50);
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        return ResponseEntity.ok(jobService.searchJobs(keyword, pageable));
    }

    /**
     * Filtros avanzados con paginación.
     */
    @GetMapping("/filter")
    public ResponseEntity<Page<JobResponse>> filterJobs(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String modality,
            @RequestParam(required = false) String experienceLevel,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Double minSalary,
            @RequestParam(required = false) Double maxSalary,
            @RequestParam(defaultValue = "0")          int page,
            @RequestParam(defaultValue = "20")         int size,
            @RequestParam(defaultValue = "postedDate") String sortBy,
            @RequestParam(defaultValue = "desc")       String direction) {

        size = Math.min(size, 50);
        Pageable pageable = buildPageable(page, size, sortBy, direction);
        return ResponseEntity.ok(
                jobService.getJobsByFilters(
                        type, modality, experienceLevel, location,
                        minSalary, maxSalary, pageable));
    }

    /**
     * Ofertas de una empresa (para perfil público y dashboard interno).
     * Sin paginar — el dashboard de empresa muestra todas sus ofertas.
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<JobResponse>> getJobsByCompany(
            @PathVariable @NonNull Long companyId) {
        return ResponseEntity.ok(jobService.getJobsByCompany(companyId));
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private Pageable buildPageable(int page, int size, String sortBy, String direction) {
        Sort sort = "asc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(page, size, sort);
    }
}