package com.bostonidentity.samlboxspmultiidp.config;


import com.bostonidentity.samlboxspmultiidp.repository.DynamicRelyingPartyRegistrationRepository;
import com.bostonidentity.samlboxspmultiidp.repository.IdpMetadataRepository;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationTokenConverter;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Date;

// SamlConfig.java
@Configuration
public class SamlConfig {

    @Value("${sp.entity-id}")
    private String spEntityId;

    @Autowired
    private IdpMetadataRepository idpMetadataRepository;

    @Bean
    public DynamicRelyingPartyRegistrationRepository dynamicRelyingPartyRegistrationRepository(
            Saml2X509Credential credential) {
        return new DynamicRelyingPartyRegistrationRepository(
                credential, spEntityId, idpMetadataRepository
        );
    }

    @Bean
    public Saml2X509Credential signingCredential() throws Exception {
        KeyPair keyPair = generateKeyPair();
        X509Certificate certificate = generateCertificate(keyPair);
        return new Saml2X509Credential(
                keyPair.getPrivate(),
                certificate,
                Saml2X509Credential.Saml2X509CredentialType.SIGNING
        );
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private X509Certificate generateCertificate(KeyPair keyPair) throws Exception {
        X500Name issuer = new X500Name("CN=SP");
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        X500Name subject = new X500Name("CN=SP");
        Date notBefore = new Date();
        Date notAfter = new Date(System.currentTimeMillis() + 365 * 86400000L);

        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer, serial, notBefore, notAfter, subject, keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
                .build(keyPair.getPrivate());

        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }

//    @Bean
//    public Saml2AuthenticationTokenConverter saml2AuthenticationTokenConverter(
//            DynamicRelyingPartyRegistrationRepository repo
//    ) {
//        return new Saml2AuthenticationTokenConverter(
//                new EntityIdRelyingPartyRegistrationResolver()
//        );
//    }
}
