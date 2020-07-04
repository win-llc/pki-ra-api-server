package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.PocEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class PocEntryRepositoryTest {

    @Autowired
    private PocEntryRepository pocEntryRepository;
    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    @Transactional
    void before(){
        Account account = Account.buildNew();
        account.setProjectName("Test Name");
        account.setKeyIdentifier("testkid1");
        account = accountRepository.save(account);

        PocEntry pocEntry = new PocEntry();
        pocEntry.setAccount(account);
        pocEntry.setEmail("test@test.com");
        pocEntry = pocEntryRepository.save(pocEntry);

        account.getPocs().add(pocEntry);
        accountRepository.save(account);
    }

    @AfterEach
    @Transactional
    void after(){
        pocEntryRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void findAllByAccount() {
        Account account = accountRepository.findByKeyIdentifierEquals("testkid1").get();
        List<PocEntry> allByAccount = pocEntryRepository.findAllByAccount(account);
        assertEquals(1, allByAccount.size());
    }

    @Test
    void findAllByEmailEquals() {
        List<PocEntry> byEmail = pocEntryRepository.findAllByEmailEquals("test@test.com");
        assertEquals(1, byEmail.size());
    }

    @Test
    void deleteAllByEmailInAndAccountEquals() {
        Account account = accountRepository.findByKeyIdentifierEquals("testkid1").get();
        assertEquals(1, pocEntryRepository.findAll().size());

        pocEntryRepository.deleteAllByEmailInAndAccountEquals(Collections.singletonList("test@test.com"), account);
        assertEquals(0, pocEntryRepository.findAll().size());
    }

    @Test
    void deleteByEmailEqualsAndAccount() {
        Account account = accountRepository.findByKeyIdentifierEquals("testkid1").get();
        assertEquals(1, pocEntryRepository.findAll().size());

        pocEntryRepository.deleteByEmailEqualsAndAccount("test@test.com", account);
        assertEquals(0, pocEntryRepository.findAll().size());
    }
}