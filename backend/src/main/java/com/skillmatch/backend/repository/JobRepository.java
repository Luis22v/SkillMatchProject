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

    // ─── Sin paginación (usados internamente / dashboard empresa) ───────────────

    @Query("SELECT j FROM Job j JOIN FETCH j.company WHERE j.company.id = :companyId")
    List<Job> findByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT j FROM Job j WHERE j.active = true AND j.status = 'abierta' " +
           "ORDER BY j.postedDate DESC")
    List<Job> findExpiredJobs(@Param("date") LocalDateTime date);

    // ─── Con paginación (endpoints públicos) ─────────────────────────────────

    @Query(value = "SELECT j FROM Job j JOIN FETCH j.company WHERE j.active = true AND j.status = 'abierta'",
           countQuery = "SELECT COUNT(j) FROM Job j WHERE j.active = true AND j.status = 'abierta'")
    Page<Job> findActiveJobs(Pageable pageable);

    @Query(value = "SELECT j FROM Job j JOIN FETCH j.company WHERE j.active = true AND " +
           "(LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%')))",
           countQuery = "SELECT COUNT(j) FROM Job j WHERE j.active = true AND " +
           "(LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Job> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT j FROM Job j JOIN FETCH j.company WHERE j.active = true " +
           "AND (:type IS NULL OR j.type = :type) " +
           "AND (:modality IS NULL OR j.modality = :modality) " +
           "AND (:experienceLevel IS NULL OR j.experienceLevel = :experienceLevel) " +
           "AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
           "AND (:minSalary IS NULL OR j.salaryMax >= :minSalary) " +
           "AND (:maxSalary IS NULL OR j.salaryMin <= :maxSalary) " +
           "AND j.status = 'abierta'",
           countQuery = "SELECT COUNT(j) FROM Job j WHERE j.active = true " +
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

    @Query(value = "SELECT j FROM Job j JOIN FETCH j.company WHERE j.active = true AND j.status = 'abierta' " +
           "ORDER BY j.postedDate DESC",
           countQuery = "SELECT COUNT(j) FROM Job j WHERE j.active = true AND j.status = 'abierta'")
    Page<Job> findRecentJobs(Pageable pageable);

    @Query("SELECT j FROM Job j JOIN FETCH j.company WHERE j.company.id = :companyId AND j.active = true " +
           "ORDER BY j.postedDate DESC")
    List<Job> findActiveByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT j FROM Job j WHERE j.active = true AND j.status = 'abierta' " +
           "AND j.expirationDate IS NOT NULL AND j.expirationDate <= :date")
    List<Job> findExpiredJobsToClose(@Param("date") LocalDateTime date);
}
