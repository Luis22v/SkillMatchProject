package com.skillmatch.backend.service;

import com.skillmatch.backend.dto.JobRequest;
import com.skillmatch.backend.dto.JobResponse;
import com.skillmatch.backend.model.Company;
import com.skillmatch.backend.model.Job;
import com.skillmatch.backend.repository.CompanyRepository;
import com.skillmatch.backend.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;

    // ─── Escritura ────────────────────────────────────────────────────────────

    @Transactional
    public JobResponse createJob(JobRequest request) {
        Long companyId = request.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("El ID de la compañía no puede ser nulo");
        }
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException(
                        "Compañía no encontrada con ID: " + companyId));

        validateSalaryRange(request);

        Job job = buildJobFromRequest(new Job(), request);
        job.setCompany(company);
        job.setStatus("abierta");
        job.setActive(true);

        return mapToResponse(jobRepository.save(job));
    }

    @Transactional
    public JobResponse updateJob(Long id, JobRequest request) {
        Job job = findJobOrThrow(id);
        validateSalaryRange(request);
        buildJobFromRequest(job, request);
        return mapToResponse(jobRepository.save(job));
    }

    @Transactional
    public JobResponse changeStatus(Long id, String status) {
        Job job = findJobOrThrow(id);
        if (!List.of("abierta", "cerrada", "pausada").contains(status)) {
            throw new RuntimeException("Estado inválido: " + status);
        }
        job.setStatus(status);
        job.setActive(!"cerrada".equalsIgnoreCase(status));
        return mapToResponse(jobRepository.save(job));
    }

    @Transactional
    public void deleteJob(Long id) {
        Job job = findJobOrThrow(id);
        job.setActive(false);
        job.setStatus("cerrada");
        jobRepository.save(job);
    }

    // ─── Lectura paginada ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public JobResponse getJobById(Long id) {
        return mapToResponse(findJobOrThrow(id));
    }

    /**
     * ✅ FIX #4: Todas las listas de jobs ahora usan paginación.
     * El controlador pasa el Pageable construido desde los query params.
     */
    @Transactional(readOnly = true)
    public Page<JobResponse> getAllActiveJobs(Pageable pageable) {
        return jobRepository.findActiveJobs(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> searchJobs(String keyword, Pageable pageable) {
        return jobRepository.searchByKeyword(keyword, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> getJobsByFilters(String type, String modality,
                                               String experienceLevel, String location,
                                               Double minSalary, Double maxSalary,
                                               Pageable pageable) {
        return jobRepository.findByFilters(
                type, modality, experienceLevel, location, minSalary, maxSalary, pageable
        ).map(this::mapToResponse);
    }

    /** Jobs recientes para la home — máximo 10, sin parámetros adicionales. */
    @Transactional(readOnly = true)
    public List<JobResponse> getRecentJobs() {
        return jobRepository.findRecentJobs(PageRequest.of(0, 10))
                .map(this::mapToResponse)
                .getContent();
    }

    /** Jobs de una empresa — lista completa para el dashboard interno. */
    @Transactional(readOnly = true)
    public List<JobResponse> getJobsByCompany(Long companyId) {
        return jobRepository.findByCompanyId(companyId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ─── Scheduler ────────────────────────────────────────────────────────────

    /**
     * ✅ FIX: Cierre automático de jobs expiradas.
     * Se ejecuta cada día a las 2:00 AM (hora del servidor).
     * Requiere @EnableScheduling en BackendApplication.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void closeExpiredJobs() {
        List<Job> expired = jobRepository.findExpiredJobsToClose(LocalDateTime.now());
        if (expired.isEmpty()) return;

        expired.forEach(job -> {
            job.setStatus("cerrada");
            job.setActive(false);
        });
        jobRepository.saveAll(expired);
        log.info("Scheduler: {} oferta(s) expirada(s) cerrada(s) automáticamente", expired.size());
    }

    // ─── Helpers privados ─────────────────────────────────────────────────────

    private Job findJobOrThrow(Long id) {
        if (id == null) throw new RuntimeException("El ID del job no puede ser nulo");
        return jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job no encontrado con ID: " + id));
    }

    private void validateSalaryRange(JobRequest request) {
        if (request.getSalaryMin() != null && request.getSalaryMax() != null
                && request.getSalaryMin() > request.getSalaryMax()) {
            throw new RuntimeException("El salario mínimo no puede ser mayor al salario máximo");
        }
    }

    private Job buildJobFromRequest(Job job, JobRequest request) {
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setType(request.getType());
        job.setExperienceLevel(request.getExperienceLevel());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setLocation(request.getLocation());
        job.setModality(request.getModality());
        job.setDuration(request.getDuration());
        job.setRequirements(request.getRequirements());
        job.setResponsibilities(request.getResponsibilities());
        job.setSkills(request.getSkills());
        job.setBenefits(request.getBenefits());
        job.setExpirationDate(request.getExpirationDate());
        return job;
    }

    private JobResponse mapToResponse(Job job) {
        JobResponse response = new JobResponse();
        response.setId(job.getId());
        response.setCompanyId(job.getCompany().getId());
        response.setCompanyName(job.getCompany().getName());
        response.setCompanyLogo(job.getCompany().getLogo());
        response.setTitle(job.getTitle());
        response.setDescription(job.getDescription());
        response.setType(job.getType());
        response.setExperienceLevel(job.getExperienceLevel());
        response.setSalaryMin(job.getSalaryMin());
        response.setSalaryMax(job.getSalaryMax());
        response.setLocation(job.getLocation());
        response.setModality(job.getModality());
        response.setDuration(job.getDuration());
        response.setRequirements(job.getRequirements());
        response.setResponsibilities(job.getResponsibilities());
        response.setSkills(job.getSkills());
        response.setBenefits(job.getBenefits());
        response.setStatus(job.getStatus());
        response.setPostedDate(job.getPostedDate());
        response.setExpirationDate(job.getExpirationDate());
        response.setActive(job.getActive());
        response.setCreatedAt(job.getCreatedAt());
        response.setUpdatedAt(job.getUpdatedAt());
        return response;
    }
}