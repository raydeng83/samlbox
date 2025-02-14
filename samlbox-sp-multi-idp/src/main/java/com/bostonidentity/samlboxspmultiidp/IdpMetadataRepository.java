package com.bostonidentity.samlboxspmultiidp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdpMetadataRepository extends JpaRepository<IdpMetadata, Long> {
    Optional<IdpMetadata> findByEntityId(String entityId);
    Optional<IdpMetadata> findByRegistrationId(String registrationId);
}
