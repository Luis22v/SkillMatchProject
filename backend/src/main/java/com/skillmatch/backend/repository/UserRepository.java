package com.skillmatch.backend.repository;

import com.skillmatch.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    @Query("SELECT u FROM User u " +
           "WHERE u.id <> :userId " +
           "AND u.location IS NOT NULL " +
           "AND LOWER(u.location) = LOWER(:location) " +
           "AND u.id NOT IN (" +
           "  SELECT c.connectedUser.id FROM Connection c WHERE c.user.id = :userId" +
           ") " +
           "AND u.id NOT IN (" +
           "  SELECT c.user.id FROM Connection c WHERE c.connectedUser.id = :userId" +
           ")")
    Page<User> findSuggestions(@Param("userId") Long userId,
                               @Param("location") String location,
                               Pageable pageable);
}
