package com.bostonidentity.samlboxspmultiidp.config;


import com.bostonidentity.samlboxspmultiidp.repository.DynamicRelyingPartyRegistrationRepository;
import com.bostonidentity.samlboxspmultiidp.repository.IdpMetadataRepository;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;

// SamlConfig.java
@Configuration
public class SamlConfig {

    @Value("${sp.entity-id}")
    private String spEntityId;

    @Value("${app.baseUrl}")
    private String baseUrl;

    private static final Logger logger = LoggerFactory.getLogger(SamlConfig.class);
    private static final Path KEY_DIR = Paths.get("saml-keys");

    // Encryption Keys
    private static final Path ENC_PRIVATE_KEY = KEY_DIR.resolve("sp-encryption.key");
    private static final Path ENC_CERTIFICATE = KEY_DIR.resolve("sp-encryption.crt");

    //Signing Keys
    private static final Path SIGN_PRIVATE_KEY = KEY_DIR.resolve("sp-signing.key");
    private static final Path SIGN_CERTIFICATE = KEY_DIR.resolve("sp-signing.crt");

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
    public Saml2AuthenticationRequestRepository<AbstractSaml2AuthenticationRequest> saml2AuthenticationRequestRepository() {
        return new HttpSessionSaml2AuthenticationRequestRepository();
    }

    @Bean
    @Qualifier("signingCredential")
    public Saml2X509Credential signingCredential() throws Exception {
        Files.createDirectories(KEY_DIR);

        KeyPair keyPair;
        X509Certificate certificate;

        if (Files.exists(SIGN_PRIVATE_KEY) && Files.exists(SIGN_CERTIFICATE)) {
            logger.info("Loading existing SAML signing keys");
            keyPair = loadSigningKeyPair();
            certificate = loadSigningCertificate();
        } else {
            logger.info("Generating new SAML signing keys");
            keyPair = generateKeyPair();
            certificate = generateCertificate(keyPair);
            saveSigningKeyPair(keyPair);
            saveSigningCertificate(certificate);
        }

        return new Saml2X509Credential(
                keyPair.getPrivate(),
                certificate,
                Saml2X509Credential.Saml2X509CredentialType.SIGNING
        );
    }

