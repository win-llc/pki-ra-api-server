package com.winllc.pki.ra.keystore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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

    private KeyStore ks;

    @PostConstruct
    private void load() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        ks = KeyStore.getInstance(keystoreType);
        FileInputStream fis = new FileInputStream(keystoreLocation);
        ks.load(fis, keystorePassword.toCharArray());
    }

    public KeyStore getKeyStore(){
        return ks;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }
}
