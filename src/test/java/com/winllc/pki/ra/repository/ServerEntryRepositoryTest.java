package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.BaseTest;
import com.winllc.pki.ra.beans.form.AccountRequestForm;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.ServerEntry;
import com.winllc.pki.ra.service.AccountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ServerEntryRepositoryTest extends BaseTest {

    @Autowired
    private ServerEntryRepository serverEntryRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountService accountService;

    @BeforeEach
    @Transactional
    void before() throws Exception {
        AccountRequestForm form = new AccountRequestForm();
        form.setProjectName("Test Project");
        Long id = accountService.createNewAccount(form);
        Account account = accountRepository.findById(id).get();

        ServerEntry serverEntry = ServerEntry.buildNew();
        serverEntry.setAccount(account);
        serverEntry.setFqdn("test.winllc-dev.com");
        serverEntry = serverEntryRepository.save(serverEntry);

        account.getServerEntries().add(serverEntry);
        accountRepository.save(account);
    }

    @AfterEach
    @Transactional
    void after(){
        accountRepository.deleteAll();
        serverEntryRepository.deleteAll();
    }

    @Test
    @Transactional
    void findDistinctByFqdnEquals() {
        Optional<ServerEntry> optionalServer = serverEntryRepository.findDistinctByFqdnEquals("test.winllc-dev.com");
        assertTrue(optionalServer.isPresent());
    }

    @Test
    @Transactional
    void findAllByAccount() {
        Account account = accountRepository.findDistinctByProjectName("Test Project").get();
        List<ServerEntry> allByAccount = serverEntryRepository.findAllByAccount(account);
        assertEquals(1, allByAccount.size());
    }

    @Test
    @Transactional
    void findAllByAccountId() {
        Account account = accountRepository.findDistinctByProjectName("Test Project").get();
        List<ServerEntry> allByAccount = serverEntryRepository.findAllByAccountId(account.getId());
        assertEquals(1, allByAccount.size());
    }

    @Test
    @Transactional
    void findDistinctByFqdnEqualsAndAccount() {
        Account account = accountRepository.findDistinctByProjectName("Test Project").get();
        Optional<ServerEntry> distinctByFqdnEqualsAndAccount = serverEntryRepository.findDistinctByFqdnEqualsAndAccountEquals("test.winllc-dev.com", account);
        assertTrue(distinctByFqdnEqualsAndAccount.isPresent());
    }
}