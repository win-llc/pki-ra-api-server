package com.winllc.pki.ra.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@SpringBootApplication(
        exclude = {
                MongoAutoConfiguration.class,
                MongoDataAutoConfiguration.class
        }
)
@ComponentScan("com.winllc.pki.ra")
@EntityScan({"com.winllc.pki.ra.domain", "com.winllc.acme.common.domain"})
@EnableJpaRepositories(basePackages = "com.winllc.pki.ra.repository")
@EnableTransactionManagement
@EnableJpaAuditing
public class AppConfig {

    @Autowired
    private KeycloakProperties keycloakConfiguration;

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

    /*
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins("http://localhost:3000");
            }
        };
    }
    */

    @Bean(destroyMethod = "close")
    @DependsOn(value = "keycloakProperties")
    public Keycloak keycloak() {

        KeycloakBuilder keycloak = KeycloakBuilder.builder() //
                .serverUrl(keycloakConfiguration.getServerBaseUrl()) //
                .realm(keycloakConfiguration.getRealm()) //
                .grantType(OAuth2Constants.PASSWORD) //
                .clientId(keycloakConfiguration.getClientId()) //
                .clientSecret(keycloakConfiguration.getClientSecret()) //
                .username(keycloakConfiguration.getClientUsername()) //
                .password(keycloakConfiguration.getClientPassword());
        return keycloak.build();
    }

    @Bean("taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor(){

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.initialize();

        return executor;
    }

}
