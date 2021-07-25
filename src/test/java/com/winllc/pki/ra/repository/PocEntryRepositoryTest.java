package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.BaseTest;
import com.winllc.pki.ra.beans.form.AccountRequestForm;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.PocEntry;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.service.AccountService;
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

class PocEntryRepositoryTest extends BaseTest {

    @Autowired
    private PocEntryRepository pocEntryRepository;
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
    @Transactional
    void findAllByAccount() throws RAObjectNotFoundException {
        Account account = accountRepository.findDistinctByProjectName("Test Project").get();
        List<PocEntry> allByAccount = pocEntryRepository.findAllByAccount(account);
        assertEquals(1, allByAccount.size());
    }

    @Test
    @Transactional
    void findAllByEmailEquals() {
        List<PocEntry> byEmail = pocEntryRepository.findAllByEmailEquals("test@test.com");
        assertEquals(1, byEmail.size());
    }

    @Test
    @Transactional
    void deleteAllByEmailInAndAccountEquals() throws RAObjectNotFoundException {
        Account account = accountRepository.findDistinctByProjectName("Test Project").get();
        assertEquals(1, pocEntryRepository.findAll().size());

        pocEntryRepository.deleteAllByEmailInAndAccountEquals(Collections.singletonList("test@test.com"), account);
        assertEquals(0, pocEntryRepository.findAll().size());
    }

    @Test
    @Transactional
    void deleteByEmailEqualsAndAccount() {
        Account account = accountRepository.findDistinctByProjectName("Test Project").get();
        assertEquals(1, pocEntryRepository.findAll().size());

        pocEntryRepository.deleteByEmailEqualsAndAccount("test@test.com", account);
        assertEquals(0, pocEntryRepository.findAll().size());
    }
}