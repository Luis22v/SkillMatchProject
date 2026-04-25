package com.skillmatch.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificationRequest {

    @NotBlank(message = "El nombre de la certificación es obligatorio")
    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    private String name;

    @NotBlank(message = "El emisor de la certificación es obligatorio")
    @Size(max = 200, message = "El emisor no puede exceder 200 caracteres")
    private String issuer;

    private LocalDate issueDate;

    private LocalDate expirationDate;

    @Size(max = 100, message = "El ID de credencial no puede exceder 100 caracteres")
    private String credentialId;

    private String credentialUrl;

    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String description;
}
