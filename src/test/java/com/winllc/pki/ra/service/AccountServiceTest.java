package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.PocFormEntry;
import com.winllc.pki.ra.beans.form.AccountUpdateForm;
import com.winllc.pki.ra.beans.info.AccountInfo;
import com.winllc.pki.ra.beans.info.UserInfo;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.AccountRequest;
import com.winllc.pki.ra.domain.PocEntry;
import com.winllc.pki.ra.domain.User;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.PocEntryRepository;
import com.winllc.pki.ra.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class AccountServiceTest {

    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PocEntryRepository pocEntryRepository;

    @BeforeEach
    @Transactional
    void before(){
        Account account = new Account();
        account.setKeyIdentifier("kidtest1");
        account.setMacKey("testmac1");
        account.setProjectName("Test Project");
        account = accountRepository.save(account);

        User user = new User();
        user.setIdentifier(UUID.randomUUID());
        user.setUsername("test@test.com");
        user.getAccounts().add(account);

        user = userRepository.save(user);

        account.getAccountUsers().add(user);
        accountRepository.save(account);
    }

    @AfterEach
    @Transactional
    void after(){
        userRepository.deleteAll();
        pocEntryRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void buildNew() {
        Account account = accountService.buildNew();

        Account byId = accountRepository.findById(account.getId()).get();
        assertTrue(byId.getMacKey().length() > 6);
    }

    @Test
    void createNewAccount() {
        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setProjectName("Test Project 3");

        Long id = accountService.createNewAccount(accountRequest);
        assertTrue(id > 0);
    }

    @Test
    void getAccountsForCurrentUser() throws RAObjectNotFoundException {
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
        entry.setEmail("test2@test.com");
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