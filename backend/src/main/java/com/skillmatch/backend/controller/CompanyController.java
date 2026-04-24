package com.skillmatch.backend.controller;

import com.skillmatch.backend.dto.CompanyRequest;
import com.skillmatch.backend.dto.CompanyResponse;
import com.skillmatch.backend.dto.MessageResponse;
import com.skillmatch.backend.model.User;
import com.skillmatch.backend.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/companies")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    // ─── Escritura (solo ADMIN) ──────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCompany(@Valid @RequestBody CompanyRequest request) {
        try {
            CompanyResponse response = companyService.createCompany(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateCompany(@PathVariable Long id,
                                           @Valid @RequestBody CompanyRequest request) {
        try {
            CompanyResponse response = companyService.updateCompany(id, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCompany(@PathVariable Long id) {
        try {
            companyService.deleteCompany(id);
            return ResponseEntity.ok(new MessageResponse("Empresa eliminada exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> verifyCompany(@PathVariable Long id) {
        try {
            companyService.verifyCompany(id);
            return ResponseEntity.ok(new MessageResponse("Empresa verificada exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    // ─── Descripción (dueño de la empresa o ADMIN) ───────────────────────────

    /**
     * ✅ FIX #2: Endpoint antes completamente abierto — cualquier usuario
     * autenticado podía modificar la descripción de cualquier empresa.
     * Ahora se verifica que el usuario sea el dueño o tenga rol ADMIN.
     */
    @PatchMapping("/{id}/description")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateCompanyDescription(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User currentUser) {
        try {
            String description = request.get("description");
            if (description == null || description.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("La descripción no puede estar vacía"));
            }

            boolean isAdmin = hasRole(currentUser, "ADMIN");
            boolean isOwner = companyService.isOwner(id, currentUser.getId());

            if (!isOwner && !isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new MessageResponse(
                                "No tienes permiso para modificar esta empresa"));
            }

            CompanyResponse response = companyService.updateCompanyDescription(id, description);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    // ─── Lectura (públicos o autenticados) ───────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<?> getCompany(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(companyService.getCompanyById(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<CompanyResponse>> getAllCompanies(
            @RequestParam(required = false) String status) {
        if ("active".equals(status)) {
            return ResponseEntity.ok(companyService.getActiveCompanies());
        }
        return ResponseEntity.ok(companyService.getAllCompanies());
    }

    @GetMapping("/search")
    public ResponseEntity<List<CompanyResponse>> searchCompanies(@RequestParam String keyword) {
        return ResponseEntity.ok(companyService.searchCompanies(keyword));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<CompanyResponse>> filterCompanies(
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String size) {
        return ResponseEntity.ok(companyService.getCompaniesByFilters(industry, location, size));
    }

    @GetMapping("/{id}/statistics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCompanyStatistics(@PathVariable Long id) {
        try {
            Map<String, Object> statistics = companyService.getCompanyStatistics(id);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error al obtener estadísticas: " + e.getMessage()));
        }
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private boolean hasRole(User user, String roleName) {
        if (user == null) return false;
        String expected = "ROLE_" + roleName.toUpperCase();
        return user.getAuthorities().stream()
                .anyMatch(a -> expected.equals(a.getAuthority()));
    }
}