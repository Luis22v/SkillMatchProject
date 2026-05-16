package com.skillmatch.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "connections",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "connected_user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Connection {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @NonNull Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Usuario que inicia la conexión
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connected_user_id", nullable = false)
    private User connectedUser; // Usuario conectado (puede ser empresa)
    
    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private ConnectionStatus status = ConnectionStatus.PENDING;
    
    @Column(name = "requested_at", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime requestedAt;
    
    @Column(name = "responded_at", columnDefinition = "DATETIME")
    private LocalDateTime respondedAt;
    
    @Column(columnDefinition = "TEXT")
    private String message; // Mensaje al solicitar conexión
    
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", columnDefinition = "DATETIME")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        requestedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
