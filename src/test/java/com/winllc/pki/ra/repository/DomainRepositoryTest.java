package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.Domain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class DomainRepositoryTest {

    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    @Transactional
    void before(){
        Account account = new Account();
        account.setProjectName("Test Name");
        account.setKeyIdentifier("testkid1");
        account = accountRepository.save(account);

        Domain domain1 = new Domain();
        domain1.setBase("test.winllc-dev.com");
        domain1.getCanIssueAccounts().add(account);
        domain1 = domainRepository.save(domain1);

        Domain domain2 = new Domain();
        domain2.setBase("test2.winllc-dev.com");
        domain2.getCanIssueAccounts().add(account);
        domain2 = domainRepository.save(domain2);

        account.getCanIssueDomains().add(domain1);
        account.getCanIssueDomains().add(domain2);
        accountRepository.save(account);
    }

    @AfterEach
    @Transactional
    void after(){
        domainRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void findAllByBaseContains() {
        List<Domain> allByBaseContains = domainRepository.findAllByBaseContains("winllc-dev.com");
        assertEquals(2, allByBaseContains.size());
    }

    @Test
    void findAllByCanIssueAccountsContains() {
        Optional<Account> account = accountRepository.findByKeyIdentifierEquals("testkid1");
        List<Domain> allByCanIssueAccountsContains = domainRepository.findAllByCanIssueAccountsContains(account.get());
        assertEquals(2, allByCanIssueAccountsContains.size());
    }

    @Test
    void findAllByIdIn() {
        Domain domain = domainRepository.findAll().get(0);
        List<Domain> allByIdIn = domainRepository.findAllByIdIn(Collections.singleton(domain.getId()));
        assertEquals(1, allByIdIn.size());
    }
}