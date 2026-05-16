package com.skillmatch.backend.service;

import com.skillmatch.backend.dto.AuthResponse;
import com.skillmatch.backend.dto.RegisterRequest;
import com.skillmatch.backend.exception.DuplicateResourceException;
import com.skillmatch.backend.model.Company;
import com.skillmatch.backend.model.Role;
import com.skillmatch.backend.model.User;
import com.skillmatch.backend.repository.CompanyRepository;
import com.skillmatch.backend.repository.RoleRepository;
import com.skillmatch.backend.repository.UserRepository;
import com.skillmatch.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider tokenProvider;

    @InjectMocks
    private RegistrationService registrationService;

    private RegisterRequest userRequest;
    private RegisterRequest empresaRequest;
    private Role userRole;
    private Role empresaRole;

    @BeforeEach
    void setUp() {
        userRequest = new RegisterRequest("user@test.com", "password123", "Ana", "García", "3001234567", "USER");
        empresaRequest = new RegisterRequest("empresa@test.com", "password123", "TechCorp", "SAS", "3009876543", "EMPRESA");

        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");

        empresaRole = new Role();
        empresaRole.setId(2L);
        empresaRole.setName("EMPRESA");
    }

    @Test
    void register_userType_createsUserWithUserRole() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));

        User savedUser = new User();
        savedUser.setId(10L);
        savedUser.setEmail("user@test.com");
        savedUser.setFirstName("Ana");
        savedUser.setLastName("García");
        savedUser.setRoles(java.util.Set.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(mockAuth);
        when(tokenProvider.generateToken(mockAuth)).thenReturn("jwt.token");
        when(tokenProvider.getExpirationMillis()).thenReturn(86400000L);

        AuthResponse result = registrationService.register(userRequest);

        assertThat(result.getRole()).isEqualTo("USER");
        assertThat(result.getEmail()).isEqualTo("user@test.com");
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getCompanyId()).isNull();
        verify(companyRepository, never()).save(any());
    }

    @Test
    void register_empresaType_createsCompanyProfile() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(roleRepository.findByName("EMPRESA")).thenReturn(Optional.of(empresaRole));

        User savedUser = new User();
        savedUser.setId(20L);
        savedUser.setEmail("empresa@test.com");
        savedUser.setFirstName("TechCorp");
        savedUser.setLastName("SAS");
        savedUser.setRoles(java.util.Set.of(empresaRole));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        Company savedCompany = new Company();
        savedCompany.setId(5L);
        when(companyRepository.save(any(Company.class))).thenReturn(savedCompany);

        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(mockAuth);
        when(tokenProvider.generateToken(mockAuth)).thenReturn("jwt.token");
        when(tokenProvider.getExpirationMillis()).thenReturn(86400000L);

        AuthResponse result = registrationService.register(empresaRequest);

        assertThat(result.getRole()).isEqualTo("EMPRESA");
        assertThat(result.getCompanyId()).isEqualTo(5L);
        verify(companyRepository).save(any(Company.class));
    }

    @Test
    void register_duplicateEmail_throwsDuplicateResourceException() {
        when(userRepository.existsByEmail("user@test.com")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(userRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_roleNotFound_createsAndSavesNewRole() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(userRole);

        User savedUser = new User();
        savedUser.setId(11L);
        savedUser.setEmail("user@test.com");
        savedUser.setFirstName("Ana");
        savedUser.setLastName("García");
        savedUser.setRoles(java.util.Set.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(mockAuth);
        when(tokenProvider.generateToken(mockAuth)).thenReturn("tok");
        when(tokenProvider.getExpirationMillis()).thenReturn(86400000L);

        registrationService.register(userRequest);

        verify(roleRepository).save(any(Role.class));
    }
}
