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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.authentication.AbstractSaml2AuthenticationRequest;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.HttpSessionSaml2AuthenticationRequestRepository;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationRequestRepository;
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

    @Value("${app.baseUrl}")
    private String baseUrl;

    @Autowired
    private IdpMetadataRepository idpMetadataRepository;

    @Bean
    public DynamicRelyingPartyRegistrationRepository dynamicRelyingPartyRegistrationRepository(
            @Qualifier("signingCredential")  Saml2X509Credential signingCredential,
            @Qualifier("decryptingCredential") Saml2X509Credential encryptingCredential) {
        return new DynamicRelyingPartyRegistrationRepository(
                signingCredential, encryptingCredential, spEntityId, idpMetadataRepository, baseUrl
        );
    }

    @Bean
    @Qualifier("signingCredential")
    public Saml2X509Credential signingCredential() throws Exception {
        KeyPair keyPair = generateKeyPair();
        X509Certificate certificate = generateCertificate(keyPair);
        return new Saml2X509Credential(
                keyPair.getPrivate(),
                certificate,
                Saml2X509Credential.Saml2X509CredentialType.SIGNING
        );
    }

    @Bean
    @Qualifier("decryptingCredential")
    public Saml2X509Credential decryptingCredential() throws Exception {
        KeyPair keyPair = generateKeyPair(); // Ensure this generates an RSA key pair
        X509Certificate certificate = generateCertificate(keyPair);

        return new Saml2X509Credential(
                keyPair.getPrivate(), // SP's private key for decryption
                certificate,
                Saml2X509Credential.Saml2X509CredentialType.DECRYPTION // Correct type
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

    @Bean
    public Saml2AuthenticationRequestRepository<AbstractSaml2AuthenticationRequest> saml2AuthenticationRequestRepository() {
        return new HttpSessionSaml2AuthenticationRequestRepository();
    }

}
