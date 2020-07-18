package com.winllc.pki.ra.service;

import com.winllc.acme.common.CertSearchParam;
import com.winllc.acme.common.CertSearchParams;
import com.winllc.acme.common.CertificateDetails;
import com.winllc.acme.common.ra.RACertificateIssueRequest;
import com.winllc.acme.common.ra.RACertificateRevokeRequest;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.beans.form.CertAuthorityConnectionInfoForm;
import com.winllc.pki.ra.ca.CertAuthority;
import com.winllc.pki.ra.ca.CertAuthorityConnectionType;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.CertAuthorityConnectionInfo;
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
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class CertAuthorityConnectionServiceTest {

    private final String testCsr =
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
                    "RTJb2gZMJXwJ8vCPugoK9g";


    @Autowired
    private CertAuthorityConnectionService connectionService;
    @Autowired
    private CertAuthorityConnectionInfoRepository connectionInfoRepository;
    @Autowired
    private CertAuthorityConnectionPropertyRepository propertyRepository;
    @Autowired
    private AuditRecordRepository auditRecordRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CertificateRequestRepository certificateRequestRepository;

    @BeforeEach
    @Transactional
    void before(){
        CertAuthority mockCa = new MockCertAuthority();
        connectionService.addLoadedCertAuthority(mockCa);

        Account account = Account.buildNew("Test Project");
        account.setKeyIdentifier("kidtest1");
        account.setMacKey("testmac1");
        accountRepository.save(account);
    }

    @AfterEach
    @Transactional
    void after(){
        connectionInfoRepository.deleteAll();
        propertyRepository.deleteAll();
        certificateRequestRepository.deleteAll();
        auditRecordRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void createConnectionInfo() throws InvalidFormException {
        CertAuthorityConnectionInfoForm form = new CertAuthorityConnectionInfoForm();
        form.setName("test1");
        form.setType(CertAuthorityConnectionType.INTERNAL.toString());

        Long connectionInfo = connectionService.createConnectionInfo(form);
        assertTrue(connectionInfo > 0);
    }

    @Test
    void updateConnectionInfo() throws InvalidFormException, RAException {
        CertAuthorityConnectionInfo info = new CertAuthorityConnectionInfo();
        info.setType(CertAuthorityConnectionType.INTERNAL);
        info.setName("mockca");
        info = connectionInfoRepository.save(info);

        CertAuthorityConnectionInfoForm form = new CertAuthorityConnectionInfoForm();
        form.setBaseUrl("http://newurl");
        form.setId(info.getId());
        CertAuthorityConnectionInfoForm certAuthorityConnectionInfoForm = connectionService.updateConnectionInfo(form);
        assertEquals("http://newurl", certAuthorityConnectionInfoForm.getBaseUrl());
    }

    @Test
    void getConnectionInfoByName() throws RAObjectNotFoundException {
        CertAuthorityConnectionInfo info = new CertAuthorityConnectionInfo();
        info.setType(CertAuthorityConnectionType.INTERNAL);
        info.setName("mockca");
        info = connectionInfoRepository.save(info);

        CertAuthorityConnectionInfoForm connectionInfoByName = connectionService.getConnectionInfoByName(info.getName());
        assertEquals(info.getName(), connectionInfoByName.getName());
    }

    @Test
    void getConnectionInfoById() throws RAObjectNotFoundException {
        CertAuthorityConnectionInfo info = new CertAuthorityConnectionInfo();
        info.setType(CertAuthorityConnectionType.INTERNAL);
        info.setName("mockca");
        info = connectionInfoRepository.save(info);

        CertAuthorityConnectionInfoForm connectionInfoByName = connectionService.getConnectionInfoById(info.getId());
        assertEquals(info.getName(), connectionInfoByName.getName());
    }

    @Test
    void getAllConnectionInfo() {
        CertAuthorityConnectionInfo info = new CertAuthorityConnectionInfo();
        info.setType(CertAuthorityConnectionType.INTERNAL);
        info.setName("mockca");
        info = connectionInfoRepository.save(info);

        List<CertAuthorityConnectionInfo> allConnectionInfo = connectionService.getAllConnectionInfo();
        assertEquals(1, allConnectionInfo.size());
    }

    @Test
    void deleteInfo() {
        CertAuthorityConnectionInfo info = new CertAuthorityConnectionInfo();
        info.setType(CertAuthorityConnectionType.INTERNAL);
        info.setName("mockca");
        info = connectionInfoRepository.save(info);

        connectionService.deleteInfo(info.getId());

        assertEquals(0, connectionInfoRepository.findAll().size());
    }

    @Test
    void getTypes() {
        List<String> types = connectionService.getTypes();
        assertEquals(CertAuthorityConnectionType.values().length, types.size());
    }

    @Test
    void issueCertificate() throws Exception {
        RACertificateIssueRequest request = new RACertificateIssueRequest("kidtest1", testCsr, "test.winllc-dev.com", "mockca");

        String s = connectionService.issueCertificate(request);
        assertTrue(s.contains("BEGIN CERTIFICATE"));
    }

    @Test
    void processIssueCertificate() throws Exception {
        RACertificateIssueRequest request = new RACertificateIssueRequest("kidtest1", testCsr, "test.winllc-dev.com", "mockca");

        X509Certificate certificate = connectionService.processIssueCertificate(request);
        assertNotNull(certificate);
    }

    @Test
    void revokeCertificate() throws Exception {
        RACertificateRevokeRequest revokeRequest = new RACertificateRevokeRequest();
        revokeRequest.setReason(1);
        revokeRequest.setRequestId(5L);
        revokeRequest.setSerial("5");
        revokeRequest.setCertAuthorityName("mockca");
        connectionService.revokeCertificate(revokeRequest);
    }

    @Test
    void getCertificateStatus() throws Exception {
        CertificateDetails certDetails = connectionService.getCertificateStatus("mockca", "5");
        assertTrue(certDetails.getCertificateBase64().contains("BEGIN"));
    }

    @Test
    void getTrustChain() throws Exception {
        String trustChain = connectionService.getTrustChain("mockca");
        Certificate[] certificates = CertUtil.trustChainStringToCertArray(trustChain);
        assertTrue(certificates.length > 0);
    }

    @Test
    void search() {
        CertSearchParam searchParam = new CertSearchParam(CertSearchParams.CertField.SUBJECT, "test.winllc-dev.com",
                CertSearchParams.CertSearchParamRelation.EQUALS);
        List<CertificateDetails> list = connectionService.search("mockca", searchParam);
        assertEquals(1, list.size());
    }

    @Test
    void getCertAuthorityByName() {
        Optional<CertAuthority> mockca = connectionService.getCertAuthorityByName("mockca");
        assertTrue(mockca.isPresent());
    }

    @Test
    void getAllCertAuthorities() {
        List<CertAuthority> allCertAuthorities = connectionService.getAllCertAuthorities();
        assertTrue(allCertAuthorities.size() > 0);
    }

}