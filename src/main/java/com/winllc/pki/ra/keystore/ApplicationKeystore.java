package com.winllc.pki.ra.keystore;

import com.winllc.pki.ra.service.AccountRestrictionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@Component
public class ApplicationKeystore {
    //todo properly save keystore as file when changes made

    private static final Logger log = LogManager.getLogger(ApplicationKeystore.class);


    @Value("${app-keystore.location}")
    private String keystoreLocation;
    @Value("${app-keystore.password}")
    private String keystorePassword;
    @Value("${app-keystore.type}")
    private String keystoreType;

    private KeyStore ks;

    @PostConstruct
    private void load() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        //Resource resource = resourceLoader.getResource(keystoreLocation);
        //InputStream input = resource.getInputStream();

        InputStream is = new FileInputStream(keystoreLocation);

        ks = KeyStore.getInstance(keystoreType);
        ks.load(is, keystorePassword.toCharArray());
    }

    private void persist(){
        try(FileOutputStream fos = new FileOutputStream(keystoreLocation);){
            ks.store(fos, keystorePassword.toCharArray());
        } catch (Exception e) {
            log.error("Could not save keystore", e);
        }
    }

    public void addKey(KeyEntryWrapper keyEntryWrapper) throws KeyStoreException {
        ks.setKeyEntry(keyEntryWrapper.getAlias(), keyEntryWrapper.getKey(), keystorePassword.toCharArray(),
                new Certificate[]{keyEntryWrapper.getCertificate()});
        persist();
    }

    public void addCertificate(KeyEntryWrapper keyEntryWrapper) throws Exception {
        //ks.setCertificateEntry(keyEntryWrapper.getAlias(), keyEntryWrapper.getCertificate());

        KeyEntryWrapper currentKeyEntry = getKeyEntry(keyEntryWrapper.getAlias());
        ks.setKeyEntry(keyEntryWrapper.getAlias(), currentKeyEntry.getKey(),
                keystorePassword.toCharArray(), new Certificate[]{keyEntryWrapper.getCertificate()});
        persist();
    }

    public void addChain(KeyEntryWrapper keyEntryWrapper){
        for(int i = 0; i < keyEntryWrapper.getChain().length; i++){
            Certificate chainEntry = keyEntryWrapper.getChain()[i];
            try {
                ks.setCertificateEntry(keyEntryWrapper.getAlias()+"_chain"+i, chainEntry);
            } catch (KeyStoreException e) {
                log.error("Could not add chain", e);
            }
        }
        persist();
    }

    public void addEntry(KeyEntryWrapper keyEntryWrapper) throws KeyStoreException {
        if(keyEntryWrapper.getKey() != null){
            addKey(keyEntryWrapper);
        }

        if(keyEntryWrapper.getChain() != null){
            addChain(keyEntryWrapper);
        }
    }

    public void deleteKey(String alias) throws KeyStoreException {
        ks.deleteEntry(alias);
        persist();
    }

    public Key getKey(String alias) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        return ks.getKey(alias, keystorePassword.toCharArray());
    }

    public Certificate getCertificate(String alias) throws KeyStoreException {
        return ks.getCertificate(alias);
    }

    public KeyEntryWrapper getKeyEntry(String alias) throws Exception {
        KeyEntryWrapper keyEntryWrapper = new KeyEntryWrapper();
        try {
            Key key = getKey(alias);
            keyEntryWrapper.setKey(key);
        } catch (Exception e) {
            log.error("Could not get key entry: "+alias);
        }

        try {
            Certificate cert = getCertificate(alias);
            keyEntryWrapper.setCertificate(cert);
        } catch (Exception e) {
            log.error("Could not get cert entry: "+alias);
        }

        try {
            Certificate[] chain = ks.getCertificateChain(alias);
            keyEntryWrapper.setChain(chain);
        } catch (KeyStoreException e) {
            log.error("Could not get chain: "+alias);
        }

        //Don't return if nothing found
        if(keyEntryWrapper.getKey() == null && keyEntryWrapper.getCertificate() == null &&
            keyEntryWrapper.getChain() == null) throw new Exception("Could not find any entries for: "+alias);

        keyEntryWrapper.setAlias(alias);
        return keyEntryWrapper;
    }

    public List<String> getAllAliases() throws KeyStoreException {
        List<String> list = new ArrayList<>();
        Enumeration<String> aliases = ks.aliases();
        while(aliases.hasMoreElements()){
            String alias = aliases.nextElement();
            list.add(alias);
        }
        return list;
    }

    public KeyStore getKeyStore(){
        return ks;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }
}
