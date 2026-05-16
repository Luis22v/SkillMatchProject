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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("Intento de registro con email duplicado: {}", registerRequest.getEmail());
            throw new DuplicateResourceException("El email ya está registrado");
        }

        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setPhone(registerRequest.getPhone());
        user.setEnabled(true);

        String roleName = "EMPRESA".equalsIgnoreCase(registerRequest.getUserType()) ? "EMPRESA" : "USER";

        Role userRole = roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(roleName);
                    newRole.setDescription(roleName.equals("EMPRESA") ? "Empresa" : "Usuario estándar");
                    return roleRepository.save(newRole);
                });

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        Long companyId = null;
        if ("EMPRESA".equals(roleName)) {
            companyId = createCompanyProfile(savedUser, registerRequest);
        }

        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                registerRequest.getEmail(),
                registerRequest.getPassword()
            )
        );

        String jwt = tokenProvider.generateToken(authentication);

        long expiresAt = System.currentTimeMillis() + tokenProvider.getExpirationMillis();
        log.info("Nuevo {} registrado: email={}, id={}", roleName, savedUser.getEmail(), savedUser.getId());
        AuthResponse response = new AuthResponse(jwt, savedUser.getId(), companyId, savedUser.getEmail(),
                savedUser.getFirstName(), savedUser.getLastName(), roleName);
        response.setExpiresAt(expiresAt);
        return response;
    }

    private Long createCompanyProfile(User owner, RegisterRequest request) {
        Company company = new Company();
        company.setUser(owner);
        company.setName(request.getFirstName());
        company.setEmail(request.getEmail());
        company.setPhone(request.getPhone());
        company.setLocation("No especificada");
        company.setDescription("Bienvenido a " + request.getFirstName()
                + ". Edita esta sección para agregar información sobre tu empresa, misión, visión y valores.");
        company.setActive(true);
        company.setIsVerified(false);
        return companyRepository.save(company).getId();
    }
}
