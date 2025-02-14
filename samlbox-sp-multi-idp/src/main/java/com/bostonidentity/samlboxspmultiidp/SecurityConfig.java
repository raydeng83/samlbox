package com.bostonidentity.samlboxspmultiidp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationTokenConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final DynamicRelyingPartyRegistrationRepository repo;

    public SecurityConfig(DynamicRelyingPartyRegistrationRepository repo) {
        this.repo = repo;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/**",
                                "/upload",
                                "/delete/**",
                                "/saml/login",
                                "saml2/metadata/**",
                                "/saml-response/**",
                                "/login/**",
                                "/css/**").permitAll()
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .csrf(csrf -> csrf.disable())
                .saml2Login(saml2 -> saml2
                        .relyingPartyRegistrationRepository(relyingPartyRegistrationRepository())
                        .successHandler(samlAuthenticationSuccessHandler())
                )
                .saml2Metadata(withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );
        return http.build();
    }

    @Bean
    public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
        return new InMemoryRelyingPartyRegistrationRepository(repo.getAllRegistrations());
    }

    @Bean
    public AuthenticationSuccessHandler samlAuthenticationSuccessHandler() {
        return new SamlResponseRedirectHandler();
    }

    @Bean
    public Saml2AuthenticationTokenConverter saml2AuthenticationTokenConverter() {
        RelyingPartyRegistrationResolver resolver =
                new DefaultRelyingPartyRegistrationResolver(repo);
        return new Saml2AuthenticationTokenConverter(resolver);
    }
}
