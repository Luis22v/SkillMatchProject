package com.skillmatch.backend.repository;

import com.skillmatch.backend.model.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends MongoRepository<Company, String> {

    Optional<Company> findByEmail(String email);

    Optional<Company> findByUserId(String userId);

    List<Company> findByActiveTrue();

    Page<Company> findByActiveTrue(Pageable pageable);

    List<Company> findByIndustry(String industry);

    List<Company> findByLocation(String location);

    List<Company> findByIsVerifiedTrue();
}
