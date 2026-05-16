package com.skillmatch.backend.service;

import com.skillmatch.backend.dto.ApplicationRequest;
import com.skillmatch.backend.dto.ApplicationResponse;
import com.skillmatch.backend.exception.DuplicateResourceException;
import com.skillmatch.backend.exception.ResourceNotFoundException;
import com.skillmatch.backend.model.Application;
import com.skillmatch.backend.model.ApplicationStatus;
import com.skillmatch.backend.model.Company;
import com.skillmatch.backend.model.Job;
import com.skillmatch.backend.model.JobStatus;
import com.skillmatch.backend.model.User;
import com.skillmatch.backend.repository.ApplicationRepository;
import com.skillmatch.backend.repository.CompanyRepository;
import com.skillmatch.backend.repository.JobRepository;
import com.skillmatch.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private JobRepository jobRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ApplicationService applicationService;

    private User applicant;
    private User companyOwner;
    private Company company;
    private Job openJob;

    @BeforeEach
    void setUp() {
        applicant = new User();
        applicant.setId(1L);
        applicant.setEmail("user@test.com");
        applicant.setFirstName("Ana");
        applicant.setLastName("García");

        companyOwner = new User();
        companyOwner.setId(2L);

        company = new Company();
        company.setId(10L);
        company.setName("TechCorp");
        company.setUser(companyOwner);

        openJob = new Job();
        openJob.setId(5L);
        openJob.setTitle("Backend Dev");
        openJob.setActive(true);
        openJob.setStatus(JobStatus.ABIERTA);
        openJob.setCompany(company);
    }

    @Test
    void createApplication_validRequest_savesAndReturnsResponse() {
        ApplicationRequest request = new ApplicationRequest(5L, "mi cv", "carta de presentación");

        when(userRepository.findById(1L)).thenReturn(Optional.of(applicant));
        when(jobRepository.findById(5L)).thenReturn(Optional.of(openJob));
        when(applicationRepository.existsByUserIdAndJobId(1L, 5L)).thenReturn(false);

        Application saved = new Application();
        saved.setId(100L);
        saved.setUser(applicant);
        saved.setJob(openJob);
        saved.setStatus(ApplicationStatus.PENDIENTE);
        when(applicationRepository.save(any(Application.class))).thenReturn(saved);

        ApplicationResponse response = applicationService.createApplication(1L, request);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo("pendiente");
        verify(applicationRepository).save(any(Application.class));
    }

    @Test
    void createApplication_closedJob_throwsRuntimeException() {
        openJob.setStatus(JobStatus.CERRADA);
        openJob.setActive(false);
        ApplicationRequest request = new ApplicationRequest(5L, null, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(applicant));
        when(jobRepository.findById(5L)).thenReturn(Optional.of(openJob));

        assertThatThrownBy(() -> applicationService.createApplication(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no está disponible");
    }

    @Test
    void createApplication_alreadyApplied_throwsDuplicateResourceException() {
        ApplicationRequest request = new ApplicationRequest(5L, null, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(applicant));
        when(jobRepository.findById(5L)).thenReturn(Optional.of(openJob));
        when(applicationRepository.existsByUserIdAndJobId(1L, 5L)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.createApplication(1L, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void updateStatus_validStatus_changesStatus() {
        Application application = new Application();
        application.setId(50L);
        application.setUser(applicant);
        application.setJob(openJob);
        application.setStatus(ApplicationStatus.PENDIENTE);

        when(applicationRepository.findById(50L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationResponse result = applicationService.updateStatus(50L, "aceptada", null, companyOwner.getId());

        assertThat(result.getStatus()).isEqualTo("aceptada");
    }

    @Test
    void updateStatus_invalidStatus_throwsIllegalArgumentException() {
        Application application = new Application();
        application.setId(50L);
        application.setUser(applicant);
        application.setJob(openJob);
        application.setStatus(ApplicationStatus.PENDIENTE);

        when(applicationRepository.findById(50L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.updateStatus(50L, "invalido", null, companyOwner.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateStatus_wrongOwner_throwsAccessDeniedException() {
        User differentOwner = new User();
        differentOwner.setId(99L);
        Company otherCompany = new Company();
        otherCompany.setUser(differentOwner);
        Job otherJob = new Job();
        otherJob.setCompany(otherCompany);

        Application application = new Application();
        application.setId(60L);
        application.setUser(applicant);
        application.setJob(otherJob);
        application.setStatus(ApplicationStatus.PENDIENTE);

        when(applicationRepository.findById(60L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.updateStatus(60L, "aceptada", null, companyOwner.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getApplicationById_notFound_throwsResourceNotFoundException() {
        when(applicationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getApplicationById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
