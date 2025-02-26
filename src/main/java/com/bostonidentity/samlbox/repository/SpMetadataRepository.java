package com.bostonidentity.samlbox.repository;

import com.bostonidentity.samlbox.model.SpMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpMetadataRepository extends JpaRepository<SpMetadata, Long> {
    Optional<SpMetadata> findByEntityId(String entityId);

    Optional<SpMetadata> findByRegistrationId(String registrationId);
}
