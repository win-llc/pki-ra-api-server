package com.winllc.pki.ra.config;

import com.winllc.pki.ra.util.CustomJacksonProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

@SpringBootApplication(
        exclude = {
                MongoAutoConfiguration.class,
                MongoDataAutoConfiguration.class
        }
)
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
        //System.setProperty("javax.net.ssl.trustStore", "C:\\Users\\jrmints\\IdeaProjects\\PKI Registration Authority\\src\\main\\resources\\trust.jks");
        //System.setProperty("javax.net.ssl.trustStorePassword", "");

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


    @Bean
    public Keycloak keycloak() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

        /*
        SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                NoopHostnameVerifier.INSTANCE);

         */

        ResteasyClient client = new ResteasyClientBuilder()
                .connectionPoolSize(10)
                .hostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .register(new CustomJacksonProvider())
                .build();

        KeycloakBuilder keycloak = KeycloakBuilder.builder()
                .realm(realm)
                .clientId(clientId)
                .username(username)
                .password(password)
                .serverUrl(serverBaseUrl)
                .clientSecret(clientSecret)
                .resteasyClient(client);
        return keycloak.build();
    }


    @PostConstruct
    private void configureSSL() {
        //set to TLSv1.1 or TLSv1.2
        //System.setProperty("https.protocols", "TLSv1.2");

        //load the 'javax.net.ssl.trustStore' and
        //'javax.net.ssl.trustStorePassword' from application.properties
        //System.setProperty("javax.net.ssl.trustStore", env.getProperty("server.ssl.trust-store"));
        //System.setProperty("javax.net.ssl.trustStorePassword",env.getProperty("server.ssl.trust-store-password"));
    }

}
