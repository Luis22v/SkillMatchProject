package com.skillmatch.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "job_id"}),
       indexes = {
           @Index(name = "idx_application_user_id",    columnList = "user_id"),
           @Index(name = "idx_application_job_id",     columnList = "job_id"),
           @Index(name = "idx_application_status",     columnList = "status"),
           @Index(name = "idx_application_created_at", columnList = "created_at")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Application {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @NonNull Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;
    
    @Column(length = 50, nullable = false)
    private ApplicationStatus status = ApplicationStatus.PENDIENTE;
    
    @Column(columnDefinition = "TEXT")
    private String resume; // CV o enlace al CV
    
    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter; // Carta de presentación
    
    @Column(name = "applied_date", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime appliedDate;
    
    @Column(name = "reviewed_date", columnDefinition = "DATETIME")
    private LocalDateTime reviewedDate;
    
    @Column(columnDefinition = "TEXT")
    private String notes; // Notas de la empresa sobre el candidato
    
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", columnDefinition = "DATETIME")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        appliedDate = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
