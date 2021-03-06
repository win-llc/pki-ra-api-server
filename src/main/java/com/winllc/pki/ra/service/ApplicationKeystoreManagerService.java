package com.winllc.pki.ra.service;

import com.winllc.acme.common.keystore.ApplicationKeystore;
import com.winllc.acme.common.keystore.KeyEntryWrapper;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.beans.form.AppKeyStoreEntryForm;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.service.validators.AppKeyStoreEntryValidator;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/applicationKeystore")
public class ApplicationKeystoreManagerService {
    //todo allow key creation, csr generation

    private static final Logger log = LogManager.getLogger(ApplicationKeystoreManagerService.class);

    private final ApplicationKeystore applicationKeystore;

    private final AppKeyStoreEntryValidator appKeyStoreEntryValidator;

    public ApplicationKeystoreManagerService(ApplicationKeystore applicationKeystore,
                                             AppKeyStoreEntryValidator appKeyStoreEntryValidator) {
        this.applicationKeystore = applicationKeystore;
        this.appKeyStoreEntryValidator = appKeyStoreEntryValidator;
    }

    @InitBinder("appKeystoreEntryForm")
    public void initAppKeystoreEntryBinder(WebDataBinder binder) {
        binder.setValidator(appKeyStoreEntryValidator);
    }

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

    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public List<KeyEntryWrapper> allEntries() throws KeyStoreException {
        return new ArrayList<>(getAll());
    }

    @GetMapping("/getEntryByAlias/{alias}")
    @ResponseStatus(HttpStatus.OK)
    public AppKeyStoreEntryForm getEntry(@PathVariable String alias) throws RAObjectNotFoundException {
        Optional<KeyEntryWrapper> optionalEntry = getKeyByAlias(alias);
        if(optionalEntry.isPresent()){
            KeyEntryWrapper keyEntry = optionalEntry.get();
            AppKeyStoreEntryForm form = new AppKeyStoreEntryForm(keyEntry);

            //todo add back
            form.setCurrentCertDetails((keyEntry.getCertificate()).toString());
            //X509CertInfo certImpl = new X509CertInfo(((X509Certificate) keyEntry.getCertificate()).getTBSCertificate());
            //form.setCurrentCertDetails(certImpl.toString());

            return form;
        }else{
            throw new RAObjectNotFoundException(KeyEntryWrapper.class, alias);
        }
    }

    @PostMapping("/addEntry")
    @ResponseStatus(HttpStatus.CREATED)
    public String addEntry(@Valid @RequestBody AppKeyStoreEntryForm form)
            throws Exception {
        String alias = form.getAlias();
        createKey(alias);
        log.info("Key and self-signed cert added to keystore: "+alias);

        if(form.isGenerateCsr()){
            return generateCsrForEntry(alias);
        }else{
            return "";
        }
    }

    @PostMapping("/addSignedCertAndChainToEntry")
    @ResponseStatus(HttpStatus.OK)
    public void addSignedCertAndChainToEntry(@Valid @RequestBody AppKeyStoreEntryForm form)
            throws Exception {
        KeyEntryWrapper wrapper = new KeyEntryWrapper();
        wrapper.setAlias(form.getAlias());

        X509Certificate x509Cert = CertUtil.base64ToCert(form.getUploadCertificate());
        wrapper.setCertificate(x509Cert);

        if(StringUtils.isNotBlank(form.getUploadChain())) {
            Certificate[] certificates = CertUtil.trustChainStringToCertArray(form.getUploadChain());
            wrapper.setChain(certificates);
        }

        updateKeyEntry(wrapper);
    }

    @PostMapping("/createCsrForEntry")
    @ResponseStatus(HttpStatus.OK)
    public String createCsrForEntry(@Valid @RequestBody AppKeyStoreEntryForm form) throws Exception {
        return generateCsrForEntry(form.getAlias());
    }

    @DeleteMapping("/deleteByAlias/{alias}")
    public void deleteEntry(@PathVariable String alias) throws KeyStoreException {
        deleteKeyByAlias(alias);
    }

    /*
    END REST METHODS
     */


    public void createKey(String alias) throws Exception {
        Optional<KeyEntryWrapper> wrapperOptional = getKeyByAlias(alias);
        if(wrapperOptional.isEmpty()) {
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
