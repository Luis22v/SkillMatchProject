package com.skillmatch.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationRequest {
    
    @NotNull(message = "{validation.application.jobId.required}")
    private Long jobId;

    @Size(max = 1000, message = "{validation.application.resume.max}")
    private String resume;

    @Size(max = 2000, message = "{validation.application.coverLetter.max}")
    private String coverLetter;
}
