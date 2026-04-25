package com.skillmatch.backend.repository;

import com.skillmatch.backend.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    @Query("SELECT a FROM Application a JOIN FETCH a.user JOIN FETCH a.job j JOIN FETCH j.company WHERE a.user.id = :userId")
    List<Application> findByUserId(@Param("userId") Long userId);

    @Query("SELECT a FROM Application a JOIN FETCH a.user JOIN FETCH a.job j JOIN FETCH j.company WHERE a.job.id = :jobId")
    List<Application> findByJobId(@Param("jobId") Long jobId);

    @Query("SELECT a FROM Application a JOIN FETCH a.user JOIN FETCH a.job j JOIN FETCH j.company WHERE a.status = :status")
    List<Application> findByStatus(@Param("status") String status);

    @Query("SELECT a FROM Application a JOIN FETCH a.user JOIN FETCH a.job j JOIN FETCH j.company WHERE a.user.id = :userId AND a.status = :status")
    List<Application> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT a FROM Application a JOIN FETCH a.user JOIN FETCH a.job j JOIN FETCH j.company WHERE a.job.id = :jobId AND a.status = :status")
    List<Application> findByJobIdAndStatus(@Param("jobId") Long jobId, @Param("status") String status);

    boolean existsByUserIdAndJobId(Long userId, Long jobId);

    Optional<Application> findByUserIdAndJobId(Long userId, Long jobId);

    @Query("SELECT a FROM Application a JOIN FETCH a.user JOIN FETCH a.job j JOIN FETCH j.company WHERE a.job.company.id = :companyId")
    List<Application> findByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(a) FROM Application a WHERE a.job.id = :jobId")
    Long countByJobId(@Param("jobId") Long jobId);

    @Query("SELECT a FROM Application a JOIN FETCH a.user JOIN FETCH a.job j JOIN FETCH j.company WHERE a.user.id = :userId ORDER BY a.appliedDate DESC")
    List<Application> findRecentByUserId(@Param("userId") Long userId);
}
