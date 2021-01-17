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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PocEntryRepository pocEntryRepository;

    @BeforeEach
    @Transactional
    void before(){
        Account account = Account.buildNew("Test Project");
        account.setKeyIdentifier("kidtest1");
        account = accountRepository.save(account);

        PocEntry pocEntry = new PocEntry();
        pocEntry.setAccount(account);
        pocEntry.setEmail("poc@test.com");

        pocEntryRepository.save(pocEntry);

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
    void findByKeyIdentifierEquals() {
        Optional<Account> optionalAccount = accountRepository.findDistinctByProjectName("Test Project");
        assertTrue(optionalAccount.isPresent());
    }

}