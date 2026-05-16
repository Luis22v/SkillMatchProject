package com.skillmatch.backend.service;

import com.skillmatch.backend.dto.CertificationRequest;
import com.skillmatch.backend.dto.CertificationResponse;
import com.skillmatch.backend.model.Certification;
import com.skillmatch.backend.model.User;
import com.skillmatch.backend.repository.CertificationRepository;
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
public class CertificationService {

    private final CertificationRepository certificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CertificationResponse> getUserCertifications(Long userId) {
        return certificationRepository.findByUserIdOrderByIssueDateDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CertificationResponse addCertification(@NonNull Long userId, CertificationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Certification certification = new Certification();
        certification.setUser(user);
        certification.setName(request.getName());
        certification.setIssuer(request.getIssuer());
        certification.setIssueDate(request.getIssueDate());
        certification.setExpirationDate(request.getExpirationDate());
        certification.setCredentialId(request.getCredentialId());
        certification.setCredentialUrl(request.getCredentialUrl());
        certification.setDescription(request.getDescription());

        CertificationResponse result = mapToResponse(certificationRepository.save(certification));
        log.info("Certificación '{}' agregada para usuario {}", request.getName(), userId);
        return result;
    }

    @Transactional
    public CertificationResponse updateCertification(Long userId, @NonNull Long certificationId,
                                                      CertificationRequest request) {
        Certification certification = certificationRepository.findById(certificationId)
                .orElseThrow(() -> new IllegalArgumentException("Certificación no encontrada"));

        if (!certification.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Esta certificación no pertenece a este usuario");
        }

        certification.setName(request.getName());
        certification.setIssuer(request.getIssuer());
        certification.setIssueDate(request.getIssueDate());
        certification.setExpirationDate(request.getExpirationDate());
        certification.setCredentialId(request.getCredentialId());
        certification.setCredentialUrl(request.getCredentialUrl());
        certification.setDescription(request.getDescription());

        CertificationResponse result = mapToResponse(certificationRepository.save(certification));
        log.info("Certificación {} actualizada para usuario {}", certificationId, userId);
        return result;
    }

    @Transactional
    public void deleteCertification(Long userId, @NonNull Long certificationId) {
        Certification certification = certificationRepository.findById(certificationId)
                .orElseThrow(() -> new IllegalArgumentException("Certificación no encontrada"));

        if (!certification.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Esta certificación no pertenece a este usuario");
        }

        certificationRepository.delete(certification);
        log.info("Certificación {} eliminada para usuario {}", certificationId, userId);
    }

    private CertificationResponse mapToResponse(Certification c) {
        return new CertificationResponse(
                c.getId(),
                c.getName(),
                c.getIssuer(),
                c.getIssueDate(),
                c.getExpirationDate(),
                c.getCredentialId(),
                c.getCredentialUrl(),
                c.getDescription(),
                c.getCreatedAt()
        );
    }
}
