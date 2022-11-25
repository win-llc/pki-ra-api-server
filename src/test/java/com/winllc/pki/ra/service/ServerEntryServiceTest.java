package com.winllc.pki.ra.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winllc.acme.common.domain.Account;
import com.winllc.pki.ra.BaseTest;
import com.winllc.pki.ra.beans.form.ServerEntryForm;
import com.winllc.pki.ra.beans.info.ServerEntryInfo;
import com.winllc.acme.common.domain.*;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.*;
import com.winllc.pki.ra.service.external.EntityDirectoryService;
import com.winllc.pki.ra.service.external.vendorimpl.KeycloakOIDCProviderConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import javax.transaction.Transactional;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServerEntryServiceTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ServerEntryService serverEntryService;
    @Autowired
    private ServerEntryRepository serverEntryRepository;
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
    @Autowired
    private AuthCredentialRepository authCredentialRepository;

    @BeforeEach
    @Transactional
    void before(){
        accountRepository.deleteAll();

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
        domainPolicy.setAccount(account);
        domainPolicy = domainPolicyRepository.save(domainPolicy);
        account.getAccountDomainPolicies().add(domainPolicy);
        account = accountRepository.save(account);

        ServerEntry serverEntry = ServerEntry.buildNew();
        serverEntry.setFqdn("test.winllc-dev.com");
        serverEntry.setAccount(account);
        serverEntry.setDomainParent(domain);
        serverEntry = serverEntryRepository.save(serverEntry);

        account.getServerEntries().add(serverEntry);
        accountRepository.save(account);

        when(entityDirectoryService.applyServerEntryToDirectory(any())).thenReturn(new HashMap<>());
    }

    @AfterEach
    @Transactional
    void after(){
        authCredentialRepository.deleteAll();
        serverEntryRepository.deleteAll();
        domainRepository.deleteAll();
        accountRepository.deleteAll();
        pocEntryRepository.deleteAll();
    }

    @Test
    void createServerEntry() throws Exception {
        Account account = accountRepository.findAll().get(0);
        ServerEntryForm serverEntryForm = new ServerEntryForm();
        serverEntryForm.setFqdn("test2.winllc-dev.com");
        serverEntryForm.setAccountId(account.getId());

        serverEntryService.add(serverEntryForm, null,null);

        Optional<ServerEntry> distinctByFqdnEquals = serverEntryRepository.findDistinctByFqdnEquals("test2.winllc-dev.com");
        assertTrue(distinctByFqdnEquals.isPresent());

        serverEntryForm.setFqdn("inva lid");
        String badJson = new ObjectMapper().writeValueAsString(serverEntryForm);
        mockMvc.perform(
                post("/api/serverEntry/add")
                        .contentType("application/json")
                        .content(badJson))
                .andExpect(status().is(400));
    }

    @Test
    @Transactional
    void getLatestAuthCredential() throws Exception {
        Account account = accountRepository.findAll().get(0);
        ServerEntryForm serverEntryForm = new ServerEntryForm();
        serverEntryForm.setFqdn("test2.winllc-dev.com");
        serverEntryForm.setAccountId(account.getId());

        serverEntryForm = serverEntryService.add(serverEntryForm, null, null);

        AuthCredential latestAuthCredential = serverEntryService.getLatestAuthCredential(serverEntryForm.getId());
        assertTrue(latestAuthCredential.getMacKey().length() > 0);
    }

    //todo @Test
    @Transactional
    void updateServerEntry() throws Exception {
        Account account = accountRepository.findAll().get(0);

        ServerEntry serverEntry = serverEntryRepository.findDistinctByFqdnEquals("test.winllc-dev.com").get();
        ServerEntryForm form = new ServerEntryForm(serverEntry);
        form.setAccountId(account.getId());

        assertEquals(0, serverEntry.getAlternateDnsValues().size());

        form.setAlternateDnsValues(Collections.singletonList("bad dns"));

        serverEntryService.update(form, null, null);

        serverEntry = serverEntryRepository.findDistinctByFqdnEquals("test.winllc-dev.com").get();
        assertEquals(1, serverEntry.getAlternateDnsValues().size());

        form.setAccountId(0L);
        String badJson = new ObjectMapper().writeValueAsString(form);
        mockMvc.perform(
                post("/api/serverEntry/update")
                        .contentType("application/json")
                        .content(badJson))
                .andExpect(status().is(400));
    }

    @Test
    void getServerEntry() throws Exception {
        ServerEntry serverEntry = serverEntryRepository.findDistinctByFqdnEquals("test.winllc-dev.com").get();
        ServerEntryForm form = serverEntryService.findRest(serverEntry.getId(), null);
        assertEquals("test.winllc-dev.com", form.getFqdn());
    }

    @Test
    void getAllServerEntriesForAccount() {
        Account account = accountRepository.findAll().get(0);
        List<ServerEntryInfo> allServerEntriesForAccount = serverEntryService.getAllServerEntriesForAccount(account.getId());
        assertEquals(1, allServerEntriesForAccount.size());
    }

    @Test
    void getAllServerEntriesForUser() throws RAObjectNotFoundException {
        UserDetails userDetails =
                new org.springframework.security.core.userdetails.User("test@test.com", "", new ArrayList<>());
        List<ServerEntryInfo> allServerEntriesForUser = serverEntryService.getAllServerEntriesForUser(userDetails);
        assertEquals(1, allServerEntriesForUser.size());
    }


    @Test
    void deleteServerEntry() throws RAException {
        ServerEntry serverEntry = serverEntryRepository.findDistinctByFqdnEquals("test.winllc-dev.com").get();
        assertNotNull(serverEntry);

        serverEntryService.delete(serverEntry.getId(), null,null);

        assertEquals(0, serverEntryRepository.findAll().size());
    }
}