package com.skillmatch.backend.service;

import com.skillmatch.backend.dto.EducationRequest;
import com.skillmatch.backend.dto.EducationResponse;
import com.skillmatch.backend.model.Education;
import com.skillmatch.backend.model.User;
import com.skillmatch.backend.repository.EducationRepository;
import com.skillmatch.backend.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EducationService {

    private final EducationRepository educationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<EducationResponse> getUserEducations(Long userId) {
        return educationRepository.findByUserIdOrderByStartDateDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EducationResponse addEducation(@NonNull Long userId, EducationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Education education = new Education();
        education.setUser(user);
        education.setSchool(request.getSchool());
        education.setDegree(request.getDegree());
        education.setFieldOfStudy(request.getFieldOfStudy());
        education.setStartDate(request.getStartDate());
        education.setEndDate(request.getEndDate());
        education.setIsCurrent(request.getIsCurrent() != null ? request.getIsCurrent() : false);
        education.setDescription(request.getDescription());

        EducationResponse result = mapToResponse(educationRepository.save(education));
        log.info("Educación en '{}' agregada para usuario {}", request.getSchool(), userId);
        return result;
    }

    @Transactional
    public EducationResponse updateEducation(Long userId, @NonNull Long educationId, EducationRequest request) {
        Education education = educationRepository.findById(educationId)
                .orElseThrow(() -> new IllegalArgumentException("Educación no encontrada"));

        if (!education.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Esta educación no pertenece a este usuario");
        }

        education.setSchool(request.getSchool());
        education.setDegree(request.getDegree());
        education.setFieldOfStudy(request.getFieldOfStudy());
        education.setStartDate(request.getStartDate());
        education.setEndDate(request.getEndDate());
        education.setIsCurrent(request.getIsCurrent() != null ? request.getIsCurrent() : false);
        education.setDescription(request.getDescription());

        EducationResponse result = mapToResponse(educationRepository.save(education));
        log.info("Educación {} actualizada para usuario {}", educationId, userId);
        return result;
    }

    @Transactional
    public void deleteEducation(Long userId, @NonNull Long educationId) {
        Education education = educationRepository.findById(educationId)
                .orElseThrow(() -> new IllegalArgumentException("Educación no encontrada"));

        if (!education.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Esta educación no pertenece a este usuario");
        }

        educationRepository.delete(education);
        log.info("Educación {} eliminada para usuario {}", educationId, userId);
    }

    private EducationResponse mapToResponse(Education e) {
        return new EducationResponse(
                e.getId(),
                e.getSchool(),
                e.getDegree(),
                e.getFieldOfStudy(),
                e.getStartDate(),
                e.getEndDate(),
                e.getIsCurrent(),
                e.getDescription(),
                e.getCreatedAt()
        );
    }
}
