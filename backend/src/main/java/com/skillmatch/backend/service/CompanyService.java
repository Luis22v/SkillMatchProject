package com.skillmatch.backend.service;

import com.skillmatch.backend.dto.CompanyRequest;
import com.skillmatch.backend.dto.CompanyResponse;
import com.skillmatch.backend.model.Company;
import com.skillmatch.backend.model.User;
import com.skillmatch.backend.repository.CompanyRepository;
import com.skillmatch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final JobService jobService;
    private final ApplicationService applicationService;

    public CompanyResponse createCompany(CompanyRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RuntimeException("El email de la empresa es obligatorio");
        }
        if (companyRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Ya existe una empresa con este email");
        }

        User owner = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException(
                        "Debe existir un usuario registrado con este email"));

        Company company = buildCompanyFromRequest(new Company(), request);
        company.setUser(owner);

        return mapToResponse(companyRepository.save(company));
    }

    public CompanyResponse updateCompany(@NonNull Long id, CompanyRequest request) {
        Company company = findCompanyOrThrow(id);

        String requestedEmail = request.getEmail();
        if (requestedEmail == null || requestedEmail.isBlank()) {
            throw new RuntimeException("El email de la empresa es obligatorio");
        }

        companyRepository.findByEmail(requestedEmail).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new RuntimeException("Ya existe una empresa con este email");
            }
        });

        // Sincronizar email del usuario dueño si cambió
        if (!company.getEmail().equals(requestedEmail)) {
            if (userRepository.existsByEmail(requestedEmail)) {
                throw new RuntimeException("El email ya está en uso por otro usuario");
            }
            User owner = company.getUser();
            owner.setEmail(requestedEmail);
            userRepository.save(owner);
        }

        buildCompanyFromRequest(company, request);

        if (request.getIsVerified() != null) company.setIsVerified(request.getIsVerified());
        if (request.getActive() != null)     company.setActive(request.getActive());

        return mapToResponse(companyRepository.save(company));
    }

    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(@NonNull Long id) {
        return mapToResponse(findCompanyOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> getAllCompanies() {
        return companyRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> getActiveCompanies() {
        return companyRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> searchCompanies(String keyword) {
        return companyRepository.searchCompanies(keyword).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> getCompaniesByFilters(String industry,
                                                        String location,
                                                        String size) {
        return companyRepository.findByFilters(industry, location, size).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public void deleteCompany(@NonNull Long id) {
        Company company = findCompanyOrThrow(id);
        company.setActive(false);
        companyRepository.save(company);
    }

    public void verifyCompany(@NonNull Long id) {
        Company company = findCompanyOrThrow(id);
        company.setIsVerified(true);
        companyRepository.save(company);
    }

    public CompanyResponse updateCompanyDescription(@NonNull Long id, String description) {
        Company company = findCompanyOrThrow(id);
        company.setDescription(description);
        return mapToResponse(companyRepository.save(company));
    }

    /**
     * Verifica que el usuario dado sea el dueño de la empresa.
     * Usado en el controlador para autorización de operaciones de empresa.
     */
    @Transactional(readOnly = true)
    public boolean isOwner(@NonNull Long companyId, @NonNull Long userId) {
        return companyRepository.findById(companyId)
                .map(c -> c.getUser() != null && userId.equals(c.getUser().getId()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCompanyStatistics(@NonNull Long companyId) {
        Company company = findCompanyOrThrow(companyId);

        var jobs = jobService.getJobsByCompany(companyId);

        // ✅ FIX #1: El dominio usa "abierta" (minúsculas), no "ACTIVA".
        // Se usa equalsIgnoreCase como defensa adicional.
        long activeJobs = jobs.stream()
                .filter(j -> "abierta".equalsIgnoreCase(j.getStatus())
                          && Boolean.TRUE.equals(j.getActive()))
                .count();

        long totalJobs = jobs.size();

        var applications = applicationService.getApplicationsByCompany(companyId);
        long totalApplications = applications.size();

        // ✅ FIX #1: mismo criterio — el dominio usa "pendiente" (minúsculas)
        long pendingApplications = applications.stream()
                .filter(a -> "pendiente".equalsIgnoreCase(a.getStatus()))
                .count();

        LocalDateTime createdAt = company.getCreatedAt();
        long daysSinceCreation = createdAt != null
                ? ChronoUnit.DAYS.between(createdAt, LocalDateTime.now()) : 0;

        long profileViews = 100
                + (totalJobs * 15L)
                + (totalApplications * 3L)
                + (daysSinceCreation * 5L);

        double responseRate = 0;
        if (totalApplications > 0) {
            long reviewed = totalApplications - pendingApplications;
            responseRate = (reviewed * 100.0) / totalApplications;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("profileViews",        profileViews);
        stats.put("activeJobs",          activeJobs);
        stats.put("totalJobs",           totalJobs);
        stats.put("totalApplications",   totalApplications);
        stats.put("pendingApplications", pendingApplications);
        stats.put("responseRate",        Math.round(responseRate));
        return stats;
    }

    @Transactional
    public void syncOwnerEmail(Long userId, String newEmail) {
        companyRepository.findByUserId(userId).ifPresent(company -> {
            company.setEmail(newEmail);
            companyRepository.save(company);
        });
    }

    // ─── Helpers privados ────────────────────────────────────────────────────

    private Company findCompanyOrThrow(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada con ID: " + id));
    }

    /** Rellena un objeto Company con los datos del request. Reutilizable en create y update. */
    private Company buildCompanyFromRequest(Company company, CompanyRequest request) {
        company.setName(request.getName());
        company.setDescription(request.getDescription());
        company.setIndustry(request.getIndustry());
        company.setSize(request.getSize());
        company.setLocation(request.getLocation());
        company.setLogo(request.getLogo());
        company.setWebsite(request.getWebsite());
        company.setEmail(request.getEmail());
        company.setPhone(request.getPhone());
        company.setFoundedYear(request.getFoundedYear());
        company.setBenefits(request.getBenefits());
        return company;
    }

    private CompanyResponse mapToResponse(Company company) {
        CompanyResponse response = new CompanyResponse();
        response.setId(company.getId());
        response.setName(company.getName());
        response.setDescription(company.getDescription());
        response.setIndustry(company.getIndustry());
        response.setSize(company.getSize());
        response.setLocation(company.getLocation());
        response.setLogo(company.getLogo());
        response.setWebsite(company.getWebsite());
        response.setEmail(company.getEmail());
        response.setPhone(company.getPhone());
        response.setUserId(company.getUser() != null ? company.getUser().getId() : null);
        response.setFoundedYear(company.getFoundedYear());
        response.setBenefits(company.getBenefits());
        response.setIsVerified(company.getIsVerified());
        response.setActive(company.getActive());
        response.setCreatedAt(company.getCreatedAt());
        response.setUpdatedAt(company.getUpdatedAt());
        return response;
    }
}