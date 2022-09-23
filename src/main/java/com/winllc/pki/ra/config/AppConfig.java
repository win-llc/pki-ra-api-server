package com.winllc.pki.ra.config;

import com.winllc.acme.common.ca.LoadedCertAuthorityStore;
import com.winllc.acme.common.domain.CertAuthorityConnectionInfo;
import com.winllc.acme.common.keystore.ApplicationKeystore;
import com.winllc.acme.common.repository.CertAuthorityConnectionInfoRepository;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;


import java.util.List;


@SpringBootApplication(
        exclude = {
                MongoAutoConfiguration.class,
                MongoDataAutoConfiguration.class
        }
)
//@EnableSwagger2
//@EnableOpenApi
//@EnableWebMvc
@ComponentScan({"com.winllc.pki.ra", "com.winllc.acme.common"})
@EntityScan({"com.winllc.acme.common.domain"})
@EnableJpaRepositories(basePackages = {"com.winllc.acme.common.repository", "com.winllc.acme.common.repository"})
@EnableTransactionManagement
@EnableJpaAuditing
@EnableScheduling
@EnableConfigurationProperties
@ConfigurationPropertiesScan({"com.winllc.pki.ra.config", "com.winllc.acme.common.config"})
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


    @Bean
    public LoadedCertAuthorityStore loadedCertAuthorityStore(ApplicationContext context,
                                                             ApplicationKeystore applicationKeystore,
                                                             CertAuthorityConnectionInfoRepository connectionInfoRepository){
        LoadedCertAuthorityStore store = new LoadedCertAuthorityStore(applicationKeystore, context);

        List<CertAuthorityConnectionInfo> all = connectionInfoRepository.findAll();
        for(CertAuthorityConnectionInfo info : all){
            store.loadCertAuthority(info);
        }

        return store;
    }

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

    /*
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("winllc-ra-api")
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.winllc.pki.ra"))
                //.paths(PathSelectors.any())
                .build();
    }

     */

}
