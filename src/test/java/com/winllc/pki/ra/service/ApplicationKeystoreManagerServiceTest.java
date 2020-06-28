package com.winllc.pki.ra.service;

import com.netscape.cms.servlet.csadmin.Cert;
import com.winllc.acme.common.SubjectAltName;
import com.winllc.acme.common.SubjectAltNames;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.beans.form.AppKeyStoreEntryForm;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.keystore.ApplicationKeystore;
import com.winllc.pki.ra.keystore.KeyEntryWrapper;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class ApplicationKeystoreManagerServiceTest {

    //NOTE app.jks has 2 key entries by default

    @Value("${internal-ca.password}")
    private String caKeystorePassword;
    @Value("${internal-ca.location}")
    private String caKeystoreLocation;
    @Value("${internal-ca.alias}")
    private String caKeystoreAlias;
    @Value("${internal-ca.type}")
    private String caKeystoreType;

    @Autowired
    private ApplicationKeystoreManagerService managerService;

    @BeforeEach
    void beforeEach() throws KeyStoreException {
        managerService.deleteKeyByAlias("test1");
        managerService.deleteKeyByAlias("test1_chain0");
    }

    @Test
    void createKeyAndGetByAlias() throws Exception {
        managerService.createKey("test1");

        Optional<KeyEntryWrapper> optionalWrapper = managerService.getKeyByAlias("test1");
        assertTrue(optionalWrapper.isPresent());

        KeyEntryWrapper wrapper = optionalWrapper.get();
        assertTrue(wrapper.getCertificate() instanceof X509Certificate);
    }

    @Test
    void deleteKeyByAlias() throws Exception {
        managerService.createKey("test1");

        List<KeyEntryWrapper> all = managerService.getAll();
        assertEquals(3, all.size());

        managerService.deleteKeyByAlias("test1");

        all = managerService.getAll();
        assertEquals(2, all.size());
    }

    @Test
    void getAll() throws Exception {
        managerService.createKey("test1");

        List<KeyEntryWrapper> all = managerService.getAll();
        assertEquals(3, all.size());
    }

    @Test
    void availableAliases() throws Exception {
        List<String> aliases = managerService.availableAliases();
        assertEquals(2, aliases.size());
    }

    @Test
    void addEntryWithCsr() throws Exception {
        AppKeyStoreEntryForm form = new AppKeyStoreEntryForm();
        form.setAlias("test2");
        form.setGenerateCsr(true);

        String csr = managerService.addEntry(form);
        PKCS10CertificationRequest certificationRequest = CertUtil.convertPemToPKCS10CertificationRequest(csr);
        String subject = certificationRequest.getSubject().toString();
        assertTrue(subject.contains("CN=test2"));

        managerService.deleteKeyByAlias("test2");
    }

    @Test
    void createCsrForEntry() throws Exception {
        managerService.createKey("test1");

        AppKeyStoreEntryForm form = new AppKeyStoreEntryForm();
        form.setAlias("test1");
        form.setGenerateCsr(true);

        String csr = managerService.createCsrForEntry(form);
        PKCS10CertificationRequest certificationRequest = CertUtil.convertPemToPKCS10CertificationRequest(csr);
        assertTrue(certificationRequest.getSubject().toString().contains("CN=test1"));
    }

    @Test
    void testCreateKey() throws Exception {
        managerService.createKey("test1");

        Optional<KeyEntryWrapper> optionalKey = managerService.getKeyByAlias("test1");
        assertTrue(optionalKey.isPresent());

        KeyEntryWrapper wrapper = optionalKey.get();
        assertTrue(wrapper.getCertificate() instanceof X509Certificate);
    }

    @Test
    void generateCsrForEntry() throws Exception {
        managerService.createKey("test1");

        String csr = managerService.generateCsrForEntry("test1");
        PKCS10CertificationRequest certificationRequest = CertUtil.convertPemToPKCS10CertificationRequest(csr);
        assertTrue(certificationRequest.getSubject().toString().contains("CN=test1"));
    }

    @Test
    void testUpdateKeyEntry() throws Exception {
        AppKeyStoreEntryForm form = new AppKeyStoreEntryForm();
        form.setAlias("test1");
        form.setGenerateCsr(true);

        String csr = managerService.addEntry(form);
        PKCS10CertificationRequest certificationRequest = CertUtil.csrBase64ToPKC10Object(csr);

        Optional<KeyEntryWrapper> optionalKey = managerService.getKeyByAlias("test1");
        KeyEntryWrapper wrapper = optionalKey.get();

        SubjectAltNames sans = new SubjectAltNames();
        KeyStore caKeyStore = loadKeystore(caKeystoreLocation, caKeystorePassword);

        X509Certificate certificate = CertUtil.signCSR(certificationRequest, sans, 30, caKeyStore,
                caKeystoreAlias, caKeystorePassword.toCharArray());

        wrapper.setCertificate(certificate);
        managerService.updateKeyEntry(wrapper);

        optionalKey = managerService.getKeyByAlias("test1");
        wrapper = optionalKey.get();

        try{
            wrapper.getCertificate().verify(caKeyStore.getCertificate(caKeystoreAlias).getPublicKey());
        } catch (Exception e){
            fail("Could not verify signed cert");
        }
    }

    @Test
    void testDeleteKeyByAlias() throws Exception {
        managerService.createKey("test1");

        List<KeyEntryWrapper> all = managerService.getAll();
        assertEquals(3, all.size());

        managerService.deleteKeyByAlias("test1");

        all = managerService.getAll();
        assertEquals(2, all.size());
    }

    private KeyStore loadKeystore(String location, String password) throws Exception {
        System.out.println("Loading keystore: "+location);
        FileInputStream fis = new FileInputStream(location);

        //ClassLoader classLoader = getClass().getClassLoader();
        //InputStream inputStream = classLoader.getResourceAsStream(location);

        KeyStore ks = KeyStore.getInstance(caKeystoreType);

        ks.load(fis, password.toCharArray());
        IOUtils.closeQuietly(fis);
        return ks;
    }

}