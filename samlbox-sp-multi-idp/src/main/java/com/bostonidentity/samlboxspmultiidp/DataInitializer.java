package com.bostonidentity.samlboxspmultiidp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final IdpMetadataRepository idpMetadataRepository;

    @Autowired
    public DataInitializer(IdpMetadataRepository idpMetadataRepository) {
        this.idpMetadataRepository = idpMetadataRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Check if the database is empty and initialize it if necessary
        if (idpMetadataRepository.count() == 0) {
            // Add default IDP metadata if needed
        }
    }


}
