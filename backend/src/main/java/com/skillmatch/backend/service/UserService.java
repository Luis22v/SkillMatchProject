package com.skillmatch.backend.service;

import com.skillmatch.backend.dto.UpdatePasswordRequest;
import com.skillmatch.backend.dto.UpdateProfileRequest;
import com.skillmatch.backend.exception.ResourceNotFoundException;
import com.skillmatch.backend.model.User;
import com.skillmatch.backend.repository.ApplicationRepository;
import com.skillmatch.backend.repository.SkillRepository;
import com.skillmatch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CompanyService companyService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationRepository applicationRepository;
    private final SkillRepository skillRepository;

    @Transactional(readOnly = true)
    public User getUserById(@NonNull Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(@NonNull String email) {
        if (email.isBlank()) {
            throw new IllegalArgumentException("El email no puede estar vacío");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));
    }

    @Transactional
    public User updateProfile(@NonNull Long userId, UpdateProfileRequest request) {
        User user = getUserById(userId);

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("El email ya está en uso");
            }
            user.setEmail(request.getEmail());
            companyService.syncOwnerEmail(userId, request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getHeadline() != null) {
            user.setHeadline(request.getHeadline());
        }
        if (request.getLocation() != null) {
            user.setLocation(request.getLocation());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        User saved = userRepository.save(Objects.requireNonNull(user));
        log.info("Perfil actualizado para usuario {}", userId);
        return saved;
    }

    @Transactional
    public User updateProfileImage(@NonNull Long userId, String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("La URL de la imagen de perfil es obligatoria");
        }
        User user = getUserById(userId);
        user.setProfileImageUrl(imageUrl);
        User saved = Objects.requireNonNull(userRepository.save(user));
        log.info("Imagen de perfil actualizada para usuario {}", userId);
        return saved;
    }

    @Transactional
    public User updateCoverImage(@NonNull Long userId, String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("La URL de la imagen de portada es obligatoria");
        }
        User user = getUserById(userId);
        user.setCoverImageUrl(imageUrl);
        User saved = Objects.requireNonNull(userRepository.save(user));
        log.info("Imagen de portada actualizada para usuario {}", userId);
        return saved;
    }

    @Transactional
    public void updatePassword(@NonNull Long userId, UpdatePasswordRequest request) {
        User user = getUserById(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Contraseña incorrecta al intentar cambio para usuario {}", userId);
            throw new IllegalArgumentException("La contraseña actual es incorrecta");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        Objects.requireNonNull(userRepository.save(user));
        log.info("Contraseña actualizada para usuario {}", userId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserProfile(@NonNull Long userId) {
        User user = getUserById(userId);

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("email", user.getEmail());
        profile.put("firstName", user.getFirstName());
        profile.put("lastName", user.getLastName());
        profile.put("phone", user.getPhone());
        profile.put("headline", user.getHeadline());
        profile.put("location", user.getLocation());
        profile.put("bio", user.getBio());
        profile.put("profileImageUrl", user.getProfileImageUrl());
        profile.put("coverImageUrl", user.getCoverImageUrl());
        profile.put("createdAt", user.getCreatedAt());

        return profile;
    }

    @Transactional(readOnly = true)
    public List<User> findSuggestionsForUser(@NonNull Long userId) {
        User user = getUserById(userId);
        if (user.getLocation() == null) {
            return List.of();
        }
        return userRepository.findSuggestions(userId, user.getLocation(), PageRequest.of(0, 10))
                .getContent();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserStatistics(@NonNull Long userId) {
        User user = getUserById(userId);

        long applicationsCount = applicationRepository.countByUserId(userId);
        long skillsCount = skillRepository.countByUserId(userId);

        LocalDateTime createdAt = user.getCreatedAt();
        long daysSinceCreation = createdAt != null ? ChronoUnit.DAYS.between(createdAt, LocalDateTime.now()) : 0;
        long profileViews = 50 + (applicationsCount * 5) + (daysSinceCreation * 2);

        int completionScore = 0;
        if (user.getFirstName() != null && !user.getFirstName().isEmpty()) completionScore += 15;
        if (user.getLastName() != null && !user.getLastName().isEmpty()) completionScore += 15;
        if (user.getPhone() != null && !user.getPhone().isEmpty()) completionScore += 10;
        if (user.getHeadline() != null && !user.getHeadline().isEmpty()) completionScore += 15;
        if (user.getBio() != null && !user.getBio().isEmpty()) completionScore += 10;
        if (skillsCount > 0) completionScore += 20;
        if (applicationsCount > 0) completionScore += 15;

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("profileViews", profileViews);
        statistics.put("applicationsCount", applicationsCount);
        statistics.put("matchRate", completionScore);
        statistics.put("skillsCount", skillsCount);
        statistics.put("profileCompleteness", completionScore);

        return statistics;
    }
}
