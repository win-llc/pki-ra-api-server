package com.winllc.pki.ra.config;


import com.winllc.pki.ra.security.AudienceValidator;
import com.winllc.pki.ra.security.RAUserDetailsService;
import com.winllc.pki.ra.security.RAUserJwtAuthenticationConverter;
import com.winllc.pki.ra.security.RAUserRolesJwtAuthenticationConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * OAuth config based on:
 * https://github.com/andifalk/oidc-workshop-spring-io-2019
 * https://github.com/bcarun/spring-oauth2-keycloak-connector
 *
 */

@Configuration
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Value("${authentication.jwt-required-audience}")
    private String jwtRequiredAudience;

    private final OAuth2ResourceServerProperties oAuth2ResourceServerProperties;

    private final SecurityProperties securityProperties;

    private final RAUserDetailsService raUserDetailsService;

    public WebSecurityConfiguration(
            OAuth2ResourceServerProperties oAuth2ResourceServerProperties,
            RAUserDetailsService raUserDetailsService, SecurityProperties securityProperties) {
        this.oAuth2ResourceServerProperties = oAuth2ResourceServerProperties;
        this.raUserDetailsService = raUserDetailsService;
        this.securityProperties = securityProperties;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                    .cors().configurationSource(corsConfigurationSource())
                .and()
                    .csrf()
                    .disable()
                    .authorizeRequests()
                    .antMatchers("/**")
                    .permitAll()
                    .antMatchers("/api/**")
                    .fullyAuthenticated()
                .and()
                    .oauth2ResourceServer()
                    .jwt()
                    .jwtAuthenticationConverter(raUserJwtAuthenticationConverter());
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedMethods("HEAD", "GET", "PUT", "POST", "DELETE", "PATCH");
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", securityProperties.getCorsConfiguration());
        return source;
    }

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder =
                (NimbusJwtDecoder)
                        JwtDecoders.fromOidcIssuerLocation(
                                oAuth2ResourceServerProperties.getJwt().getIssuerUri());

        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(jwtRequiredAudience);
        OAuth2TokenValidator<Jwt> withIssuer =
                JwtValidators.createDefaultWithIssuer(
                        oAuth2ResourceServerProperties.getJwt().getIssuerUri());
        OAuth2TokenValidator<Jwt> withAudience =
                new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

        jwtDecoder.setJwtValidator(withAudience);

        return jwtDecoder;
    }

    @Bean
    RAUserJwtAuthenticationConverter raUserJwtAuthenticationConverter() {
        return new RAUserJwtAuthenticationConverter(raUserDetailsService, jwtRequiredAudience);
    }

    @Bean
    RAUserRolesJwtAuthenticationConverter raUserRolesJwtAuthenticationConverter() {
        return new RAUserRolesJwtAuthenticationConverter(raUserDetailsService);
    }

    @Override
    protected UserDetailsService userDetailsService() {
        return raUserDetailsService;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(raUserDetailsService);
    }
}