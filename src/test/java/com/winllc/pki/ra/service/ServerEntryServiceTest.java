package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.OIDCClientDetails;
import com.winllc.pki.ra.beans.form.ServerEntryForm;
import com.winllc.pki.ra.beans.info.ServerEntryInfo;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.Domain;
import com.winllc.pki.ra.domain.ServerEntry;
import com.winllc.pki.ra.domain.User;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.DomainRepository;
import com.winllc.pki.ra.repository.ServerEntryRepository;
import com.winllc.pki.ra.repository.UserRepository;
import com.winllc.pki.ra.service.external.KeycloakService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class ServerEntryServiceTest {

    @Autowired
    private ServerEntryService serverEntryService;
    @Autowired
    private ServerEntryRepository serverEntryRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DomainRepository domainRepository;
    @MockBean
    private KeycloakService keycloakService;

    @BeforeEach
    @Transactional
    void before(){
        Account account = new Account();
        account.setKeyIdentifier("kidtest1");
        account.setProjectName("Test Project 3");
        account.setMacKey("testmac1");
        account = accountRepository.save(account);

        User user = new User();
        user.setUsername("test@test.com");
        user.setIdentifier(UUID.randomUUID());
        user.getAccounts().add(account);
        userRepository.save(user);

        account.getAccountUsers().add(user);
        accountRepository.save(account);

        Domain domain = new Domain();
        domain.setBase("winllc-dev.com");
        domain.getCanIssueAccounts().add(account);
        domain = domainRepository.save(domain);

        account.getCanIssueDomains().add(domain);
        accountRepository.save(account);

        ServerEntry serverEntry = new ServerEntry();
        serverEntry.setFqdn("test.winllc-dev.com");
        serverEntry.setAccount(account);
        serverEntry.setDomainParent(domain);
        serverEntry = serverEntryRepository.save(serverEntry);

        account.getServerEntries().add(serverEntry);
        accountRepository.save(account);
    }

    @AfterEach
    @Transactional
    void after(){
        domainRepository.deleteAll();
        serverEntryRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createServerEntry() throws RAObjectNotFoundException {
        Account account = accountRepository.findAll().get(0);
        ServerEntryForm serverEntryForm = new ServerEntryForm();
        serverEntryForm.setFqdn("test2.winllc-dev.com");
        serverEntryForm.setAccountId(account.getId());

        serverEntryService.createServerEntry(serverEntryForm);

        Optional<ServerEntry> distinctByFqdnEquals = serverEntryRepository.findDistinctByFqdnEquals("test2.winllc-dev.com");
        assertTrue(distinctByFqdnEquals.isPresent());
    }

    @Test
    @Transactional
    void updateServerEntry() throws RAException {
        ServerEntry serverEntry = serverEntryRepository.findDistinctByFqdnEquals("test.winllc-dev.com").get();
        ServerEntryForm form = new ServerEntryForm(serverEntry);

        assertEquals(0, serverEntry.getAlternateDnsValues().size());

        form.setAlternateDnsValues(Collections.singletonList("tester.winllc-dev.com"));

        serverEntryService.updateServerEntry(form);

        serverEntry = serverEntryRepository.findDistinctByFqdnEquals("test.winllc-dev.com").get();
        assertEquals(1, serverEntry.getAlternateDnsValues().size());
    }

    @Test
    void getServerEntry() throws RAObjectNotFoundException {
        ServerEntry serverEntry = serverEntryRepository.findDistinctByFqdnEquals("test.winllc-dev.com").get();
        ServerEntryInfo info = serverEntryService.getServerEntry(serverEntry.getId());
        assertEquals("test.winllc-dev.com", info.getFqdn());
    }

    @Test
    void getAllServerEntriesForAccount() {
        Account account = accountRepository.findAll().get(0);
        List<ServerEntry> allServerEntriesForAccount = serverEntryService.getAllServerEntriesForAccount(account.getId());
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
    @Transactional
    void enableForOIDConnect() throws Exception {
        ServerEntry serverEntry = serverEntryRepository.findDistinctByFqdnEquals("test.winllc-dev.com").get();
        when(keycloakService.createClient(any())).thenReturn(serverEntry);

        ServerEntryForm form = new ServerEntryForm();
        form.setId(serverEntry.getId());
        ServerEntryInfo serverEntryInfo = serverEntryService.enableForOIDConnect(form);
        assertNotNull(serverEntryInfo);
    }

    @Test
    @Transactional
    void disableForOIDConnect() throws RAException {
        ServerEntry serverEntry = serverEntryRepository.findDistinctByFqdnEquals("test.winllc-dev.com").get();
        when(keycloakService.deleteClient(any())).thenReturn(serverEntry);

        ServerEntryForm form = new ServerEntryForm();
        form.setId(serverEntry.getId());
        ServerEntryInfo serverEntryInfo = serverEntryService.disableForOIDConnect(form);
        assertNotNull(serverEntryInfo);
    }

    @Test
    void buildDeploymentPackage() throws RAObjectNotFoundException {
        ServerEntry serverEntry = serverEntryRepository.findDistinctByFqdnEquals("test.winllc-dev.com").get();

        OIDCClientDetails oidcClientDetails = new OIDCClientDetails();
        when(keycloakService.getClient(any())).thenReturn(oidcClientDetails);

        ServerEntryForm form = new ServerEntryForm();
        form.setId(serverEntry.getId());

        List<String> strings = serverEntryService.buildDeploymentPackage(form);
        assertTrue(strings.size() > 0);
    }

    @Test
    void deleteServerEntry() throws RAException {
        ServerEntry serverEntry = serverEntryRepository.findDistinctByFqdnEquals("test.winllc-dev.com").get();
        assertNotNull(serverEntry);

        serverEntryService.deleteServerEntry(serverEntry.getId());

        assertEquals(0, serverEntryRepository.findAll().size());
    }
}