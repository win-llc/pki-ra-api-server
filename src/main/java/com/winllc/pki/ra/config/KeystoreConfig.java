package com.winllc.pki.ra.config;

import com.winllc.acme.common.config.ApplicationKeystoreProperties;
import com.winllc.acme.common.keystore.ApplicationKeystore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeystoreConfig {

    @Bean
    public ApplicationKeystore applicationKeystore(ApplicationKeystoreProperties applicationKeystoreProperties){
        ApplicationKeystore applicationKeystore = new ApplicationKeystore(applicationKeystoreProperties);
        return applicationKeystore;
    }
}
