package com.winllc.pki.ra.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winllc.pki.ra.BaseTest;
import com.winllc.pki.ra.beans.OIDCClientDetails;
import com.winllc.pki.ra.beans.form.ServerEntryForm;
import com.winllc.pki.ra.beans.info.ServerEntryInfo;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.*;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.*;
import com.winllc.pki.ra.service.external.EntityDirectoryService;
import com.winllc.pki.ra.service.external.vendorimpl.KeycloakOIDCProviderConnection;
import com.winllc.pki.ra.util.EmailUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
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

    @BeforeEach
    @Transactional
    void before(){
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

        serverEntryService.createServerEntry(serverEntryForm);

        Optional<ServerEntry> distinctByFqdnEquals = serverEntryRepository.findDistinctByFqdnEquals("test2.winllc-dev.com");
        assertTrue(distinctByFqdnEquals.isPresent());

        serverEntryForm.setFqdn("inva lid");
        String badJson = new ObjectMapper().writeValueAsString(serverEntryForm);
        mockMvc.perform(
                post("/api/serverEntry/create")
                        .contentType("application/json")
                        .content(badJson))
                .andExpect(status().is(400));
    }

    @Test
    @Transactional
    void getLatestAuthCredential() throws RAObjectNotFoundException {
        Account account = accountRepository.findAll().get(0);
        ServerEntryForm serverEntryForm = new ServerEntryForm();
        serverEntryForm.setFqdn("test2.winllc-dev.com");
        serverEntryForm.setAccountId(account.getId());

        Long serverId = serverEntryService.createServerEntry(serverEntryForm);

        AuthCredential latestAuthCredential = serverEntryService.getLatestAuthCredential(serverId);
        assertTrue(latestAuthCredential.getMacKey().length() > 0);
    }

    @Test
    @Transactional
    void updateServerEntry() throws Exception {
        ServerEntry serverEntry = serverEntryRepository.findDistinctByFqdnEquals("test.winllc-dev.com").get();
        ServerEntryForm form = new ServerEntryForm(serverEntry);

        assertEquals(0, serverEntry.getAlternateDnsValues().size());

        form.setAlternateDnsValues(Collections.singletonList("bad dns"));

        serverEntryService.updateServerEntry(form);

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
    void getServerEntry() throws RAObjectNotFoundException {
        ServerEntry serverEntry = serverEntryRepository.findDistinctByFqdnEquals("test.winllc-dev.com").get();
        ServerEntryInfo info = serverEntryService.getServerEntryInfo(serverEntry.getId());
        assertEquals("test.winllc-dev.com", info.getFqdn());
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

        serverEntryService.deleteServerEntry(serverEntry.getId());

        assertEquals(0, serverEntryRepository.findAll().size());
    }
}