package com.winllc.pki.ra.service;

import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.model.data.DirectoryData;
import com.winllc.pki.ra.acme.AcmeServerConnection;
import com.winllc.pki.ra.acme.AcmeServerService;
import com.winllc.pki.ra.acme.AcmeServerServiceImpl;
import com.winllc.pki.ra.beans.PocFormEntry;
import com.winllc.pki.ra.beans.form.AccountUpdateForm;
import com.winllc.pki.ra.beans.info.AccountInfo;
import com.winllc.pki.ra.beans.info.UserInfo;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.*;
import com.winllc.pki.ra.exception.AcmeConnectionException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.PocEntryRepository;
import com.winllc.pki.ra.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class AccountServiceTest {

    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PocEntryRepository pocEntryRepository;
    @MockBean
    private AcmeServerManagementService acmeServerManagementService;

    @BeforeEach
    @Transactional
    void before(){
        Account account = Account.buildNew("Test Project");
        account.setKeyIdentifier("kidtest1");
        account.setMacKey("testmac1");
        account = accountRepository.save(account);

        PocEntry pocEntry = new PocEntry();
        pocEntry.setAccount(account);
        pocEntry.setEmail("test@test.com");
        pocEntry.setEnabled(true);
        pocEntryRepository.save(pocEntry);
    }

    @AfterEach
    @Transactional
    void after(){
        pocEntryRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void createNewAccount() {
        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setProjectName("Test Project 3");

        Long id = accountService.createNewAccount(accountRequest);
        assertTrue(id > 0);
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

        UserDetails userDetails = new org.springframework.security.core.userdetails.User("test@test.com", "", Collections.emptyList());
        List<AccountInfo> accountsForCurrentUser = accountService.getAccountsForCurrentUser(userDetails);
        assertEquals(1, accountsForCurrentUser.size());
    }

    @Test
    void updateAccount() throws Exception {
        Account account = accountRepository.findByKeyIdentifierEquals("kidtest1").get();
        AccountUpdateForm form = new AccountUpdateForm(account);

        List<PocFormEntry> pocFormEntries = new ArrayList<>();
        PocFormEntry entry = new PocFormEntry();
        entry.setEmail("test@test.com");
        pocFormEntries.add(entry);

        form.setPocEmails(pocFormEntries);

        AccountInfo accountInfo = accountService.updateAccount(form);
        assertEquals(1, accountInfo.getPocs().size());
    }

    @Test
    void getAll() {
        UserDetails userDetails = new org.springframework.security.core.userdetails.User("test@test.com", "", Collections.emptyList());
        List<Account> all = accountService.getAll(userDetails);
        assertEquals(1, all.size());
    }

    @Test
    void findByKeyIdentifier() throws RAObjectNotFoundException {
        AccountInfo kidtest1 = accountService.findByKeyIdentifier("kidtest1");
        assertNotNull(kidtest1);
    }

    @Test
    void findById() throws RAObjectNotFoundException {
        Account account = accountRepository.findByKeyIdentifierEquals("kidtest1").get();
        AccountInfo byId = accountService.findById(account.getId());
        assertNotNull(byId);
    }

    @Test
    void findInfoById() throws RAObjectNotFoundException {
        Account account = accountRepository.findByKeyIdentifierEquals("kidtest1").get();
        AccountInfo infoById = accountService.findInfoById(account.getId());
        assertNotNull(infoById);
    }

    @Test
    @Transactional
    void getAccountPocs() throws RAObjectNotFoundException {
        Account account = accountRepository.findByKeyIdentifierEquals("kidtest1").get();

        PocEntry pocEntry = new PocEntry();
        pocEntry.setEmail("test2@test.com");
        pocEntry.setAccount(account);
        pocEntry = pocEntryRepository.save(pocEntry);

        account.getPocs().add(pocEntry);
        accountRepository.save(account);

        List<UserInfo> kidtest1 = accountService.getAccountPocs("kidtest1");
        assertEquals(2, kidtest1.size());
    }

    @Test
    void delete() {
        Account account = accountRepository.findByKeyIdentifierEquals("kidtest1").get();
        assertNotNull(account);

        accountService.delete(account.getId());

        Optional<Account> optionalAccount = accountRepository.findByKeyIdentifierEquals("kidtest1");
        assertFalse(optionalAccount.isPresent());
    }
}