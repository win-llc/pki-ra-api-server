package com.winllc.pki.ra.service;

import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.keystore.ApplicationKeystore;
import com.winllc.pki.ra.keystore.KeyEntryWrapper;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import sun.security.x509.*;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/applicationKeystore")
public class ApplicationKeystoreManagerService {
    //todo allow key creation, csr generation

    private static final Logger log = LogManager.getLogger(ApplicationKeystoreManagerService.class);

    @Autowired
    private ApplicationKeystore applicationKeystore;

    /*
    REST METHODS
     */
    @GetMapping("/availableAliases")
    @ResponseStatus(HttpStatus.OK)
    public List<String> availableAliases() throws KeyStoreException {
        return getAll().stream()
                .map(e -> e.getAlias())
                .collect(Collectors.toList());
    }

    @PostMapping("/addEntry")
    @ResponseStatus(HttpStatus.CREATED)
    public String addEntry(@RequestParam String alias, @RequestParam boolean buildCsr)
            throws Exception {
        createKey(alias);
        log.info("Key and self-signed cert added to keystore: "+alias);

        if(buildCsr){
            String csr = generateCsrForEntry(alias);
            return csr;
        }else{
            return "";
        }
    }

    @PostMapping("/addSignedCertAndChainToEntry")
    @ResponseStatus(HttpStatus.OK)
    public void addSignedCertAndChainToEntry(@RequestParam String alias, @RequestParam String certificate,
                                               @RequestParam String chain)
            throws Exception {
        KeyEntryWrapper wrapper = new KeyEntryWrapper();
        wrapper.setAlias(alias);

        X509Certificate x509Cert = CertUtil.base64ToCert(certificate);
        wrapper.setCertificate(x509Cert);

        if(StringUtils.isNotBlank(chain)) {
            Certificate[] certificates = CertUtil.trustChainStringToCertArray(chain);
            wrapper.setChain(certificates);
        }

        updateKeyEntry(wrapper);
    }

    @PostMapping("/createCsrForEntry")
    @ResponseStatus(HttpStatus.OK)
    public String createCsrForEntry(@RequestParam String alias) throws Exception {
        return generateCsrForEntry(alias);
    }

    /*
    END REST METHODS
     */


    //todo generate key and save it to keystore
    public void createKey(String alias) throws Exception {
        Optional<KeyEntryWrapper> wrapperOptional = getKeyByAlias(alias);
        if(!wrapperOptional.isPresent()) {
            KeyPair keyPair = CertUtil.generateRSAKeyPair();
            X509Certificate certificate = CertUtil.generateSelfSignedCertificate(keyPair,
                    "SHA256withRSA", "cn=" + alias, 365);
            KeyEntryWrapper wrapper = new KeyEntryWrapper();
            wrapper.setAlias(alias);
            wrapper.setCertificate(certificate);
            wrapper.setKey(keyPair.getPrivate());

            applicationKeystore.addEntry(wrapper);
        }else{
            throw new Exception("Key with alias already exists: "+alias);
        }
    }

    public String generateCsrForEntry(String alias) throws Exception {
        Optional<KeyEntryWrapper> optionalWrapper = getKeyByAlias(alias);
        if(optionalWrapper.isPresent()){
            KeyEntryWrapper keyEntryWrapper = optionalWrapper.get();
            PKCS10CertificationRequest csr = CertUtil.generatePKCS10("cn="+alias, keyEntryWrapper.getCertificate().getPublicKey(),
                    (PrivateKey) keyEntryWrapper.getKey());
            return CertUtil.toPEM(csr);
        }else{
            throw new Exception("Could not find entry: "+alias);
        }
    }

    //allow for signed certificate to be added to entry, update trust chains
    public void updateKeyEntry(KeyEntryWrapper wrapper) throws Exception {
        if(wrapper.getCertificate() == null && wrapper.getChain() == null)
            throw new IllegalArgumentException("No cert or chain to update");
        Optional<KeyEntryWrapper> wrapperOptional = getKeyByAlias(wrapper.getAlias());
        if(wrapperOptional.isPresent()){
            if(wrapper.getChain() != null) {
                applicationKeystore.addChain(wrapper);
            }
            if(wrapper.getCertificate() != null){

                applicationKeystore.addCertificate(wrapper);
            }
        }
    }

    public void deleteKeyByAlias(String alias) throws KeyStoreException {
        applicationKeystore.deleteKey(alias);
    }

    public Optional<KeyEntryWrapper> getKeyByAlias(String alias){
        try {
            KeyEntryWrapper keyEntry = applicationKeystore.getKeyEntry(alias);
            return Optional.of(keyEntry);
        } catch (Exception e) {
            log.info("Could not find a key with alias: "+alias);
        }
        return Optional.empty();
    }

    public List<KeyEntryWrapper> getAll() throws KeyStoreException {
        List<KeyEntryWrapper> entries = new ArrayList<>();
        List<String> aliases = applicationKeystore.getAllAliases();

        for(String alias : aliases){
            Optional<KeyEntryWrapper> optionalKey = getKeyByAlias(alias);
            optionalKey.ifPresent(w -> entries.add(w));
        }
        return entries;
    }

}
