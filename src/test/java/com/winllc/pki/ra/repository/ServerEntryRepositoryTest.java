package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.ServerEntry;
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

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class ServerEntryRepositoryTest {

    @Autowired
    private ServerEntryRepository serverEntryRepository;
    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    @Transactional
    void before(){
        Account account = new Account();
        account.setKeyIdentifier("testkid1");
        account.setProjectName("Test Project");
        account = accountRepository.save(account);

        ServerEntry serverEntry = new ServerEntry();
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
    void findDistinctByFqdnEquals() {
        Optional<ServerEntry> optionalServer = serverEntryRepository.findDistinctByFqdnEquals("test.winllc-dev.com");
        assertTrue(optionalServer.isPresent());
    }

    @Test
    void findAllByAccount() {
        Account account = accountRepository.findByKeyIdentifierEquals("testkid1").get();
        List<ServerEntry> allByAccount = serverEntryRepository.findAllByAccount(account);
        assertEquals(1, allByAccount.size());
    }

    @Test
    void findAllByAccountId() {
        Account account = accountRepository.findByKeyIdentifierEquals("testkid1").get();
        List<ServerEntry> allByAccount = serverEntryRepository.findAllByAccountId(account.getId());
        assertEquals(1, allByAccount.size());
    }

    @Test
    void findDistinctByFqdnEqualsAndAccount() {
        Account account = accountRepository.findByKeyIdentifierEquals("testkid1").get();
        Optional<ServerEntry> distinctByFqdnEqualsAndAccount = serverEntryRepository.findDistinctByFqdnEqualsAndAccount("test.winllc-dev.com", account);
        assertTrue(distinctByFqdnEqualsAndAccount.isPresent());
    }
}