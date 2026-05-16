package com.skillmatch.backend.controller;

import com.skillmatch.backend.dto.MessageResponse;
import com.skillmatch.backend.dto.SkillRequest;
import com.skillmatch.backend.security.UserDetailsImpl;
import com.skillmatch.backend.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/{userId}/skills")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class SkillController {
    
    private final SkillService skillService;
    
    // Obtener todas las skills de un usuario
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getUserSkills(@PathVariable(required = false) String userId) {
        Long targetUserId = resolveTargetUserId(userId, extractCurrentUserId());

        List<Map<String, Object>> skills = skillService.getUserSkills(targetUserId);
        return ResponseEntity.ok(skills);
    }
    
    // Agregar una skill al usuario
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addSkill(
            @PathVariable(required = false) String userId,
            @Valid @RequestBody SkillRequest request) {
        
        Long currentUserId = extractCurrentUserId();
        Long targetUserId = resolveTargetUserId(userId, currentUserId);

        // Solo el propio usuario puede agregar skills a su perfil
        if (!currentUserId.equals(targetUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("No tienes permiso para agregar skills a este perfil"));
        }
        
        try {
            Map<String, Object> skill = skillService.addSkill(targetUserId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Habilidad agregada exitosamente",
                "skill", skill
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(e.getMessage()));
        }
    }
    
    // Actualizar una skill
    @PutMapping("/{skillId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateSkill(
            @PathVariable(required = false) String userId,
            @PathVariable Long skillId,
            @Valid @RequestBody SkillRequest request) {
        
        Long currentUserId = extractCurrentUserId();
        Long targetUserId = resolveTargetUserId(userId, currentUserId);

        if (!currentUserId.equals(targetUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("No tienes permiso para actualizar esta skill"));
        }
        
        try {
            Map<String, Object> skill = skillService.updateSkill(targetUserId, skillId, request);
            return ResponseEntity.ok(Map.of(
                "message", "Habilidad actualizada exitosamente",
                "skill", skill
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(e.getMessage()));
        }
    }
    
    // Eliminar una skill
    @DeleteMapping("/{skillId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteSkill(
            @PathVariable(required = false) String userId,
            @PathVariable Long skillId) {
        
        Long currentUserId = extractCurrentUserId();
        Long targetUserId = resolveTargetUserId(userId, currentUserId);

        if (!currentUserId.equals(targetUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("No tienes permiso para eliminar esta skill"));
        }
        
        try {
            skillService.deleteSkill(targetUserId, skillId);
            return ResponseEntity.ok(new MessageResponse("Habilidad eliminada exitosamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(e.getMessage()));
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
