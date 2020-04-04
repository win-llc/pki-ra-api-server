package com.winllc.pki.ra.config;

import com.winllc.pki.ra.util.CustomJacksonProvider;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.Ordered;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.Collections;

@SpringBootApplication
@ComponentScan("com.winllc.pki.ra")
@EntityScan("com.winllc.pki.ra.domain")
@EnableJpaRepositories(basePackages = "com.winllc.pki.ra.repository")
@EnableTransactionManagement
public class AppConfig {

    @Value("${keycloak.admin-interface.server-base-url}")
    private String serverBaseUrl;
    //private String serverUrlAuth = serverBaseUrl+"/auth";
    @Value("${keycloak.admin-interface.realm}")
    private String realm;
    @Value("${keycloak.admin-interface.client-id}")
    private String clientId;
    @Value("${keycloak.admin-interface.client-secret}")
    private String clientSecret;
    @Value("${keycloak.admin-interface.custom-client-scope}")
    private String customClientScope;
    @Value("${keycloak.admin-interface.client-username}")
    private String username;
    @Value("${keycloak.admin-interface.client-password}")
    private String password;

    static {
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
                new javax.net.ssl.HostnameVerifier(){

                    public boolean verify(String hostname,
                                          javax.net.ssl.SSLSession sslSession) {
                        return true;
                    }
                });
    }

    public static void main(String[] args){
        SpringApplication.run(AppConfig.class, args);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins("http://localhost:3000");
            }
        };
    }


    @Bean(destroyMethod = "close")
    public Keycloak keycloak(){

        Keycloak keycloak = KeycloakBuilder.builder()
                .realm(realm)
                .clientId(clientId)
                .username(username)
                .password(password)
                .serverUrl(serverBaseUrl)
                .clientSecret(clientSecret)
                .resteasyClient(new ResteasyClientBuilder().connectionPoolSize(10).register(new CustomJacksonProvider()).build())
                .build();
        return keycloak;
    }

}
