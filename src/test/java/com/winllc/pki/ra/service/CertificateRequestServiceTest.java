package com.winllc.pki.ra.service;

import com.winllc.acme.common.SubjectAltName;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.beans.form.CertificateRequestDecisionForm;
import com.winllc.pki.ra.beans.form.CertificateRequestForm;
import com.winllc.pki.ra.beans.info.CertificateRequestInfo;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.CertificateRequest;
import com.winllc.pki.ra.exception.InvalidFormException;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.mock.MockCertAuthority;
import com.winllc.pki.ra.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class CertificateRequestServiceTest {

    private final String testCsr =
                    "-----BEGIN CERTIFICATE REQUEST-----\n" +
                    "MIIDDDCCAfQCAQAwgZkxCzAJBgNVBAYTAlVTMREwDwYDVQQIDAhWaXJnaW5pYTET\n" +
                    "MBEGA1UEBwwKQWxleGFuZHJpYTEQMA4GA1UECgwHV0lOIExMQzEMMAoGA1UECwwD\n" +
                    "RGV2MSgwJgYJKoZIhvcNAQkBFhlwb3N0bWFzdGVyQHdpbmxsYy1kZXYuY29tMRgw\n" +
                    "FgYDVQQDDA90ZXN0LndpbmxsYy5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAw\n" +
                    "ggEKAoIBAQDL70P1WTSrj8qeC2tcVRVuPzLs7DwA6nkPaSk+2YHlQFsb5MpkMexQ\n" +
                    "zpyDKPTXRAUmffBVOtMUeC9Npzdnu1K5bdWtjPMCoP24v/wJFTzx49KX/jdRrrYw\n" +
                    "J68nMOvz3jyF0ISN3kmWXNE3rQk5mQ3NUKnBWU/j7T8UVBRdLPY1l2scLWfJINgm\n" +
                    "bFDA1zg/KurEuTLfpJrKKOkNd2Nx1DstOMACtbMJOr7aQ0wPifWUCknF1Zo93osB\n" +
                    "eqWr7qPUJnkEQrBkzM8qWNYo8JWd1A6e/5CMnL+0iJBxSswrJXXkVie3qSwZuhhH\n" +
                    "AUAg2V9R82mtQgcDMzj2eTHWNsX5vhQHAgMBAAGgLTArBgkqhkiG9w0BCQ4xHjAc\n" +
                    "MBoGA1UdEQQTMBGCD3Rlc3Qud2lubGxjLmNvbTANBgkqhkiG9w0BAQsFAAOCAQEA\n" +
                    "jz/5pQk5kB798/5LN4rqgs02oic3rsKAQk99qW8ty+MWQh/Q4Jx5URW/RsFbjn64\n" +
                    "fZ6rgVD491TNrse2ZE8/7iyjHEmn0vJyZ8aAVraxo445+lXcNYiluFvEdEG0v3qv\n" +
                    "kUEry9++H5BXjx/EDwI7atY+1U9pmxKvzAoinBBrkxXsC49BY1+PNGRmfJPxznmN\n" +
                    "poF6hkCJVX5Ygw6Ib4qdPAonbCiGM7yq6ur9V3K6HpOVcHEIErSCD4j4+mX//8JV\n" +
                    "zkzJN+9CSiuL7eXJKoZbbYF/3EnlCKCFx+u//WfqbAsdBJL9s+FB7crUBdMgT0UY\n" +
                    "RTJb2gZMJXwJ8vCPugoK9g\n" +
                    "-----END CERTIFICATE REQUEST-----";

    String testCsr2 = "-----BEGIN CERTIFICATE REQUEST-----\n" +
            "MIICVzCCAT8CAQAwEjEQMA4GA1UEAxMHdGVzdGtleTCCASIwDQYJKoZIhvcNAQEB\n" +
            "BQADggEPADCCAQoCggEBAMqIvm215QC1E2CwEsNMpn44foQY8sIymQAT4AYMC3UF\n" +
            "Dtdcf1G9FY0gdtr4hP+FS3XuYR+V06j66RQg0cdUax4NFGeCuPcAN5q8g6Qj7Zp2\n" +
            "OrLm8shemmUTjT+VoA1s1kbw6nZuF+3I9Z5BEV2wnG2j9kBEk7JnWwKzU0zggEY5\n" +
            "5RqOYN09dw6GKSCkc7fZjGB8mdAZX643pF3fHi1KSzEiRGmeDTCxx0Gw5S+KngoN\n" +
            "jlpQKcL64TRE5T14qcm9gWaDrdkkV4HjDtyQ1HPFM0g6sEQ5GjAEsirnI7VszIBV\n" +
            "2P0Ed8xE5z2Vm9bzqqOrrtfn0klw44BtyqeklfxNdKkCAwEAAaAAMA0GCSqGSIb3\n" +
            "DQEBCwUAA4IBAQC42u5jUspAOHg7P/aK+DJFqKfUHA/qPkzMvIdyHCmpvs/aPoyj\n" +
            "8kVpPAGp/mweVq/EClsnDq2y5ml4uFfcsFTVcpDRIzVOu5Bi0Et+s1T0+Y6nABBK\n" +
            "f76VPTBFCOkokSb2XCEBV6JRSz8PG47+6HdoijHqpvusrNhF5SBy8FCYTnCwWGbw\n" +
            "ZUujaOiuT+htifDY7sIYB08SMA7anUBNGznmj+cgJ2+LRZjbqNMXJQYAuU8VR9jf\n" +
            "abOIZU/OJnAm6jk68HUY6ov63t0onT9H+QNk0HJbVz+v5J7K2XQd+lM0iQk85PnD\n" +
            "FdOUg6b53ONKhTy6BglmCd+0t5+aW46MOBgZ\n" +
            "-----END CERTIFICATE REQUEST-----";

    @Autowired
    private CertificateRequestService certificateRequestService;
    @Autowired
    private CertificateRequestRepository certificateRequestRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AuditRecordRepository auditRecordRepository;
    @Autowired
    private ServerEntryRepository serverEntryRepository;
    @MockBean
    private CertAuthorityConnectionService certAuthorityConnectionService;

    @BeforeEach
    @Transactional
    void before() throws Exception {
        when(certAuthorityConnectionService.processIssueCertificate(any())).thenReturn(CertUtil.base64ToCert(MockCertAuthority.testX509Cert));

        Account account = Account.buildNew();
        account.setKeyIdentifier("testkid1");
        account.setProjectName("Test Project");
        account = accountRepository.save(account);

        CertificateRequest request = new CertificateRequest();
        request.setCsr(testCsr);
        request.setStatus("new");
        request.setRequestedBy("test@test.com");
        request.setAccount(account);

        certificateRequestRepository.save(request);
    }

    @AfterEach
    @Transactional
    void after(){
        certificateRequestRepository.deleteAll();
        accountRepository.deleteAll();
        auditRecordRepository.deleteAll();
        serverEntryRepository.deleteAll();
    }

    @Test
    void getAll() {
        List<CertificateRequest> all = certificateRequestService.getAll();
        assertEquals(1, all.size());
    }

    @Test
    void getAllWithStatus() {
        List<CertificateRequest> all = certificateRequestService.getAllWithStatus("new");
        assertEquals(1, all.size());
    }

    @Test
    void byId() throws RAObjectNotFoundException {
        CertificateRequest request = certificateRequestRepository.findAll().get(0);
        CertificateRequestInfo certificateRequestInfo = certificateRequestService.byId(request.getId());
        assertNotNull(certificateRequestInfo);
    }

    @Test
    void byIdFull() throws RAObjectNotFoundException {
        CertificateRequest request = certificateRequestRepository.findAll().get(0);
        CertificateRequest certificateRequestInfo = certificateRequestService.byIdFull(request.getId());
        assertNotNull(certificateRequestInfo);
    }

    @Test
    void submitRequest() throws RAObjectNotFoundException, InvalidFormException {
        Account account = accountRepository.findByKeyIdentifierEquals("testkid1").get();
        UserDetails userDetails =
                new org.springframework.security.core.userdetails.User("test@test.com", "", new ArrayList<>());

        SubjectAltName san = new SubjectAltName();
        san.setType("dns");
        san.setValue("test.winllc-dev.com");

        CertificateRequestForm form = new CertificateRequestForm();
        form.setCertAuthorityName("mockca");
        form.setCsr(testCsr2);
        form.setAccountId(account.getId());
        form.setRequestedDnsNames(Collections.singletonList(san));
        Long aLong = certificateRequestService.submitRequest(form, userDetails);
        assertTrue(aLong > 0);
    }

    @Test
    void reviewRequestGet() throws RAObjectNotFoundException {
        CertificateRequest request = certificateRequestRepository.findAll().get(0);
        CertificateRequest request1 = certificateRequestService.reviewRequestGet(request.getId());
        assertNotNull(request1);
    }

    @Test
    void reviewRequest() throws RAException {
        CertificateRequest request = certificateRequestRepository.findAll().get(0);
        CertificateRequestDecisionForm form = new CertificateRequestDecisionForm();
        form.setRequestId(request.getId());
        form.setStatus("approved");

        request = certificateRequestService.reviewRequest(form);
        assertEquals("issued", request.getStatus());
    }

    @Test
    void myRequests() throws RAObjectNotFoundException {
        UserDetails userDetails =
                new org.springframework.security.core.userdetails.User("test@test.com", "", new ArrayList<>());

        List<CertificateRequest> certificateRequests = certificateRequestService.myRequests(userDetails);
        assertEquals(1, certificateRequests.size());
    }
}