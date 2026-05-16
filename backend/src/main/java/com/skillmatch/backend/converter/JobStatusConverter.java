package com.skillmatch.backend.converter;

import com.skillmatch.backend.model.JobStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class JobStatusConverter implements AttributeConverter<JobStatus, String> {

    @Override
    public String convertToDatabaseColumn(JobStatus status) {
        return status != null ? status.getValue() : null;
    }

    @Override
    public JobStatus convertToEntityAttribute(String value) {
        return JobStatus.fromValue(value);
    }
}
