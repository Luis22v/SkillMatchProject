package com.skillmatch.backend.controller;

import com.skillmatch.backend.dto.EducationRequest;
import com.skillmatch.backend.dto.EducationResponse;
import com.skillmatch.backend.dto.MessageResponse;
import com.skillmatch.backend.security.UserDetailsImpl;
import com.skillmatch.backend.service.EducationService;
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
@RequestMapping("/api/users/{userId}/educations")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class EducationController {

    private final EducationService educationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EducationResponse>> getUserEducations(
            @PathVariable(required = false) String userId) {
        Long targetUserId = resolveTargetUserId(userId, extractCurrentUserId());
        return ResponseEntity.ok(educationService.getUserEducations(targetUserId));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addEducation(
            @PathVariable(required = false) String userId,
            @Valid @RequestBody EducationRequest request) {

        Long currentUserId = extractCurrentUserId();
        Long targetUserId = resolveTargetUserId(userId, currentUserId);

        if (!currentUserId.equals(targetUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("No tienes permiso para agregar educación a este perfil"));
        }

        try {
            EducationResponse education = educationService.addEducation(Objects.requireNonNull(targetUserId), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(education);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PutMapping("/{educationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateEducation(
            @PathVariable(required = false) String userId,
            @PathVariable @NonNull Long educationId,
            @Valid @RequestBody EducationRequest request) {

        Long currentUserId = extractCurrentUserId();
        Long targetUserId = resolveTargetUserId(userId, currentUserId);

        if (!currentUserId.equals(targetUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("No tienes permiso para actualizar esta educación"));
        }

        try {
            EducationResponse updated = educationService.updateEducation(targetUserId, educationId, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{educationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteEducation(
            @PathVariable(required = false) String userId,
            @PathVariable @NonNull Long educationId) {

        Long currentUserId = extractCurrentUserId();
        Long targetUserId = resolveTargetUserId(userId, currentUserId);

        if (!currentUserId.equals(targetUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("No tienes permiso para eliminar esta educación"));
        }

        try {
            educationService.deleteEducation(targetUserId, educationId);
            return ResponseEntity.ok(new MessageResponse("Educación eliminada exitosamente"));
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