    @Bean
    @Qualifier("decryptingCredential")
    public Saml2X509Credential decryptingCredential() throws Exception {
        Files.createDirectories(KEY_DIR);

        KeyPair keyPair;
        X509Certificate certificate;

        if (Files.exists(ENC_PRIVATE_KEY) && Files.exists(ENC_CERTIFICATE)) {
            logger.info("Loading existing SAML decryption keys");
            keyPair = loadEncryptingKeyPair();
            certificate = loadEncryptingCertificate();
        } else {
            logger.info("Generating new SAML decryption keys");
            keyPair = generateKeyPair();
            certificate = generateCertificate(keyPair);
            saveEncryptingKeyPair(keyPair);
            saveEncryptingCertificate(certificate);
        }

        return new Saml2X509Credential(
                keyPair.getPrivate(),
                certificate,
                Saml2X509Credential.Saml2X509CredentialType.DECRYPTION
        );
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    private X509Certificate generateCertificate(KeyPair keyPair) throws Exception {
        X500Name issuer = new X500Name("CN=BostonIdentity");
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        X500Name subject = new X500Name("CN=BostonIdentity");
        Date notBefore = new Date();
        Date notAfter = new Date(System.currentTimeMillis() + 365 * 86400000L);

        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer, serial, notBefore, notAfter, subject, keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
                .build(keyPair.getPrivate());

        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }

    private void saveEncryptingKeyPair(KeyPair keyPair) throws IOException {
        try (PEMWriter writer = new PEMWriter(new FileWriter(ENC_PRIVATE_KEY.toFile()))) {
            writer.writeObject(new JcaPKCS8Generator(keyPair.getPrivate(), null).generate());
        }

        try (PEMWriter writer = new PEMWriter(new FileWriter(SIGN_PRIVATE_KEY.toFile()))) {
            writer.writeObject(new JcaPKCS8Generator(keyPair.getPrivate(), null).generate());
        }
    }

    private KeyPair loadEncryptingKeyPair() throws Exception {
        try (PEMParser parser = new PEMParser(new FileReader(ENC_PRIVATE_KEY.toFile()))) {
            Object pemObject = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter()
                    .setProvider(new BouncyCastleProvider()); // Explicit provider

            if (pemObject instanceof PEMKeyPair) {
                PEMKeyPair pemKeyPair = (PEMKeyPair) pemObject;
                return new KeyPair(
                        converter.getPublicKey(pemKeyPair.getPublicKeyInfo()),
                        converter.getPrivateKey(pemKeyPair.getPrivateKeyInfo())
                );
            } else if (pemObject instanceof PrivateKeyInfo) {
                PrivateKey privateKey = converter.getPrivateKey((PrivateKeyInfo) pemObject);
                X509Certificate cert = loadEncryptingCertificate();
                return new KeyPair(cert.getPublicKey(), privateKey);
            }
            throw new IOException("Unsupported key format");
        }
    }

    private void saveEncryptingCertificate(X509Certificate certificate) throws Exception {
        try (PEMWriter writer = new PEMWriter(new FileWriter(ENC_CERTIFICATE.toFile()))) {
            writer.writeObject(certificate);
        }

        try (PEMWriter writer = new PEMWriter(new FileWriter(SIGN_CERTIFICATE.toFile()))) {
            writer.writeObject(certificate);
        }
    }

    private X509Certificate loadEncryptingCertificate() throws Exception {
        try (PEMParser parser = new PEMParser(new FileReader(ENC_CERTIFICATE.toFile()))) {
            Object pemObject = parser.readObject();
            if (pemObject instanceof X509CertificateHolder) {
                return new JcaX509CertificateConverter()
                        .setProvider(new BouncyCastleProvider()) // Use provider instance directly
                        .getCertificate((X509CertificateHolder) pemObject);
            }
            throw new IOException("File does not contain a X.509 certificate");
        }
    }



    private void saveSigningKeyPair(KeyPair keyPair) throws IOException {
        try (PEMWriter writer = new PEMWriter(new FileWriter(ENC_PRIVATE_KEY.toFile()))) {
            writer.writeObject(new JcaPKCS8Generator(keyPair.getPrivate(), null).generate());
        }

        try (PEMWriter writer = new PEMWriter(new FileWriter(SIGN_PRIVATE_KEY.toFile()))) {
            writer.writeObject(new JcaPKCS8Generator(keyPair.getPrivate(), null).generate());
        }
    }

    private KeyPair loadSigningKeyPair() throws Exception {
        try (PEMParser parser = new PEMParser(new FileReader(ENC_PRIVATE_KEY.toFile()))) {
            Object pemObject = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter()
                    .setProvider(new BouncyCastleProvider()); // Explicit provider

            if (pemObject instanceof PEMKeyPair) {
                PEMKeyPair pemKeyPair = (PEMKeyPair) pemObject;
                return new KeyPair(
                        converter.getPublicKey(pemKeyPair.getPublicKeyInfo()),
                        converter.getPrivateKey(pemKeyPair.getPrivateKeyInfo())
                );
            } else if (pemObject instanceof PrivateKeyInfo) {
                PrivateKey privateKey = converter.getPrivateKey((PrivateKeyInfo) pemObject);
                X509Certificate cert = loadEncryptingCertificate();
                return new KeyPair(cert.getPublicKey(), privateKey);
            }
            throw new IOException("Unsupported key format");
        }
    }

    private void saveSigningCertificate(X509Certificate certificate) throws Exception {
        try (PEMWriter writer = new PEMWriter(new FileWriter(ENC_CERTIFICATE.toFile()))) {
            writer.writeObject(certificate);
        }

        try (PEMWriter writer = new PEMWriter(new FileWriter(SIGN_CERTIFICATE.toFile()))) {
            writer.writeObject(certificate);
        }
    }

    private X509Certificate loadSigningCertificate() throws Exception {
        try (PEMParser parser = new PEMParser(new FileReader(ENC_CERTIFICATE.toFile()))) {
            Object pemObject = parser.readObject();
            if (pemObject instanceof X509CertificateHolder) {
                return new JcaX509CertificateConverter()
                        .setProvider(new BouncyCastleProvider()) // Use provider instance directly
                        .getCertificate((X509CertificateHolder) pemObject);
            }
            throw new IOException("File does not contain a X.509 certificate");
        }
    }
}
