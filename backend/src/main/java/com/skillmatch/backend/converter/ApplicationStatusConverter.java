package com.skillmatch.backend.converter;

import com.skillmatch.backend.model.ApplicationStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ApplicationStatusConverter implements AttributeConverter<ApplicationStatus, String> {

    @Override
    public String convertToDatabaseColumn(ApplicationStatus status) {
        return status != null ? status.getValue() : null;
    }

    @Override
    public ApplicationStatus convertToEntityAttribute(String value) {
        return ApplicationStatus.fromValue(value);
    }
}
