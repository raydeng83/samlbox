package com.bostonidentity.samlbox.config;

import com.bostonidentity.samlbox.repository.DynamicRelyingPartyRegistrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationTokenConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private EntityIdLookupAuthenticationConverter authenticationConverter;

    @Autowired
    private Saml2X509Credential signingCredential;

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
                                "/logout/**"
                        )
                        .permitAll()
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .csrf(AbstractHttpConfigurer::disable)
//                .saml2Login(saml2 -> saml2.relyingPartyRegistrationRepository(relyingPartyRegistrationRepository(repo)))
                .saml2Login(saml2 -> saml2
                                .relyingPartyRegistrationRepository(relyingPartyRegistrationRepository(repo))
//                        .loginProcessingUrl("/login/saml2/sso/" + spEntityId)
                                .loginProcessingUrl("/login/saml2/sso")
                                .authenticationConverter(authenticationConverter)
                                .successHandler(samlAuthenticationSuccessHandler())
                                .failureHandler(new Saml2AuthenticationFailureHandler())
                )
                .exceptionHandling(exceptions -> exceptions
                                .authenticationEntryPoint(samlAuthenticationEntryPoint())
//                        .accessDeniedHandler(accessDeniedHandler())
                )
                .saml2Metadata(withDefaults())
//                .saml2Logout(withDefaults());
                .saml2Logout(logout -> logout
                        .logoutRequest((request) -> request.logoutUrl("/logout/saml2/slo"))
                        .logoutResponse((response) -> response.logoutUrl("/logout/saml2/slo"))


                )
                .logout(logout -> logout
                        .logoutSuccessHandler(saml2LogoutSuccessHandler())
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                )
//                .logout(logout -> logout
//                        .addLogoutHandler(new Saml2LogoutHandler(repo, signingCredential))
//                        .logoutSuccessUrl("/logged-out"))
        ;
//                .sessionManagement(session -> session
//                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//                )
        ;
        return http.build();
    }

    @Bean
    public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository(DynamicRelyingPartyRegistrationRepository repo) {
        return repo;
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

    // SAML-specific authentication exception handler
    private AuthenticationEntryPoint samlAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            if (authException instanceof Saml2AuthenticationException) {
                // Forward SAML errors to the error page
                request.setAttribute("errorMessage", authException.getMessage());
                request.setAttribute("errorDetails", authException);
                request.getRequestDispatcher("/saml-error").forward(request, response);
            } else {
                // Default behavior for other auth errors
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized");
            }
        };
    }

    @Bean
    public Saml2LogoutSuccessHandler saml2LogoutSuccessHandler() {
        return new Saml2LogoutSuccessHandler();
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.sendError(HttpStatus.FORBIDDEN.value(), "Access Denied");
        };
    }
}
