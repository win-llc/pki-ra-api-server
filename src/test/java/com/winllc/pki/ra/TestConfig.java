package com.winllc.pki.ra;

import com.winllc.acme.common.ca.LoadedCertAuthorityStore;
import com.winllc.acme.common.domain.CertAuthorityConnectionInfo;
import com.winllc.acme.common.keystore.ApplicationKeystore;
import com.winllc.acme.common.repository.CertAuthorityConnectionInfoRepository;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.config.WebSecurityConfiguration;
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
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.List;

@SpringBootApplication(
        exclude = {
                MongoAutoConfiguration.class,
                MongoDataAutoConfiguration.class
        }
)
@ComponentScan(value = {"com.winllc.pki.ra", "com.winllc.acme.common"},
        excludeFilters  = {@ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = {AppConfig.class,
                WebSecurityConfiguration.class})})
@EntityScan({"com.winllc.acme.common.domain"})
@EnableJpaRepositories(basePackages = {"com.winllc.acme.common.repository"})
@EnableTransactionManagement
@EnableJpaAuditing
@EnableScheduling
@EnableConfigurationProperties
@ConfigurationPropertiesScan({"com.winllc.pki.ra.config", "com.winllc.acme.common.config"})
public class TestConfig {

    public static void main(String[] args){
        SpringApplication.run(TestConfig.class, args);
    }


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
}
