package com.winllc.pki.ra.service;

import com.winllc.acme.common.domain.Account;
import com.winllc.pki.ra.BaseTest;
import com.winllc.pki.ra.beans.OIDCClientDetails;
import com.winllc.pki.ra.beans.form.ServerEntryForm;
import com.winllc.pki.ra.beans.info.ServerEntryInfo;
import com.winllc.acme.common.domain.*;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.*;
import com.winllc.pki.ra.service.external.EntityDirectoryService;
import com.winllc.pki.ra.service.external.vendorimpl.KeycloakOIDCProviderConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class OIDCManagementServiceTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private OIDCManagementService oidcManagementService;
    @Autowired
    private ServerEntryRepository serverEntryRepository;
    @Autowired
    private ServerEntryService serverEntryService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private DomainPolicyRepository domainPolicyRepository;
    @Autowired
    private PocEntryRepository pocEntryRepository;
    @MockBean
    private KeycloakOIDCProviderConnection oidcProviderConnection;
    @MockBean
    private EntityDirectoryService entityDirectoryService;

    @BeforeEach
    @Transactional
    void before() throws Exception {
        Account account = Account.buildNew("Test Project 3");
        account.setKeyIdentifier("kidtest1");
        //account.setMacKey("testmac1");
        account = accountRepository.save(account);

        PocEntry pocEntry = PocEntry.buildNew("test@test.com", account);
        pocEntryRepository.save(pocEntry);

        Domain domain = new Domain();
        domain.setBase("winllc-dev.com");
        domain = domainRepository.save(domain);

        DomainPolicy domainPolicy = new DomainPolicy(domain);
        domainPolicy = domainPolicyRepository.save(domainPolicy);
        account.getAccountDomainPolicies().add(domainPolicy);
        account = accountRepository.save(account);

        ServerEntryForm form = new ServerEntryForm();
        form.setAccountId(account.getId());
        form.setFqdn("test2.winllc-dev.com");

        form = serverEntryService.add(form, null, null);
        ServerEntry serverEntry = serverEntryRepository.findById(form.getId()).get();

        account.getServerEntries().add(serverEntry);
        accountRepository.save(account);

        when(entityDirectoryService.applyServerEntryToDirectory(any())).thenReturn(new HashMap<>());
    }

    @AfterEach
    @Transactional
    void after(){
        domainRepository.deleteAll();
        serverEntryRepository.deleteAll();
        accountRepository.deleteAll();
        pocEntryRepository.deleteAll();
    }

    @Test
    @Transactional
    void enableForOIDConnect() throws Exception {
        ServerEntry serverEntry = serverEntryRepository.findDistinctByFqdnEquals("test2.winllc-dev.com").get();
        when(oidcProviderConnection.createClient(any())).thenReturn(serverEntry);

        ServerEntryForm form = new ServerEntryForm();
        form.setId(serverEntry.getId());
        ServerEntryInfo serverEntryInfo = oidcManagementService.enableForOIDConnect(form);
        assertNotNull(serverEntryInfo);
    }

    @Test
    @Transactional
    void disableForOIDConnect() throws Exception {
        ServerEntry serverEntry = serverEntryRepository.findDistinctByFqdnEquals("test2.winllc-dev.com").get();
        when(oidcProviderConnection.deleteClient(any())).thenReturn(serverEntry);

        ServerEntryForm form = new ServerEntryForm();
        form.setId(serverEntry.getId());
        ServerEntryInfo serverEntryInfo = oidcManagementService.disableForOIDConnect(form);
        assertNotNull(serverEntryInfo);
    }

    @Test
    @Transactional
    void buildDeploymentPackage() throws RAObjectNotFoundException {
        ServerEntry serverEntry = serverEntryRepository.findDistinctByFqdnEquals("test2.winllc-dev.com").get();

        OIDCClientDetails oidcClientDetails = new OIDCClientDetails();
        when(oidcProviderConnection.getClient(any())).thenReturn(oidcClientDetails);

        ServerEntryForm form = new ServerEntryForm();
        form.setId(serverEntry.getId());

        List<String> strings = oidcManagementService.buildDeploymentPackage(form);
        assertTrue(strings.size() > 0);
    }
}