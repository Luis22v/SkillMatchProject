package com.skillmatch.backend.service;

import com.skillmatch.backend.dto.CompanyRequest;
import com.skillmatch.backend.dto.CompanyResponse;
import com.skillmatch.backend.exception.DuplicateResourceException;
import com.skillmatch.backend.exception.ResourceNotFoundException;
import com.skillmatch.backend.model.Company;
import com.skillmatch.backend.model.User;
import com.skillmatch.backend.repository.CompanyRepository;
import com.skillmatch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
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
            log.warn("Intento de crear empresa con email duplicado: {}", request.getEmail());
            throw new DuplicateResourceException("Ya existe una empresa con este email");
        }

        User owner = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException(
                        "Debe existir un usuario registrado con este email"));

        Company company = buildCompanyFromRequest(new Company(), request);
        company.setUser(owner);

        Company saved = companyRepository.save(company);
        log.info("Empresa creada: '{}' (id={})", saved.getName(), saved.getId());
        return mapToResponse(saved);
    }

    public CompanyResponse updateCompany(@NonNull Long id, CompanyRequest request) {
        Company company = findCompanyOrThrow(id);

        String requestedEmail = request.getEmail();
        if (requestedEmail == null || requestedEmail.isBlank()) {
            throw new RuntimeException("El email de la empresa es obligatorio");
        }

        companyRepository.findByEmail(requestedEmail).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("Ya existe una empresa con este email");
            }
        });

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

        Company updated = companyRepository.save(company);
        log.info("Empresa actualizada: id={}", id);
        return mapToResponse(updated);
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
    public Page<CompanyResponse> getAllCompanies(Pageable pageable) {
        return companyRepository.findAll(Objects.requireNonNull(pageable)).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> getActiveCompanies() {
        return companyRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<CompanyResponse> getActiveCompanies(Pageable pageable) {
        return companyRepository.findByActiveTrue(pageable).map(this::mapToResponse);
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
        log.info("Empresa desactivada: id={}", id);
    }

    public void verifyCompany(@NonNull Long id) {
        Company company = findCompanyOrThrow(id);
        company.setIsVerified(true);
        companyRepository.save(company);
        log.info("Empresa verificada: id={}", id);
    }

    public CompanyResponse updateCompanyDescription(@NonNull Long id, String description) {
        Company company = findCompanyOrThrow(id);
        company.setDescription(description);
        Company updated = companyRepository.save(company);
        log.info("Descripción actualizada para empresa id={}", id);
        return mapToResponse(updated);
    }

    @Transactional(readOnly = true)
    public boolean isOwner(@NonNull Long companyId, @NonNull Long userId) {
        return companyRepository.findById(companyId)
                .map(c -> c.getUser() != null && userId.equals(c.getUser().getId()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCompanyStatistics(@NonNull Long companyId) {
        Company company = findCompanyOrThrow(companyId);

        long totalJobs = jobService.countJobsByCompany(companyId);
        long activeJobs = jobService.countActiveJobsByCompany(companyId);
        long totalApplications = applicationService.countApplicationsByCompany(companyId);
        long pendingApplications = applicationService.countPendingApplicationsByCompany(companyId);

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
    public void syncOwnerEmail(@NonNull Long userId, String newEmail) {
        companyRepository.findByUserId(userId).ifPresent(company -> {
            company.setEmail(newEmail);
            companyRepository.save(company);
        });
    }

    // ─── Helpers privados ────────────────────────────────────────────────────

    private Company findCompanyOrThrow(@NonNull Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada con ID: " + id));
    }

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
