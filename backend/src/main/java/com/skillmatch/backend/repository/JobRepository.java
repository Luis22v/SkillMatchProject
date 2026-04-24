package com.skillmatch.backend.repository;

import com.skillmatch.backend.model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    // ─── Sin paginación (usados internamente / seeder) ───────────────────────

    List<Job> findByCompanyId(Long companyId);

    @Query("SELECT j FROM Job j WHERE j.active = true AND j.status = 'abierta' " +
           "ORDER BY j.postedDate DESC")
    List<Job> findExpiredJobs(@Param("date") LocalDateTime date);

    // ─── Con paginación (endpoints públicos) ─────────────────────────────────

    /**
     * Lista paginada de ofertas activas.
     * ✅ Reemplaza findActiveJobs() sin Pageable para endpoints de listing.
     */
    @Query("SELECT j FROM Job j WHERE j.active = true AND j.status = 'abierta'")
    Page<Job> findActiveJobs(Pageable pageable);

    /**
     * Búsqueda por palabra clave con paginación.
     */
    @Query("SELECT j FROM Job j WHERE j.active = true AND " +
           "(LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Job> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Filtros avanzados con paginación.
     */
    @Query("SELECT j FROM Job j WHERE j.active = true " +
           "AND (:type IS NULL OR j.type = :type) " +
           "AND (:modality IS NULL OR j.modality = :modality) " +
           "AND (:experienceLevel IS NULL OR j.experienceLevel = :experienceLevel) " +
           "AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
           "AND (:minSalary IS NULL OR j.salaryMax >= :minSalary) " +
           "AND (:maxSalary IS NULL OR j.salaryMin <= :maxSalary) " +
           "AND j.status = 'abierta'")
    Page<Job> findByFilters(
            @Param("type")            String type,
            @Param("modality")        String modality,
            @Param("experienceLevel") String experienceLevel,
            @Param("location")        String location,
            @Param("minSalary")       Double minSalary,
            @Param("maxSalary")       Double maxSalary,
            Pageable pageable
    );

    /**
     * Jobs más recientes (para home/landing) — lista corta, sin paginación explícita.
     * Se limita desde el servicio con PageRequest.of(0, 10).
     */
    @Query("SELECT j FROM Job j WHERE j.active = true AND j.status = 'abierta' " +
           "ORDER BY j.postedDate DESC")
    Page<Job> findRecentJobs(Pageable pageable);

    /**
     * Jobs por compañía — sin paginar (uso interno del dashboard de empresa).
     */
    @Query("SELECT j FROM Job j WHERE j.company.id = :companyId AND j.active = true " +
           "ORDER BY j.postedDate DESC")
    List<Job> findActiveByCompanyId(@Param("companyId") Long companyId);

    /**
     * Jobs expirados para el scheduler.
     */
    @Query("SELECT j FROM Job j WHERE j.active = true AND j.status = 'abierta' " +
           "AND j.expirationDate IS NOT NULL AND j.expirationDate <= :date")
    List<Job> findExpiredJobsToClose(@Param("date") LocalDateTime date);
}