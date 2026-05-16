package com.skillmatch.backend.service;

import com.skillmatch.backend.dto.ApplicationRequest;
import com.skillmatch.backend.dto.ApplicationResponse;
import com.skillmatch.backend.exception.DuplicateResourceException;
import com.skillmatch.backend.exception.ResourceNotFoundException;
import com.skillmatch.backend.model.Application;
import com.skillmatch.backend.model.ApplicationStatus;
import com.skillmatch.backend.model.Job;
import com.skillmatch.backend.model.JobStatus;
import com.skillmatch.backend.model.User;
import com.skillmatch.backend.repository.ApplicationRepository;
import com.skillmatch.backend.repository.CompanyRepository;
import com.skillmatch.backend.repository.JobRepository;
import com.skillmatch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    @Transactional
    public ApplicationResponse createApplication(@NonNull Long userId, ApplicationRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));

        Long jobId = request.getJobId();
        if (jobId == null) {
            throw new IllegalArgumentException("El ID del job no puede ser nulo");
        }
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job no encontrado con ID: " + jobId));

        if (!Boolean.TRUE.equals(job.getActive()) || job.getStatus() != JobStatus.ABIERTA) {
            throw new RuntimeException("El job no está disponible para postulaciones");
        }

        if (applicationRepository.existsByUserIdAndJobId(userId, request.getJobId())) {
            throw new DuplicateResourceException("Ya te has postulado a esta oferta");
        }

        Application application = new Application();
        application.setUser(user);
        application.setJob(job);
        application.setResume(request.getResume());
        application.setCoverLetter(request.getCoverLetter());
        application.setStatus(ApplicationStatus.PENDIENTE);

        Application saved = applicationRepository.save(application);
        log.info("Postulación creada: userId={}, jobId={}, applicationId={}", userId, jobId, saved.getId());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(@NonNull Long id) {
        Application application = applicationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Postulación no encontrada con ID: " + id));
        return mapToResponse(application);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByUser(@NonNull Long userId) {
        return applicationRepository.findByUserId(userId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByJob(@NonNull Long jobId) {
        return applicationRepository.findByJobId(jobId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByJob(@NonNull Long jobId, @NonNull Long requesterId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job no encontrado con ID: " + jobId));
        verifyJobOwnership(job, requesterId);
        return applicationRepository.findByJobId(jobId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByCompany(@NonNull Long companyId) {
        return applicationRepository.findByCompanyId(companyId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByCompany(@NonNull Long companyId, @NonNull Long requesterId) {
        verifyCompanyOwnership(companyId, requesterId);
        return applicationRepository.findByCompanyId(companyId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getApplicationsByCompany(@NonNull Long companyId, @NonNull Long requesterId, Pageable pageable) {
        verifyCompanyOwnership(companyId, requesterId);
        return applicationRepository.findPageByCompanyId(companyId, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByStatus(String status) {
        return applicationRepository.findByStatus(ApplicationStatus.fromValue(status)).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public ApplicationResponse updateStatus(@NonNull Long id, String status, String notes, @NonNull Long requesterId) {
        Application application = applicationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Postulación no encontrada con ID: " + id));

        verifyJobOwnership(application.getJob(), requesterId);

        ApplicationStatus appStatus = ApplicationStatus.fromValue(status);
        application.setStatus(appStatus);
        if (notes != null) {
            application.setNotes(notes);
        }

        if (appStatus != ApplicationStatus.PENDIENTE && application.getReviewedDate() == null) {
            application.setReviewedDate(LocalDateTime.now());
        }

        Application updated = applicationRepository.save(application);
        log.info("Estado de postulación {} cambiado a '{}'", id, status);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteApplication(@NonNull Long id, Long requesterId, boolean hasManagementPrivileges) {
        Application application = applicationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Postulación no encontrada con ID: " + id));

        boolean isOwner = application.getUser().getId().equals(requesterId);
        if (!hasManagementPrivileges && !isOwner) {
            throw new AccessDeniedException("No tienes permiso para eliminar esta postulación");
        }

        applicationRepository.delete(application);
        log.info("Postulación {} eliminada por usuario {}", id, requesterId);
    }

    @Transactional(readOnly = true)
    public long countApplicationsByCompany(@NonNull Long companyId) {
        return applicationRepository.countByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public long countPendingApplicationsByCompany(@NonNull Long companyId) {
        return applicationRepository.countByCompanyIdAndStatus(companyId, ApplicationStatus.PENDIENTE);
    }

    @Transactional(readOnly = true)
    public Long countApplicationsByJob(@NonNull Long jobId) {
        Long total = applicationRepository.countByJobId(jobId);
        log.debug("Total de postulaciones para job {}: {}", jobId, total);
        return total;
    }

    @Transactional(readOnly = true)
    public boolean hasUserApplied(@NonNull Long userId, @NonNull Long jobId) {
        boolean exists = applicationRepository.existsByUserIdAndJobId(userId, jobId);
        log.debug("Usuario {} ya aplicado a job {}: {}", userId, jobId, exists);
        return exists;
    }

    private void verifyJobOwnership(Job job, Long requesterId) {
        if (job.getCompany() == null || job.getCompany().getUser() == null
                || !requesterId.equals(job.getCompany().getUser().getId())) {
            throw new AccessDeniedException("No tienes permiso para acceder a estas postulaciones");
        }
    }

    private void verifyCompanyOwnership(@NonNull Long companyId, Long requesterId) {
        boolean isOwner = companyRepository.findById(companyId)
            .map(c -> c.getUser() != null && requesterId.equals(c.getUser().getId()))
            .orElse(false);
        if (!isOwner) {
            throw new AccessDeniedException("No tienes permiso para acceder a esta empresa");
        }
    }

    private ApplicationResponse mapToResponse(Application application) {
        ApplicationResponse response = new ApplicationResponse();
        User user = application.getUser();
        Job job = application.getJob();

        response.setId(application.getId());
        response.setUserId(user.getId());

        response.setUserName(user.getFirstName() + " " + user.getLastName());
        response.setUserEmail(user.getEmail());
        response.setUserPhone(user.getPhone());
        response.setUserHeadline(user.getHeadline());
        response.setUserLocation(user.getLocation());
        response.setUserProfileImageUrl(user.getProfileImageUrl());

        response.setJobId(job.getId());
        response.setJobTitle(job.getTitle());
        response.setCompanyName(job.getCompany().getName());

        response.setStatus(application.getStatus().getValue());
        response.setResume(application.getResume());
        response.setCoverLetter(application.getCoverLetter());
        response.setNotes(application.getNotes());
        response.setAppliedDate(application.getAppliedDate());
        response.setReviewedDate(application.getReviewedDate());
        response.setCreatedAt(application.getCreatedAt());
        response.setUpdatedAt(application.getUpdatedAt());

        return response;
    }
}
