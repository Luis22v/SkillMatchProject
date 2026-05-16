package com.skillmatch.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_jobs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "job_id"}),
       indexes = {
           @Index(name = "idx_saved_job_user_id", columnList = "user_id"),
           @Index(name = "idx_saved_job_job_id",  columnList = "job_id")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavedJob {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @NonNull Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;
    
    @Column(name = "saved_at", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime savedAt;
    
    @Column(columnDefinition = "TEXT")
    private String notes; // Notas personales del usuario sobre esta oferta
    
    @PrePersist
    protected void onCreate() {
        savedAt = LocalDateTime.now();
    }
}
