package com.winllc.pki.ra.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winllc.acme.common.CertSearchParam;
import com.winllc.acme.common.CertSearchParams;
import com.winllc.acme.common.CertificateDetails;
import com.winllc.acme.common.ca.ConnectionProperty;
import com.winllc.acme.common.domain.CertAuthorityConnectionProperty;
import com.winllc.acme.common.ra.RACertificateIssueRequest;
import com.winllc.acme.common.ra.RACertificateRevokeRequest;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.beans.form.AccountRequestForm;
import com.winllc.pki.ra.beans.form.CertAuthorityConnectionInfoForm;
import com.winllc.acme.common.ca.CertAuthority;
import com.winllc.pki.ra.ca.LoadedCertAuthorityStore;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.Account;
import com.winllc.acme.common.domain.CertAuthorityConnectionInfo;
import com.winllc.pki.ra.domain.AuthCredential;
import com.winllc.pki.ra.domain.Domain;
import com.winllc.pki.ra.domain.DomainPolicy;
import com.winllc.pki.ra.exception.InvalidFormException;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.mock.MockAbstractCertAuthority;
import com.winllc.pki.ra.mock.MockCertAuthority;
import com.winllc.pki.ra.repository.*;
import com.winllc.pki.ra.service.external.EntityDirectoryService;
import com.winllc.pki.ra.service.transaction.CertIssuanceTransaction;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.transaction.Transactional;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
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
    private MockMvc mockMvc;
    @Autowired
    private CertAuthorityConnectionService connectionService;
    @Autowired
    private LoadedCertAuthorityStore certAuthorityStore;
    @Autowired
    private CertAuthorityConnectionInfoRepository connectionInfoRepository;
    @Autowired
    private CertAuthorityConnectionPropertyRepository propertyRepository;
    @Autowired
    private AuditRecordRepository auditRecordRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountService accountService;
    @Autowired
    private CertificateRequestRepository certificateRequestRepository;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private DomainPolicyRepository domainPolicyRepository;
    @Autowired
    private ApplicationContext context;
    @MockBean
    private EntityDirectoryService entityDirectoryService;
    @Autowired
    private ServerEntryRepository serverEntryRepository;
    @Autowired
    private AuthCredentialService authCredentialService;

    @BeforeEach
    @Transactional
    void before() throws Exception {
        CertAuthority mockCa = new MockCertAuthority();
        certAuthorityStore.addLoadedCertAuthority(mockCa);

        Domain domain = new Domain();
        domain.setBase("winllc-dev.com");
        domain = domainRepository.save(domain);

        AccountRequestForm form = new AccountRequestForm();
        form.setProjectName("Test Project");
        Long id = accountService.createNewAccount(form);
        Account account = accountRepository.findById(id).get();

        DomainPolicy domainPolicy = new DomainPolicy();
        domainPolicy.setTargetDomain(domain);
        domainPolicy.setAllowIssuance(true);
        domainPolicy = domainPolicyRepository.save(domainPolicy);

        Hibernate.initialize(account.getAccountDomainPolicies());
        account.getAccountDomainPolicies().add(domainPolicy);
        accountRepository.save(account);

        when(entityDirectoryService.applyServerEntryToDirectory(any())).thenReturn(new HashMap<>());
    }

    @AfterEach
    @Transactional
    void after(){
        connectionInfoRepository.deleteAll();
        propertyRepository.deleteAll();
        certificateRequestRepository.deleteAll();
        auditRecordRepository.deleteAll();
        accountRepository.deleteAll();
        serverEntryRepository.deleteAll();
        domainPolicyRepository.deleteAll();
        domainRepository.deleteAll();;
    }

    @Test
    @Transactional
    void createConnectionInfo() throws Exception {
        CertAuthorityConnectionInfoForm form = new CertAuthorityConnectionInfoForm();
        form.setName("mockca");
        form.setType(MockAbstractCertAuthority.class.getCanonicalName());
        //form.setType(CertAuthorityConnectionType.INTERNAL.toString());

        Long connectionInfo = connectionService.createConnectionInfo(form);
        assertTrue(connectionInfo > 0);

        form.setBaseUrl("invalidurl");
        String badJson = new ObjectMapper().writeValueAsString(form);
        mockMvc.perform(
                post("/ca/api/info/create")
                        .contentType("application/json")
                        .content(badJson))
                .andExpect(status().is(400));
    }

    @Test
    @Transactional
    void updateConnectionInfo() throws Exception {
        CertAuthorityConnectionInfo info = new CertAuthorityConnectionInfo();
        //info.setType(CertAuthorityConnectionType.INTERNAL);
        info.setName("mockca");
        info = connectionInfoRepository.save(info);

        CertAuthorityConnectionInfoForm form = new CertAuthorityConnectionInfoForm();
        form.setBaseUrl("http://newurl");
        form.setId(info.getId());
        CertAuthorityConnectionInfoForm certAuthorityConnectionInfoForm = connectionService.updateConnectionInfo(form);
        assertEquals("http://newurl", certAuthorityConnectionInfoForm.getBaseUrl());

        CertAuthorityConnectionProperty property = new CertAuthorityConnectionProperty();
        property.setName("");
        property.setValue("val");

        form.setProperties(Collections.singleton(property));
        String badJson = new ObjectMapper().writeValueAsString(form);
        mockMvc.perform(
                post("/ca/api/info/update")
                        .contentType("application/json")
                        .content(badJson))
                .andExpect(status().is(409));
    }

    @Test
    @Transactional
    void getConnectionInfoByName() throws RAObjectNotFoundException {
        CertAuthorityConnectionInfo info = new CertAuthorityConnectionInfo();
        //info.setType(CertAuthorityConnectionType.INTERNAL);
        info.setName("mockca");
        info = connectionInfoRepository.save(info);

        CertAuthorityConnectionInfoForm connectionInfoByName = connectionService.getConnectionInfoByName(info.getName());
        assertEquals(info.getName(), connectionInfoByName.getName());
    }

    @Test
    @Transactional
    void getConnectionInfoById() throws RAObjectNotFoundException {
        CertAuthorityConnectionInfo info = new CertAuthorityConnectionInfo();
        //info.setType(CertAuthorityConnectionType.INTERNAL);
        info.setName("mockca");
        info = connectionInfoRepository.save(info);

        CertAuthorityConnectionInfoForm connectionInfoByName = connectionService.getConnectionInfoById(info.getId());
        assertEquals(info.getName(), connectionInfoByName.getName());
    }

    @Test
    @Transactional
    void getAllConnectionInfo() {
        CertAuthorityConnectionInfo info = new CertAuthorityConnectionInfo();
       //info.setType(CertAuthorityConnectionType.INTERNAL);
        info.setName("mockca");
        info = connectionInfoRepository.save(info);

        List<CertAuthorityConnectionInfo> allConnectionInfo = connectionService.getAllConnectionInfo();
        assertEquals(1, allConnectionInfo.size());
    }

    @Test
    @Transactional
    void deleteInfo() {
        CertAuthorityConnectionInfo info = new CertAuthorityConnectionInfo();
        //info.setType(CertAuthorityConnectionType.INTERNAL);
        info.setName("mockca");
        info = connectionInfoRepository.save(info);

        connectionService.deleteInfo(info.getId());

        assertEquals(0, connectionInfoRepository.findAll().size());
    }

    @Test
    @Transactional
    void getTypes() {
        List<String> types = connectionService.getTypes();
        assertTrue(types.size() > 0);
    }

    @Test
    @Transactional
    void getRequiredPropertiesForType() throws Exception {
        List<ConnectionProperty> requiredPropertiesForType
                = connectionService.getRequiredPropertiesForType(MockCertAuthority.class.getName());
        assertEquals(1, requiredPropertiesForType.size());
    }

    @Test
    @Transactional
    void issueCertificate() throws Exception {
        Account account = accountRepository.findDistinctByProjectName("Test Project").get();
        Optional<AuthCredential> latestAuthCredentialForAccount = authCredentialService.getLatestAuthCredentialForAccount(account);

        RACertificateIssueRequest request =
                new RACertificateIssueRequest(latestAuthCredentialForAccount.get().getKeyIdentifier(),
                        testCsr, "test.winllc-dev.com", "test.winllc-dev.com", "mockca", "test");

        String s = connectionService.issueCertificate(request);
        assertTrue(s.contains("BEGIN CERTIFICATE"));
    }

    @Test
    @Transactional
    void processIssueCertificate() throws Exception {
        Account account = accountRepository.findDistinctByProjectName("Test Project").get();
        RACertificateIssueRequest request =
                new RACertificateIssueRequest("kidtest1", testCsr, "test.winllc-dev.com", "test.winllc-dev.com", "mockca", "test");

        CertIssuanceTransaction transaction = new CertIssuanceTransaction(certAuthorityStore.getLoadedCertAuthority("mockca"),
                context);
        X509Certificate certificate = transaction.processIssueCertificate(request, account);
        assertNotNull(certificate);
    }

    @Test
    @Transactional
    void revokeCertificate() throws Exception {
        RACertificateRevokeRequest revokeRequest = new RACertificateRevokeRequest();
        revokeRequest.setReason(1);
        revokeRequest.setRequestId(5L);
        revokeRequest.setSerial("5");
        revokeRequest.setCertAuthorityName("mockca");
        connectionService.revokeCertificate(revokeRequest);
    }

    @Test
    @Transactional
    void getCertificateStatus() throws Exception {
        CertificateDetails certDetails = connectionService.getCertificateStatus("mockca", "5");
        assertNotNull(certDetails.getStatus());
    }

    @Test
    @Transactional
    void getTrustChain() throws Exception {
        String trustChain = connectionService.getTrustChain("mockca");
        Certificate[] certificates = CertUtil.trustChainStringToCertArray(trustChain);
        assertTrue(certificates.length > 0);
    }

    @Test
    @Transactional
    void search() {
        CertSearchParam searchParam = new CertSearchParam(CertSearchParams.CertField.SUBJECT, "test.winllc-dev.com",
                CertSearchParams.CertSearchParamRelation.EQUALS);
        List<CertificateDetails> list = connectionService.search("mockca", searchParam);
        assertEquals(1, list.size());
    }

    @Test
    @Transactional
    void getCertAuthorityByName() {
        Optional<CertAuthority> mockca = connectionService.getCertAuthorityByName("mockca");
        assertTrue(mockca.isPresent());
    }

}