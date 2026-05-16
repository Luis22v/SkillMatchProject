package com.skillmatch.backend.controller;

import com.skillmatch.backend.dto.ExperienceRequest;
import com.skillmatch.backend.dto.ExperienceResponse;
import com.skillmatch.backend.dto.MessageResponse;
import com.skillmatch.backend.security.UserDetailsImpl;
import com.skillmatch.backend.service.ExperienceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/experiences")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class ExperienceController {

    private final ExperienceService experienceService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ExperienceResponse>> getUserExperiences(
            @PathVariable(required = false) String userId) {
        Long targetUserId = resolveTargetUserId(userId, extractCurrentUserId());
        return ResponseEntity.ok(experienceService.getUserExperiences(targetUserId));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addExperience(
            @PathVariable(required = false) String userId,
            @Valid @RequestBody ExperienceRequest request) {

        Long currentUserId = extractCurrentUserId();
        Long targetUserId = resolveTargetUserId(userId, currentUserId);

        if (!currentUserId.equals(targetUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("No tienes permiso para agregar experiencia a este perfil"));
        }

        try {
            ExperienceResponse experience = experienceService.addExperience(targetUserId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(experience);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PutMapping("/{experienceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateExperience(
            @PathVariable(required = false) String userId,
            @PathVariable Long experienceId,
            @Valid @RequestBody ExperienceRequest request) {

        Long currentUserId = extractCurrentUserId();
        Long targetUserId = resolveTargetUserId(userId, currentUserId);

        if (!currentUserId.equals(targetUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("No tienes permiso para actualizar esta experiencia"));
        }

        try {
            ExperienceResponse updated = experienceService.updateExperience(targetUserId, experienceId, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{experienceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteExperience(
            @PathVariable(required = false) String userId,
            @PathVariable Long experienceId) {

        Long currentUserId = extractCurrentUserId();
        Long targetUserId = resolveTargetUserId(userId, currentUserId);

        if (!currentUserId.equals(targetUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("No tienes permiso para eliminar esta experiencia"));
        }

        try {
            experienceService.deleteExperience(targetUserId, experienceId);
            return ResponseEntity.ok(new MessageResponse("Experiencia eliminada exitosamente"));
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
