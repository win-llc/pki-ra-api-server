package com.winllc.pki.ra.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.domain.Account;
import com.winllc.acme.common.repository.AuthCredentialRepository;
import com.winllc.pki.ra.BaseTest;
import com.winllc.pki.ra.beans.form.AccountRequestForm;
import com.winllc.pki.ra.endpoint.acme.AcmeServerConnection;
import com.winllc.pki.ra.endpoint.acme.AcmeServerService;
import com.winllc.pki.ra.endpoint.acme.AcmeServerServiceImpl;
import com.winllc.pki.ra.beans.PocFormEntry;
import com.winllc.pki.ra.beans.form.AccountUpdateForm;
import com.winllc.pki.ra.beans.info.AccountInfo;
import com.winllc.acme.common.domain.*;
import com.winllc.pki.ra.exception.AcmeConnectionException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.AccountRepository;
import com.winllc.acme.common.repository.PocEntryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.transaction.Transactional;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountServiceTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PocEntryRepository pocEntryRepository;
    @Autowired
    private AuthCredentialRepository authCredentialRepository;
    @MockBean
    private AcmeServerManagementService acmeServerManagementService;

    @BeforeEach
    @Transactional
    void before() throws Exception {
        AccountRequestForm form = new AccountRequestForm();
        form.setProjectName("Test Project");
        Long id = accountService.createNewAccount(form);
        Account account = accountRepository.findById(id).get();

        PocEntry pocEntry = new PocEntry();
        pocEntry.setAccount(account);
        pocEntry.setEmail("test@test.com");
        pocEntry.setEnabled(true);
        pocEntryRepository.save(pocEntry);
    }

    @AfterEach
    @Transactional
    void after(){
        //authCredentialRepository.deleteAll();
        pocEntryRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    @Transactional
    void createNewAccount() throws Exception {
        AccountRequestForm accountRequest = new AccountRequestForm();
        accountRequest.setProjectName("Test Project 3");

        Long id = accountService.createNewAccount(accountRequest);
        assertTrue(id > 0);

        Account account = accountRepository.findById(id).get();

        assertEquals(1, account.getAuthCredentials().size());

        accountRequest.setAccountOwnerEmail("bademail");
        String badJson = new ObjectMapper().writeValueAsString(accountRequest);
        mockMvc.perform(
                post("/api/account/create")
                        .contentType("application/json")
                        .content(badJson))
                .andExpect(status().is(400));
    }

    @Test
    void getAccountsForCurrentUser() throws  AcmeConnectionException {
        DirectoryDataSettings settings = new DirectoryDataSettings();
        settings.setName("acme");

        AcmeServerConnectionInfo info = new AcmeServerConnectionInfo();
        info.setName("acme");
        info.setUrl("https://test.com");
        AcmeServerConnection connection = new AcmeServerConnection(info);
        AcmeServerService acmeServerService = new AcmeServerServiceImpl(connection);

        when(acmeServerManagementService.getAllDirectorySettings(any())).thenReturn(Collections.singletonList(settings));
        when(acmeServerManagementService.getAcmeServerServiceByName(any())).thenReturn(Optional.of(acmeServerService));

        Authentication authentication = new TestingAuthenticationToken("test@test.com", "");
        UserDetails userDetails = new org.springframework.security.core.userdetails.User("test@test.com", "", Collections.emptyList());
        List<AccountInfo> accountsForCurrentUser = accountService.getAccountsForCurrentUser(authentication);
        assertEquals(1, accountsForCurrentUser.size());
    }

    @Test
    void updateAccount() throws Exception {
        Account account = accountRepository.findDistinctByProjectName("Test Project").get();
        AccountUpdateForm form = new AccountUpdateForm(account);

        List<PocFormEntry> pocFormEntries = new ArrayList<>();
        PocFormEntry entry = new PocFormEntry();
        entry.setEmail("test@test.com");
        pocFormEntries.add(entry);

        form.setPocEmails(pocFormEntries);

        Authentication authentication = new TestingAuthenticationToken("test@test.com", "");
        AccountInfo accountInfo = accountService.updateAccount(form, authentication);
        assertEquals(1, accountInfo.getPocs().size());

        PocFormEntry pocEntry = new PocFormEntry();
        pocEntry.setEmail("bademail");

        form.setPocEmails(Collections.singletonList(pocEntry));

        String badJson = new ObjectMapper().writeValueAsString(form);
        mockMvc.perform(
                post("/api/account/update")
                        .contentType("application/json")
                        .content(badJson))
                .andExpect(status().is(400));
    }

    @Test
    void getAll() {
        UserDetails userDetails = new org.springframework.security.core.userdetails.User("test@test.com", "", Collections.emptyList());
        List<AccountInfo> all = accountService.getAll(userDetails);
        assertEquals(1, all.size());
    }

    @Test
    @WithMockUser(value = "test@test.com", authorities = {"super_admin"})
    void findById() throws RAObjectNotFoundException {
        Account account = accountRepository.findDistinctByProjectName("Test Project").get();
        Authentication authentication = new TestingAuthenticationToken("test@test.com", "");
        AccountInfo byId = accountService.findById(account.getId(), authentication);
        assertNotNull(byId);
    }

    @Test
    @WithMockUser(value = "test@test.com", authorities = {"super_admin"})
    void findInfoById() throws RAObjectNotFoundException {
        Account account = accountRepository.findDistinctByProjectName("Test Project").get();
        Authentication authentication = new TestingAuthenticationToken("test@test.com", "");
        AccountInfo infoById = accountService.findInfoById(account.getId(), authentication);
        assertNotNull(infoById);
    }

    @Test
    @Transactional
    void getAccountPocs() throws RAObjectNotFoundException {
        Account account = accountRepository.findDistinctByProjectName("Test Project").get();

        PocEntry pocEntry = new PocEntry();
        pocEntry.setEmail("test2@test.com");
        pocEntry.setAccount(account);
        pocEntry = pocEntryRepository.save(pocEntry);

        account.getPocs().add(pocEntry);
        account = accountRepository.save(account);

        //todo this should use the http interface
        //List<UserInfo> kidtest1 = accountService.getAccountPocs(account.getId());
        //assertEquals(2, kidtest1.size());
    }

    @Test
    void delete() {
        Account account = accountRepository.findDistinctByProjectName("Test Project").get();
        assertNotNull(account);

        accountService.delete(account.getId());

        Optional<Account> optionalAccount = accountService.getByKeyIdentifier("kidtest1");
        assertFalse(optionalAccount.isPresent());
    }
}