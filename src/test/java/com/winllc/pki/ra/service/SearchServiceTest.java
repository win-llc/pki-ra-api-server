package com.winllc.pki.ra.service;

import com.winllc.acme.common.cache.CachedCertificateService;
import com.winllc.acme.common.client.ca.CachedCertificate;
import com.winllc.pki.ra.BaseTest;
import com.winllc.pki.ra.mock.SelfSignedCertGenerator;
import com.winllc.ra.integration.ca.CertificateDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.cert.X509Certificate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchServiceTest extends BaseTest {

    @Autowired
    private CachedCertificateService cachedCertificateService;
    @Autowired
    private SearchService searchService;

    @Test
    void searchCertificates() throws Exception {
        List<X509Certificate> certificates = SelfSignedCertGenerator.generateCertificate("cn=test.com");
        cachedCertificateService.persist(certificates.get(0), "VALID", "ca1");
        Thread.sleep(5 * 1000);

        List<CachedCertificate> found = searchService.searchCertificates("test");
        assertEquals(1, found.size());
    }

    @Test
    void advancedSearchCertificates() {
    }

    @Test
    void getCertDetails() {
    }
}