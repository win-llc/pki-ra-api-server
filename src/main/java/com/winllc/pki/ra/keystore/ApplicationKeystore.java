package com.winllc.pki.ra.keystore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

@Component
public class ApplicationKeystore {
    //todo

    @Value("${app-keystore.location}")
    private String keystoreLocation;
    @Value("${app-keystore.password}")
    private String keystorePassword;
    @Value("${app-keystore.type}")
    private String keystoreType;

    @Autowired
    @Qualifier("webApplicationContext")
    private ResourceLoader resourceLoader;


    private KeyStore ks;

    @PostConstruct
    private void load() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        //Resource resource = resourceLoader.getResource(keystoreLocation);
        //InputStream input = resource.getInputStream();

        InputStream is = getClass().getResourceAsStream(keystoreLocation);

        ks = KeyStore.getInstance(keystoreType);
        ks.load(is, keystorePassword.toCharArray());
    }

    public KeyStore getKeyStore(){
        return ks;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }
}
