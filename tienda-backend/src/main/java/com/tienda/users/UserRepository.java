package com.tienda.users;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @org.springframework.data.jpa.repository.Query(value = """
            SELECT u FROM User u JOIN FETCH u.role r
            WHERE (:q IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            AND (:role IS NULL OR r.name = :role)
            """,
            countQuery = """
            SELECT COUNT(u) FROM User u
            WHERE (:q IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            AND (:role IS NULL OR u.role.name = :role)
            """)
    org.springframework.data.domain.Page<User> search(
            @org.springframework.data.repository.query.Param("q") String q,
            @org.springframework.data.repository.query.Param("role") String role,
            org.springframework.data.domain.Pageable pageable);

    long countByRoleNameAndEnabledTrue(String roleName);
}