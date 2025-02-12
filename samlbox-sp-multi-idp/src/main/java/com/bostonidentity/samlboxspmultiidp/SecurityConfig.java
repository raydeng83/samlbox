package com.bostonidentity.samlboxspmultiidp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            RelyingPartyRegistrationRepository relyingPartyRepo) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/", "/upload", "/css/**").permitAll()
//                        .anyRequest().authenticated()
                                .anyRequest().permitAll()
                ).csrf(csrf -> csrf.disable())
                .saml2Login(saml2 -> saml2
                        .relyingPartyRegistrationRepository(relyingPartyRepo)
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                );
        return http.build();
    }
}
