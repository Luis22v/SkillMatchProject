package com.skillmatch.backend.service;

import com.skillmatch.backend.dto.SkillRequest;
import com.skillmatch.backend.model.Skill;
import com.skillmatch.backend.model.User;
import com.skillmatch.backend.repository.SkillRepository;
import com.skillmatch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserSkills(Long userId) {
        List<Skill> skills = skillRepository.findByUserId(userId);
        return skills.stream()
                .map(this::mapSkillToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> addSkill(Long userId, SkillRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("El ID de usuario no puede ser nulo");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        List<Skill> existingSkills = skillRepository.findByUserId(userId);
        boolean skillExists = existingSkills.stream()
                .anyMatch(s -> s.getName().equalsIgnoreCase(request.getName()));

        if (skillExists) {
            throw new IllegalArgumentException("Esta habilidad ya existe en tu perfil");
        }

        Skill skill = new Skill();
        skill.setUser(user);
        skill.setName(request.getName());
        skill.setLevel(request.getLevel());

        Map<String, Object> result = mapSkillToResponse(skillRepository.save(skill));
        log.info("Habilidad '{}' agregada para usuario {}", request.getName(), userId);
        return result;
    }

    @Transactional
    public Map<String, Object> updateSkill(Long userId, Long skillId, SkillRequest request) {
        if (skillId == null) {
            throw new IllegalArgumentException("El ID de habilidad no puede ser nulo");
        }
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new IllegalArgumentException("Habilidad no encontrada"));

        if (!skill.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Esta habilidad no pertenece a este usuario");
        }

        List<Skill> existingSkills = skillRepository.findByUserId(userId);
        boolean duplicateExists = existingSkills.stream()
                .anyMatch(s -> !s.getId().equals(skillId) &&
                              s.getName().equalsIgnoreCase(request.getName()));

        if (duplicateExists) {
            throw new IllegalArgumentException("Ya existe otra habilidad con este nombre");
        }

        skill.setName(request.getName());
        skill.setLevel(request.getLevel());

        Map<String, Object> result = mapSkillToResponse(skillRepository.save(skill));
        log.info("Habilidad {} actualizada para usuario {}", skillId, userId);
        return result;
    }

    @Transactional
    public void deleteSkill(Long userId, Long skillId) {
        if (skillId == null) {
            throw new IllegalArgumentException("El ID de habilidad no puede ser nulo");
        }
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new IllegalArgumentException("Habilidad no encontrada"));

        if (!skill.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Esta habilidad no pertenece a este usuario");
        }

        skillRepository.delete(skill);
        log.info("Habilidad {} eliminada para usuario {}", skillId, userId);
    }

    private Map<String, Object> mapSkillToResponse(Skill skill) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", skill.getId());
        response.put("name", skill.getName());
        response.put("level", skill.getLevel());
        if (skill.getYearsOfExperience() != null) {
            response.put("yearsOfExperience", skill.getYearsOfExperience());
        }
        if (skill.getDescription() != null) {
            response.put("description", skill.getDescription());
        }
        response.put("createdAt", skill.getCreatedAt());
        return response;
    }
}
