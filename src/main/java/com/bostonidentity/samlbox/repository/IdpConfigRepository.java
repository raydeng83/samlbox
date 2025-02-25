package com.bostonidentity.samlbox.repository;

import com.bostonidentity.samlbox.model.IdpConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdpConfigRepository extends JpaRepository<IdpConfig, Long> {
    Optional<IdpConfig> findByEntityId(String entityId);

    Optional<IdpConfig> findByRegistrationId(String registrationId);
}
