package com.skillmatch.backend.controller;

import com.skillmatch.backend.dto.CertificationRequest;
import com.skillmatch.backend.dto.CertificationResponse;
import com.skillmatch.backend.dto.MessageResponse;
import com.skillmatch.backend.security.UserDetailsImpl;
import com.skillmatch.backend.service.CertificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/certifications")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class CertificationController {

    private final CertificationService certificationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CertificationResponse>> getUserCertifications(
            @PathVariable(required = false) String userId) {
        Long targetUserId = resolveTargetUserId(userId, extractCurrentUserId());
        return ResponseEntity.ok(certificationService.getUserCertifications(targetUserId));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addCertification(
            @PathVariable(required = false) String userId,
            @Valid @RequestBody CertificationRequest request) {

        Long currentUserId = extractCurrentUserId();
        Long targetUserId = resolveTargetUserId(userId, currentUserId);

        if (!currentUserId.equals(targetUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("No tienes permiso para agregar certificación a este perfil"));
        }

        try {
            CertificationResponse certification = certificationService.addCertification(Objects.requireNonNull(targetUserId), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(certification);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PutMapping("/{certificationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateCertification(
            @PathVariable(required = false) String userId,
            @PathVariable @NonNull Long certificationId,
            @Valid @RequestBody CertificationRequest request) {

        Long currentUserId = extractCurrentUserId();
        Long targetUserId = resolveTargetUserId(userId, currentUserId);

        if (!currentUserId.equals(targetUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("No tienes permiso para actualizar esta certificación"));
        }

        try {
            CertificationResponse updated = certificationService.updateCertification(targetUserId, certificationId, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{certificationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteCertification(
            @PathVariable(required = false) String userId,
            @PathVariable @NonNull Long certificationId) {

        Long currentUserId = extractCurrentUserId();
        Long targetUserId = resolveTargetUserId(userId, currentUserId);

        if (!currentUserId.equals(targetUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("No tienes permiso para eliminar esta certificación"));
        }

        try {
            certificationService.deleteCertification(targetUserId, certificationId);
            return ResponseEntity.ok(new MessageResponse("Certificación eliminada exitosamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    private Long extractCurrentUserId() {
        return ((UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
    }

    private Long resolveTargetUserId(String pathUserId, Long fallbackId) {
        return (pathUserId != null && !pathUserId.equals("undefined"))
                ? Long.parseLong(pathUserId)
                : fallbackId;
    }
}
