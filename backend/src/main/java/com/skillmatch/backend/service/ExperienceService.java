package com.skillmatch.backend.service;

import com.skillmatch.backend.dto.ExperienceRequest;
import com.skillmatch.backend.dto.ExperienceResponse;
import com.skillmatch.backend.model.Experience;
import com.skillmatch.backend.model.User;
import com.skillmatch.backend.repository.ExperienceRepository;
import com.skillmatch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExperienceService {

    private final ExperienceRepository experienceRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ExperienceResponse> getUserExperiences(Long userId) {
        return experienceRepository.findByUserIdOrderByStartDateDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ExperienceResponse addExperience(Long userId, ExperienceRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("El ID de usuario no puede ser nulo");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Experience experience = new Experience();
        experience.setUser(user);
        experience.setCompany(request.getCompany());
        experience.setPosition(request.getPosition());
        experience.setStartDate(request.getStartDate());
        experience.setEndDate(request.getEndDate());
        experience.setIsCurrent(request.getIsCurrent() != null ? request.getIsCurrent() : false);
        experience.setDescription(request.getDescription());
        experience.setLocation(request.getLocation());

        ExperienceResponse result = mapToResponse(experienceRepository.save(experience));
        log.info("Experiencia en '{}' agregada para usuario {}", request.getCompany(), userId);
        return result;
    }

    @Transactional
    public ExperienceResponse updateExperience(Long userId, Long experienceId, ExperienceRequest request) {
        if (experienceId == null) {
            throw new IllegalArgumentException("El ID de experiencia no puede ser nulo");
        }
        Experience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new IllegalArgumentException("Experiencia no encontrada"));

        if (!experience.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Esta experiencia no pertenece a este usuario");
        }

        experience.setCompany(request.getCompany());
        experience.setPosition(request.getPosition());
        experience.setStartDate(request.getStartDate());
        experience.setEndDate(request.getEndDate());
        experience.setIsCurrent(request.getIsCurrent() != null ? request.getIsCurrent() : false);
        experience.setDescription(request.getDescription());
        experience.setLocation(request.getLocation());

        ExperienceResponse result = mapToResponse(experienceRepository.save(experience));
        log.info("Experiencia {} actualizada para usuario {}", experienceId, userId);
        return result;
    }

    @Transactional
    public void deleteExperience(Long userId, Long experienceId) {
        if (experienceId == null) {
            throw new IllegalArgumentException("El ID de experiencia no puede ser nulo");
        }
        Experience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new IllegalArgumentException("Experiencia no encontrada"));

        if (!experience.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Esta experiencia no pertenece a este usuario");
        }

        experienceRepository.delete(experience);
        log.info("Experiencia {} eliminada para usuario {}", experienceId, userId);
    }

    private ExperienceResponse mapToResponse(Experience e) {
        return new ExperienceResponse(
                e.getId(),
                e.getCompany(),
                e.getPosition(),
                e.getStartDate(),
                e.getEndDate(),
                e.getIsCurrent(),
                e.getDescription(),
                e.getLocation(),
                e.getCreatedAt()
        );
    }
}
