package com.winllc.acme.common.repository;

import com.winllc.pki.ra.BaseTest;
import com.winllc.acme.common.domain.Account;
import com.winllc.acme.common.domain.PocEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AccountRepositoryTest extends BaseTest {

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